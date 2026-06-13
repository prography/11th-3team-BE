package org.prography.samsung.backend.conversation

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.prography.samsung.backend.support.IntegrationTestSupport
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@DisplayName("Teach API 통합 테스트")
class TeachApiIntegrationTest : IntegrationTestSupport() {
    @Test
    @DisplayName("ai_loop 세션에서 teach 1턴 후 JSON 응답과 진행도를 반환한다")
    fun shouldReturnTeachTurnResponseForAiLoopSession() {
        val deviceId = newDeviceId()
        completeOnboarding(deviceId)
        val sessionId = startAiLoopSession(deviceId)

        expectApiSuccess(get("/session/$sessionId/lesson", deviceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.conversationMode").value("ai_loop"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.question.speak").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.question.emotion").value("curious"))

        expectApiSuccess(
            post(
                "/session/$sessionId/teach",
                deviceId,
                mapOf("userText" to "분수는 전체를 똑같이 나눈 거 중 일부를 나타내는 수예요"),
            ),
        )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.turn").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.aiResponse.speak").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.aiResponse.emotion").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.aiResponse.covered").isArray())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.progress.total").value(4))

        expectApiSuccess(get("/session/$sessionId/teach/status", deviceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.turn").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.lastAiResponse.speak").exists())
    }

    @Test
    @DisplayName("static 세션에 teach 호출하면 403을 반환한다")
    fun shouldRejectTeachForStaticSession() {
        val deviceId = newDeviceId()
        completeOnboarding(deviceId)
        val sessionId = startSession(deviceId)

        expectApiFailure(
            post(
                "/session/$sessionId/teach",
                deviceId,
                mapOf("userText" to "분수 설명"),
            ),
            httpStatus = 403,
            businessCode = 40310,
        )
    }

    @Test
    @DisplayName("빈 userText면 400을 반환한다")
    fun shouldRejectEmptyUserText() {
        val deviceId = newDeviceId()
        completeOnboarding(deviceId)
        val sessionId = startAiLoopSession(deviceId)

        expectApiFailure(
            post(
                "/session/$sessionId/teach",
                deviceId,
                mapOf("userText" to "   "),
            ),
            httpStatus = 400,
            businessCode = 40030,
        )
    }

    @Test
    @DisplayName("ai_loop teach 플로우로 session_done까지 진행할 수 있다")
    fun shouldCompleteAiLoopTeachFlow() {
        val deviceId = newDeviceId()
        completeOnboarding(deviceId)
        val sessionId = startAiLoopSession(deviceId)

        val turns =
            listOf(
                "분수는 전체를 똑같이 나눈 거 중 일부를 나타내는 수예요",
                "분모는 아래 숫자로, 전체를 똑같이 나눈 개수예요",
                "분자는 위 숫자로, 가지고 있는 조각 수예요",
                "분수는 크기를 비교할 수 있어요. 같은 분모면 분자가 큰 게 더 커요",
            )

        turns.forEach { userText ->
            expectApiSuccess(
                post("/session/$sessionId/teach", deviceId, mapOf("userText" to userText)),
            )
        }

        expectApiSuccess(get("/session/$sessionId/teach/status", deviceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.sessionDone").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.progress.coveredCount").value(4))
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
