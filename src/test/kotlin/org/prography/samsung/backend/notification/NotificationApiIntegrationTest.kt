package org.prography.samsung.backend.notification

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.prography.samsung.backend.support.IntegrationTestSupport
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@DisplayName("알림 API 통합 테스트")
class NotificationApiIntegrationTest : IntegrationTestSupport() {
    @Test
    @DisplayName("시간표 없이 알림 등록하면 400을 반환한다")
    fun shouldReturnBadRequestWhenRegisteringNotificationWithoutSchedule() {
        val deviceId = newDeviceId()

        expectApiFailure(
            post(
                "/notifications/register",
                deviceId,
                mapOf("deviceToken" to "fcm-token", "platform" to "ANDROID"),
            ),
            httpStatus = 400,
            businessCode = 40000,
        )
    }

    @Test
    @DisplayName("시간표 저장 후 알림을 등록하면 scheduleCount를 반환한다")
    fun shouldRegisterNotificationAfterScheduleSaved() {
        val deviceId = newDeviceId()
        post("/user/onboarding", deviceId, mapOf("curriculumId" to FRACTION_CURRICULUM_ID, "step" to 1))
        post(
            "/user/schedule",
            deviceId,
            mapOf(
                "frequency" to 3,
                "days" to listOf("TUE", "THU", "SAT"),
                "time" to "17:00",
            ),
        )

        expectApiSuccess(
            post(
                "/notifications/register",
                deviceId,
                mapOf("deviceToken" to "fcm-token", "platform" to "IOS"),
            ),
        )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.registered").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.scheduleCount").value(3))
    }

    @Test
    @DisplayName("설정에서 시간표 변경 후 reschedule이 성공한다")
    fun shouldRescheduleNotificationAfterSettingsUpdate() {
        val deviceId = newDeviceId()
        completeOnboarding(deviceId)

        expectApiSuccess(
            put(
                "/user/settings",
                deviceId,
                mapOf(
                    "frequency" to 2,
                    "days" to listOf("MON", "WED"),
                    "time" to "18:00",
                ),
            ),
        )

        expectApiSuccess(
            post(
                "/notifications/reschedule",
                deviceId,
                mapOf("deviceToken" to "fcm-token-updated", "platform" to "ANDROID"),
            ),
        )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.rescheduled").value(true))
    }
}
