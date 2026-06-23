package org.prography.samsung.backend.conversation.client

import org.prography.samsung.backend.conversation.dto.AiTurnResponse
import org.prography.samsung.backend.conversation.service.AiResponseValidator
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["conversation.llm.provider"], havingValue = "stub")
class StubLlmClient(private val aiResponseValidator: AiResponseValidator) : LlmClient {
    override fun complete(systemPrompt: String, userPrompt: String): String {
        val conceptOrder = aiResponseValidator.parseConceptIdOrder(extractUnitJson(systemPrompt))

        // Naive "raw LLM" output — simple keyword heuristic only (no accum, no policy)
        val teacherText =
            userPrompt.substringAfter("## 이번 턴 — 선생님 발화", userPrompt).trim().lineSequence().firstOrNull()?.trim()
                ?: userPrompt

        val naiveCovered =
            when {
                matchesConceptKeywords(teacherText, listOf("크기", "비교")) -> conceptOrder
                matchesConceptKeywords(teacherText, listOf("분자")) -> conceptOrder.take(3)
                matchesConceptKeywords(teacherText, listOf("분모")) -> conceptOrder.take(2)
                matchesConceptKeywords(teacherText, listOf("똑같이", "일부")) -> conceptOrder.take(1)
                else -> emptyList()
            }

        val naiveMissing = aiResponseValidator.resolveMissing(conceptOrder, naiveCovered)
        val naiveDone = naiveMissing.isEmpty()

        val naiveSpeak = when {
            naiveDone -> "아하! 이제 분수가 뭔지 완전히 이해했어요. 고마워요 선생님!"
            naiveCovered.size >= 2 -> "그럼 분자는 위에 있는 숫자인 거죠?"
            naiveCovered.isNotEmpty() -> "아하! 그럼 분모는 나눈 개수구나?"
            else -> "음... 분수가 뭐예요? 좀 더 쉽게 설명해 주세요."
        }

        val raw = AiTurnResponse(
            speak = naiveSpeak,
            emotion = if (naiveDone) {
                org.prography.samsung.backend.common.domain.AiEmotion.HAPPY
            } else if (naiveCovered.isNotEmpty()) {
                org.prography.samsung.backend.common.domain.AiEmotion.AHA
            } else {
                org.prography.samsung.backend.common.domain.AiEmotion.CURIOUS
            },
            covered = naiveCovered,
            missing = naiveMissing,
            misconceptionsDetected = emptyList(),
            correctionStage = 0,
            focusConcept = aiResponseValidator.resolveFocusConcept(conceptOrder, naiveMissing, null),
            sessionDone = naiveDone,
        )

        // For stub tests to exercise the semantic/retry/fallback path:
        // for pure affirm, produce intentionally weak speak (no ? , no hint keyword)
        // so validateSemantic will fail, leading to retry or final fallback.
        val finalForStub = if (aiResponseValidator.isPureAffirmation(teacherText) && !naiveDone) {
            raw.copy(speak = "네", covered = emptyList())
        } else {
            raw
        }

        // Return the (weak for affirm) JSON. The service layer (validator semantic + guard) will handle.
        return buildString {
            appendLine("{")
            appendLine("  \"speak\": \"${finalForStub.speak.replace("\"", "\\\"")}\",")
            appendLine("  \"emotion\": \"${finalForStub.emotion.value}\",")
            appendLine("  \"covered\": ${toJsonArray(finalForStub.covered)},")
            appendLine("  \"missing\": ${toJsonArray(finalForStub.missing)},")
            appendLine("  \"misconceptions_detected\": [],")
            appendLine("  \"correction_stage\": ${finalForStub.correctionStage},")
            appendLine("  \"focus_concept\": \"${finalForStub.focusConcept}\",")
            appendLine("  \"session_done\": ${finalForStub.sessionDone}")
            appendLine("}")
        }
    }

    private fun parseAccumulated(userPrompt: String): List<String> {
        val after = userPrompt.substringAfter("## 누적 이해한 개념 (이전 턴까지)", "")
        val line = after.lineSequence().map { it.trim() }.firstOrNull { it.isNotBlank() } ?: ""
        if (line == "없음" || line.isEmpty()) return emptyList()
        return line.split(',').map { it.trim() }.filter { it.isNotBlank() }
    }

    private fun extractUnitJson(systemPrompt: String): String = systemPrompt.lineSequence()
        .first { it.contains("\"unit_id\"") && it.contains("\"concepts\"") }
        .trim()

    private fun matchesConceptKeywords(text: String, keywords: List<String>): Boolean =
        keywords.any { text.contains(it) }

    private fun toJsonArray(values: List<String>): String =
        values.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
}
