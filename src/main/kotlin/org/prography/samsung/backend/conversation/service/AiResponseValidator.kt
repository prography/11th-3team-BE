package org.prography.samsung.backend.conversation.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.prography.samsung.backend.common.domain.AiEmotion
import org.prography.samsung.backend.conversation.dto.AiTurnResponse
import org.springframework.stereotype.Component

@Component
class AiResponseValidator(private val objectMapper: ObjectMapper) {
    private val forbiddenWords = listOf("씨발", "시발", "병신", "좆", "개새", "fuck", "shit")

    fun parseAndValidate(raw: String, conceptOrder: List<String>): AiTurnResponse {
        val validConceptIds = conceptOrder.toSet()
        val json = extractJsonObject(raw)
        val node: JsonNode = objectMapper.readTree(json)

        val speak = node.path("speak").asText("").trim()
        require(speak.isNotBlank()) { "speak is blank or missing" }

        // 길이 제한 (한국어 기준) - 장황 방지. 반응 + 유도 질문 허용
        require(speak.length <= 180) {
            "speak too long (max ~160 chars, got ${speak.length}). 1~2문장(반응+짧은 질문)으로 짧게 다시 출력하세요."
        }

        val emotion = parseEmotion(node.path("emotion").asText("curious"))
        val covered = parseStringList(node.get("covered"))
        val missing = parseStringList(node.get("missing"))
        val misconceptions = parseStringList(node.get("misconceptions_detected"))
        val correctionStage = node.path("correction_stage").asInt(0).coerceIn(0, 4)
        val explicitFocus = node.path("focus_concept").asText("").ifBlank { null }
        val focusConcept = resolveFocusConcept(conceptOrder, missing, explicitFocus)
        val sessionDone = node.path("session_done").asBoolean(false)

        // Concept id 검증
        require(covered.all { it in validConceptIds }) {
            "invalid covered concept id(s): ${covered.filterNot { it in validConceptIds }}"
        }
        require(missing.all { it in validConceptIds }) {
            "invalid missing concept id(s): ${missing.filterNot { it in validConceptIds }}"
        }

        // session_done 일관성 (강력 권장, violation 시 retry에서 잡힘)
        if (sessionDone && missing.isNotEmpty()) {
            // 여기서는 일단 통과시키고 semantic에서 더 강하게 체크 (retry 유도)
        }
        if (!sessionDone && missing.isEmpty() && conceptOrder.isNotEmpty()) {
            // missing이 비었는데 done=false → 나중에 semantic에서 교정 유도
        }

        return AiTurnResponse(
            speak = filterForbiddenWords(speak),
            emotion = emotion,
            covered = covered.distinct(),
            missing = missing.distinct(),
            misconceptionsDetected = misconceptions,
            correctionStage = correctionStage,
            focusConcept = focusConcept,
            sessionDone = sessionDone,
        )
    }

    fun toJson(response: AiTurnResponse): String = objectMapper.writeValueAsString(
        mapOf(
            "speak" to response.speak,
            "emotion" to response.emotion.value,
            "covered" to response.covered,
            "missing" to response.missing,
            "misconceptions_detected" to response.misconceptionsDetected,
            "correction_stage" to response.correctionStage,
            "focus_concept" to response.focusConcept,
            "session_done" to response.sessionDone,
        ),
    )

    fun fromJson(json: String): AiTurnResponse {
        val node = objectMapper.readTree(json)
        val rawFocus = node.path("focus_concept").asText("").ifBlank { null }
        return AiTurnResponse(
            speak = node.path("speak").asText(),
            emotion = parseEmotion(node.path("emotion").asText()),
            covered = parseStringList(node.get("covered")),
            missing = parseStringList(node.get("missing")),
            misconceptionsDetected = parseStringList(node.get("misconceptions_detected")),
            correctionStage = node.path("correction_stage").asInt(0),
            focusConcept = rawFocus ?: "",
            sessionDone = node.path("session_done").asBoolean(false),
        )
    }

    fun parseConceptIdOrder(unitJson: String): List<String> {
        val root = objectMapper.readTree(unitJson)
        return root.path("concepts")
            .mapNotNull { it.path("id").asText(null)?.takeIf(String::isNotBlank) }
    }

    fun parseConceptIds(unitJson: String): Set<String> = parseConceptIdOrder(unitJson).toSet()

    fun resolveFocusConcept(conceptOrder: List<String>, missing: List<String>, explicit: String?): String {
        // Only use explicit if it is still a missing concept (prevents returning covered focus after non-teach turns)
        if (!explicit.isNullOrBlank() && explicit in missing) return explicit
        val orderedMissing = conceptOrder.filter { it in missing }
        return orderedMissing.firstOrNull() ?: conceptOrder.firstOrNull() ?: ""
    }

    fun resolveMissing(conceptOrder: List<String>, covered: List<String>): List<String> =
        conceptOrder.filter { it !in covered }

    fun totalConcepts(unitJson: String): Int {
        val root = objectMapper.readTree(unitJson)
        return root.path("max_concepts").asInt(root.path("concepts").size())
    }

    fun mergeCovered(existing: List<String>, current: List<String>): List<String> = (existing + current).distinct()

