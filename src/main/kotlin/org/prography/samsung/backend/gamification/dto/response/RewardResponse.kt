package org.prography.samsung.backend.gamification.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import org.prography.samsung.backend.gamification.entity.BadgeLevel
import org.prography.samsung.backend.session.SessionConstants
import org.prography.samsung.backend.session.entity.TutoringSession
import org.prography.samsung.backend.user.entity.UserProfile

data class RewardResponse(
    val sessionId: String,
    val coinsAwarded: Int,
    val badgeLevelUp: Boolean,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val newLevel: LevelResponse?,
    val progressPercent: Int,
    val totalCoins: Int,
) {
    companion object {
        fun from(
            session: TutoringSession,
            profile: UserProfile,
            badgeLevelUp: Boolean = session.badgeLevelUp ?: false,
            newBadge: BadgeLevel = profile.badgeLevel,
        ): RewardResponse = RewardResponse(
            sessionId = session.id,
            coinsAwarded = session.coinsAwarded ?: SessionConstants.COINS_PER_SESSION,
            badgeLevelUp = badgeLevelUp,
            newLevel = if (badgeLevelUp) LevelResponse(number = newBadge.level, name = newBadge.name) else null,
            progressPercent = session.progressAfter ?: 0,
            totalCoins = profile.totalCoins,
        )
    }
}
