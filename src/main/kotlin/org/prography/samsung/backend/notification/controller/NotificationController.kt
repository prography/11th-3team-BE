package org.prography.samsung.backend.notification.controller

import org.prography.samsung.backend.common.auth.CurrentUserHolder
import org.prography.samsung.backend.common.response.SuccessCode
import org.prography.samsung.backend.common.web.ApiResponseFactory
import org.prography.samsung.backend.notification.dto.NotificationRegisterRequest
import org.prography.samsung.backend.notification.service.NotificationService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class NotificationController(private val notificationService: NotificationService) {
    @PostMapping("/notifications/register")
    fun register(@RequestBody request: NotificationRegisterRequest) = ApiResponseFactory.success(
        SuccessCode.OK,
        notificationService.register(CurrentUserHolder.get().userId, request),
    )

    @PostMapping("/notifications/reschedule")
    fun reschedule(@RequestBody request: NotificationRegisterRequest) = ApiResponseFactory.success(
        SuccessCode.OK,
        notificationService.reschedule(CurrentUserHolder.get().userId, request),
    )
}
