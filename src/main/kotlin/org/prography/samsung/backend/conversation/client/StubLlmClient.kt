package org.prography.samsung.backend.conversation.client

import org.prography.samsung.backend.conversation.dto.AiTurnResponse
import org.prography.samsung.backend.conversation.service.AiResponseValidator
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["conversation.llm.provider"], havingValue = "stub")
class StubLlmClient(private val aiResponseValidator: AiResponseValidator) : LlmClient {
    override fun complete(systemPrompt: String, userPrompt: String): String {
        val conceptOrder = extractConceptOrder(userPrompt)
        val unitJson = extractUnitJsonFromSystem(systemPrompt, conceptOrder)
        val accumulated = parseAccumulated(userPrompt)

        val teacherText =
            userPrompt.substringAfter("## 이번 턴 — 선생님 발화", userPrompt).trim().lineSequence().firstOrNull()?.trim()
                ?: userPrompt

        val deltaCovered = aiResponseValidator.expectedDeltaCovered(
            userText = teacherText,
            accumulatedCovered = accumulated,
            conceptOrder = conceptOrder,
            unitJson = unitJson,
        )
        val mergedCovered = aiResponseValidator.mergeCovered(accumulated, deltaCovered)
        val missing = aiResponseValidator.resolveMissing(conceptOrder, mergedCovered)
        val sessionDone = missing.isEmpty()
        val focusConcept = aiResponseValidator.resolveFocusConcept(conceptOrder, missing, null)

        // 모르는 학생다운 질문. 정답 용어(key_point/name)를 학생이 선창하지 않는다 (개선된 설계).
        val naiveSpeak = when {
            sessionDone -> "선생님, 이제 다 이해했어요. 정말 고마워요!"
            deltaCovered.isNotEmpty() -> "아, 그렇구나! 그럼 그건 왜 그렇게 되는 거예요?"
            else -> "선생님, 그건 왜 그렇게 돼요? 예를 들면 어떤 거예요?"
        }

        val raw = AiTurnResponse(
            speak = naiveSpeak,
            emotion = if (sessionDone) {
                org.prography.samsung.backend.common.domain.AiEmotion.HAPPY
            } else if (deltaCovered.isNotEmpty()) {
                org.prography.samsung.backend.common.domain.AiEmotion.AHA
            } else {
                org.prography.samsung.backend.common.domain.AiEmotion.CURIOUS
            },
            covered = deltaCovered,
            missing = missing,
            misconceptionsDetected = emptyList(),
            correctionStage = 0,
            focusConcept = focusConcept,
            sessionDone = sessionDone,
        )

        return buildString {
            appendLine("{")
            appendLine("  \"speak\": \"${raw.speak.replace("\"", "\\\"")}\",")
            appendLine("  \"emotion\": \"${raw.emotion.value}\",")
            appendLine("  \"covered\": ${toJsonArray(raw.covered)},")
            appendLine("  \"missing\": ${toJsonArray(raw.missing)},")
            appendLine("  \"misconceptions_detected\": [],")
            appendLine("  \"correction_stage\": ${raw.correctionStage},")
            appendLine("  \"focus_concept\": \"${raw.focusConcept}\",")
            appendLine("  \"session_done\": ${raw.sessionDone}")
            appendLine("}")
        }
    }

    private fun parseAccumulated(userPrompt: String): List<String> {
        val after = userPrompt.substringAfter("## 누적 이해한 개념 (이전 턴까지)", "")
        val line = after.lineSequence().map { it.trim() }.firstOrNull { it.isNotBlank() } ?: ""
        if (line == "없음" || line.isEmpty()) return emptyList()
        return line.split(',').map { it.trim() }.filter { it.isNotBlank() }
    }

    private fun extractConceptOrder(userPrompt: String): List<String> {
        val after = userPrompt.substringAfter("## 제공된 개념 ID 목록", "")
        val line = after.lineSequence().map { it.trim() }.firstOrNull { it.isNotBlank() } ?: ""
        return line.split(',').map { it.trim() }.filter { it.isNotBlank() }
    }

    /**
     * Reconstructs minimal unit JSON from formatted lesson-concepts block in system prompt.
     */
    private fun extractUnitJsonFromSystem(systemPrompt: String, conceptOrder: List<String>): String {
        val section = systemPrompt.substringAfter("## 수업 개념", "").substringBefore("## 응답 규칙")
        val concepts = mutableListOf<Map<String, Any>>()
        var currentId = ""
        var currentName = ""
        val keyPoints = mutableListOf<String>()

        fun flush() {
            if (currentId.isNotBlank()) {
                concepts.add(
                    mapOf(
                        "id" to currentId,
                        "name" to currentName,
                        "key_points" to keyPoints.toList(),
                    ),
                )
            }
            currentId = ""
            currentName = ""
            keyPoints.clear()
        }

        section.lineSequence().forEach { line ->
            val trimmed = line.trim()
            val header = Regex("""- \[(c\d+)\] (.+)""").find(trimmed)
            if (header != null) {
                flush()
                currentId = header.groupValues[1]
                currentName = header.groupValues[2]
            } else if (trimmed.startsWith("•")) {
                keyPoints.add(trimmed.removePrefix("•").trim())
            }
        }
        flush()

        if (concepts.isEmpty()) {
            return """{"concepts":[${conceptOrder.joinToString { """{"id":"$it"}""" }}]}"""
        }
        val conceptsJson = concepts.joinToString(",") { c ->
            val kps = (c["key_points"] as List<*>).joinToString(",") { """"$it"""" }
            """{"id":"${c["id"]}","name":"${c["name"]}","key_points":[$kps]}"""
        }
        return """{"concepts":[$conceptsJson]}"""
    }

    private fun toJsonArray(values: List<String>): String =
        values.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
}
