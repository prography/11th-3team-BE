package org.prography.samsung.backend.user.usecase

import org.prography.samsung.backend.user.dto.request.UserSettingsRequest
import org.prography.samsung.backend.user.dto.response.UserProfileResponse
import org.prography.samsung.backend.user.dto.response.UserSettingsResponse
import org.prography.samsung.backend.user.service.UserProfileService
import org.prography.samsung.backend.user.service.UserSettingsService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserProfileUsecase(
    private val userProfileService: UserProfileService,
    private val userSettingsService: UserSettingsService,
) {
    @Transactional(readOnly = true)
    fun getProfile(userId: Long): UserProfileResponse = userProfileService.getUserProfileResponse(userId)

    @Transactional(readOnly = true)
    fun getSettings(userId: Long): UserSettingsResponse = userSettingsService.getSettings(userId)

    @Transactional
    fun updateSettings(userId: Long, request: UserSettingsRequest): UserSettingsResponse =
        userSettingsService.updateSettings(userId, request)
}
