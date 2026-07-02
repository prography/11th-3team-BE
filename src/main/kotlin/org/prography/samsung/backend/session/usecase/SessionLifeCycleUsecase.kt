package org.prography.samsung.backend.session.usecase

import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.session.dto.request.SessionStartRequest
import org.prography.samsung.backend.session.dto.response.SessionAbortResponse
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
        val targetCurriculumId = request?.curriculumId ?: userCurriculum.curriculum.id
        if (userCurriculum.curriculum.id != targetCurriculumId) {
            throw CustomException(DomainErrorCode.CURRICULUM_MISMATCH)
        }
        return sessionLifecycleService.start(user, userCurriculum, request)
    }

    @Transactional
    fun abort(userId: Long, sessionId: String): SessionAbortResponse = sessionLifecycleService.abort(userId, sessionId)
}
