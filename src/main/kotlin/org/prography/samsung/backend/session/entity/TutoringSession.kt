package org.prography.samsung.backend.session.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.prography.samsung.backend.common.domain.RewardStatus
import org.prography.samsung.backend.common.domain.SessionPhase
import org.prography.samsung.backend.common.domain.SessionStatus
import org.prography.samsung.backend.common.entity.BaseEntity
import org.prography.samsung.backend.curriculum.entity.Curriculum
import org.prography.samsung.backend.user.entity.User
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "tutoring_sessions")
class TutoringSession(
    @Id
    @Column(length = 36)
    val id: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_id", nullable = false)
    val curriculum: Curriculum,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: SessionStatus,

    @Enumerated(EnumType.STRING)
    @Column(name = "current_phase", length = 20)
    var currentPhase: SessionPhase? = null,

    @Column(name = "session_date", nullable = false)
    val sessionDate: LocalDate,

    @Column(name = "started_at", nullable = false)
    val startedAt: Instant,

    @Column(name = "completed_at")
    var completedAt: Instant? = null,

    @Column(name = "primary_topic_title", length = 200)
    var primaryTopicTitle: String? = null,

    @Column(name = "coins_awarded")
    var coinsAwarded: Int? = null,

    @Column(name = "badge_level_up")
    var badgeLevelUp: Boolean? = null,

    @Column(name = "progress_after")
    var progressAfter: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_status", length = 20)
    var rewardStatus: RewardStatus? = null,

    @Column(name = "reward_acknowledged_at")
    var rewardAcknowledgedAt: Instant? = null,

    @Version
    @Column(nullable = false)
    var version: Long = 0,
) : BaseEntity() {

    fun advancePhase(nextPhase: SessionPhase) {
        currentPhase = nextPhase
    }

    fun abort() {
        status = SessionStatus.ABORTED
        currentPhase = null
    }

    fun complete(
        completedAt: Instant,
        primaryTopicTitle: String,
        coinsAwarded: Int,
        badgeLevelUp: Boolean,
        progressAfter: Int,
    ) {
        status = SessionStatus.COMPLETED
        currentPhase = null
        this.completedAt = completedAt
        this.primaryTopicTitle = primaryTopicTitle
        this.coinsAwarded = coinsAwarded
        this.badgeLevelUp = badgeLevelUp
        this.progressAfter = progressAfter
        rewardStatus = RewardStatus.GRANTED
    }

    fun acknowledgeReward(acknowledgedAt: Instant) {
        rewardStatus = RewardStatus.ACKNOWLEDGED
        rewardAcknowledgedAt = acknowledgedAt
    }
}
