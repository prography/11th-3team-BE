package org.prography.samsung.backend.gamification.repository

import org.prography.samsung.backend.gamification.entity.BadgeLevel
import org.springframework.data.jpa.repository.JpaRepository

interface BadgeLevelRepository : JpaRepository<BadgeLevel, Long> {
    fun findByLevel(level: Int): BadgeLevel?

    fun findTopByRequiredCompletedSessionsLessThanEqualOrderByLevelDesc(completedSessions: Int): BadgeLevel?
}
