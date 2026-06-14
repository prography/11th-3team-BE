package org.prography.samsung.backend.user.dto

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class OnboardingStatusResponse(val completed: Boolean, val step: Int)

data class OnboardingRequest(@field:NotNull val curriculumId: Long, @field:NotNull val step: Int)

data class OnboardingResponse(val curriculumId: Long, val step: Int)

data class UserScheduleRequest(
    @field:NotNull
    @field:Schema(description = "주당 수업 횟수", allowableValues = ["2", "3"], example = "3")
    val frequency: Int,

    @field:NotNull
    @field:ArraySchema(
        schema = Schema(
            description = "수업 요일",
            allowableValues = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"],
            example = "MON",
        ),
    )
    val days: List<String>,

    @field:NotNull
    @field:Schema(
        description = "수업 시간 (KST, HH:mm 형식)",
        pattern = "^([01]\\d|2[0-3]):00$",
        example = "18:00",
    )
    val time: String,
)

data class OnboardingCompleteResponse(val onboardingCompleted: Boolean)
