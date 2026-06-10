package org.prography.samsung.backend.user

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.prography.samsung.backend.support.IntegrationTestSupport
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@DisplayName("사용자 API 통합 테스트")
class UserApiIntegrationTest : IntegrationTestSupport() {
    @Test
    @DisplayName("프로필 조회 시 레벨·코인·단원·진척도를 반환한다")
    fun shouldReturnUserProfileAfterOnboarding() {
        val deviceId = newDeviceId()
        completeOnboarding(deviceId)

        expectApiSuccess(get("/user/profile", deviceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.level.number").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.level.name").value("새싹 선생님"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.totalCoins").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.curriculum.id").value(FRACTION_CURRICULUM_ID))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.curriculum.displayName").value("3단원 분수"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.progressPercent").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.homeMessage").exists())
    }

    @Test
    @DisplayName("홈 조회 시 프로필과 세션 상태를 함께 반환한다")
    fun shouldReturnCombinedHomeData() {
        val deviceId = newDeviceId()
        completeOnboarding(deviceId)

        expectApiSuccess(get("/user/home", deviceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.curriculum.name").value("분수의 계산"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.lessonCompletedToday").value(false))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.activeSession").doesNotExist())
    }

    @Test
    @DisplayName("설정 조회 시 현재 단원과 시간표를 반환한다")
    fun shouldReturnCurrentSettings() {
        val deviceId = newDeviceId()
        completeOnboarding(deviceId)

        expectApiSuccess(get("/user/settings", deviceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.curriculum.code").value("FRACTION_CALC"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.schedule.frequency").value(3))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.schedule.days.length()").value(3))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.schedule.time").value("17:00"))
    }

    @Test
    @DisplayName("설정 변경 없이 저장해도 200과 기존 설정을 반환한다")
    fun shouldReturnExistingSettingsWhenNoChanges() {
        val deviceId = newDeviceId()
        completeOnboarding(deviceId)

        expectApiSuccess(put("/user/settings", deviceId, emptyMap<String, Any>()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.schedule.time").value("17:00"))
    }

    @Test
    @DisplayName("진행 중 세션이 있으면 단원 변경 시 409를 반환한다")
    fun shouldReturnConflictWhenChangingCurriculumWithActiveSession() {
        val deviceId = newDeviceId()
        completeOnboarding(deviceId)
        startSession(deviceId)

        expectApiFailure(
            put(
                "/user/settings",
                deviceId,
                mapOf("curriculumId" to 1, "resetProgress" to true),
            ),
            httpStatus = 409,
            businessCode = 40900,
        )
    }

    @Test
    @DisplayName("수업 완료 후 홈 메시지가 완료 상태 문구로 바뀐다")
    fun shouldUpdateHomeMessageAfterLessonCompleted() {
        val deviceId = newDeviceId()
        completeOnboarding(deviceId)
        val sessionId = startSession(deviceId)
        post("/session/$sessionId/advance-phase", deviceId)
        post("/session/complete", deviceId, mapOf("sessionId" to sessionId))

        expectApiSuccess(get("/user/profile", deviceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.homeMessage").value("선생님 덕분에 분수의 계산 마스터! 다음에 또 만나요!"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.progressPercent").value(45))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.totalCoins").value(500))
    }
}
