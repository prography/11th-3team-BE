package org.prography.samsung.backend.notification.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.prography.samsung.backend.common.domain.DayOfWeekCode
import org.prography.samsung.backend.user.entity.User
import java.time.Instant
import java.time.LocalTime

@Entity
@Table(name = "notification_schedules")
class NotificationSchedule(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 3)
    val dayOfWeek: DayOfWeekCode,
    @Column(name = "notify_time", nullable = false)
    val notifyTime: LocalTime,
    @Column(nullable = false, length = 50)
    val timezone: String = "Asia/Seoul",
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)
