package org.prography.samsung.backend.conversation.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.prography.samsung.backend.conversation.enums.TeacherTurnKind
import org.springframework.stereotype.Component

private const val MIN_CONCEPT_TERM_LENGTH = 2
private const val SHORT_AFFIRMATION_MAX_LENGTH = 5
private const val HINT_KEYWORD_MAX_LENGTH = 12

private val PURE_AFFIRMATIONS =
    setOf("그렇지", "맞아", "네", "좋아요", "응", "그래", "알겠어요", "맞습니다", "좋아요 선생님", "네 선생님", "yes", "yeah", "ok", "okay")

@Component
class TeacherTurnClassifier(private val objectMapper: ObjectMapper) {
    /** 누적 covered를 기준으로 conceptOrder상 가장 먼저 등장하는 미이해 개념(first_missing) id를 반환한다. 없으면 null. */
    fun firstMissingId(conceptOrder: List<String>, accumulatedCovered: List<String>): String? =
        conceptOrder.firstOrNull { it !in accumulatedCovered }

    /**
     * 단원 전체 개념의 매칭 후보 용어(key_points, keywords, name/label)를 2자 이상만 모아 반환한다.
     * 발화가 단순 확인인지 개념 설명인지 가르는 [isPureAffirmation] 등에서 사용한다.
     */
    fun extractConceptTerms(unitJson: String): List<String> {
        if (unitJson.isBlank()) return emptyList()
        return try {
            val root = objectMapper.readTree(unitJson)
            val terms = mutableListOf<String>()
            root.path("concepts").forEach { concept ->
                terms.addAll(collectConceptTerms(concept) { term -> term.length >= MIN_CONCEPT_TERM_LENGTH })
            }
            terms.distinct()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * 특정 [conceptId] 하나에 속한 매칭 후보 용어(key_points, keywords, name/label)를 반환한다.
     * [extractConceptTerms]와 달리 길이 제한 없이 해당 개념의 용어만 수집하며,
     * advance 판정([userTextExplainsConcept])과 힌트 키워드 산출에 쓰인다.
     */
    fun termsForConcept(conceptId: String, unitJson: String): List<String> {
        if (unitJson.isBlank()) return emptyList()
        return try {
            val root = objectMapper.readTree(unitJson)
            val terms = mutableListOf<String>()
            root.path("concepts").forEach { concept ->
                if (concept.path("id").asText("") != conceptId) return@forEach
                terms.addAll(collectConceptTerms(concept, String::isNotBlank))
            }
            terms.distinct()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** [terms] 중 하나라도 [text]에 부분 문자열(substring)로 포함되면 true를 반환한다. */
    fun textContainsAnyTerm(text: String, terms: List<String>): Boolean =
        terms.any { term -> term.isNotBlank() && text.contains(term) }

    /** [userText]가 [conceptId]의 용어를 substring으로 포함하는지, 즉 그 개념을 설명한 것으로 볼지 판정한다. */
    fun userTextExplainsConcept(userText: String, conceptId: String, unitJson: String): Boolean =
        textContainsAnyTerm(userText, termsForConcept(conceptId, unitJson))

    /**
     * 이번 유저 발화를 AFFIRM/EXPLAIN/GARBAGE로 분류한다.
     * first_missing 개념을 설명하면 EXPLAIN, 단순 확인이면 AFFIRM, 나머지는 GARBAGE로 본다.
     */
    fun classifyTeacherTurn(
        userText: String,
        accumulatedCovered: List<String>,
        conceptOrder: List<String>,
        unitJson: String,
    ): TeacherTurnKind {
        val firstMissing = firstMissingId(conceptOrder, accumulatedCovered) ?: return TeacherTurnKind.GARBAGE
        if (userTextExplainsConcept(userText, firstMissing, unitJson)) return TeacherTurnKind.EXPLAIN
        if (isPureAffirmation(userText, unitJson)) return TeacherTurnKind.AFFIRM
        return TeacherTurnKind.GARBAGE
    }

    /**
     * 이번 턴에 새로 covered되어야 할 개념을 서버가 결정론적으로 계산한다.
     * first_missing을 설명했으면 그 id 하나만, 아니면 빈 목록을 반환한다.
     */
    fun expectedDeltaCovered(
        userText: String,
        accumulatedCovered: List<String>,
        conceptOrder: List<String>,
        unitJson: String,
    ): List<String> {
        val firstMissing = firstMissingId(conceptOrder, accumulatedCovered) ?: return emptyList()
        return if (userTextExplainsConcept(userText, firstMissing, unitJson)) listOf(firstMissing) else emptyList()
    }

    /**
     * 발화가 개념 설명 없는 단순 확인/긍정("네", "그렇지" 등)인지 판정한다.
     * 사전 정의된 긍정어 목록에 정확히 일치하거나,
     * 5자 이하이면서 개념 용어를 전혀 포함하지 않으면 순수 확인으로 본다.
     */
    fun isPureAffirmation(text: String, unitJson: String = ""): Boolean {
        val normalized = text.trim().lowercase()
        if (normalized in PURE_AFFIRMATIONS) return true
        if (
            normalized.length <= SHORT_AFFIRMATION_MAX_LENGTH &&
            !textContainsAnyTerm(text, extractConceptTerms(unitJson))
        ) {
            return true
        }
        return false
    }

    /**
     * 개념 [id]에 대해 speak 유도 질문에 넣을 힌트 키워드를 반환한다.
     * 해당 개념의 첫 용어를 최대 12자로 잘라 쓰며, 용어가 없으면 id를 그대로 반환한다.
     */
    fun hintKeywordFor(id: String, unitJson: String = ""): String {
        val terms = termsForConcept(id, unitJson)
        if (terms.isNotEmpty()) {
            return terms.first().take(HINT_KEYWORD_MAX_LENGTH)
        }
        return id
    }

    private fun collectConceptTerms(concept: JsonNode, includeTerm: (String) -> Boolean): List<String> {
        val terms = mutableListOf<String>()
        appendTextTerms(concept.path("key_points"), terms, includeTerm)
        appendTextTerms(concept.path("keywords"), terms, includeTerm)

        val name = conceptName(concept)
        if (includeTerm(name)) terms.add(name)

        return terms
    }

    private fun appendTextTerms(values: JsonNode, terms: MutableList<String>, includeTerm: (String) -> Boolean) {
        values.forEach { value ->
            val text = value.asText("").trim()
            if (includeTerm(text)) terms.add(text)
        }
    }

    private fun conceptName(concept: JsonNode): String =
        concept.path("name").asText("").ifBlank { concept.path("label").asText("") }.trim()
}
