package org.prography.samsung.backend.user.dto.response

import org.prography.samsung.backend.common.dto.CurriculumSummaryResponse
import org.prography.samsung.backend.common.dto.LevelResponse

data class UserProfileResponse(
    val level: LevelResponse,
    val totalCoins: Int,
    val curriculum: CurriculumSummaryResponse,
    val progressPercent: Int,
    val homeMessage: String,
)
