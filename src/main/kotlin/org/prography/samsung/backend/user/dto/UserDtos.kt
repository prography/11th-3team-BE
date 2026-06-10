package org.prography.samsung.backend.user.dto

import com.fasterxml.jackson.annotation.JsonInclude
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
    val curriculumId: Long? = null,
    val frequency: Int? = null,
    val days: List<String>? = null,
    val time: String? = null,
    val resetProgress: Boolean = false,
)
