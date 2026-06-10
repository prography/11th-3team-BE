package org.prography.samsung.backend.user.entity

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
import org.prography.samsung.backend.common.entity.BaseEntity

@Entity
@Table(name = "user_schedule_days")
class UserScheduleDay(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_schedule_id", nullable = false)
    val userSchedule: UserSchedule,

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 3)
    val dayOfWeek: DayOfWeekCode,

    @Column(name = "selected_order", nullable = false)
    val selectedOrder: Int,
) : BaseEntity()
