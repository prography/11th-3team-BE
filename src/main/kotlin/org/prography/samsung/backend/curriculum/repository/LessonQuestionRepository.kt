package org.prography.samsung.backend.curriculum.repository

import org.prography.samsung.backend.common.domain.SessionPhase
import org.prography.samsung.backend.curriculum.entity.LessonQuestion
import org.springframework.data.jpa.repository.JpaRepository

interface LessonQuestionRepository : JpaRepository<LessonQuestion, Long> {
    fun findByLessonTopicIdAndPhase(lessonTopicId: Long, phase: SessionPhase): LessonQuestion?
}
