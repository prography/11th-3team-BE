package org.prography.samsung.backend.notification.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.prography.samsung.backend.common.domain.DevicePlatform
import org.prography.samsung.backend.common.entity.BaseEntity
import org.prography.samsung.backend.user.entity.User

@Entity
@Table(name = "device_tokens")
class DeviceToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, length = 512)
    var token: String,

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    var platform: DevicePlatform? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
) : BaseEntity() {

    fun reactivate(newPlatform: DevicePlatform?) {
        platform = newPlatform
        isActive = true
    }
}
