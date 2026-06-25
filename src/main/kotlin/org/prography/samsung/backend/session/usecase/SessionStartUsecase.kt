package org.prography.samsung.backend.session.usecase

import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.curriculum.service.CurriculumService
import org.prography.samsung.backend.session.dto.request.SessionStartRequest
import org.prography.samsung.backend.session.dto.response.SessionStartResponse
import org.prography.samsung.backend.session.service.SessionService
import org.prography.samsung.backend.user.service.UserProfileService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SessionStartUsecase(
    private val sessionService: SessionService,
    private val curriculumService: CurriculumService,
    private val userProfileService: UserProfileService,
) {
    @Transactional
    fun start(userId: Long, request: SessionStartRequest?): SessionStartResponse {
        val userCurriculum = userProfileService.getUserCurriculum(userId)
        val targetCurriculumId = request?.curriculumId ?: userCurriculum.curriculum.id
        if (userCurriculum.curriculum.id != targetCurriculumId) {
            throw CustomException(DomainErrorCode.CURRICULUM_MISMATCH)
        }
        val topics = curriculumService.getLessonTopics(targetCurriculumId)
        return sessionService.start(userId, request, topics)
    }
}
