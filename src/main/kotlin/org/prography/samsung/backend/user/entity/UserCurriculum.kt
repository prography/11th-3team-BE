package org.prography.samsung.backend.user.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.prography.samsung.backend.common.entity.BaseEntity
import org.prography.samsung.backend.curriculum.entity.Curriculum

@Entity
@Table(name = "user_curriculums")
class UserCurriculum(
    @Id
    val userId: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_id", nullable = false)
    var curriculum: Curriculum,

    @Column(name = "progress_percent", nullable = false)
    var progressPercent: Int = 0,
) : BaseEntity() {

    fun changeCurriculum(newCurriculum: Curriculum) {
        curriculum = newCurriculum
    }

    fun resetProgress() {
        progressPercent = 0
    }

    fun updateProgress(newPercent: Int) {
        progressPercent = newPercent
    }
}
