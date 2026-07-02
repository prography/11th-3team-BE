package org.prography.samsung.backend.session.service

import org.prography.samsung.backend.common.domain.SessionStatus
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.common.util.KstDateTimeUtils
import org.prography.samsung.backend.gamification.dto.response.RewardResponse
import org.prography.samsung.backend.session.dto.response.RewardAckResponse
import org.prography.samsung.backend.session.repository.TutoringSessionRepository
import org.prography.samsung.backend.user.service.UserProfileService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class SessionRewardService(
    private val sessionQueryService: SessionQueryService,
    private val userProfileService: UserProfileService,
    private val tutoringSessionRepository: TutoringSessionRepository,
) {
    @Transactional(readOnly = true)
    fun getReward(userId: Long, sessionId: String): RewardResponse {
        val session = sessionQueryService.getOwnedSession(userId, sessionId)
        if (session.status != SessionStatus.COMPLETED) {
            throw CustomException(DomainErrorCode.SESSION_NOT_COMPLETED)
        }
        val profile = userProfileService.getUserProfile(userId)
        return RewardResponse.from(session, profile)
    }

    @Transactional
    fun acknowledgeReward(userId: Long, sessionId: String): RewardAckResponse {
        val session = sessionQueryService.getOwnedSession(userId, sessionId)
        if (session.status != SessionStatus.COMPLETED) {
            throw CustomException(DomainErrorCode.SESSION_NOT_COMPLETED)
        }
        val now = Instant.now()
        session.acknowledgeReward(now)
        tutoringSessionRepository.save(session)
        return RewardAckResponse(
            sessionId = session.id,
            acknowledged = true,
            rewardAcknowledgedAt = KstDateTimeUtils.toOffsetDateTimeString(now),
        )
    }
}
