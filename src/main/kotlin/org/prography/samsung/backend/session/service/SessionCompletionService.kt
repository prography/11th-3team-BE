package org.prography.samsung.backend.session.service

import org.prography.samsung.backend.common.domain.CoinLedgerType
import org.prography.samsung.backend.common.domain.SessionStatus
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.gamification.dto.response.RewardResponse
import org.prography.samsung.backend.gamification.repository.BadgeLevelRepository
import org.prography.samsung.backend.session.SessionConstants
import org.prography.samsung.backend.session.entity.CoinLedgerEntry
import org.prography.samsung.backend.session.repository.CoinLedgerEntryRepository
import org.prography.samsung.backend.session.repository.SessionTopicSnapshotRepository
import org.prography.samsung.backend.session.repository.TutoringSessionRepository
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
                ?: throw CustomException(DomainErrorCode.SESSION_NOT_FOUND)

        if (session.status == SessionStatus.COMPLETED) {
            return RewardResponse.from(session, userProfileRepository.findById(userId).orElseThrow())
        }

        if (session.status != SessionStatus.STARTED) {
            throw CustomException(DomainErrorCode.SESSION_NOT_STARTED)
        }

        val profile =
            userProfileRepository.findById(userId).orElseThrow {
                CustomException(DomainErrorCode.USER_NOT_FOUND)
            }
        val userCurriculum =
            userCurriculumRepository.findById(userId).orElseThrow {
                CustomException(DomainErrorCode.CURRICULUM_NOT_SELECTED)
            }

        val primaryTopic =
            sessionTopicSnapshotRepository.findBySessionIdAndSequence(sessionId, SessionConstants.SNAPSHOT_SEQUENCE)
                ?: throw CustomException(DomainErrorCode.LESSON_TOPIC_NOT_FOUND)

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

        return RewardResponse.from(session, profile, badgeLevelUp, nextBadge)
    }
}
