package org.prography.samsung.backend.user.dto.response

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

data class UserScheduleResponse(
    @field:Schema(description = "주당 수업 횟수", allowableValues = ["2", "3"], example = "3")
    val frequency: Int,

    @field:ArraySchema(
        schema = Schema(
            description = "수업 요일",
            allowableValues = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"],
            example = "MON",
        ),
    )
    val days: List<String>,

    @field:Schema(
        description = "수업 시간 (KST, HH:mm 형식, 15:00~20:00 정시)",
        example = "18:00",
    )
    val time: String,
)
