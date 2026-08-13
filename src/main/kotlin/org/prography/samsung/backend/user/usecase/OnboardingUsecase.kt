package org.prography.samsung.backend.user.usecase

import org.prography.samsung.backend.user.dto.request.OnboardingRequest
import org.prography.samsung.backend.user.dto.request.UserScheduleRequest
import org.prography.samsung.backend.user.dto.response.OnboardingCompleteResponse
import org.prography.samsung.backend.user.dto.response.OnboardingResponse
import org.prography.samsung.backend.user.dto.response.OnboardingStatusResponse
import org.prography.samsung.backend.user.dto.response.UserScheduleResponse
import org.prography.samsung.backend.user.service.OnboardingService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OnboardingUsecase(private val onboardingService: OnboardingService) {
    @Transactional(readOnly = true)
    fun getStatus(userId: Long): OnboardingStatusResponse = onboardingService.getStatus(userId)

    @Transactional
    fun saveCurriculum(userId: Long, request: OnboardingRequest): OnboardingResponse =
        onboardingService.saveCurriculum(userId, request)

    @Transactional
    fun saveSchedule(userId: Long, request: UserScheduleRequest): UserScheduleResponse =
        onboardingService.saveSchedule(userId, request)

    @Transactional
    fun completeOnboarding(userId: Long): OnboardingCompleteResponse = onboardingService.completeOnboarding(userId)
}
