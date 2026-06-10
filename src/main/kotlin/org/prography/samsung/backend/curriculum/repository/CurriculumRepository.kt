package org.prography.samsung.backend.curriculum.repository

import org.prography.samsung.backend.curriculum.entity.Curriculum
import org.springframework.data.jpa.repository.JpaRepository

interface CurriculumRepository : JpaRepository<Curriculum, Long> {
    fun findAllByIsActiveTrueOrderByDisplayOrderAsc(): List<Curriculum>

    fun findByIdAndIsActiveTrue(id: Long): Curriculum?
}
