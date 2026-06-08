package org.prography.samsung.backend.notification.repository

import org.prography.samsung.backend.notification.entity.DeviceToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface DeviceTokenRepository : JpaRepository<DeviceToken, Long> {
    @Query(
        """
        SELECT d FROM DeviceToken d
        WHERE d.user.id = :userId AND d.token = :token
        """,
    )
    fun findByUserIdAndToken(@Param("userId") userId: Long, @Param("token") token: String): DeviceToken?
}