    /**
     * parseAndValidate 통과 후에도 semantic 규칙을 추가 검사.
     * null 반환 = 통과, String 반환 = 위반 사유 (retry correction message로 사용)
     */
    fun validateSemanticRules(
        response: AiTurnResponse,
        accumulatedCovered: List<String>,
        conceptOrder: List<String>,
        currentUserText: String? = null,
    ): String? {
        val valid = conceptOrder.toSet()

        // covered는 accumulated에 없던 "새로운" 것만
        if (response.covered.any { it in accumulatedCovered }) {
            val bad = response.covered.filter { it in accumulatedCovered }
            return "covered에 이미 이해한 개념($bad)이 포함. 이번 턴 새로 이해한 것만."
        }

        // covered가 valid id만
        if (response.covered.any { it !in valid }) {
            return "covered에 허용되지 않은 id가 있습니다."
        }

        // focus_concept은 missing에 있거나, done 상태면 유연
        if (
            !response.sessionDone &&
            response.focusConcept !in response.missing &&
            response.missing.isNotEmpty()
        ) {
            return "focus_concept가 missing에 속하지 않습니다."
        }

        // speak 품질 (장황 방지 강화) — 반응 + 짧은 유도 질문 허용
        val punctCount = response.speak.count { it in setOf('.', '?', '!', '。', '？', '！') }
        val sentenceCount = punctCount + 1
        if (sentenceCount > 3) {
            return "speak이 너무 깁니다. 1~2문장(반응 + 유도 질문 1개) 정도로 줄이세요."
        }
        if (response.speak.length > 180) {
            return "speak이 180자를 초과했습니다. 140~160자 이하로 (반응 + 짧은 질문)."
        }

        // Speak quality for non-explanatory turns (affirm/garbage) and missing: must have ? + hint keyword for first missing (to make LLM produce it via prompt/retry)
        if (!response.sessionDone && response.missing.isNotEmpty()) {
            val hasQuestion =
                response.speak.contains('?') ||
                    response.speak.contains('？') ||
                    response.speak.contains("뭐") ||
                    response.speak.contains("어떻게") ||
                    response.speak.contains("왜")
            val firstMissing = response.missing.firstOrNull()
            val requiredKw = firstMissing?.let { hintKeywordFor(it) } ?: ""
            val hasKw = requiredKw.isEmpty() ||
                response.speak.contains(requiredKw) ||
                (firstMissing == "c1" && (response.speak.contains("일부") || response.speak.contains("부분")))
            if (!hasQuestion || !hasKw) {
                return (
                    "For non-teach (affirm): speak needs '?' + kw '$requiredKw' (or 일부 for c1). " +
                        "covered must be []."
                    )
            }
        }

        // session_done 일관성
        if (response.sessionDone && response.missing.isNotEmpty()) {
            return "session_done=true인데 missing이 비어있지 않습니다."
        }
        if (!response.sessionDone && response.missing.isEmpty() && conceptOrder.isNotEmpty()) {
            return "missing 비었는데 session_done=false. true로 해야 합니다."
        }

        // AC1/AC3 deterministic guard: short affirmation from teacher must not claim any new covered
        if (currentUserText != null && isPureAffirmation(currentUserText) && response.covered.isNotEmpty()) {
            return "short affirmation; covered must be []. Speak must have ? + hint kw."
        }

        return null // 통과
    }

    fun isPureAffirmation(text: String): Boolean {
        val t = text.trim().lowercase()
        val exact = setOf("그렇지", "맞아", "네", "좋아요", "응", "그래", "알겠어요", "맞습니다", "yes", "yeah", "ok", "okay")
        if (t in exact) return true
        // very short utterances without core hint keywords are treated as non-explanatory
        if (t.length <= 5 && !containsHintKeyword(t)) return true
        return false
    }

    private fun containsHintKeyword(t: String): Boolean = t.contains("전체") ||
        t.contains("똑같이") ||
        t.contains("일부") ||
        t.contains("분모") ||
        t.contains("아래") ||
        t.contains("분자") ||
        t.contains("위") ||
        t.contains("크기") ||
        t.contains("비교")

    fun hintKeywordFor(id: String): String = when (id) {
        "c1" -> "일부분"
        "c2" -> "분모"
        "c3" -> "분자"
        "c4" -> "크기"
        else -> id
    }

    private fun extractJsonObject(raw: String): String {
        val trimmed = raw.trim()

        // 1. markdown fence 우선 추출 (```json ... ``` 또는 ``` ... ```)
        val fenceMatch = Regex("""```(?:json)?\s*([\s\S]*?)```""").find(trimmed)
        if (fenceMatch != null) {
            return fenceMatch.groupValues[1].trim()
        }

        // 2. 첫 { 와 마지막 } 사이 (가장 흔한 경우)
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start >= 0 && end > start) {
            val candidate = trimmed.substring(start, end + 1).trim()
            // 간단한 균형 체크 (너무 많은 경우 방지)
            if (candidate.count { it == '{' } >= 1 && candidate.count { it == '}' } >= 1) {
                return candidate
            }
        }

        // 3. 마지막 수단: 전체가 JSON이라고 가정하고 시도 (executeStructured 성공 시 거의 여기 안 옴)
        require(trimmed.startsWith("{") && trimmed.endsWith("}")) {
            "no valid JSON object could be extracted from LLM output"
        }
        return trimmed
    }

    private fun parseEmotion(value: String): AiEmotion = AiEmotion.fromValue(value.trim())

    private fun parseStringList(node: JsonNode?): List<String> {
        if (node == null || !node.isArray) return emptyList()
        return node.mapNotNull { it.asText(null)?.takeIf(String::isNotBlank) }
    }

    private fun filterForbiddenWords(text: String): String {
        var result = text
        forbiddenWords.forEach { word ->
            result = result.replace(word, "***", ignoreCase = true)
        }
        return result
    }
}
