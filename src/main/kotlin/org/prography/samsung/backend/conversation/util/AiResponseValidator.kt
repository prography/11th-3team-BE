package org.prography.samsung.backend.conversation.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.prography.samsung.backend.common.domain.AiEmotion
import org.prography.samsung.backend.conversation.dto.response.AiTurnResponse
import org.springframework.stereotype.Component

private const val SPEAK_MAX_LENGTH = 180
private const val MIN_CORRECTION_STAGE = 0
private const val MAX_CORRECTION_STAGE = 4
private const val MAX_SENTENCE_COUNT = 3

private val JSON_FENCE_REGEX = Regex("""```(?:json)?\s*([\s\S]*?)```""")
private val FORBIDDEN_WORDS = listOf("씨발", "시발", "병신", "좆", "개새", "fuck", "shit")
private val SENTENCE_PUNCTUATION = setOf('.', '?', '!', '。', '？', '！')
private val QUESTION_MARKERS = listOf("?", "？", "뭐", "어떻게", "왜")
private val LEADING_AFFIRMATION_PREFIXES =
    listOf("네", "맞아", "그렇지", "좋아요", "응", "그래", "알겠어요", "아", "음")

@Component
class AiResponseValidator(
    private val objectMapper: ObjectMapper,
    private val teacherTurnClassifier: TeacherTurnClassifier,
) {
    /**
     * LLM 원시 출력에서 JSON을 추출·파싱하고 구조 규칙을 강제해 [AiTurnResponse]로 변환한다.
     * speak 공백/길이, emotion 값, covered·missing의 유효 concept id 여부를 검증하며
     * focus_concept은 missing 기준으로 재해석하고 금칙어는 마스킹한다.
     * 규칙 위반 시 예외를 던져 상위의 재시도(retry)를 유발한다.
     */
    fun parseAndValidate(raw: String, conceptOrder: List<String>): AiTurnResponse {
        val validConceptIds = conceptOrder.toSet()
        val json = extractJsonObject(raw)
        val node: JsonNode = objectMapper.readTree(json)

        val speak = node.path("speak").asText("").trim()
        require(speak.isNotBlank()) { "speak is blank or missing" }

        // 길이 제한 (한국어 기준) - 장황 방지. 반응 + 유도 질문 허용
        require(speak.length <= SPEAK_MAX_LENGTH) {
            "speak too long (max ~160 chars, got ${speak.length}). 1~2문장(반응+짧은 질문)으로 짧게 다시 출력하세요."
        }

        val emotion = parseEmotion(node.path("emotion").asText("curious"))
        val covered = parseStringList(node.get("covered"))
        val missing = parseStringList(node.get("missing"))
        val misconceptions = parseStringList(node.get("misconceptions_detected"))
        val correctionStage =
            node.path("correction_stage").asInt(0).coerceIn(
                MIN_CORRECTION_STAGE,
                MAX_CORRECTION_STAGE,
            )
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

    /**
     * [AiTurnResponse]를 LLM 응답 스키마의 스네이크 케이스 키(misconceptions_detected 등)로 직렬화한다.
     * 저장·재주입용 JSON 문자열을 생성한다.
     */
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

    /**
     * 저장된 응답 JSON을 [AiTurnResponse]로 역직렬화한다.
     * [parseAndValidate]와 달리 검증 없이 관대하게 읽으며(누락 필드는 기본값),
     * 주로 이전 턴의 저장된 응답을 프롬프트 컨텍스트로 복원할 때 사용한다.
     */
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

    /**
     * 단원 JSON의 concepts 배열에서 concept id를 정의된 순서대로 추출한다.
     * 반환된 순서가 곧 학습 진행 순서(first_missing 계산의 기준)가 된다.
     */
    fun parseConceptIdOrder(unitJson: String): List<String> {
        val root = objectMapper.readTree(unitJson)
        return root.path("concepts")
            .mapNotNull { it.path("id").asText(null)?.takeIf(String::isNotBlank) }
    }

    /** 임의의 JSON 문자열을 Jackson [JsonNode] 트리로 파싱한다. */
    internal fun readJsonTree(json: String): JsonNode = objectMapper.readTree(json)

    /**
     * 이번 턴의 focus_concept을 결정한다.
     * LLM이 제시한 [explicit]이 아직 missing에 남아있으면 그대로 쓰고(진도 후퇴 방지),
     * 아니면 conceptOrder 순서상 가장 앞선 missing 개념으로 대체한다.
     */
    fun resolveFocusConcept(conceptOrder: List<String>, missing: List<String>, explicit: String?): String {
        // Only use explicit if it is still a missing concept (prevents returning covered focus after non-teach turns)
        if (!explicit.isNullOrBlank() && explicit in missing) return explicit
        val orderedMissing = conceptOrder.filter { it in missing }
        return orderedMissing.firstOrNull() ?: conceptOrder.firstOrNull() ?: ""
    }

    /** conceptOrder에서 아직 covered되지 않은 개념을 순서를 유지한 채 missing 목록으로 반환한다. */
    fun resolveMissing(conceptOrder: List<String>, covered: List<String>): List<String> =
        conceptOrder.filter { it !in covered }

    /** 단원의 총 개념 수를 반환한다. max_concepts가 지정돼 있으면 그 값을, 없으면 concepts 배열 크기를 쓴다. */
    fun totalConcepts(unitJson: String): Int {
        val root = objectMapper.readTree(unitJson)
        return root.path("max_concepts").asInt(root.path("concepts").size())
    }

    /** 기존 누적 covered와 이번 턴 covered를 합쳐 중복을 제거한 누적 목록을 만든다. */
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
        unitJson: String = "",
    ): String? {
        val valid = conceptOrder.toSet()
        return validateNewCoveredConcepts(response, accumulatedCovered)
            ?: validateCoveredConceptIds(response, valid)
            ?: validateFocusConcept(response)
            ?: validateSpeakQuality(response.speak)
            ?: validateIncompleteSpeak(response, unitJson)
            ?: validateSessionDoneConsistency(response, conceptOrder)
            ?: validatePureAffirmationCovered(response, currentUserText, unitJson)
            ?: validatePureAffirmationOpening(response, currentUserText, unitJson)
    }

    private fun validateNewCoveredConcepts(response: AiTurnResponse, accumulatedCovered: List<String>): String? {
        if (response.covered.any { it in accumulatedCovered }) {
            val bad = response.covered.filter { it in accumulatedCovered }
            return "covered에 이미 이해한 개념($bad)이 포함. 이번 턴 새로 이해한 것만."
        }
        return null
    }

    private fun validateCoveredConceptIds(response: AiTurnResponse, validConceptIds: Set<String>): String? {
        if (response.covered.any { it !in validConceptIds }) {
            return "covered에 허용되지 않은 id가 있습니다."
        }
        return null
    }

    private fun validateFocusConcept(response: AiTurnResponse): String? {
        if (
            !response.sessionDone &&
            response.focusConcept !in response.missing &&
            response.missing.isNotEmpty()
        ) {
            return "focus_concept가 missing에 속하지 않습니다."
        }
        return null
    }

    private fun validateSpeakQuality(speak: String): String? {
        val punctCount = speak.count { it in SENTENCE_PUNCTUATION }
        val sentenceCount = punctCount + 1
        if (sentenceCount > MAX_SENTENCE_COUNT) {
            return "speak이 너무 깁니다. 1~2문장(반응 + 유도 질문 1개) 정도로 줄이세요."
        }
        if (speak.length > SPEAK_MAX_LENGTH) {
            return "speak이 ${SPEAK_MAX_LENGTH}자를 초과했습니다. 140~160자 이하로 (반응 + 짧은 질문)."
        }
        return null
    }

    private fun validateIncompleteSpeak(response: AiTurnResponse, unitJson: String): String? {
        if (response.sessionDone || response.missing.isEmpty()) return null

        val hasQuestion = QUESTION_MARKERS.any { marker -> response.speak.contains(marker) }
        val firstMissing = response.missing.firstOrNull()
        val focusTerms = firstMissing?.let { teacherTurnClassifier.termsForConcept(it, unitJson) } ?: emptyList()
        val requiredKw = firstMissing?.let { teacherTurnClassifier.hintKeywordFor(it, unitJson) } ?: ""
        val hasKw = focusTerms.isEmpty() || focusTerms.any { term -> response.speak.contains(term) }
        if (!hasQuestion || !hasKw) {
            return (
                "For non-teach (affirm): speak needs '?' + lesson term from '$requiredKw'. " +
                    "covered must be []."
                )
        }
        return null
    }

    private fun validateSessionDoneConsistency(response: AiTurnResponse, conceptOrder: List<String>): String? {
        if (response.sessionDone && response.missing.isNotEmpty()) {
            return "session_done=true인데 missing이 비어있지 않습니다."
        }
        if (!response.sessionDone && response.missing.isEmpty() && conceptOrder.isNotEmpty()) {
            return "missing 비었는데 session_done=false. true로 해야 합니다."
        }
        return null
    }

    private fun validatePureAffirmationCovered(
        response: AiTurnResponse,
        currentUserText: String?,
        unitJson: String,
    ): String? {
        if (
            currentUserText != null &&
            teacherTurnClassifier.isPureAffirmation(currentUserText, unitJson) &&
            response.covered.isNotEmpty()
        ) {
            return "short affirmation; covered must be []. Speak must have ? + hint kw."
        }
        return null
    }

    private fun validatePureAffirmationOpening(
        response: AiTurnResponse,
        currentUserText: String?,
        unitJson: String,
    ): String? {
        if (currentUserText != null &&
            teacherTurnClassifier.isPureAffirmation(currentUserText, unitJson) &&
            isLeadingAffirmationOnly(response.speak)
        ) {
            return "affirm speak must not start with leading 단답형 prefix; use open question with hint kw."
        }
        return null
    }

    /**
     * speak가 "네", "아", "음" 등 단답형 긍정 접두사로 시작하는지 검사한다.
     * affirm 응답에서 이런 시작을 금지하고 개방형 질문을 강제하기 위한 [validateSemanticRules]용 헬퍼.
     */
    private fun isLeadingAffirmationOnly(s: String): Boolean {
        val t = s.trim().lowercase()
        return LEADING_AFFIRMATION_PREFIXES.any { prefix -> t.startsWith(prefix) }
    }

    /**
     * LLM 원시 출력에서 JSON 객체 문자열만 추출한다.
     * 마크다운 코드펜스(```json ... ```) → 첫 '{' ~ 마지막 '}' → 전체가 JSON이라고 가정, 순으로 시도하며
     * 어느 방법으로도 JSON을 얻지 못하면 예외를 던진다.
     */
    private fun extractJsonObject(raw: String): String {
        val trimmed = raw.trim()

        // 1. markdown fence 우선 추출 (```json ... ``` 또는 ``` ... ```)
        val fenceMatch = JSON_FENCE_REGEX.find(trimmed)
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

    /** 문자열 값을 [AiEmotion]으로 변환한다(공백 제거 후 매핑). */
    private fun parseEmotion(value: String): AiEmotion = AiEmotion.fromValue(value.trim())

    /** JSON 노드를 문자열 리스트로 변환한다. 배열이 아니거나 null이면 빈 리스트, 공백 원소는 제외한다. */
    private fun parseStringList(node: JsonNode?): List<String> {
        if (node == null || !node.isArray) return emptyList()
        return node.mapNotNull { it.asText(null)?.takeIf(String::isNotBlank) }
    }

    /** speak에 포함된 금칙어를 "***"로 치환한다(대소문자 무시). */
    private fun filterForbiddenWords(text: String): String =
        FORBIDDEN_WORDS.fold(text) { result, word -> result.replace(word, "***", ignoreCase = true) }
}
