package org.prography.samsung.backend.user.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.prography.samsung.backend.common.entity.BaseEntity
import org.prography.samsung.backend.gamification.entity.BadgeLevel

@Entity
@Table(name = "user_profiles")
class UserProfile(
    @Id
    val userId: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    val user: User,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_level_id", nullable = false)
    var badgeLevel: BadgeLevel,

    @Column(name = "total_coins", nullable = false)
    var totalCoins: Int = 0,

    @Column(name = "completed_session_count", nullable = false)
    var completedSessionCount: Int = 0,

    @Column(name = "onboarding_completed", nullable = false)
    var onboardingCompleted: Boolean = false,

    @Column(name = "onboarding_step", nullable = false)
    var onboardingStep: Int = 0,
) : BaseEntity() {

    fun advanceOnboardingStep(step: Int) {
        onboardingStep = maxOf(onboardingStep, step)
    }

    fun completeOnboarding() {
        onboardingCompleted = true
        onboardingStep = 2
    }

    fun applySessionReward(coins: Int, nextBadge: BadgeLevel) {
        completedSessionCount += 1
        totalCoins += coins
        badgeLevel = nextBadge
    }
}
