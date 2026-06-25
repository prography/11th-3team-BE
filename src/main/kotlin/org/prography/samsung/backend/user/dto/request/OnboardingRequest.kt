package org.prography.samsung.backend.user.dto.request

import jakarta.validation.constraints.NotNull

data class OnboardingRequest(@field:NotNull val curriculumId: Long, @field:NotNull val step: Int)
