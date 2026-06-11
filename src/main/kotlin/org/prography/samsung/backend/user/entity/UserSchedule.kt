package org.prography.samsung.backend.user.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.prography.samsung.backend.common.entity.BaseEntity
import java.time.LocalTime

@Entity
@Table(name = "user_schedules")
class UserSchedule(
    @Id
    val userId: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    val user: User,

    @Column(name = "frequency_per_week", nullable = false)
    var frequencyPerWeek: Int,

    @Column(name = "lesson_time", nullable = false)
    var lessonTime: LocalTime,

    @OneToMany(mappedBy = "userSchedule", cascade = [CascadeType.ALL], orphanRemoval = true)
    val days: MutableList<UserScheduleDay> = mutableListOf(),
) : BaseEntity() {

    fun update(frequency: Int, time: LocalTime, newDays: List<UserScheduleDay>) {
        frequencyPerWeek = frequency
        lessonTime = time
        days.clear()
        days.addAll(newDays)
    }
}
