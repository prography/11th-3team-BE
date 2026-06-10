package org.prography.samsung.backend.notification.dto

import org.prography.samsung.backend.common.domain.DevicePlatform

data class NotificationRegisterRequest(val deviceToken: String? = null, val platform: DevicePlatform? = null)

data class NotificationRegisterResponse(val registered: Boolean, val scheduleCount: Int)

data class NotificationRescheduleResponse(val rescheduled: Boolean)
