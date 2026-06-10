package org.prography.samsung.backend.user.service

import org.prography.samsung.backend.common.dto.CurriculumSummaryResponse
import org.prography.samsung.backend.common.dto.LevelResponse
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.common.response.ErrorBaseCode
import org.prography.samsung.backend.session.service.SessionService
import org.prography.samsung.backend.user.dto.UserHomeResponse
import org.prography.samsung.backend.user.dto.UserProfileResponse
import org.prography.samsung.backend.user.repository.UserCurriculumRepository
import org.prography.samsung.backend.user.repository.UserProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserProfileService(
    private val userProfileRepository: UserProfileRepository,
    private val userCurriculumRepository: UserCurriculumRepository,
    private val sessionService: SessionService,
) {
    @Transactional(readOnly = true)
    fun getProfile(userId: Long): UserProfileResponse {
        val profile =
            userProfileRepository.findById(userId).orElseThrow {
                CustomException(ErrorBaseCode.NOT_FOUND_ENTITY)
            }
        val userCurriculum =
            userCurriculumRepository.findById(userId).orElseThrow {
                CustomException(DomainErrorCode.CURRICULUM_NOT_SELECTED)
            }
        val sessionStatus = sessionService.getStatus(userId)

        return UserProfileResponse(
            level = LevelResponse(profile.badgeLevel.level, profile.badgeLevel.name),
            totalCoins = profile.totalCoins,
            curriculum =
            CurriculumSummaryResponse(
                id = userCurriculum.curriculum.id,
                name = userCurriculum.curriculum.name,
                displayName = userCurriculum.curriculum.chapterLabel,
            ),
            progressPercent = userCurriculum.progressPercent,
            homeMessage = buildHomeMessage(sessionStatus.lessonCompletedToday, userCurriculum.curriculum.name),
        )
    }

    @Transactional(readOnly = true)
    fun getHome(userId: Long): UserHomeResponse {
        val profile = getProfile(userId)
        val sessionStatus = sessionService.getStatus(userId)
        return UserHomeResponse(
            level = profile.level,
            totalCoins = profile.totalCoins,
            curriculum = profile.curriculum,
            progressPercent = profile.progressPercent,
            homeMessage = profile.homeMessage,
            lessonCompletedToday = sessionStatus.lessonCompletedToday,
            activeSession = sessionStatus.activeSession,
            pendingRewardSessionId = sessionStatus.pendingRewardSessionId,
        )
    }

    private fun buildHomeMessage(lessonCompletedToday: Boolean, curriculumName: String): String =
        if (lessonCompletedToday) {
            "선생님 덕분에 $curriculumName 마스터! 다음에 또 만나요!"
        } else {
            "쌤 오늘 학교에서 $curriculumName 배웠는데 하나도 모르겠어요 ㅠㅠ"
        }
}
