package org.prography.samsung.backend.conversation.repository

import org.prography.samsung.backend.conversation.entity.CurriculumUnit
import org.springframework.data.jpa.repository.JpaRepository

interface CurriculumUnitRepository : JpaRepository<CurriculumUnit, Long> {
    fun findFirstByCurriculumIdOrderByIdAsc(curriculumId: Long): CurriculumUnit?
}
