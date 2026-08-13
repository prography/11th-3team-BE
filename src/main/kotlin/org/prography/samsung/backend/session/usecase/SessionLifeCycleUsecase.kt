package org.prography.samsung.backend.session.usecase

import org.prography.samsung.backend.session.dto.request.SessionStartRequest
import org.prography.samsung.backend.session.dto.response.SessionAbortResponse
import org.prography.samsung.backend.session.dto.response.SessionPhaseResponse
import org.prography.samsung.backend.session.dto.response.SessionStartResponse
import org.prography.samsung.backend.session.service.SessionLifecycleService
import org.prography.samsung.backend.user.service.UserProfileService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SessionLifeCycleUsecase(
    private val sessionLifecycleService: SessionLifecycleService,
    private val userProfileService: UserProfileService,
) {
    @Transactional
    fun start(userId: Long, request: SessionStartRequest?): SessionStartResponse {
        val user = userProfileService.getUser(userId)
        val userCurriculum = userProfileService.getUserCurriculum(userId)
        return sessionLifecycleService.start(user, userCurriculum, request)
    }

    @Transactional
    fun advancePhase(userId: Long, sessionId: String): SessionPhaseResponse =
        sessionLifecycleService.advancePhase(userId, sessionId)

    @Transactional
    fun abort(userId: Long, sessionId: String): SessionAbortResponse = sessionLifecycleService.abort(userId, sessionId)
}
