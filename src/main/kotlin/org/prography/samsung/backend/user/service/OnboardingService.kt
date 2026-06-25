package org.prography.samsung.backend.user.service

import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.common.util.KstDateTimeUtils
import org.prography.samsung.backend.common.util.ScheduleValidator
import org.prography.samsung.backend.curriculum.service.CurriculumService
import org.prography.samsung.backend.user.dto.request.OnboardingRequest
import org.prography.samsung.backend.user.dto.request.UserScheduleRequest
import org.prography.samsung.backend.user.dto.response.OnboardingCompleteResponse
import org.prography.samsung.backend.user.dto.response.OnboardingResponse
import org.prography.samsung.backend.user.dto.response.OnboardingStatusResponse
import org.prography.samsung.backend.user.dto.response.UserScheduleResponse
import org.prography.samsung.backend.user.entity.UserCurriculum
import org.prography.samsung.backend.user.entity.UserSchedule
import org.prography.samsung.backend.user.entity.UserScheduleDay
import org.prography.samsung.backend.user.repository.UserCurriculumRepository
import org.prography.samsung.backend.user.repository.UserProfileRepository
import org.prography.samsung.backend.user.repository.UserRepository
import org.prography.samsung.backend.user.repository.UserScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Service
class OnboardingService(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
    private val userCurriculumRepository: UserCurriculumRepository,
    private val userScheduleRepository: UserScheduleRepository,
    private val curriculumService: CurriculumService,
) {
    @Transactional(readOnly = true)
    fun getStatus(userId: Long): OnboardingStatusResponse {
        val profile =
            userProfileRepository.findById(userId).orElseThrow {
                CustomException(DomainErrorCode.USER_NOT_FOUND)
            }
        return OnboardingStatusResponse(
            completed = profile.onboardingCompleted,
            step = profile.onboardingStep,
        )
    }

    @Transactional
    fun saveCurriculum(userId: Long, request: OnboardingRequest): OnboardingResponse {
        val user = userRepository.findById(userId).orElseThrow {
            CustomException(DomainErrorCode.USER_NOT_FOUND)
        }
        val curriculum = curriculumService.getActiveCurriculumOrThrow(request.curriculumId)
        val profile =
            userProfileRepository.findById(userId).orElseThrow {
                CustomException(DomainErrorCode.USER_NOT_FOUND)
            }

        val userCurriculum =
            userCurriculumRepository.findById(userId).orElse(null)
                ?: UserCurriculum(user = user, curriculum = curriculum)

        userCurriculum.changeCurriculum(curriculum)
        userCurriculumRepository.save(userCurriculum)

        profile.advanceOnboardingStep(1)
        userProfileRepository.save(profile)

        return OnboardingResponse(curriculumId = curriculum.id, step = 1)
    }

    @Transactional
    fun saveSchedule(userId: Long, request: UserScheduleRequest): UserScheduleResponse {
        val days = ScheduleValidator.parseDays(request.days)
        ScheduleValidator.validateSchedule(request.frequency, days, request.time)

        val user = userRepository.findById(userId).orElseThrow {
            CustomException(DomainErrorCode.USER_NOT_FOUND)
        }
        val lessonTime = LocalTime.parse(request.time, DateTimeFormatter.ofPattern("HH:mm"))

        val schedule =
            userScheduleRepository.findWithDaysByUserId(userId)
                ?: UserSchedule(user = user, frequencyPerWeek = request.frequency, lessonTime = lessonTime)

        val newDays =
            days.mapIndexed { index, day ->
                UserScheduleDay(
                    userSchedule = schedule,
                    dayOfWeek = day,
                    selectedOrder = index + 1,
                )
            }
        schedule.clearDays()
        userScheduleRepository.saveAndFlush(schedule)
        schedule.update(request.frequency, lessonTime, newDays)
        userScheduleRepository.save(schedule)

        val profile =
            userProfileRepository.findById(userId).orElseThrow {
                CustomException(DomainErrorCode.USER_NOT_FOUND)
            }
        profile.advanceOnboardingStep(2)
        userProfileRepository.save(profile)

        return toScheduleResponse(schedule)
    }

    @Transactional
    fun completeOnboarding(userId: Long): OnboardingCompleteResponse {
        val profile =
            userProfileRepository.findById(userId).orElseThrow {
                CustomException(DomainErrorCode.USER_NOT_FOUND)
            }
        if (userScheduleRepository.findById(userId).isEmpty) {
            throw CustomException(DomainErrorCode.SCHEDULE_NOT_CONFIGURED)
        }

        profile.completeOnboarding()
        userProfileRepository.save(profile)

        return OnboardingCompleteResponse(onboardingCompleted = true)
    }

    fun toScheduleResponse(schedule: UserSchedule): UserScheduleResponse = UserScheduleResponse(
        frequency = schedule.frequencyPerWeek,
        days = schedule.days.sortedBy { it.selectedOrder }.map { it.dayOfWeek.name },
        time = KstDateTimeUtils.toTimeString(schedule.lessonTime),
    )
}
