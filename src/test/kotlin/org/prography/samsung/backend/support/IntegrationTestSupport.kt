package org.prography.samsung.backend.support

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
abstract class IntegrationTestSupport {
    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    protected fun newDeviceId(): String = UUID.randomUUID().toString()

    protected fun bearer(deviceId: String): String = "Bearer $deviceId"

    protected fun get(url: String, deviceId: String): ResultActions = mockMvc.perform(
        MockMvcRequestBuilders.get(url)
            .header("Authorization", bearer(deviceId)),
    )

    protected fun post(url: String, deviceId: String, body: Any? = null): ResultActions = mockMvc.perform(
        MockMvcRequestBuilders.post(url)
            .header("Authorization", bearer(deviceId))
            .contentType(MediaType.APPLICATION_JSON)
            .apply {
                if (body != null) {
                    content(objectMapper.writeValueAsString(body))
                }
            },
    )

    protected fun put(url: String, deviceId: String, body: Any): ResultActions = mockMvc.perform(
        MockMvcRequestBuilders.put(url)
            .header("Authorization", bearer(deviceId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)),
    )

    protected fun expectApiSuccess(
        result: ResultActions,
        httpStatus: Int = 200,
        businessCode: Int = httpStatus,
    ): ResultActions = result
        .andExpect(MockMvcResultMatchers.status().`is`(httpStatus))
        .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(businessCode))
        .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())

    protected fun expectApiFailure(result: ResultActions, httpStatus: Int, businessCode: Int): ResultActions = result
        .andExpect(MockMvcResultMatchers.status().`is`(httpStatus))
        .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(businessCode))
        .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist())

    protected fun completeOnboarding(deviceId: String, curriculumId: Long = FRACTION_CURRICULUM_ID) {
        expectApiSuccess(
            post(
                "/user/onboarding",
                deviceId,
                mapOf("curriculumId" to curriculumId, "step" to 1),
            ),
        )
        expectApiSuccess(
            post(
                "/user/schedule",
                deviceId,
                mapOf(
                    "frequency" to 3,
                    "days" to listOf("TUE", "THU", "SAT"),
                    "time" to "17:00",
                ),
            ),
        )
        expectApiSuccess(
            post(
                "/notifications/register",
                deviceId,
                mapOf("deviceToken" to "test-fcm-token", "platform" to "IOS"),
            ),
        )
        expectApiSuccess(post("/user/onboarding/complete", deviceId))
    }

    protected fun startSession(deviceId: String): String {
        val result =
            expectApiSuccess(
                post("/session/start", deviceId, mapOf("curriculumId" to FRACTION_CURRICULUM_ID)),
                httpStatus = 201,
                businessCode = 201,
            ).andReturn()

        return objectMapper.readTree(result.response.contentAsString)
            .path("data")
            .path("sessionId")
            .asText()
    }

    companion object {
        const val FRACTION_CURRICULUM_ID = 3L
    }
}
