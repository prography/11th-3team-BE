package org.prography.samsung.backend.user.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.prography.samsung.backend.common.auth.CurrentUserHolder
import org.prography.samsung.backend.common.response.SuccessCode
import org.prography.samsung.backend.common.web.ApiResponseFactory
import org.prography.samsung.backend.user.dto.UserSettingsRequest
import org.prography.samsung.backend.user.service.UserProfileService
import org.prography.samsung.backend.user.service.UserSettingsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Tag(name = "User", description = "사용자 프로필·설정 API")
@RestController
class UserController(
    private val userProfileService: UserProfileService,
    private val userSettingsService: UserSettingsService,
) {
    @Operation(
        summary = "사용자 프로필 조회",
        description = "배지 레벨, 총 코인, 커리큘럼 진행률, 홈 메시지를 반환합니다. 홈 화면(SCR-HOME) 진입 시 호출합니다.",
    )
    @GetMapping("/user/profile")
    fun getProfile() = ApiResponseFactory.success(
        SuccessCode.OK,
        userProfileService.getProfile(CurrentUserHolder.get().userId),
    )

    @Operation(
        summary = "홈 화면 통합 조회",
        description = "프로필과 세션 상태를 한 번에 반환합니다. GET /user/profile + GET /session/status 두 번 호출 대신 사용할 수 있습니다.",
    )
    @GetMapping("/user/home")
    fun getHome() = ApiResponseFactory.success(
        SuccessCode.OK,
        userProfileService.getHome(CurrentUserHolder.get().userId),
    )

    @Operation(
        summary = "설정 조회",
        description = "현재 커리큘럼과 과외 스케줄(빈도·요일·시간)을 반환합니다. 설정 화면(SCR-SETTINGS) 진입 시 prefill에 사용합니다.",
    )
    @GetMapping("/user/settings")
    fun getSettings() = ApiResponseFactory.success(
        SuccessCode.OK,
        userSettingsService.getSettings(CurrentUserHolder.get().userId),
    )

    @Operation(
        summary = "설정 변경",
        description = "커리큘럼 또는 과외 스케줄을 변경합니다. 단원 변경 시 resetProgress=true를 전달하면 진행률이 초기화됩니다.\n" +
            "STARTED 상태의 세션이 있으면 409를 반환합니다.",
    )
    @PutMapping("/user/settings")
    fun updateSettings(@Valid @RequestBody request: UserSettingsRequest) = ApiResponseFactory.success(
        SuccessCode.OK,
        userSettingsService.updateSettings(CurrentUserHolder.get().userId, request),
    )
}
