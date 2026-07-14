package org.prography.samsung.backend.conversation.prompt

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.prography.samsung.backend.conversation.config.ConversationLlmProperties
import org.prography.samsung.backend.conversation.entity.ConversationTurn
import org.prography.samsung.backend.conversation.entity.CurriculumUnit
import org.prography.samsung.backend.conversation.util.AiResponseValidator
import org.prography.samsung.backend.conversation.util.TeacherTurnClassifier
import org.prography.samsung.backend.curriculum.entity.Curriculum
import org.prography.samsung.backend.support.TestFixtures
import java.io.File
import java.security.MessageDigest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Direct prompt builder test (same package so internal fun is visible).
 * Verifies critical instruction text for AC4 (no LLM call).
 */
@DisplayName("TeachPromptBuilder prompt string checks (buildUserPrompt)")
class TeachPromptBuilderTest {

    private val properties = ConversationLlmProperties()
    private val objectMapper = ObjectMapper()
    private val teacherTurnClassifier = TeacherTurnClassifier(objectMapper)
    private val validator = AiResponseValidator(objectMapper, teacherTurnClassifier)

    private val builder = TeachPromptBuilder(properties, validator)

    private val generalSystemTemplate =
        """
        당신은 초등학생 AI 학생입니다. 선생님(유저)의 설명을 듣고 자연스럽게 반응합니다.

        ## 수업 개념
        {{lesson_concepts}}

        ## 응답 규칙
        1. 반드시 아래 JSON 스키마만 출력하세요.
        """.trimIndent()

    private fun assertPromptIsGeneral(prompt: String, curriculumSpecificPhrases: List<String> = emptyList()) {
        assertFalse(prompt.contains("unit_json"), "must not reference unit_json")
        assertFalse(prompt.contains("unitJson"), "must not reference unitJson")
        assertFalse(prompt.contains("{{unit_json}}"), "must not contain template placeholder")
        assertFalse(prompt.contains("\"unit_id\""), "must not contain raw JSON unit_id")
        assertFalse(prompt.contains("\"concepts\""), "must not contain raw JSON concepts key")
        curriculumSpecificPhrases.forEach { phrase ->
            assertFalse(
                prompt.contains(phrase),
                "curriculum-specific phrase '$phrase' must not appear in general instruction sections",
            )
        }
    }

    @Test
    fun buildUserPrompt_forConfirmation_mustContainStrictCoveredRules_andConfirmationFewShot_andHintElicitDirective() {
        val conceptOrder = listOf("c1", "c2", "c3", "c4")
        val userText = "그렇지"

        val prompt = builder.buildUserPrompt(
            previousTurns = emptyList(),
            userText = userText,
            accumulatedCovered = emptyList(),
            conceptOrder = conceptOrder,
            previousError = null,
            attempt = 1,
        )

        assertPromptIsGeneral(prompt)
        assertTrue(prompt.contains("covered 판단 기준"), "must contain covered judgment section header")
        assertTrue(
            prompt.contains("절대") && prompt.contains("covered에 아무것도 추가하지 마세요"),
            "must contain strict no-covered rule for affirms",
        )
        assertTrue(prompt.contains("힌트 키워드"), "must contain hint keyword guidance")
        assertTrue(prompt.contains("힌트 내용을"), "must contain elicitation directive")
        assertTrue(prompt.contains("\"covered\":[]"), "must contain confirmation few-shot with covered:[]")
        assertTrue(prompt.contains("단답형"), "should discourage pure short answers")
        assertTrue(prompt.contains("focus_concept"), "focus must be present")
        assertTrue(prompt.contains("시스템 프롬프트"), "must reference system prompt for lesson concepts")
    }

    @Test
    fun buildUserPrompt_mustInstructQuestionsToElicitTeacherHintText() {
        val prompt = builder.buildUserPrompt(
            previousTurns = emptyList(),
            userText = "네",
            accumulatedCovered = listOf("c1"),
            conceptOrder = listOf("c1", "c2", "c3", "c4"),
            previousError = null,
            attempt = 1,
        )

        assertPromptIsGeneral(prompt)
        assertTrue(prompt.contains("유도하는 짧은 질문"), "must direct to produce eliciting questions")
        assertTrue(prompt.contains("힌트 키워드"), "must contain hint guidance")
        assertTrue(
            prompt.contains("주제에서 벗어나거나 애매") || prompt.contains("잘 모르겠어요"),
            "should handle off-topic redirect",
        )
    }

