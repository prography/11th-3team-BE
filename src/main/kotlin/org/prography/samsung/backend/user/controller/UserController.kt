package org.prography.samsung.backend.user.controller

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

@RestController
class UserController(
    private val userProfileService: UserProfileService,
    private val userSettingsService: UserSettingsService,
) {
    @GetMapping("/user/profile")
    fun getProfile() = ApiResponseFactory.success(
        SuccessCode.OK,
        userProfileService.getProfile(CurrentUserHolder.get().userId),
    )

    @GetMapping("/user/home")
    fun getHome() = ApiResponseFactory.success(
        SuccessCode.OK,
        userProfileService.getHome(CurrentUserHolder.get().userId),
    )

    @GetMapping("/user/settings")
    fun getSettings() = ApiResponseFactory.success(
        SuccessCode.OK,
        userSettingsService.getSettings(CurrentUserHolder.get().userId),
    )

    @PutMapping("/user/settings")
    fun updateSettings(@Valid @RequestBody request: UserSettingsRequest) = ApiResponseFactory.success(
        SuccessCode.OK,
        userSettingsService.updateSettings(CurrentUserHolder.get().userId, request),
    )
}
