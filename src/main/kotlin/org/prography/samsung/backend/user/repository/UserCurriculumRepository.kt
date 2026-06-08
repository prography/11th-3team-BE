package org.prography.samsung.backend.user.repository

import org.prography.samsung.backend.user.entity.UserCurriculum
import org.springframework.data.jpa.repository.JpaRepository

interface UserCurriculumRepository : JpaRepository<UserCurriculum, Long>
