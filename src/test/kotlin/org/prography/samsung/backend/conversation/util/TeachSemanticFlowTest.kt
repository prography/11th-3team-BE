package org.prography.samsung.backend.conversation.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.prography.samsung.backend.common.domain.AiEmotion
import org.prography.samsung.backend.conversation.dto.response.AiTurnResponse
import org.prography.samsung.backend.conversation.enums.TeacherTurnKind
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DisplayName("Teach semantic flow — curriculum-agnostic classify, guard delta, advance")
class TeachSemanticFlowTest {
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val teacherTurnClassifier = TeacherTurnClassifier(objectMapper)
    private val validator = AiResponseValidator(objectMapper, teacherTurnClassifier)
    private val guard = TeachProgressGuard(validator, teacherTurnClassifier)

    private val heritageJson =
        java.io.File("docs/curriculum/단원JSON_초등4학년사회_지역의문화유산.json").readText()

    private val conceptOrder = validator.parseConceptIdOrder(heritageJson)

    @Test
    fun classifyTeacherTurn_affirm_with_social_curriculum() {
        assertEquals(
            TeacherTurnKind.AFFIRM,
            teacherTurnClassifier.classifyTeacherTurn("그렇지", emptyList(), conceptOrder, heritageJson),
        )
        assertEquals(
            TeacherTurnKind.AFFIRM,
            teacherTurnClassifier.classifyTeacherTurn("네", listOf("c1"), conceptOrder, heritageJson),
        )
    }

    @Test
    fun classifyTeacherTurn_explain_advances_exactly_one_from_key_point() {
        assertEquals(
            TeacherTurnKind.EXPLAIN,
            teacherTurnClassifier.classifyTeacherTurn("조상들이 물려준 소중한 것", emptyList(), conceptOrder, heritageJson),
        )
        assertEquals(
            listOf("c1"),
            teacherTurnClassifier.expectedDeltaCovered("조상들이 물려준 소중한 것", emptyList(), conceptOrder, heritageJson),
        )
        assertEquals(
            listOf("c2"),
            teacherTurnClassifier.expectedDeltaCovered("직접 찾아가는 답사", listOf("c1"), conceptOrder, heritageJson),
        )
    }

    @Test
    fun teachProgressGuard_returns_delta_covered_not_accumulated() {
        val raw =
            AiTurnResponse(
                speak = "선생님, 직접 찾아가는 답사는 어떻게 하나요?",
                emotion = AiEmotion.AHA,
                covered = listOf("c1"),
                missing = listOf("c2", "c3", "c4"),
                misconceptionsDetected = emptyList(),
                correctionStage = 0,
                focusConcept = "c2",
                sessionDone = false,
            )
        val normalized =
            guard.normalize(
                userText = "조상들이 물려준 소중한 것",
                accumulatedCovered = emptyList(),
                conceptOrder = conceptOrder,
                repeatedFocusCount = 0,
                raw = raw,
                unitJson = heritageJson,
            )
        assertEquals(listOf("c1"), normalized.covered, "covered must be this-turn delta only")
        assertEquals(listOf("c2", "c3", "c4"), normalized.missing)
        assertEquals("c2", normalized.focusConcept)
    }

    @Test
    fun teachProgressGuard_affirm_strips_covered_to_empty() {
        val raw =
            AiTurnResponse(
                speak = "선생님, 조상들이 물려준 소중한 것은 어떻게 설명하시나요?",
                emotion = AiEmotion.CURIOUS,
                covered = listOf("c1"),
                missing = listOf("c1", "c2", "c3", "c4"),
                misconceptionsDetected = emptyList(),
                correctionStage = 0,
                focusConcept = "c1",
                sessionDone = false,
            )
        val normalized =
            guard.normalize(
                userText = "그렇지",
                accumulatedCovered = emptyList(),
                conceptOrder = conceptOrder,
                repeatedFocusCount = 0,
                raw = raw,
                unitJson = heritageJson,
            )
        assertEquals(emptyList(), normalized.covered)
        assertEquals("c1", normalized.focusConcept)
    }

    @Test
    fun parseAndValidate_plus_semantic_passes_for_social_explain_turn() {
        val raw =
            """
            {"speak":"선생님, 직접 찾아가는 답사는 어떻게 하나요?","emotion":"aha","covered":["c1"],
            "missing":["c2","c3","c4"],"misconceptions_detected":[],"correction_stage":0,
            "focus_concept":"c2","session_done":false}
            """.trimIndent()
        val parsed = validator.parseAndValidate(raw, conceptOrder)
        val semantic =
            validator.validateSemanticRules(
                parsed,
                accumulatedCovered = emptyList(),
                conceptOrder = conceptOrder,
                currentUserText = "조상들이 물려준 소중한 것",
                unitJson = heritageJson,
            )
        assertNull(semantic)
        assertEquals(listOf("c1"), parsed.covered)
    }

    @Test
    fun hintKeywordFor_uses_social_key_points_not_fraction_fallback() {
        val kw = teacherTurnClassifier.hintKeywordFor("c1", heritageJson)
        assertTrue(kw.contains("조상") || kw.contains("물려"), "must use social curriculum term, got: $kw")
        assertEquals(false, kw == "분모" || kw == "분자")
    }
}
