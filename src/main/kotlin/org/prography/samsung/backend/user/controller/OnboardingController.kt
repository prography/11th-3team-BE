package org.prography.samsung.backend.user.controller

import jakarta.validation.Valid
import org.prography.samsung.backend.common.auth.CurrentUserHolder
import org.prography.samsung.backend.common.response.SuccessCode
import org.prography.samsung.backend.common.web.ApiResponseFactory
import org.prography.samsung.backend.user.dto.OnboardingRequest
import org.prography.samsung.backend.user.dto.UserScheduleRequest
import org.prography.samsung.backend.user.service.OnboardingService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class OnboardingController(private val onboardingService: OnboardingService) {
    @GetMapping("/user/onboarding/status")
    fun getStatus() = ApiResponseFactory.success(
        SuccessCode.OK,
        onboardingService.getStatus(CurrentUserHolder.get().userId),
    )

    @PostMapping("/user/onboarding")
    fun saveCurriculum(@Valid @RequestBody request: OnboardingRequest) = ApiResponseFactory.success(
        SuccessCode.OK,
        onboardingService.saveCurriculum(CurrentUserHolder.get().userId, request),
    )

    @PostMapping("/user/schedule")
    fun saveSchedule(@Valid @RequestBody request: UserScheduleRequest) = ApiResponseFactory.success(
        SuccessCode.OK,
        onboardingService.saveSchedule(CurrentUserHolder.get().userId, request),
    )

    @PostMapping("/user/onboarding/complete")
    fun completeOnboarding() = ApiResponseFactory.success(
        SuccessCode.OK,
        onboardingService.completeOnboarding(CurrentUserHolder.get().userId),
    )
}
