package org.prography.samsung.backend.user.dto.response

import org.prography.samsung.backend.curriculum.dto.response.CurriculumSummaryResponse
import org.prography.samsung.backend.gamification.dto.response.LevelResponse

data class UserProfileResponse(
    val level: LevelResponse,
    val totalCoins: Int,
    val curriculum: CurriculumSummaryResponse,
    val progressPercent: Int,
    val homeMessage: String,
)
