package org.prography.samsung.backend.notification.repository

import org.prography.samsung.backend.notification.entity.NotificationSchedule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface NotificationScheduleRepository : JpaRepository<NotificationSchedule, Long> {
    @Query(
        """
        SELECT n FROM NotificationSchedule n
        WHERE n.user.id = :userId AND n.isActive = true
        """,
    )
    fun findAllByUserIdAndIsActiveTrue(@Param("userId") userId: Long): List<NotificationSchedule>

    @Modifying
    @Query("DELETE FROM NotificationSchedule n WHERE n.user.id = :userId")
    fun deleteAllByUserId(@Param("userId") userId: Long)
}
