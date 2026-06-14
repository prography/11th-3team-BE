package org.prography.samsung.backend.user.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.prography.samsung.backend.common.dto.ActiveSessionResponse
import org.prography.samsung.backend.common.dto.CurriculumChipResponse
import org.prography.samsung.backend.common.dto.CurriculumSummaryResponse
import org.prography.samsung.backend.common.dto.LevelResponse
import org.prography.samsung.backend.common.dto.UserScheduleResponse

data class UserProfileResponse(
    val level: LevelResponse,
    val totalCoins: Int,
    val curriculum: CurriculumSummaryResponse,
    val progressPercent: Int,
    val homeMessage: String,
)

data class UserHomeResponse(
    val level: LevelResponse,
    val totalCoins: Int,
    val curriculum: CurriculumSummaryResponse,
    val progressPercent: Int,
    val homeMessage: String,
    val lessonCompletedToday: Boolean,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val activeSession: ActiveSessionResponse?,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val pendingRewardSessionId: String?,
)

data class UserSettingsResponse(val curriculum: CurriculumChipResponse, val schedule: UserScheduleResponse)

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
