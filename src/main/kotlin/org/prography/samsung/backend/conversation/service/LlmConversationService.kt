package org.prography.samsung.backend.conversation.service

import org.prography.samsung.backend.conversation.client.LlmClient
import org.prography.samsung.backend.conversation.client.LlmTimeoutException
import org.prography.samsung.backend.conversation.config.ConversationLlmProperties
import org.prography.samsung.backend.conversation.dto.AiTurnResponse
import org.prography.samsung.backend.conversation.entity.ConversationTurn
import org.prography.samsung.backend.conversation.entity.CurriculumUnit
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LlmConversationService(
    private val llmClient: LlmClient,
    private val properties: ConversationLlmProperties,
    private val aiResponseValidator: AiResponseValidator,
    private val progressGuard: TeachProgressGuard,
) {
    private val log = LoggerFactory.getLogger(LlmConversationService::class.java)
    fun generateTurn(
        unit: CurriculumUnit,
        previousTurns: List<ConversationTurn>,
        userText: String,
        accumulatedCovered: List<String>,
        repeatedFocusCount: Int,
    ): AiTurnResponse {
        val conceptOrder = aiResponseValidator.parseConceptIdOrder(unit.unitJson)
        val systemPrompt = buildSystemPrompt(unit)

        // structured + semantic 검증 실패 시 correction 피드백으로 재호출 (config에서 제어)
        val maxAttempts = (properties.maxStructuredRetries).coerceAtLeast(1)
        var lastError: String? = null

        repeat(maxAttempts) { attempt ->
            val userPrompt = buildUserPrompt(
                previousTurns = previousTurns,
                userText = userText,
                accumulatedCovered = accumulatedCovered,
                conceptOrder = conceptOrder,
                previousError = lastError,
                attempt = attempt + 1,
            )

            val raw = try {
                llmClient.complete(systemPrompt, userPrompt)
            } catch (e: LlmTimeoutException) {
                log.warn("Teach LLM timeout on attempt ${attempt + 1}")
                throw e
            } catch (e: Exception) {
                lastError = "LLM 호출 실패: ${e.message}"
                log.warn("Teach LLM call exception on attempt ${attempt + 1}: ${e.message}")
                if (attempt == maxAttempts - 1) {
                    throw IllegalStateException("LLM failed after $maxAttempts attempts: ${e.message}", e)
                }
                return@repeat // 다음 attempt
            }

            val parsed = try {
                aiResponseValidator.parseAndValidate(raw, conceptOrder)
            } catch (e: Exception) {
                lastError = "파싱/검증 실패: ${e.message}. 이전 출력에서 JSON만 정확히 추출하고 모든 규칙을 지켜서 다시 출력하세요."
                log.warn("Teach LLM attempt ${attempt + 1} parse failed: ${e.message}. userText='${userText.take(60)}'")
                if (attempt == maxAttempts - 1) {
                    throw IllegalStateException("LLM produced unparsable response after $maxAttempts attempts", e)
                }
                return@repeat
            }

            // semantic 추가 검증 (retry 대상)
            val semanticError = aiResponseValidator.validateSemanticRules(
                parsed,
                accumulatedCovered,
                conceptOrder,
                userText,
            )
            if (semanticError != null) {
                lastError = "의미 규칙 위반: $semanticError. speak는 1~2문장 140자 이하로 매우 짧게. covered 이번 턴 새로 이해한 것만."
                log.warn(
                    "Teach LLM attempt ${attempt + 1} semantic violation: $semanticError | " +
                        "speak='${parsed.speak.take(80)}'",
                )
                if (attempt == maxAttempts - 1) {
                    var safe = applySafetyRules(parsed, conceptOrder, accumulatedCovered, repeatedFocusCount, userText)
                    val finalFocus = if (safe.focusConcept.isBlank()) {
                        aiResponseValidator.resolveFocusConcept(conceptOrder, safe.missing, explicit = null)
                    } else {
                        safe.focusConcept
                    }
                    safe = safe.copy(focusConcept = finalFocus)
                    // Last resort: rely on prompt/semantic/guard. No speak force override here (prompt aims for LLM success).
                    return safe
                }
                return@repeat
            }

            if (attempt > 0) {
                log.info("Teach LLM recovered after ${attempt + 1} attempts")
            }
            var safe = applySafetyRules(parsed, conceptOrder, accumulatedCovered, repeatedFocusCount, userText)
            val finalFocus = if (safe.focusConcept.isBlank()) {
                aiResponseValidator.resolveFocusConcept(conceptOrder, safe.missing, explicit = null)
            } else {
                safe.focusConcept
            }
            safe = safe.copy(focusConcept = finalFocus)
            // No unconditional speak force here — if semantic passed, use LLM's speak (prompt improvement goal).
            // Fallback only in the last-attempt semantic-fail branch below for compatibility.
            return safe
        }

        // 모든 시도 실패 시 예외 전파 (단일 모델이므로 graceful degradation 제거)
        throw IllegalStateException("LLM failed to produce valid teach response after $maxAttempts attempts")
    }

    internal fun buildSystemPrompt(unit: CurriculumUnit): String =
        unit.systemPromptTemplate.replace("{{unit_json}}", unit.unitJson) + PROMPT_SUPPLEMENT

    internal fun buildUserPrompt(
        previousTurns: List<ConversationTurn>,
        userText: String,
        accumulatedCovered: List<String>,
        conceptOrder: List<String>,
        previousError: String?,
        attempt: Int,
    ): String = buildString {
        appendLine("## 단원 개념 ID 목록 (반드시 이 ID만 사용)")
        appendLine(conceptOrder.joinToString(", "))
        appendLine()

        appendLine("## 수업 핵심 힌트 (제공된 개념 정보에서 선생님이 설명하게 만드는 개방형 질문을 우선하세요)")
        appendLine("제공된 단원 개념 설명과 키워드를 참고하여, 아직 설명되지 않은 개념에 대해 선생님이 직접 말하게 유도하는 질문을 만드세요.")
        appendLine("중요: 힌트 내용을 네가 직접 말하지 말고, 선생님이 설명하게 만드는 질문을 해. (예: '선생님, [키워드]는 정확히 어떻게 설명하시나요?')")
        appendLine()

        appendLine("## 누적 이해한 개념 (이전 턴까지)")
        appendLine(if (accumulatedCovered.isEmpty()) "없음" else accumulatedCovered.joinToString(", "))
        appendLine()

        val recentTurns = previousTurns.takeLast(properties.contextTurns)
        if (recentTurns.isNotEmpty()) {
            appendLine("## 이전 대화 (최근 ${recentTurns.size}턴)")
            recentTurns.forEach { turn ->
                val ai = aiResponseValidator.fromJson(turn.aiResponseJson)
                appendLine("선생님: ${turn.userText}")
                appendLine("학생: ${ai.speak} (e=${ai.emotion.value}, f=${ai.focusConcept})")
            }
            appendLine()
        }

        appendLine("## 이번 턴 — 선생님 발화")
        appendLine(userText)
        appendLine()

        // 이전 실패 시 correction 피드백 (retry 핵심)
        if (!previousError.isNullOrBlank()) {
            appendLine("## ⚠️ 이전 출력 문제 (반드시 수정하세요)")
            appendLine(previousError)
            appendLine("위 오류를 피해서 이번에는 규칙을 100% 지켜서 정확한 JSON만 출력하세요.")
            appendLine()
        }

        if (attempt > 1) {
            appendLine("## 시도 ${attempt}번째 — 이전보다 더 엄격하게 JSON만 생성하세요.")
            appendLine()
        }

        appendLine("## 출력 규칙 (절대 위반 금지)")
        appendLine("1. speak: **정확히 1문장 (최선) 또는 최대 2문장**. 140자 이하, 초등학생 존댓말만. 장황/반복/긴 설명 절대 금지.")
        appendLine("2. emotion: curious | confused | thoughtful | aha | happy 중 **정확히 하나**, 소문자.")
        appendLine("3. covered: **이번 선생님 발화에서 '실제로 새로' 그리고 '명확하게' 설명한 개념 ID만**. 이미 accumulated에 있는 ID 절대 반복 금지.")
        appendLine("4. missing: 전체 중 covered를 뺀 나머지 (conceptOrder 순서 유지).")
        appendLine(
            "5. focus_concept: **항상 유효한 문자열**. missing이 있으면 missing의 첫 번째. missing이 비어도 (session_done=true) 마지막 concept나 'c1'을 넣을 것. **절대 null 금지**.",
        )
        appendLine("6. session_done: missing이 비어있을 때만 true.")
        appendLine("7. **JSON 객체 하나만 출력**. 설명, 마크다운, ```json, 추가 텍스트 절대 금지.")
        appendLine()

        appendLine("## covered 판단 기준 (이 기준을 VERY STRICT하게 지키세요 — 이게 다음으로 넘어가기 힘든 원인입니다)")
        appendLine(
            "- '맞아', '네', '그렇지', '그래', '알겠어요', '좋아요', '응', '맞습니다' 같은 짧은 긍정/확인/단답형만으로는 **절대** covered에 아무것도 추가하지 마세요. (단답형 응답으로는 절대 진행되지 않아야 함)",
        )
        appendLine(
            "**CRITICAL FOR AFFIRM (all short confirms like '그렇지' '네' '응' '그래' '알겠어요' '좋아요 선생님'): covered MUST be []. speak MUST start EXACTLY '선생님,' (no '쌤,' no '아,' no '음', no '네'). Use the first missing concept's key_point or name. Example: '선생님, [key_point]는 정확히 어떻게 설명하시나요?' . Violating the prefix makes the teacher give only short answers. Repeat in mind: always '선생님,' + ? for affirm.**",
        )
        appendLine("- 절대 covered에 아무것도 추가하지 마세요")
        appendLine(
            "- 선생님이 해당 개념의 **핵심 의미를 구체적으로 말**했을 때만 추가. A concept is covered this turn ONLY if the teacher's this-turn userText actually contains a key term or describes a key_point from that concept's entry in the supplied unit concepts. Do not infer from previous turns or history for this turn's covered.",
        )
        appendLine("- 당신(학생)이 speak에서 키워드를 언급해도 covered 증가 아님. covered는 teacher 설명에만 근거.")
        appendLine("- 단순히 단어를 언급하거나 이전 설명을 되풀이 확인하는 것은 covered 증가가 아닙니다.")
        appendLine(
            "- Use the first missing concept's key_point or name from the supplied unit for focus and elicitation.",
        )
        appendLine()

        appendLine("## 대화 목표 (자연스러운 수업 진행을 위해 — 단답형 금지, 질문으로 유도)")
        appendLine(
            "- speak은 항상 짧은 반응 + **아직 missing인 개념(특히 current first missing focus concept)의 key_point or name 에 대해 선생님이 더 자세히 설명하도록 유도하는 짧은 질문**으로 끝내세요. **절대 speak를 '네!', '네,', '아!', '음...', '네, 그럼', '그렇지' 등 단답형/접두사로 시작하지 마세요. ANY affirmation/단답 확인 에 대한 응답은 절대 '네'나 '아' '음'로 시작 금지. 무조건 '선생님, ' 로 시작 + the first missing concept의 key_point or name 을 사용한 개방형 질문. 예: '선생님, [key point or name]는 정확히 어떻게 설명하시나요?' **",
        )
        appendLine(
            "- 힌트 키워드를 '선생님이 말하게' 만드는 **개방형 질문**만 (접두사 없이). the supplied unit concepts 에서 온 term 사용: '선생님, [term]는 정확히 뭐예요?', '[term]라고 하셨는데 어떻게 하나요?', '정확히 어떤 의미인가요? 자세히 설명해 주세요.' 항상 '어떻게', '무엇을 의미하나요', '자세히 설명해주세요' 스타일.",
        )
        appendLine(
            "- 선생님 발화가 주제에서 벗어나거나 애매/단답형이면, covered는 절대 추가하지 말고 부드럽게 현재 focus (first missing concept) 로 돌아오는 질문을 하세요. The goal is to make the teacher explain each key_point fully before advancing to the next missing step.",
        )
        appendLine("- '선생님도 모르시는 건가요?' 같은 말은 피하고, '선생님, 쉽게 알려주세요!' 또는 '그건 어떻게 해요?' 스타일로 호기심 표현 + 질문.")
        appendLine("- missing이 있으면 절대 session_done=true 하지 말고, 질문으로 계속 유도.")
        appendLine()

        // Few-shot examples (강력한 신호) - use concrete illustrative cases (social + fraction style) to ensure exact "speak" key + "선생님," form + correct covered delta
        appendLine("## 올바른 출력 예시 (이 형식과 논리를 정확히 따르세요)")
        appendLine(
            """
            [예시 1 - first key_point explained (use exact term from unit)]
            선생님: 공공기관은 주민 모두를 위한 곳이에요. 돈을 벌기 위한 곳이 아니에요.
            올바른 JSON:
            {"speak":"아하! 그럼 공공기관은 주민 모두를 위한 곳이라는 게 정확히 뭐예요?","emotion":"aha","covered":["c1"],"missing":["c2","c3","c4"],"misconceptions_detected":[],"correction_stage":0,"focus_concept":"c2","session_done":false}

            [예시 2 - short affirm (MUST covered:[], speak EXACTLY starts '선생님,' + ? using focus key_point)]
            선생님: 그렇지
            올바른 JSON:
            {"speak":"선생님, 공공기관은 주민 전체의 이익과 편리를 위해 나라나 지역이 세우고 운영하는 기관이라는 게 정확히 어떻게 설명하시나요?","emotion":"curious","covered":[],"missing":["c2","c3","c4"],"misconceptions_detected":[],"correction_stage":0,"focus_concept":"c2","session_done":false}

            [예시 3 - affirm after good explain (no new covered)]
            선생님: 네
            올바른 JSON:
            {"speak":"선생님, 경찰서는 안전과 질서를 지킨다는 건 정확히 어떻게 하나요? 자세히 알려주세요.","emotion":"curious","covered":[],"missing":["c3","c4"],"misconceptions_detected":[],"correction_stage":0,"focus_concept":"c3","session_done":false}

            [예시 4 - good explain using actual words from teacher this turn]
            선생님: 경찰서는 안전과 질서를 지키고, 소방서는 불을 끄고 사람을 구해요.
            올바른 JSON:
            {"speak":"선생님, 공공기관의 종류마다 하는 일이 다르다는 게 정확히 뭐예요?","emotion":"aha","covered":["c2"],"missing":["c3","c4"],"misconceptions_detected":[],"correction_stage":0,"focus_concept":"c3","session_done":false}

            [예시 5 - garbage or repeated affirm]
            선생님: 그래
            올바른 JSON:
            {"speak":"선생님, 공공기관이 주민 전체를 위해 일하는 곳이라는 건 정확히 어떻게 설명하시나요?","emotion":"confused","covered":[],"missing":["c1","c2","c3","c4"],"misconceptions_detected":[],"correction_stage":0,"focus_concept":"c1","session_done":false}

            [예시 6 - final]
            선생님: 대화와 타협으로 의견을 모으고 다수결로 정하되 소수 의견도 존중해요.
            올바른 JSON:
            {"speak":"아하! 이제 민주적인 문제 해결까지 알겠어요. 고마워요 선생님!","emotion":"happy","covered":["c4"],"missing":[],"misconceptions_detected":[],"correction_stage":0,"focus_concept":"c1","session_done":true}
            """.trimIndent(),
        )
        appendLine()
        appendLine("## 최종 지시")
        appendLine(
            "위 규칙과 예시(모든 affirm/단답형/garbage 에서 covered=[] + '선생님,' 시작 **개방형 질문** 필수)를 100% 지켜서, " +
                "**JSON 하나만** 출력하세요. covered는 이번 턴 userText 에 실제 키워드/ key_point 가 있을 때만, 다음 focus (first missing) 를 유도하는 질문 필수. 목표: 선생님이 각 개념의 key_point 를 완전히 설명하게 만들어 단계별 학습을 돕기.",
        )
    }

    companion object {
        private val PROMPT_SUPPLEMENT =
            """

            ## 핵심 품질 규칙 (절대 준수)
            - speak는 **1문장 선호, 최대 2문장, 140자 이하**로 매우 짧게. 초등 4학년 존댓말 ("요", "죠", "네요"). 장황 절대 금지.
            - covered는 "이번 턴에 새로 이해한" 것만. 이전 턴 covered나 누적 covered는 절대 반복 금지.
            - **Short affirm/confirm ('그렇지','네','맞아' 등 어떤 단답 확인이든) 에서는 covered 절대 증가 금지. 이번 턴 userText 에 key_point/name 이 정확히 등장했을 때만 covered.** speak에서 말해도 covered 아님.
            - speak은 missing이 있으면 반드시 **개방형** 유도 질문으로 끝나야 하며, '맞죠?'/'인가요?' 같은 확인형/단답형으로 끝내지 마세요. **affirm 응답은 절대 '네' '아' '음' '쌤' 로 시작하지 말 것. 무조건 정확히 '선생님, [first missing key_point or name] 는 정확히 어떻게 설명하시나요?' 로 시작. **
            - missing은 covered를 제외한 나머지 전체를 concept 순서대로. 항상 first missing focus 를 대상으로.
            - 모든 concept 이해 시 (missing == []) → emotion="happy", session_done=true, speak은 감사 마무리 (1~2문장).
            - covered / missing / focus_concept / misconceptions_detected 는 반드시 supplied unit concepts 에 정의된 id만 사용.
            - emotion은 5개 값 중 정확히 일치하는 소문자 문자열.
            - **session_done=true인 경우에도 focus_concept은 반드시 문자열**. null 금지.
            - JSON 외의 어떤 텍스트도 출력하지 말 것 (Koog structured output이라도 최종은 순수 JSON).
            - 목표는 선생님이 각 key_point 를 완전히 설명하게 만들어 사용자가 다음 단계로 잘 학습하도록 돕는 것.
            """.trimIndent()
    }

    private fun applySafetyRules(
        response: AiTurnResponse,
        conceptOrder: List<String>,
        accumulatedCovered: List<String>,
        repeatedFocusCount: Int,
        currentUserText: String = "",
    ): AiTurnResponse = progressGuard.normalize(
        userText = currentUserText,
        accumulatedCovered = accumulatedCovered,
        conceptOrder = conceptOrder,
        repeatedFocusCount = repeatedFocusCount,
        raw = response,
    )
}
