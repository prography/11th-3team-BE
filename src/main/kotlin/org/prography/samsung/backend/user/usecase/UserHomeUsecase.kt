package org.prography.samsung.backend.user.usecase

import org.prography.samsung.backend.session.service.SessionQueryService
import org.prography.samsung.backend.user.dto.response.UserHomeResponse
import org.prography.samsung.backend.user.service.UserProfileService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserHomeUsecase(
    private val userProfileService: UserProfileService,
    private val sessionQueryService: SessionQueryService,
) {

    @Transactional(readOnly = true)
    fun getHome(userId: Long): UserHomeResponse {
        val profile = userProfileService.getUserProfileResponse(userId)
        val sessionStatus = sessionQueryService.getStatus(userId)
        val homeMessage =
            if (sessionStatus.lessonCompletedToday) {
                "선생님 덕분에 ${profile.curriculum.name} 마스터! 다음에 또 만나요!"
            } else {
                profile.homeMessage
            }
        return UserHomeResponse(
            level = profile.level,
            totalCoins = profile.totalCoins,
            curriculum = profile.curriculum,
            progressPercent = profile.progressPercent,
            homeMessage = homeMessage,
            lessonCompletedToday = sessionStatus.lessonCompletedToday,
            activeSession = sessionStatus.activeSession,
            pendingRewardSessionId = sessionStatus.pendingRewardSessionId,
        )
    }
}
