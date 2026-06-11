package org.prography.samsung.backend.user.service

import org.prography.samsung.backend.common.domain.SessionStatus
import org.prography.samsung.backend.common.dto.CurriculumChipResponse
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.common.response.ErrorBaseCode
import org.prography.samsung.backend.curriculum.service.CurriculumService
import org.prography.samsung.backend.notification.service.NotificationService
import org.prography.samsung.backend.session.repository.TutoringSessionRepository
import org.prography.samsung.backend.user.dto.UserScheduleRequest
import org.prography.samsung.backend.user.dto.UserSettingsRequest
import org.prography.samsung.backend.user.dto.UserSettingsResponse
import org.prography.samsung.backend.user.entity.UserCurriculum
import org.prography.samsung.backend.user.repository.UserCurriculumRepository
import org.prography.samsung.backend.user.repository.UserRepository
import org.prography.samsung.backend.user.repository.UserScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserSettingsService(
    private val userRepository: UserRepository,
    private val userCurriculumRepository: UserCurriculumRepository,
    private val userScheduleRepository: UserScheduleRepository,
    private val tutoringSessionRepository: TutoringSessionRepository,
    private val curriculumService: CurriculumService,
    private val onboardingService: OnboardingService,
    private val notificationService: NotificationService,
) {
    @Transactional(readOnly = true)
    fun getSettings(userId: Long): UserSettingsResponse {
        val userCurriculum =
            userCurriculumRepository.findById(userId).orElseThrow {
                CustomException(DomainErrorCode.CURRICULUM_NOT_SELECTED)
            }
        val schedule =
            userScheduleRepository.findWithDaysByUserId(userId)
                ?: throw CustomException(DomainErrorCode.SCHEDULE_NOT_CONFIGURED)

        return UserSettingsResponse(
            curriculum =
            CurriculumChipResponse(
                id = userCurriculum.curriculum.id,
                code = userCurriculum.curriculum.code,
                name = userCurriculum.curriculum.name,
                displayOrder = userCurriculum.curriculum.displayOrder,
            ),
            schedule = onboardingService.toScheduleResponse(schedule),
        )
    }

    @Transactional
    fun updateSettings(userId: Long, request: UserSettingsRequest): UserSettingsResponse {
        val current = getSettings(userId)
        var changed = false

        if (request.curriculumId != null && request.curriculumId != current.curriculum.id) {
            if (tutoringSessionRepository.findByUserIdAndStatus(userId, SessionStatus.STARTED) != null) {
                throw CustomException(DomainErrorCode.ACTIVE_SESSION_EXISTS)
            }
            val curriculum = curriculumService.getActiveCurriculumOrThrow(request.curriculumId)
            val user = userRepository.findById(userId).orElseThrow { CustomException(ErrorBaseCode.NOT_FOUND_ENTITY) }
            val userCurriculum =
                userCurriculumRepository.findById(userId).orElse(
                    UserCurriculum(user = user, curriculum = curriculum),
                )
            userCurriculum.changeCurriculum(curriculum)
            if (request.resetProgress) {
                userCurriculum.resetProgress()
            }
            userCurriculumRepository.save(userCurriculum)
            changed = true
        }

        val frequency = request.frequency ?: current.schedule.frequency
        val days = request.days ?: current.schedule.days
        val time = request.time ?: current.schedule.time
        val scheduleChanged =
            request.frequency != null || request.days != null || request.time != null

        if (scheduleChanged) {
            onboardingService.saveSchedule(
                userId,
                UserScheduleRequest(
                    frequency = frequency,
                    days = days,
                    time = time,
                ),
            )
            notificationService.rescheduleFromUserSchedule(userId)
            changed = true
        }

        return if (changed) getSettings(userId) else current
    }
}
