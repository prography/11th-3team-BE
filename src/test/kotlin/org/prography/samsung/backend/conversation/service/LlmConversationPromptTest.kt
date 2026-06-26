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
            unitJson = "{" + "\"concepts\":[" +
                "{\"id\":\"c1\",\"label\":\"분수는 전체를 똑같이 나눈 것 중 일부를 나타내는 수\"}," +
                "{\"id\":\"c2\",\"label\":\"분모는 전체를 똑같이 나눈 개수(아래 숫자)\"}," +
                "{\"id\":\"c3\",\"label\":\"분자는 가지고 있는 조각의 수(위 숫자)\"}," +
                "{\"id\":\"c4\",\"label\":\"분수는 크기를 비교할 수 있다\"}]" + "}",
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
            unitJson = "{" + "\"concepts\":[" +
                "{\"id\":\"c1\",\"label\":\"분수는 전체를 똑같이 나눈 것 중 일부를 나타내는 수\"}," +
                "{\"id\":\"c2\",\"label\":\"분모는 전체를 똑같이 나눈 개수(아래 숫자)\"}," +
                "{\"id\":\"c3\",\"label\":\"분자는 가지고 있는 조각의 수(위 숫자)\"}," +
                "{\"id\":\"c4\",\"label\":\"분수는 크기를 비교할 수 있다\"}]" + "}",
            previousError = null,
            attempt = 1,
        )

        assertTrue(prompt.contains("유도하는 짧은 질문"), "must direct to produce eliciting questions")
        assertTrue(prompt.contains("힌트 키워드"), "must contain hint guidance")
        // ensure garbage redirect language present
        assertTrue(prompt.contains("주제에서 벗어나거나 애매") || prompt.contains("잘 모르겠어요"), "should handle off-topic redirect")
    }
}
