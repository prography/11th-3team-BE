package org.prography.samsung.backend.session.service

import org.prography.samsung.backend.common.domain.CoinLedgerType
import org.prography.samsung.backend.common.domain.SessionStatus
import org.prography.samsung.backend.common.dto.LevelResponse
import org.prography.samsung.backend.common.dto.RewardResponse
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.common.response.ErrorBaseCode
import org.prography.samsung.backend.gamification.entity.BadgeLevel
import org.prography.samsung.backend.gamification.repository.BadgeLevelRepository
import org.prography.samsung.backend.session.SessionConstants
import org.prography.samsung.backend.session.entity.CoinLedgerEntry
import org.prography.samsung.backend.session.entity.TutoringSession
import org.prography.samsung.backend.session.repository.CoinLedgerEntryRepository
import org.prography.samsung.backend.session.repository.SessionTopicSnapshotRepository
import org.prography.samsung.backend.session.repository.TutoringSessionRepository
import org.prography.samsung.backend.user.entity.UserProfile
import org.prography.samsung.backend.user.repository.UserCurriculumRepository
import org.prography.samsung.backend.user.repository.UserProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import kotlin.math.min

@Service
class SessionCompletionService(
    private val tutoringSessionRepository: TutoringSessionRepository,
    private val sessionTopicSnapshotRepository: SessionTopicSnapshotRepository,
    private val coinLedgerEntryRepository: CoinLedgerEntryRepository,
    private val userProfileRepository: UserProfileRepository,
    private val userCurriculumRepository: UserCurriculumRepository,
    private val badgeLevelRepository: BadgeLevelRepository,
) {
    @Transactional
    fun complete(userId: Long, sessionId: String): RewardResponse {
        val session =
            tutoringSessionRepository.findByUserIdAndId(userId, sessionId)
                ?: throw CustomException(ErrorBaseCode.NOT_FOUND_ENTITY)

        if (session.status == SessionStatus.COMPLETED) {
            return toRewardResponse(session, userProfileRepository.findById(userId).orElseThrow())
        }

        if (session.status != SessionStatus.STARTED) {
            throw CustomException(DomainErrorCode.SESSION_NOT_STARTED)
        }

        val profile =
            userProfileRepository.findById(userId).orElseThrow {
                CustomException(ErrorBaseCode.NOT_FOUND_ENTITY)
            }
        val userCurriculum =
            userCurriculumRepository.findById(userId).orElseThrow {
                CustomException(DomainErrorCode.CURRICULUM_NOT_SELECTED)
            }

        val primaryTopic =
            sessionTopicSnapshotRepository.findBySessionIdAndSequence(sessionId, 1)
                ?: throw CustomException(ErrorBaseCode.NOT_FOUND_ENTITY)

        val previousLevel = profile.badgeLevel.level
        val newProgress = min(100, userCurriculum.progressPercent + SessionConstants.PROGRESS_INCREMENT)
        val coinsAwarded = SessionConstants.COINS_PER_SESSION

        val nextBadge =
            badgeLevelRepository.findTopByRequiredCompletedSessionsLessThanEqualOrderByLevelDesc(
                profile.completedSessionCount + 1,
            ) ?: profile.badgeLevel
        val badgeLevelUp = nextBadge.level > previousLevel
        profile.applySessionReward(coinsAwarded, nextBadge)
        userProfileRepository.save(profile)

        userCurriculum.updateProgress(newProgress)
        userCurriculumRepository.save(userCurriculum)

        session.complete(
            completedAt = Instant.now(),
            primaryTopicTitle = primaryTopic.title,
            coinsAwarded = coinsAwarded,
            badgeLevelUp = badgeLevelUp,
            progressAfter = newProgress,
        )
        tutoringSessionRepository.save(session)

        if (!coinLedgerEntryRepository.existsBySessionId(sessionId)) {
            coinLedgerEntryRepository.save(
                CoinLedgerEntry(
                    user = session.user,
                    session = session,
                    amount = coinsAwarded,
                    type = CoinLedgerType.SESSION_REWARD,
                ),
            )
        }

        return toRewardResponse(session, profile, badgeLevelUp, nextBadge)
    }

    fun toRewardResponse(
        session: TutoringSession,
        profile: UserProfile,
        badgeLevelUp: Boolean = session.badgeLevelUp ?: false,
        newBadge: BadgeLevel = profile.badgeLevel,
    ): RewardResponse = RewardResponse(
        sessionId = session.id,
        coinsAwarded = session.coinsAwarded ?: SessionConstants.COINS_PER_SESSION,
        badgeLevelUp = badgeLevelUp,
        newLevel =
        if (badgeLevelUp) {
            LevelResponse(number = newBadge.level, name = newBadge.name)
        } else {
            null
        },
        progressPercent = session.progressAfter ?: 0,
        totalCoins = profile.totalCoins,
    )
}
