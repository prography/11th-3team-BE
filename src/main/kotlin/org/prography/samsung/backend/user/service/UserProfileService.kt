package org.prography.samsung.backend.user.service

import org.prography.samsung.backend.common.dto.CurriculumSummaryResponse
import org.prography.samsung.backend.common.dto.LevelResponse
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.user.dto.response.UserProfileResponse
import org.prography.samsung.backend.user.entity.User
import org.prography.samsung.backend.user.entity.UserCurriculum
import org.prography.samsung.backend.user.entity.UserProfile
import org.prography.samsung.backend.user.repository.UserCurriculumRepository
import org.prography.samsung.backend.user.repository.UserProfileRepository
import org.prography.samsung.backend.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserProfileService(
    private val userProfileRepository: UserProfileRepository,
    private val userCurriculumRepository: UserCurriculumRepository,
    private val userRepository: UserRepository,
) {
    @Transactional(readOnly = true)
    fun getUserProfileResponse(userId: Long): UserProfileResponse {
        val profile =
            userProfileRepository.findById(userId).orElseThrow {
                CustomException(DomainErrorCode.USER_NOT_FOUND)
            }
        val userCurriculum = getUserCurriculum(userId)

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
            homeMessage = buildHomeMessage(userCurriculum.curriculum.name),
        )
    }

    @Transactional(readOnly = true)
    fun getUserProfile(userId: Long): UserProfile = userProfileRepository.findById(userId).orElseThrow {
        CustomException(DomainErrorCode.USER_NOT_FOUND)
    }

    @Transactional(readOnly = true)
    fun getUser(userId: Long): User = userRepository.findById(userId).orElseThrow {
        CustomException(DomainErrorCode.USER_NOT_FOUND)
    }

    @Transactional(readOnly = true)
    fun getUserCurriculum(userId: Long): UserCurriculum = userCurriculumRepository.findById(userId).orElseThrow {
        CustomException(DomainErrorCode.CURRICULUM_NOT_SELECTED)
    }

    private fun buildHomeMessage(curriculumName: String): String = "쌤 오늘 학교에서 $curriculumName 배웠는데 하나도 모르겠어요 ㅠㅠ"
}
