package org.prography.samsung.backend.user.repository

import org.prography.samsung.backend.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByExternalId(externalId: String): User?
}
