package org.prography.samsung.backend.session.repository

import org.prography.samsung.backend.common.domain.SessionStatus
import org.prography.samsung.backend.session.entity.TutoringSession
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.time.LocalDate

interface TutoringSessionRepository : JpaRepository<TutoringSession, String> {
    @Query(
        """
        SELECT s FROM TutoringSession s
        WHERE s.user.id = :userId AND s.status = :status
        """,
    )
    fun findByUserIdAndStatus(@Param("userId") userId: Long, @Param("status") status: SessionStatus): TutoringSession?

    @Query(
        """
        SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
        FROM TutoringSession s
        WHERE s.user.id = :userId AND s.sessionDate = :sessionDate AND s.status = :status
        """,
    )
    fun existsByUserIdAndSessionDateAndStatus(
        @Param("userId") userId: Long,
        @Param("sessionDate") sessionDate: LocalDate,
        @Param("status") status: SessionStatus,
    ): Boolean

    @Query(
        """
        SELECT s FROM TutoringSession s
        WHERE s.user.id = :userId AND s.id = :id
        """,
    )
    fun findByUserIdAndId(@Param("userId") userId: Long, @Param("id") id: String): TutoringSession?

    @Query(
        """
        SELECT s FROM TutoringSession s
        WHERE s.user.id = :userId
          AND s.status = org.prography.samsung.backend.common.domain.SessionStatus.COMPLETED
          AND s.rewardStatus = org.prography.samsung.backend.common.domain.RewardStatus.GRANTED
          AND s.rewardAcknowledgedAt IS NULL
        ORDER BY s.completedAt DESC
        """,
    )
    fun findPendingRewardSession(@Param("userId") userId: Long): List<TutoringSession>

    @Query(
        """
        SELECT s FROM TutoringSession s
        WHERE s.user.id = :userId
          AND s.status = org.prography.samsung.backend.common.domain.SessionStatus.COMPLETED
          AND (:cursorCompletedAt IS NULL OR s.completedAt < :cursorCompletedAt
               OR (s.completedAt = :cursorCompletedAt AND s.id < :cursorSessionId))
        ORDER BY s.completedAt DESC, s.id DESC
        """,
    )
    fun findCompletedHistory(
        @Param("userId") userId: Long,
        @Param("cursorCompletedAt") cursorCompletedAt: Instant?,
        @Param("cursorSessionId") cursorSessionId: String?,
        pageable: Pageable,
    ): List<TutoringSession>
}
