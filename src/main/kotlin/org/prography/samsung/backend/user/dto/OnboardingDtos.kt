package org.prography.samsung.backend.user.dto

import jakarta.validation.constraints.NotNull

data class OnboardingStatusResponse(val completed: Boolean, val step: Int)

data class OnboardingRequest(@field:NotNull val curriculumId: Long, @field:NotNull val step: Int)

data class OnboardingResponse(val curriculumId: Long, val step: Int)

data class UserScheduleRequest(
    @field:NotNull val frequency: Int,
    @field:NotNull val days: List<String>,
    @field:NotNull val time: String,
)

data class OnboardingCompleteResponse(val onboardingCompleted: Boolean)