    @Test
    fun promptOutputs_shouldMatchGoldenHashes_forSystemAndUserRetryContext() {
        val curriculum = Curriculum(
            code = "GOLDEN",
            name = "golden",
            chapterLabel = "chapter",
            sessionTitleTemplate = "title",
            displayOrder = 1,
        )
        val unit = CurriculumUnit(
            unitId = "golden_unit",
            curriculum = curriculum,
            unitJson =
            """
                {
                  "concepts": [
                    {
                      "id": "c1",
                      "name": "주민 참여",
                      "key_points": ["주민은 의견을 낼 수 있다"],
                      "description": "주민이 지역 문제 해결에 참여하는 방법"
                    },
                    {
                      "id": "c2",
                      "label": "공공기관",
                      "keywords": ["공공 서비스"]
                    }
                  ]
                }
            """.trimIndent(),
            systemPromptTemplate = generalSystemTemplate,
        )
        val previousTurn = ConversationTurn(
            session = TestFixtures.tutoringSession(),
            turnNumber = 1,
            userText = "주민은 의견을 낼 수 있어요.",
            aiResponseJson =
            """{"speak":"지난 설명을 기억해요.","emotion":"thoughtful","covered":["c1"],"missing":["c2"],"misconceptions_detected":[],"correction_stage":0,"focus_concept":"c2","session_done":false}""",
        )

        val systemPrompt = builder.buildSystemPrompt(unit)
        val userPrompt = builder.buildUserPrompt(
            previousTurns = listOf(previousTurn),
            userText = "공공 서비스가 필요해요.",
            accumulatedCovered = listOf("c1"),
            conceptOrder = listOf("c1", "c2"),
            previousError = "focus_concept가 missing에 속하지 않습니다.",
            attempt = 2,
        )

        assertEquals(
            "640a8062796af551252dcbf4932a94096c8e8a8afaf03b6620451fb40ddbb644",
            sha256(systemPrompt),
        )
        assertEquals(
            "b45b5cb262269a147532bdafe46fe5b2bcaf4c14a72e42a7f7739e2fa4b466b8",
            sha256(userPrompt),
        )
    }

    @Test
    fun buildSystemPrompt_for_diverse_curricula_should_format_lesson_concepts_not_raw_json() {
        val curricula = listOf(
            "docs/curriculum/단원JSON_초등4학년사회_공공기관과주민참여.json",
            "docs/curriculum/단원JSON_초등4학년사회_사회변화와문화다양성.json",
            "docs/curriculum/단원JSON_초등4학년사회_생산과교환.json",
            "docs/curriculum/단원JSON_초등4학년사회_지역의문화유산.json",
        )
        val curriculum = Curriculum(
            code = "TEST",
            name = "test",
            chapterLabel = "ch",
            sessionTitleTemplate = "t",
            displayOrder = 1,
        )

        curricula.forEach { path ->
            val unitJson = File(path).readText()
            val unit = CurriculumUnit(
                unitId = "test_unit",
                curriculum = curriculum,
                unitJson = unitJson,
                systemPromptTemplate = generalSystemTemplate,
            )
            val systemPrompt = builder.buildSystemPrompt(unit)

            assertFalse(systemPrompt.contains("{{lesson_concepts}}"), "placeholder must be replaced")
            assertFalse(systemPrompt.contains("\"unit_id\""), "must not paste raw JSON")
            assertFalse(systemPrompt.contains("unit_json"), "must not reference unit_json")
            assertFalse(systemPrompt.contains("{{unit_json}}"), "must not retain unit_json placeholder")
            assertTrue(systemPrompt.contains("## 수업 개념"), "must have lesson concepts section")
            assertTrue(systemPrompt.contains("[c1]"), "must contain formatted concept id")
        }
    }

    @Test
    fun buildUserPrompt_for_diverse_curricula_from_docs_should_produce_general_prompts_and_handle_affirms() {
        val curricula = listOf(
            "docs/curriculum/단원JSON_초등4학년사회_공공기관과주민참여.json",
            "docs/curriculum/단원JSON_초등4학년사회_사회변화와문화다양성.json",
            "docs/curriculum/단원JSON_초등4학년사회_생산과교환.json",
            "docs/curriculum/단원JSON_초등4학년사회_지역의문화유산.json",
        )

        val mapper = ObjectMapper()
        curricula.forEach { path ->
            val json = File(path).readText()
            val root = mapper.readTree(json)
            val concepts = root.path("concepts")
            val conceptOrder = concepts.map { it.path("id").asText() }
            val title = root.path("title").asText(path)
            val keyPointPhrases =
                concepts.flatMap { c ->
                    val kps = c.path("key_points")
                    if (kps.isArray) kps.map { it.asText() } else emptyList()
                }.filter { it.isNotBlank() }

            val affirmPrompt = builder.buildUserPrompt(
                previousTurns = emptyList(),
                userText = "그렇지",
                accumulatedCovered = emptyList(),
                conceptOrder = conceptOrder,
                previousError = null,
                attempt = 1,
            )

            assertPromptIsGeneral(affirmPrompt, keyPointPhrases)
            assertTrue(
                affirmPrompt.contains("단답형") || affirmPrompt.contains("절대") && affirmPrompt.contains("covered"),
                "[$title] must keep strict affirm covered rule",
            )
            assertTrue(
                affirmPrompt.contains("선생님,") || affirmPrompt.contains("어떻게 설명"),
                "[$title] must instruct '선생님,' elicitation style",
            )

            val sampleExplain = keyPointPhrases.firstOrNull() ?: "개념 설명 예시"
            val explainPrompt = builder.buildUserPrompt(
                previousTurns = emptyList(),
                userText = sampleExplain.take(80),
                accumulatedCovered = emptyList(),
                conceptOrder = conceptOrder,
                previousError = null,
                attempt = 1,
            )
            // userText may contain key_point phrases by design; only check structural generality
            assertPromptIsGeneral(explainPrompt)
            assertTrue(
                explainPrompt.contains(conceptOrder.first()),
                "[$title] user prompt must list concept ids",
            )
        }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
