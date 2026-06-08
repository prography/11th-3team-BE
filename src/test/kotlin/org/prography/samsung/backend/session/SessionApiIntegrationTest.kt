package org.prography.samsung.backend.session

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.prography.samsung.backend.support.IntegrationTestSupport
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@DisplayName("세션 API 통합 테스트")
class SessionApiIntegrationTest : IntegrationTestSupport() {
    @Test
    @DisplayName("온보딩 완료 전 today 조회하면 400을 반환한다")
    fun shouldReturnBadRequestWhenTodayRequestedBeforeCurriculumSelected() {
        val deviceId = newDeviceId()

        expectApiFailure(get("/session/today", deviceId), httpStatus = 400, businessCode = 40000)
    }

    @Test
    @DisplayName("오늘 수업 준비 화면에 토픽 2건과 sessionTitle을 반환한다")
    fun shouldReturnTodaySessionWithTwoTopics() {
        val deviceId = newDeviceId()
        completeOnboarding(deviceId)

        expectApiSuccess(get("/session/today", deviceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.curriculumId").value(FRACTION_CURRICULUM_ID))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.sessionTitle").value("분수의 세계"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.topics.length()").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.topics[0].sequence").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.topics[0].title").value("분수란?"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.topics[1].topicType").value("CALCULATION"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.activeSession").doesNotExist())
    }

    @Test
    @DisplayName("세션 시작 후 재호출하면 resumed true로 기존 세션을 반환한다")
    fun shouldResumeExistingSessionWhenStartCalledAgain() {
        val deviceId = newDeviceId()
        completeOnboarding(deviceId)

        expectApiSuccess(
            post("/session/start", deviceId),
            httpStatus = 201,
            businessCode = 201,
        )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.resumed").value(false))

        expectApiSuccess(
            post("/session/start", deviceId),
            httpStatus = 201,
            businessCode = 201,
        )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.resumed").value(true))
    }

    @Test
    @DisplayName("수업 전체 플로우를 완료하면 코인·진척도·기록이 갱신된다")
    fun shouldCompleteFullLessonFlowAndUpdateRewards() {
        val deviceId = newDeviceId()
        completeOnboarding(deviceId)
        val sessionId = startSession(deviceId)

        expectApiSuccess(get("/session/$sessionId/lesson", deviceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.currentPhase").value("INTRO"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.topicLabel").value("3. 분수의 개념"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.question.bubbleText").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.hintNote.sections[0].highlight").value(false))

        expectApiSuccess(post("/session/$sessionId/advance-phase", deviceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.currentPhase").value("REACTION"))

        expectApiSuccess(get("/session/$sessionId/reaction", deviceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.currentPhase").value("REACTION"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.question.displayAnswerHtml").value("3/10"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.hintNote.sections[0].highlight").value(true))

        expectApiSuccess(
            post("/session/complete", deviceId, mapOf("sessionId" to sessionId)),
        )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.coinsAwarded").value(500))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.progressPercent").value(45))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.totalCoins").value(500))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.badgeLevelUp").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.newLevel.number").value(2))

        expectApiSuccess(get("/session/$sessionId/reward", deviceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.coinsAwarded").value(500))

        expectApiSuccess(post("/session/$sessionId/reward/ack", deviceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.acknowledged").value(true))

        expectApiSuccess(get("/session/status", deviceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.lessonCompletedToday").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.pendingRewardSessionId").doesNotExist())

        expectApiSuccess(get("/sessions/history", deviceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.sessions.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.sessions[0].coins").value(500))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.sessions[0].badgeLevelUp").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.hasMore").value(false))
    }

    @Test
    @DisplayName("complete를 중복 호출해도 멱등하게 동일 보상을 반환한다")
    fun shouldReturnSameRewardWhenCompleteCalledTwice() {
        val deviceId = newDeviceId()
        completeOnboarding(deviceId)
        val sessionId = startSession(deviceId)
        post("/session/$sessionId/advance-phase", deviceId)

        expectApiSuccess(post("/session/complete", deviceId, mapOf("sessionId" to sessionId)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.totalCoins").value(500))

        expectApiSuccess(post("/session/complete", deviceId, mapOf("sessionId" to sessionId)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.totalCoins").value(500))
    }

    @Test
    @DisplayName("INTRO 단계에서 reaction 조회하면 403을 반환한다")
    fun shouldReturnForbiddenWhenReactionRequestedInIntroPhase() {
        val deviceId = newDeviceId()
        completeOnboarding(deviceId)
        val sessionId = startSession(deviceId)

        expectApiFailure(
            get("/session/$sessionId/reaction", deviceId),
            httpStatus = 403,
            businessCode = 40300,
        )
    }

    @Test
    @DisplayName("세션 abort 후 STARTED 세션이 없어진다")
    fun shouldAbortSessionAndClearActiveSession() {
        val deviceId = newDeviceId()
        completeOnboarding(deviceId)
        val sessionId = startSession(deviceId)

        expectApiSuccess(post("/session/$sessionId/abort", deviceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("ABORTED"))

        expectApiSuccess(get("/session/status", deviceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.activeSession").doesNotExist())
    }

    @Test
    @DisplayName("완료 전 reward 조회하면 409를 반환한다")
    fun shouldReturnConflictWhenRewardRequestedBeforeCompletion() {
        val deviceId = newDeviceId()
        completeOnboarding(deviceId)
        val sessionId = startSession(deviceId)

        expectApiFailure(
            get("/session/$sessionId/reward", deviceId),
            httpStatus = 409,
            businessCode = 40900,
        )
    }
}
