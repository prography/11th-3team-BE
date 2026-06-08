package org.prography.samsung.backend.user.repository

import org.prography.samsung.backend.user.entity.UserSchedule
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface UserScheduleRepository : JpaRepository<UserSchedule, Long> {
    @EntityGraph(attributePaths = ["days"])
    fun findWithDaysByUserId(userId: Long): UserSchedule?
}
