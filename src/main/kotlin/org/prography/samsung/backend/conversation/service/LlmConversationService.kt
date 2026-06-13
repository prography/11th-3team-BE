package org.prography.samsung.backend.conversation.service

import org.prography.samsung.backend.common.domain.AiEmotion
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
            )
            if (semanticError != null) {
                lastError = "의미 규칙 위반: $semanticError. speak는 1~2문장 140자 이하로 매우 짧게. covered 이번 턴 새로 이해한 것만."
                log.warn(
                    "Teach LLM attempt ${attempt + 1} semantic violation: $semanticError | " +
                        "speak='${parsed.speak.take(80)}'",
                )
                if (attempt == maxAttempts - 1) {
                    val safe = applySafetyRules(parsed, conceptOrder, accumulatedCovered, repeatedFocusCount)
                    val finalFocus = if (safe.focusConcept.isBlank()) {
                        aiResponseValidator.resolveFocusConcept(conceptOrder, safe.missing, explicit = null)
                    } else {
                        safe.focusConcept
                    }
                    return safe.copy(focusConcept = finalFocus)
                }
                return@repeat
            }

            if (attempt > 0) {
                log.info("Teach LLM recovered after ${attempt + 1} attempts")
            }
            val safe = applySafetyRules(parsed, conceptOrder, accumulatedCovered, repeatedFocusCount)
            val finalFocus = if (safe.focusConcept.isBlank()) {
                aiResponseValidator.resolveFocusConcept(conceptOrder, safe.missing, explicit = null)
            } else {
                safe.focusConcept
            }
            return safe.copy(focusConcept = finalFocus)
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
        appendLine("3. covered: **이번 선생님 발화로 새로 이해한 id만**. 이미 accumulated에 있는 id 절대 넣지 말 것.")
        appendLine("4. missing: 전체 중 covered를 뺀 나머지 (conceptOrder 순서 유지).")
        appendLine(
            "5. focus_concept: **항상 유효한 문자열**. missing이 있으면 missing의 첫 번째. missing이 비어도 (session_done=true) 마지막 concept나 'c1'을 넣을 것. **절대 null 금지**.",
        )
        appendLine("6. session_done: missing이 비어있을 때만 true.")
        appendLine("7. **JSON 객체 하나만 출력**. 설명, 마크다운, ```json, 추가 텍스트 절대 금지.")
        appendLine()

        // Few-shot examples (강력한 신호)
        appendLine("## 올바른 출력 예시 (이 형식과 논리를 정확히 따르세요)")
        appendLine(
            """
            [예시 1 - 첫 이해]
            선생님: 분수는 전체를 똑같이 나눈 거 중 일부를 나타내는 수예요.
            올바른 JSON:
            {"speak":"아하! 그럼 분수는 전체를 똑같이 나눈 것 중 일부군요?","emotion":"aha","covered":["c1"],"missing":["c2","c3","c4"],"misconceptions_detected":[],"correction_stage":0,"focus_concept":"c2","session_done":false}

            [예시 2 - 두 번째 이해]
            선생님: 분모는 전체를 똑같이 나눈 개수, 아래에 있는 숫자예요.
            올바른 JSON:
            {"speak":"음... 분모가 아래 숫자라는 건 알겠는데, 왜 더하면 안 돼요?","emotion":"confused","covered":["c2"],"missing":["c3","c4"],"misconceptions_detected":[],"correction_stage":0,"focus_concept":"c3","session_done":false}

            [예시 3 - 완료]
            선생님: 분수는 크기를 비교할 수 있어요. 분모가 같으면 분자가 큰 게 더 커요.
            올바른 JSON:
            {"speak":"아하! 이제 분수 크기도 알겠어요. 고마워요 선생님!","emotion":"happy","covered":["c3","c4"],"missing":[],"misconceptions_detected":[],"correction_stage":0,"focus_concept":"c1","session_done":true}
            """.trimIndent(),
        )
        appendLine()
        appendLine("## 최종 지시")
        appendLine("위 규칙과 예시를 모두 지켜서, **이번 선생님 발화에 대한 응답 JSON 객체 하나만** 출력하세요.")
    }

    companion object {
        private val PROMPT_SUPPLEMENT =
            """

            ## 핵심 품질 규칙 (절대 준수)
            - speak는 **1문장 선호, 최대 2문장, 140자 이하**로 매우 짧게. 초등 4학년 존댓말 ("요", "죠", "네요"). 장황 절대 금지.
            - covered는 "이번 턴에 새로 이해한" 것만. 이전 턴 covered나 누적 covered는 절대 반복 금지.
            - missing은 covered를 제외한 나머지 전체를 concept 순서대로.
            - 모든 concept 이해 시 (missing == []) → emotion="happy", session_done=true, speak은 감사 마무리 (1~2문장).
            - covered / missing / focus_concept / misconceptions_detected 는 반드시 unit_json에 정의된 id만 사용.
            - emotion은 5개 값 중 정확히 일치하는 소문자 문자열.
            - **session_done=true인 경우에도 focus_concept은 반드시 문자열** (마지막 id 또는 c1). null 금지.
            - JSON 외의 어떤 텍스트도 출력하지 말 것 (Koog structured output이라도 최종은 순수 JSON).
            """.trimIndent()
    }

    private fun applySafetyRules(
        response: AiTurnResponse,
        conceptOrder: List<String>,
        accumulatedCovered: List<String>,
        repeatedFocusCount: Int,
    ): AiTurnResponse {
        val mergedCovered = aiResponseValidator.mergeCovered(accumulatedCovered, response.covered)
        val missing = aiResponseValidator.resolveMissing(conceptOrder, mergedCovered)
        val sessionDone = missing.isEmpty()
        val correctionStage =
            if (repeatedFocusCount >= 3) {
                4
            } else {
                response.correctionStage
            }

        val speak =
            if (correctionStage == 4 && response.correctionStage < 4) {
                "음... 아직 헷갈리지만 일단 넘어갈게요. 나중에 다시 알려주세요!"
            } else {
                response.speak
            }

        return response.copy(
            speak = speak,
            covered = mergedCovered,
            missing = missing,
            correctionStage = correctionStage,
            focusConcept = aiResponseValidator.resolveFocusConcept(
                conceptOrder = conceptOrder,
                missing = missing,
                explicit = if (missing.isEmpty()) response.focusConcept else null,
            ),
            sessionDone = sessionDone,
            emotion = if (sessionDone) AiEmotion.HAPPY else response.emotion,
        )
    }
}
