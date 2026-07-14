package org.prography.samsung.backend.conversation.client.stub

import org.prography.samsung.backend.conversation.client.LlmClient
import org.prography.samsung.backend.conversation.client.LlmRequest
import org.prography.samsung.backend.conversation.client.LlmResult
import org.prography.samsung.backend.conversation.dto.response.AiTurnResponse
import org.prography.samsung.backend.conversation.util.AiResponseValidator
import org.prography.samsung.backend.conversation.util.TeacherTurnClassifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["conversation.llm.provider"], havingValue = "stub")
class StubLlmClient(
    private val aiResponseValidator: AiResponseValidator,
    private val teacherTurnClassifier: TeacherTurnClassifier,
) : LlmClient {
    override fun complete(request: LlmRequest): LlmResult {
        val systemPrompt = request.systemPrompt
        val userPrompt = request.userPrompt
        val conceptOrder = extractConceptOrder(userPrompt)
        val unitJson = extractUnitJsonFromSystem(systemPrompt, conceptOrder)
        val accumulated = parseAccumulated(userPrompt)

        val teacherText =
            userPrompt.substringAfter("## 이번 턴 — 선생님 발화", userPrompt).trim().lineSequence().firstOrNull()?.trim()
                ?: userPrompt

        val deltaCovered = teacherTurnClassifier.expectedDeltaCovered(
            userText = teacherText,
            accumulatedCovered = accumulated,
            conceptOrder = conceptOrder,
            unitJson = unitJson,
        )
        val mergedCovered = aiResponseValidator.mergeCovered(accumulated, deltaCovered)
        val missing = aiResponseValidator.resolveMissing(conceptOrder, mergedCovered)
        val sessionDone = missing.isEmpty()
        val focusConcept = aiResponseValidator.resolveFocusConcept(conceptOrder, missing, null)

        val firstMissing = teacherTurnClassifier.firstMissingId(conceptOrder, accumulated)
        val elicitTerm =
            firstMissing?.let { teacherTurnClassifier.hintKeywordFor(it, unitJson) }?.takeIf { it.isNotBlank() }
                ?: "그건"

        val naiveSpeak = when {
            sessionDone -> "선생님, 이제 다 이해했어요. 정말 고마워요!"
            deltaCovered.isNotEmpty() -> {
                val nextTerm = teacherTurnClassifier.hintKeywordFor(focusConcept, unitJson)
                "선생님, $nextTerm 는 정확히 어떻게 설명하시나요?"
            }
            else -> "선생님, $elicitTerm 는 정확히 어떻게 설명하시나요?"
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

        val content = buildString {
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

        return LlmResult(
            content = content,
            provider = PROVIDER,
            model = PROVIDER,
        )
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

    private companion object {
        const val PROVIDER = "stub"
    }
}
