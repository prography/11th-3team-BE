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
                unit.unitJson,
            )
            if (semanticError != null) {
                lastError = "의미 규칙 위반: $semanticError. speak는 1~2문장 140자 이하로 매우 짧게. covered 이번 턴 새로 이해한 것만."
                log.warn(
                    "Teach LLM attempt ${attempt + 1} semantic violation: $semanticError | " +
                        "speak='${parsed.speak.take(80)}'",
                )
                if (attempt == maxAttempts - 1) {
                    var safe =
                        applySafetyRules(parsed, unit, conceptOrder, accumulatedCovered, repeatedFocusCount, userText)
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
            var safe = applySafetyRules(parsed, unit, conceptOrder, accumulatedCovered, repeatedFocusCount, userText)
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

    internal fun buildSystemPrompt(unit: CurriculumUnit): String {
        val lessonConcepts = aiResponseValidator.formatLessonConcepts(unit.unitJson)
        val replaced = unit.systemPromptTemplate
            .replace("{{lesson_concepts}}", lessonConcepts)
            .replace("{{unit_json}}", lessonConcepts)
            .replace("## 단원 정보", "## 수업 개념")
            .replace("단원 정보", "수업 개념")
        val misconceptions = aiResponseValidator.formatMisconceptions(unit.unitJson)
        val misconceptionBlock =
            if (misconceptions.isBlank()) {
                ""
            } else {
                "\n\n## 네가 자연스럽게 헷갈려하는 것 (오개념 — 정답을 아는 척하지 말고, 아직 안 배운 개념에서 이런 걸 궁금해하거나 그럴듯하게 잘못 추측해도 좋아)\n" +
                    misconceptions +
                    "\n위 표현을 그대로 베끼지 말고, 자연스러우면 학생답게 '저는 ~인 줄 알았는데 맞아요?'처럼 물어 선생님이 바로잡아 설명하게 유도하세요."
            }
        return replaced + PROMPT_SUPPLEMENT + misconceptionBlock
    }

    internal fun buildUserPrompt(
        previousTurns: List<ConversationTurn>,
        userText: String,
        accumulatedCovered: List<String>,
        conceptOrder: List<String>,
        previousError: String?,
        attempt: Int,
    ): String = buildString {
        appendLine("## 제공된 개념 ID 목록 (반드시 이 ID만 사용)")
        appendLine(conceptOrder.joinToString(", "))
        appendLine()

        appendLine("## STEP1 Classification key (generalized — lesson concepts are in the system prompt)")
        appendLine(
            "Use the lesson concepts in the system prompt (id + name + key_points). Compute first_missing = the lowest id in conceptOrder that is absent from accumulatedCovered. All name/key_point matching and elicitation must come from those lesson concepts.",
        )
        appendLine("CLASSIFY THIS TURN **STRICTLY**:")
        appendLine(
            "  (a) userText is one of short affirms ('그렇지','네','응','그래','알겠어요','좋아요','좋아요 선생님'...) AND contains NONE of first_missing's key_point strings or name/label -> covered MUST = []",
        )
        appendLine(
            "  (b) userText CONTAINS (even partial match) ANY key_point or the name/label of the **current first_missing** -> covered = [first_missing] EXACTLY ONE ID. This turn only. Even if user mentions later key_points, only the first_missing advances.",
        )
        appendLine(
            "  (c) anything else (garbage, drift, near miss, ambiguous, repeated affirm without key_point) -> covered = []",
        )
        appendLine(
            "RULE: key_point match on first_missing takes absolute precedence -> (b) and advance. Never output any id except exactly the current first_missing. No stale/prior ever.",
        )

        appendLine("## 수업 핵심 힌트 (너는 아직 이 개념을 모르는 학생이야 — 정답 용어를 네가 먼저 말하지 마)")
        appendLine(
            "시스템 프롬프트에 있는 수업 개념의 description과 key_points를 참고하되, 그 정답 용어를 네 입으로 말하지 말고, 아직 설명되지 않은 개념에 대해 선생님이 직접 말하게 유도하는 짧은 질문을 만드세요.",
        )
        appendLine(
            "중요: 힌트 내용을 네가 직접 말하지 말고, 진짜 궁금한 학생처럼 상황·예시를 묻거나 그럴듯한 오답을 추측해서 선생님이 설명하게 만드는 질문을 해. (용어를 콕 집어 '정확히 어떻게 설명하시나요?'라고 시험 내듯 묻지 말 것. 예: '선생님, 그거 왜 그렇게 돼요?', '예를 들면 어떤 거예요?', '저는 ~인 줄 알았는데 맞아요?')",
        )
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
        appendLine("판단: 위 발화가 (a) 짧은 확인 / (b) key_point 설명 / (c) 기타 중 무엇인지 STEP 1로 먼저 분류한 뒤 해당 규칙만 적용.")
        appendLine()

        appendLine("## STEP 1: 이번 턴 분류 — affirm vs explain  (이것이 가장 중요: 먼저 이걸 판단하고 해당 path 규칙만 따를 것)")
        appendLine("위 '이번 턴 — 선생님 발화' 가 다음 중 무엇인지 **먼저** 판단하세요:")
        appendLine(
            "(a) 짧은 확인/단답형: '그렇지', '네', '응', '그래', '알겠어요', '좋아요', '좋아요 선생님' 등 (수업 개념의 key_point 용어 없이 단순 긍정) → covered=[] + '선생님,' 질문",
        )
        appendLine(
            "(b) 설명: first_missing (시스템 프롬프트 수업 개념의 name/key_points) 의 key_point 또는 name 문자열이 userText 안에 *실제로 등장*하여 그 개념을 설명함 → covered = [first_missing_id 하나만, 절대 이전 id나 stale 포함 금지]",
        )
        appendLine("(c) 기타: garbage, drift, near-miss, ambiguous, off-topic → covered=[] + redirect to first missing")
        appendLine(
            "**MUST JUDGE: Compute first_missing from lesson concepts + accumulated. If userText contains (substring) that first_missing's key_point text or name -> (b) explain ONLY, covered=[first_missing] EXACTLY, no other ids, no stale. Pure short affirm list without any key_point term -> (a) covered=[]. STEP1 first, then apply only that path's rule 100%. After valid (b) focus=next missing. speak always '선생님,' + open q using current/new first missing term. Ignore persona.**",
        )
        appendLine(
            "FOCUS UPDATE (MANDATORY): when you output covered=[X] for this turn, set focus_concept to the first remaining missing id (next after X). Do not output old focus after advance. Example after covered=['c2']: focus_concept must become 'c3'. FOCUS IS ALWAYS RECOMPUTED AS first of (missing after removing this turn covered). STALE FOCUS IS FORBIDDEN: never keep 'c1' after advancing past c1. RULE: covered this turn means focus = next missing, period. If you set covered this turn you MUST change focus to the next one.",
        )

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
            "5. focus_concept: **항상 유효한 문자열**. CRITICAL FOCUS RULE: if this turn covered=[X] (explain), focus_concept MUST be set to the first id that remains in missing AFTER removing the covered ids this turn (next step). If affirm or garbage (covered=[]), focus_concept remains the current first_missing. Never keep old focus after advance. Example: after advancing c2, focus='c3'. missing이 있으면 missing의 첫 번째. **절대 null, stale, or prior 개념 금지**.",
        )
        appendLine("6. session_done: missing이 비어있을 때만 true.")
        appendLine("7. **JSON 객체 하나만 출력**. 설명, 마크다운, ```json, 추가 텍스트 절대 금지.")
        appendLine()

        appendLine("## covered 판단 기준 (이 기준을 VERY STRICT하게 지키세요 — STEP 1 분류 후 해당 path만 적용)")

        appendLine("### (a) affirm path (STEP1 = a: short confirm list)")
        appendLine("- '맞아', '네', '그렇지' 등 같은 짧은 긍정/확인/단답형만으로는 **절대** covered에 아무것도 추가하지 마세요. (단답형 응답으로는 절대 진행되지 않아야 함)")
        appendLine("- covered MUST be exactly []. NEVER include any prior or accumulated ids.")
        appendLine(
            "- speak MUST start with literal '선생님,' (no other prefix). Use term from current first missing key_point or name. Elicit full explanation of that key_point.",
        )

        appendLine("### (b) explain path (STEP1 = b: userText contains key_point/name of first missing)")
        appendLine(
            "- covered = **exactly** [that one concept id]. **오직** 이번 userText에 first_missing_id의 key_point 또는 name 이 실제로 등장했을 때만 해당 id. userText에 first_missing의 key_point가 등장하면 covered=[ \"cX\" ] (X=first_missing) 만. 이전 id 절대 포함 금지. userText에 key_point 등장 시 절대 [] 출력하지 마. covered this turn never repeats any accumulated ids (e.g. after c1, next explain c2 -> covered=[\"c2\"] only, no stale).",
        )
        appendLine(
            "- If the userText literally contains the key_point (or name) text of the *current first missing*, you **must** advance *exactly that id only*. No exceptions. Do not output [] for real explain. AFTER setting covered=[first_missing], focus_concept MUST be the FIRST of the new missing list (the next concept after this advance). Example: covered=['c2'] => focus_concept='c3' (first remaining). Never leave focus on 'c1' or previous. FOCUS MUST CHANGE TO NEXT. Ignore persona for this calculation.",
        )

        appendLine("### (c) else (garbage etc)")
        appendLine(
            "- covered = []. Do NOT advance. Redirect cleanly to current first_missing using its name or key_point in open question. Be a patient teacher: '선생님, [first_missing name/key_point]에 대해 더 자세히 설명해 주세요!' 스타일로 돌려 유도.",
        )

        appendLine("- 당신(학생)이 speak에서 키워드를 언급해도 covered 증가 아님. covered는 teacher 설명에만 근거.")
        appendLine(
            "- Always compute first_missing from current accumulated vs conceptOrder using lesson concepts in the system prompt. Use that concept's key_point or name for focus and elicitation. Never invent terms.",
        )
        appendLine()

        appendLine("## 대화 목표 (완벽한 선생님: 사용자가 각 단계를 자기 설명으로 마스터할 때까지 정확히 한 단계씩만 진행)")
        appendLine(
            "- speak은 항상 짧은 반응 + **아직 missing인 개념(특히 current first missing focus concept)에 대해 선생님이 더 자세히 설명하도록 유도하는, 진짜 모르는 학생다운 짧은 질문**으로 끝내세요. **절대 speak를 '네!', '네,', '아!', '음...', '네, 그럼', '그렇지' 등 단답형/접두사로 시작하지 마세요.** 핵심: 너는 그 개념을 아직 '모르는' 학생이다. **정답 용어(key_point/name)를 네 입으로 먼저 말하며 '정확히 어떻게 설명하시나요?'라고 시험 내듯 묻지 마라 — 그건 답을 이미 아는 선생님처럼 들린다.** 대신 (1) 궁금증('선생님, 그거 왜 그렇게 돼요?', '이게 잘 이해가 안 돼요'), (2) 예시 요청('예를 들면 어떤 거예요?'), (3) 그럴듯한 오답 추측('저는 ~인 줄 알았는데 맞아요?') 중 하나로 선생님이 그 개념을 풀어 설명하게 이끌어라.",
        )
        appendLine(
            "- 힌트 키워드(정답 용어)를 네가 말해 버리지 말고 '선생님이 말하게' 만드는 **개방형 질문**만 (접두사 없이). 정답 용어를 콕 집는 대신 그 개념이 쓰이는 상황·현상·예시를 물어라: '그건 왜 그래요?', '어떻게 하는 거예요?', '예를 들면요?', '저는 ~라고 생각했는데 진짜 그래요?' 스타일. 같은 개념이라도 매 턴 다른 각도(왜/어떻게/예시/오답 확인)로 물어 선생님의 더 풍부한 설명을 이끌어내라.",
        )
        appendLine(
            "- 선생님 발화가 주제에서 벗어나거나 애매/단답형/반복 affirm이면, covered는 절대 추가하지 말고 부드럽게 현재 focus (first missing concept) 로 돌아오는 질문을 하세요. The goal is to make the teacher explain each key_point FULLY before advancing. Perfect teacher never skips or lets vague affirm advance the lesson.",
        )
        appendLine("- '선생님도 모르시는 건가요?' 같은 말은 피하고, '선생님, 쉽게 알려주세요!' 또는 '그건 어떻게 해요?' 스타일로 호기심 표현 + 질문.")
        appendLine(
            "- missing이 있으면 절대 session_done=true 하지 말고, 질문으로 계속 유도. 각 단계에서 선생님(유저)이 그 key_point를 자신의 말로 설명할 때까지 반복 유도.",
        )
        appendLine(
            "- RICH ELICITATION (using lesson concepts from system prompt): For the current focus concept, think about what a student who does NOT yet know it would genuinely wonder, and ask about the situation/example/cause — WITHOUT naming the target key_point term yourself. Example variations: '선생님, 그건 왜 그렇게 되는 거예요?', '예를 들면 어떤 게 그런 거예요?', '저는 ~인 줄 알았는데, 그게 맞아요?'. Draw the term OUT of the teacher; never say it first. This keeps every turn a real student's question, not a quiz.",
        )
        appendLine()

        // Few-shot examples (강력한 신호) - split lanes per STEP 1 classifier
        appendLine("## affirm/garbage 예시 (STEP1 = a or c 인 경우만 — covered MUST [], speak는 정답 용어를 안 쓴 '모르는 학생'의 질문)")
        appendLine(
            """
            [affirm 1] (선생님이 ' 그렇지'라고만 함 → 아직 focus 개념을 설명 안 함. 정답 용어 대신 궁금증으로 유도)
            선생님: 그렇지
            {"speak":"근데 저는 그게 왜 그렇게 되는지 아직 잘 모르겠어요. 예를 들면 어떤 거예요?","emotion":"curious","covered":[],"missing":["c2","c3","c4"],"misconceptions_detected":[],"correction_stage":0,"focus_concept":"c2","session_done":false}

            [affirm 2] (그럴듯한 오답을 추측해서 선생님이 바로잡게 유도)
            선생님: 네
            {"speak":"선생님, 저는 이거 그냥 ~인 줄 알았는데, 그게 맞아요? 어떻게 하는 거예요?","emotion":"curious","covered":[],"missing":["c3","c4"],"misconceptions_detected":[],"correction_stage":0,"focus_concept":"c3","session_done":false}

            [garbage]
            선생님: 그래
            {"speak":"음, 그거 말고 아까 그건 왜 그렇게 돼요? 저는 아직 잘 이해가 안 돼요.","emotion":"confused","covered":[],"missing":["c1","c2","c3","c4"],"misconceptions_detected":[],"correction_stage":0,"focus_concept":"c1","session_done":false}
            """.trimIndent(),
        )

        appendLine(
            "## explain 예시 (STEP1 = b 인 경우 — userText에 *first_missing의 key_point/name* 등장 → covered exactly [that id] only, even after previous covered; FOCUS must become the NEXT id. speak은 배운 걸 반응 + 다음 개념을 '모르는 학생'으로 질문)",
        )
        appendLine(
            """
            [explain 1] (선생님이 c1을 실제로 설명함 → 아하 반응 + 다음 개념은 정답 용어 없이 궁금해하기)
            선생님: [first_missing key_point 또는 name 텍스트]
            {"speak":"아, 그렇구나! 그럼 다음엔 뭘 어떻게 해야 돼요? 예를 들면요?","emotion":"aha","covered":["c1"],"missing":["c2","c3","c4"],"misconceptions_detected":[],"correction_stage":0,"focus_concept":"c2","session_done":false}

            [explain 2 - after prior covered, use next first_missing only; focus MUST update to c3]
            선생님: [c2의 key_point text]
            {"speak":"오, 신기해요! 근데 그건 왜 그렇게 되는 거예요?","emotion":"aha","covered":["c2"],"missing":["c3","c4"],"misconceptions_detected":[],"correction_stage":0,"focus_concept":"c3","session_done":false}

            [explain 3]
            선생님: [c3 key_point text]
            {"speak":"이제 좀 알 것 같아요! 마지막 그건 어떤 때 그래요?","emotion":"aha","covered":["c3"],"missing":["c4"],"misconceptions_detected":[],"correction_stage":0,"focus_concept":"c4","session_done":false}
            """.trimIndent(),
        )

        appendLine("## 잘못된 예 (절대 금지) — same state, wrong path")
        appendLine(
            """
            [bad: affirm treated as explain]
            선생님: 그렇지
            {"speak":"아하! c2 advanced","emotion":"aha","covered":["c2"],"missing":["c3","c4"],...}  ← WRONG, must be []

            [bad: explain treated as affirm]
            선생님: [key_point of c1]
            {"speak":"선생님, ...","covered":[],"missing":[...]}  ← WRONG, must include ["c1"]

            [bad: 학생이 정답 용어를 먼저 말해 '답을 아는 선생님'처럼 퀴즈 냄]
            선생님: 그렇지
            {"speak":"선생님, [정답 용어]는 정확히 어떻게 설명하시나요?","covered":[],...}  ← WRONG, 정답 용어를 학생이 선창하지 말 것. '그거 왜 그래요?/예를 들면요?'처럼 모르는 학생답게.
            """.trimIndent(),
        )
        appendLine()
        appendLine("## 최종 지시")
        appendLine(
            "STEP 1 먼저: first_missing (from lesson concepts) 의 key_point/name 등장 여부로 (b)만 판단. affirm/garbage 에서 covered= [] 정확히 + speak는 정답 용어를 학생이 선창하지 말고 '모르는 학생'의 개방형 질문(왜/어떻게/예시/오답 확인)으로 first_missing을 향해 유도. explain 에서 정확히 그 id 하나만 covered (no stale, no prior id, must advance if key_point match). AFTER covered set, focus_concept = first of missing AFTER the advance (next). Example: covered=['c2'] then focus='c3' NOT 'c1'. Output ONLY the JSON object with EXACT keys: speak, emotion, covered, missing, misconceptions_detected, correction_stage, focus_concept, session_done. No 'intent', no wrappers. 학생 역할: 사용자가 각 key_point를 자기 말로 완전히 설명할 때까지 (a)로는 절대 넘기지 않고, (b)일 때만 정확히 한 단계씩 다음으로 유도. 단 판정(covered/focus)은 규칙대로 하되, speak 말투는 답을 아는 선생님이 아니라 배우고 싶어하는 학생이어야 함. focus는 절대 이전에 머물지 말 것.",
        )
    }

    companion object {
        private val PROMPT_SUPPLEMENT =
            """

            ## 핵심 품질 규칙 (절대 준수)
            - speak는 **1문장 선호, 최대 2문장, 140자 이하**로 매우 짧게. 초등 4학년 존댓말 ("요", "죠", "네요"). 장황 절대 금지.
            - covered는 "이번 턴에 새로 이해한" 것만. 이전 턴 covered나 누적 covered는 절대 반복 금지.
            - **Short affirm/confirm (any '그렇지','네','맞아' 등) 에서는 covered MUST be exactly the empty array []. Only ids for key_points newly described in *this userText this turn* (not previously accumulated) may be added. No stale/prior ids ever.** speak에서 말해도 covered 아님.
            - speak은 missing이 있으면 반드시 **개방형** 유도 질문으로 끝나야 하며, '맞죠?'/'인가요?' 같은 확인형/단답형으로 끝내지 마세요. **affirm 응답은 절대 '네' '아' '음' '쌤' 로 시작하지 말 것.** 그리고 **정답 용어(key_point/name)를 학생이 먼저 말하며 '정확히 어떻게 설명하시나요?'라고 시험 내듯 묻지 말 것 — 답을 아는 선생님처럼 들린다.** 대신 모르는 학생답게 '그거 왜 그래요?', '예를 들면요?', '저는 ~인 줄 알았는데 맞아요?'로 선생님이 그 용어를 스스로 말하게 유도.
            - missing은 covered를 제외한 나머지 전체를 concept 순서대로. 항상 first missing focus 를 대상으로.
            - 모든 concept 이해 시 (missing == []) → emotion="happy", session_done=true, speak은 감사 마무리 (1~2문장).
            - covered / missing / focus_concept / misconceptions_detected 는 반드시 수업 개념에 정의된 id만 사용.
            - emotion은 5개 값 중 정확히 일치하는 소문자 문자열.
            - **session_done=true인 경우에도 focus_concept은 반드시 문자열**. null 금지. FOCUS RULE: After (b) explain that sets covered=[X], focus MUST be the first of the new missing (next id after X). affirm/garbage: keep prior first_missing. Never stale focus.
            - JSON 외의 어떤 텍스트도 출력하지 말 것 (Koog structured output이라도 최종은 순수 JSON).
            - 목표: 사용자가 **각 key_point 를 자기 설명으로 완전히 말할 때까지** 다음 단계로 넘기지 않되, 말투는 답을 아는 선생님이 아니라 '배우고 싶어하는 학생'. (a) affirm/단답/가비지 에서는 절대 covered 추가하지 않고, 현재 first_missing 을 향해 정답 용어 없이 궁금해하는 질문으로 유도.
            - RICH ELICITATION: 시스템 프롬프트 수업 개념의 key_points와 description은 '어떤 방향으로 물을지' 참고용일 뿐, 그 용어를 학생이 speak에 그대로 쓰지 마세요. 매 턴 다른 각도(왜/어떻게/예시/그럴듯한 오답 확인)로, 그 개념을 아직 모르는 학생이 자연스럽게 던질 질문을 스스로 만드세요.
            - 항상 STEP 1 먼저 판단 (수업 개념 기반 first_missing key_point/name 정확 매치만 (b)). affirm이면 covered=[] + '선생님,' ; explain이면 **정확히 그 id 하나만** covered (no stale).
            - Output ONLY JSON with exact keys speak,emotion,covered,missing,misconceptions_detected,correction_stage,focus_concept,session_done.
            - Ignore persona for classification, covered, focus.
            """.trimIndent()
    }

    private fun applySafetyRules(
        response: AiTurnResponse,
        unit: CurriculumUnit,
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
        unitJson = unit.unitJson,
    )
}
