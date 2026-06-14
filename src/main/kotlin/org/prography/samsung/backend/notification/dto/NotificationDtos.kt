package org.prography.samsung.backend.notification.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.prography.samsung.backend.common.domain.DevicePlatform

data class NotificationRegisterRequest(
    @field:Schema(description = "FCM 디바이스 토큰", example = "dGhpcyBpcyBhIHNhbXBsZSB0b2tlbg==")
    val deviceToken: String? = null,

    @field:Schema(description = "디바이스 플랫폼", allowableValues = ["IOS", "ANDROID"], example = "IOS")
    val platform: DevicePlatform? = null,
)

data class NotificationRegisterResponse(val registered: Boolean, val scheduleCount: Int)

data class NotificationRescheduleResponse(val rescheduled: Boolean)
