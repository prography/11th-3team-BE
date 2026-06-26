package org.prography.samsung.backend.conversation.service

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.prography.samsung.backend.conversation.client.LlmClient
import org.prography.samsung.backend.conversation.config.ConversationLlmProperties
import kotlin.test.assertTrue

/**
 * Direct prompt builder test (same package so internal fun is visible).
 * Verifies critical instruction text for AC4 (no LLM call).
 */
@DisplayName("LlmConversationService prompt string checks (buildUserPrompt)")
class LlmConversationPromptTest {

    private val mockLlmClient = Mockito.mock(LlmClient::class.java)
    private val properties = ConversationLlmProperties()
    private val validator = AiResponseValidator(com.fasterxml.jackson.databind.ObjectMapper())
    private val mockGuard = Mockito.mock(TeachProgressGuard::class.java)

    private val service = LlmConversationService(mockLlmClient, properties, validator, mockGuard)

    @Test
    fun buildUserPrompt_forConfirmation_mustContainStrictCoveredRules_andConfirmationFewShot_andHintElicitDirective() {
        val conceptOrder = listOf("c1", "c2", "c3", "c4")
        val userText = "그렇지" // short affirmation case from log

        val prompt = service.buildUserPrompt(
            previousTurns = emptyList(),
            userText = userText,
            accumulatedCovered = emptyList(),
            conceptOrder = conceptOrder,
            previousError = null,
            attempt = 1,
        )

        println("=== PROMPT START ===\n$prompt\n=== PROMPT END ===")

        // AC4 literals required (match current prompt text)
        assertTrue(prompt.contains("covered 판단 기준"), "must contain covered judgment section header")
        assertTrue(
            prompt.contains("절대") && prompt.contains("covered에 아무것도 추가하지 마세요"),
            "must contain '절대 covered에 아무것도 추가하지 마세요' per verification",
        )
        assertTrue(prompt.contains("힌트 키워드"), "must contain hint keyword guidance")
        assertTrue(prompt.contains("힌트 내용을"), "must contain '힌트 내용을 ... 설명하게 만드는 질문' per verification")
        assertTrue(prompt.contains("\"covered\":[]"), "must contain confirmation few-shot with covered:[]")
        assertTrue(prompt.contains("단답형"), "should discourage pure short answers")
        assertTrue(prompt.contains("focus_concept"), "focus must be present")
    }

    @Test
    fun buildUserPrompt_mustInstructQuestionsToElicitTeacherHintText() {
        val prompt = service.buildUserPrompt(
            previousTurns = emptyList(),
            userText = "네",
            accumulatedCovered = listOf("c1"),
            conceptOrder = listOf("c1", "c2", "c3", "c4"),
            previousError = null,
            attempt = 1,
        )

        assertTrue(prompt.contains("유도하는 짧은 질문"), "must direct to produce eliciting questions")
        assertTrue(prompt.contains("힌트 키워드"), "must contain hint guidance")
        // ensure garbage redirect language present
        assertTrue(prompt.contains("주제에서 벗어나거나 애매") || prompt.contains("잘 모르겠어요"), "should handle off-topic redirect")
    }

    @Test
    fun buildUserPrompt_for_diverse_curricula_from_docs_should_produce_general_prompts_and_handle_affirms() {
        val curricula = listOf(
            "docs/curriculum/단원JSON_초등4학년사회_공공기관과주민참여.json",
            "docs/curriculum/단원JSON_초등4학년사회_사회변화와문화다양성.json",
            "docs/curriculum/단원JSON_초등4학년사회_생산과교환.json",
            "docs/curriculum/단원JSON_초등4학년사회_지역의문화유산.json",
        )

        val mapper = com.fasterxml.jackson.databind.ObjectMapper()
        curricula.forEach { path ->
            val json = java.io.File(path).readText()
            val root = mapper.readTree(json)
            val concepts = root.path("concepts")
            val conceptOrder = concepts.map { it.path("id").asText() }
            val title = root.path("title").asText(path)
            val sampleExplain = concepts.firstOrNull()?.path("key_points")?.firstOrNull()?.asText()
                ?: concepts.firstOrNull()?.path("description")?.asText()
                ?: "개념 설명 예시"
            val affirmTexts = listOf("네", "그렇지", "알겠어요", "좋아요")

            // test affirm - should have strict no covered rule in prompt
            val affirmPrompt = service.buildUserPrompt(
                previousTurns = emptyList(),
                userText = affirmTexts.random(),
                accumulatedCovered = emptyList(),
                conceptOrder = conceptOrder,
                previousError = null,
                attempt = 1,
            )
            println("=== DIVERSE CURRICULUM TEST: $title ===")
            println("CONCEPTS: $conceptOrder")
            println(
                "AFFIRM userText='${affirmTexts.random()}' -> has strict no-covered rule: ${affirmPrompt.contains(
                    "절대",
                ) &&
                    affirmPrompt.contains("covered에 아무것도 추가하지 마세요")}",
            )
            println(
                "AFFIRM has '선생님,' open elicit style in rules: ${affirmPrompt.contains(
                    "선생님,",
                ) ||
                    affirmPrompt.contains("어떻게 설명")}",
            )
            assertTrue(
                affirmPrompt.contains("단답형") || affirmPrompt.contains("절대") && affirmPrompt.contains("covered"),
                "must keep strict affirm covered rule",
            )

            // test explain from the curriculum content
            val explainPrompt = service.buildUserPrompt(
                previousTurns = emptyList(),
                userText = sampleExplain.take(80),
                accumulatedCovered = emptyList(),
                conceptOrder = conceptOrder,
                previousError = null,
                attempt = 1,
            )
            println(
                "EXPLAIN userText sample from key_point -> prompt has first concept: ${explainPrompt.contains(
                    conceptOrder.first(),
                )}",
            )
            println("=== END DIVERSE CURRICULUM TEST ===\n")
        }
    }
}
