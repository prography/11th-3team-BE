package org.prography.samsung.backend.user.repository

import org.prography.samsung.backend.user.entity.UserProfile
import org.springframework.data.jpa.repository.JpaRepository

interface UserProfileRepository : JpaRepository<UserProfile, Long>
