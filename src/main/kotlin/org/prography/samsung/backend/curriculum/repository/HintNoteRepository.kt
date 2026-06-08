package org.prography.samsung.backend.curriculum.repository

import org.prography.samsung.backend.common.domain.SessionPhase
import org.prography.samsung.backend.curriculum.entity.HintNote
import org.springframework.data.jpa.repository.JpaRepository

interface HintNoteRepository : JpaRepository<HintNote, Long> {
    fun findByLessonTopicIdAndPhase(lessonTopicId: Long, phase: SessionPhase): HintNote?
}
