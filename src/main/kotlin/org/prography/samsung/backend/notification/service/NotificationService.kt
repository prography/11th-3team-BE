package org.prography.samsung.backend.notification.service

import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.common.response.ErrorBaseCode
import org.prography.samsung.backend.notification.dto.NotificationRegisterRequest
import org.prography.samsung.backend.notification.dto.NotificationRegisterResponse
import org.prography.samsung.backend.notification.dto.NotificationRescheduleResponse
import org.prography.samsung.backend.notification.entity.DeviceToken
import org.prography.samsung.backend.notification.entity.NotificationSchedule
import org.prography.samsung.backend.notification.repository.DeviceTokenRepository
import org.prography.samsung.backend.notification.repository.NotificationScheduleRepository
import org.prography.samsung.backend.user.repository.UserRepository
import org.prography.samsung.backend.user.repository.UserScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class NotificationService(
    private val userRepository: UserRepository,
    private val userScheduleRepository: UserScheduleRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val notificationScheduleRepository: NotificationScheduleRepository,
) {
    @Transactional
    fun register(userId: Long, request: NotificationRegisterRequest): NotificationRegisterResponse {
        val schedule =
            userScheduleRepository.findWithDaysByUserId(userId)
                ?: throw CustomException(DomainErrorCode.SCHEDULE_NOT_CONFIGURED)

        if (!request.deviceToken.isNullOrBlank()) {
            val user = userRepository.findById(userId).orElseThrow { CustomException(ErrorBaseCode.NOT_FOUND_ENTITY) }
            val existing = deviceTokenRepository.findByUserIdAndToken(userId, request.deviceToken)
            if (existing != null) {
                existing.platform = request.platform
                existing.isActive = true
                existing.updatedAt = Instant.now()
                deviceTokenRepository.save(existing)
            } else {
                deviceTokenRepository.save(
                    DeviceToken(
                        user = user,
                        token = request.deviceToken,
                        platform = request.platform,
                    ),
                )
            }
        }

        rebuildNotificationSchedules(userId, schedule)

        return NotificationRegisterResponse(
            registered = true,
            scheduleCount = schedule.days.size,
        )
    }

    @Transactional
    fun reschedule(userId: Long, request: NotificationRegisterRequest): NotificationRescheduleResponse {
        register(userId, request)
        return NotificationRescheduleResponse(rescheduled = true)
    }

    @Transactional
    fun rescheduleFromUserSchedule(userId: Long) {
        val schedule =
            userScheduleRepository.findWithDaysByUserId(userId) ?: return
        rebuildNotificationSchedules(userId, schedule)
    }

    private fun rebuildNotificationSchedules(
        userId: Long,
        schedule: org.prography.samsung.backend.user.entity.UserSchedule,
    ) {
        val user = userRepository.findById(userId).orElseThrow { CustomException(ErrorBaseCode.NOT_FOUND_ENTITY) }
        notificationScheduleRepository.deleteAllByUserId(userId)
        schedule.days.forEach { day ->
            notificationScheduleRepository.save(
                NotificationSchedule(
                    user = user,
                    dayOfWeek = day.dayOfWeek,
                    notifyTime = schedule.lessonTime,
                    timezone = user.timezone,
                ),
            )
        }
    }
}
