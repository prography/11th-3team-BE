package org.prography.samsung.backend.user.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import org.prography.samsung.backend.common.dto.ActiveSessionResponse
import org.prography.samsung.backend.common.dto.CurriculumSummaryResponse
import org.prography.samsung.backend.common.dto.LevelResponse

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
