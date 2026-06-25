package org.prography.samsung.backend.user.dto.request

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

data class UserSettingsRequest(
    @field:Schema(description = "변경할 커리큘럼 ID (생략 시 유지)", example = "1")
    val curriculumId: Long? = null,

    @field:Schema(description = "주당 수업 횟수 (생략 시 유지)", allowableValues = ["2", "3"], example = "3")
    val frequency: Int? = null,

    @field:ArraySchema(
        schema = Schema(
            description = "수업 요일 (생략 시 유지)",
            allowableValues = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"],
            example = "MON",
        ),
    )
    val days: List<String>? = null,

    @field:Schema(
        description = "수업 시간 (KST, HH:mm 형식, 15:00~20:00 정시만 허용. 생략 시 유지)",
        pattern = "^([01]\\d|2[0-3]):00$",
        example = "18:00",
    )
    val time: String? = null,

    @field:Schema(description = "커리큘럼 변경 시 진행률 초기화 여부", example = "false")
    val resetProgress: Boolean = false,
)
