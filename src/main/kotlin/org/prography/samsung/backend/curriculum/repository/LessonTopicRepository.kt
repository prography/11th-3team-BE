package org.prography.samsung.backend.curriculum.repository

import org.prography.samsung.backend.curriculum.entity.LessonTopic
import org.springframework.data.jpa.repository.JpaRepository

interface LessonTopicRepository : JpaRepository<LessonTopic, Long> {
    fun findAllByCurriculumIdOrderBySequenceAsc(curriculumId: Long): List<LessonTopic>

    fun findByCurriculumIdAndSequence(curriculumId: Long, sequence: Int): LessonTopic?

    fun findAllByCurriculumUnitIdOrderBySequenceAsc(curriculumUnitId: Long): List<LessonTopic>
}
