package org.prography.samsung.backend.notification.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.prography.samsung.backend.common.auth.CurrentUserHolder
import org.prography.samsung.backend.common.response.SuccessCode
import org.prography.samsung.backend.common.web.ApiResponseFactory
import org.prography.samsung.backend.notification.dto.NotificationRegisterRequest
import org.prography.samsung.backend.notification.service.NotificationService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Notification", description = "알림 API")
@RestController
class NotificationController(private val notificationService: NotificationService) {
    @Operation(
        summary = "알림 등록",
        description = "디바이스 토큰을 등록하고 사용자의 과외 스케줄 기반으로 알림 스케줄을 생성합니다. 온보딩 완료(SCR-OB02) 시 호출합니다.",
    )
    @PostMapping("/notifications/register")
    fun register(@RequestBody request: NotificationRegisterRequest) = ApiResponseFactory.success(
        SuccessCode.OK,
        notificationService.register(CurrentUserHolder.get().userId, request),
    )

    @Operation(
        summary = "알림 재설정",
        description = "설정 변경(SCR-SETTINGS) 후 스케줄이 바뀌었을 때 알림을 재등록합니다.",
    )
    @PostMapping("/notifications/reschedule")
    fun reschedule(@RequestBody request: NotificationRegisterRequest) = ApiResponseFactory.success(
        SuccessCode.OK,
        notificationService.reschedule(CurrentUserHolder.get().userId, request),
    )
}
