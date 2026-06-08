package org.prography.samsung.backend.user

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.prography.samsung.backend.support.IntegrationTestSupport
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@DisplayName("온보딩 API 통합 테스트")
class OnboardingApiIntegrationTest : IntegrationTestSupport() {
    @Test
    @DisplayName("단원 선택 후 step이 1로 갱신된다")
    fun shouldSaveCurriculumAndUpdateOnboardingStep() {
        val deviceId = newDeviceId()

        expectApiSuccess(
            post(
                "/user/onboarding",
                deviceId,
                mapOf("curriculumId" to FRACTION_CURRICULUM_ID, "step" to 1),
            ),
        )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.curriculumId").value(FRACTION_CURRICULUM_ID))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.step").value(1))

        expectApiSuccess(get("/user/onboarding/status", deviceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.completed").value(false))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.step").value(1))
    }

    @Test
    @DisplayName("요일 개수가 frequency와 다르면 400을 반환한다")
    fun shouldReturnBadRequestWhenScheduleDayCountMismatch() {
        val deviceId = newDeviceId()
        post("/user/onboarding", deviceId, mapOf("curriculumId" to FRACTION_CURRICULUM_ID, "step" to 1))

        expectApiFailure(
            post(
                "/user/schedule",
                deviceId,
                mapOf(
                    "frequency" to 3,
                    "days" to listOf("TUE", "THU"),
                    "time" to "17:00",
                ),
            ),
            httpStatus = 400,
            businessCode = 40010,
        )
    }

    @Test
    @DisplayName("허용되지 않은 수업 시간이면 400을 반환한다")
    fun shouldReturnBadRequestWhenLessonTimeInvalid() {
        val deviceId = newDeviceId()
        post("/user/onboarding", deviceId, mapOf("curriculumId" to FRACTION_CURRICULUM_ID, "step" to 1))

        expectApiFailure(
            post(
                "/user/schedule",
                deviceId,
                mapOf(
                    "frequency" to 3,
                    "days" to listOf("TUE", "THU", "SAT"),
                    "time" to "14:00",
                ),
            ),
            httpStatus = 400,
            businessCode = 40000,
        )
    }

    @Test
    @DisplayName("시간표 없이 온보딩 완료하면 400을 반환한다")
    fun shouldReturnBadRequestWhenCompletingOnboardingWithoutSchedule() {
        val deviceId = newDeviceId()
        post("/user/onboarding", deviceId, mapOf("curriculumId" to FRACTION_CURRICULUM_ID, "step" to 1))

        expectApiFailure(
            post("/user/onboarding/complete", deviceId),
            httpStatus = 400,
            businessCode = 40000,
        )
    }

    @Test
    @DisplayName("온보딩 전체 플로우를 완료하면 completed가 true가 된다")
    fun shouldCompleteFullOnboardingFlow() {
        val deviceId = newDeviceId()

        completeOnboarding(deviceId)

        expectApiSuccess(get("/user/onboarding/status", deviceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.completed").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.step").value(2))
    }
}
