package org.prography.samsung.backend.auth

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.prography.samsung.backend.support.IntegrationTestSupport
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@DisplayName("기기 UUID 인증 통합 테스트")
class DeviceAuthIntegrationTest : IntegrationTestSupport() {
    @Test
    @DisplayName("Authorization 헤더 없으면 401을 반환한다")
    fun shouldReturnUnauthorizedWhenAuthorizationHeaderMissing() {
        mockMvc.perform(MockMvcRequestBuilders.get("/curriculum"))
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(40110))
    }

    @Test
    @DisplayName("Bearer 형식이 아니면 401을 반환한다")
    fun shouldReturnUnauthorizedWhenBearerFormatInvalid() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/curriculum")
                .header("Authorization", "Token invalid"),
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(40110))
    }

    @Test
    @DisplayName("UUID 형식이 아니면 400을 반환한다")
    fun shouldReturnBadRequestWhenDeviceIdIsNotUuid() {
        expectApiFailure(
            get("/curriculum", "not-a-valid-uuid"),
            httpStatus = 400,
            businessCode = 40000,
        )
    }

    @Test
    @DisplayName("유효한 UUID면 신규 사용자를 생성하고 API를 처리한다")
    fun shouldUpsertUserAndAllowRequestWhenValidUuid() {
        val deviceId = newDeviceId()

        expectApiSuccess(get("/user/onboarding/status", deviceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.completed").value(false))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.step").value(0))
    }

    @Test
    @DisplayName("actuator 엔드포인트는 인증 없이 접근 가능하다")
    fun shouldAllowActuatorWithoutAuth() {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"))
    }
}
