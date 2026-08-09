package org.prography.samsung.backend.conversation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.prography.samsung.backend.conversation.repository.ConversationTurnRepository
import org.prography.samsung.backend.support.IntegrationTestSupport
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@DisplayName("Teach 대화 저장 검증")
class TeachPersistenceTest : IntegrationTestSupport() {

    @Autowired
    private lateinit var conversationTurnRepository: ConversationTurnRepository

    @Test
    @DisplayName("ai_loop 세션에서 teach 1턴 후 conversation_turns에 행이 저장된다")
    fun shouldPersistConversationTurn() {
        val deviceId = newDeviceId()
        completeOnboarding(deviceId)
        val sessionId = startAiLoopSession(deviceId)

        // 저장 전엔 0건
        assertEquals(0, conversationTurnRepository.countBySessionId(sessionId))

        expectApiSuccess(
            post(
                "/session/$sessionId/teach",
                deviceId,
                mapOf("userText" to "분수는 전체를 똑같이 나눈 거 중 일부를 나타내는 수예요"),
            ),
        )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.turn").value(1))

        // 저장 후엔 1건
        assertEquals(1, conversationTurnRepository.countBySessionId(sessionId))

        val turns = conversationTurnRepository.findAllBySessionIdOrderByTurnNumberAsc(sessionId)
        assertEquals(1, turns.size)
        assertEquals(1, turns[0].turnNumber)
        assertTrue(turns[0].userText.contains("분수"))
        assertTrue(turns[0].aiResponseJson.contains("speak"))
        assertTrue(turns[0].aiResponseJson.contains("thinking"))
    }

    private fun startAiLoopSession(deviceId: String): String {
        val result =
            expectApiSuccess(
                post(
                    "/session/start",
                    deviceId,
                    mapOf(
                        "curriculumId" to FRACTION_CURRICULUM_ID,
                        "conversationMode" to "ai_loop",
                    ),
                ),
                httpStatus = 201,
                businessCode = 201,
            ).andReturn()

        return objectMapper.readTree(result.response.contentAsString)
            .path("data")
            .path("sessionId")
            .asText()
    }
}
