package org.prography.samsung.backend.session.usecase

import org.prography.samsung.backend.gamification.dto.response.RewardResponse
import org.prography.samsung.backend.session.dto.response.RewardAckResponse
import org.prography.samsung.backend.session.service.SessionCompletionService
import org.prography.samsung.backend.session.service.SessionRewardService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SessionCompletionUsecase(
    private val sessionCompletionService: SessionCompletionService,
    private val sessionRewardService: SessionRewardService,
) {
    @Transactional
    fun complete(userId: Long, sessionId: String): RewardResponse = sessionCompletionService.complete(userId, sessionId)

    @Transactional(readOnly = true)
    fun getReward(userId: Long, sessionId: String): RewardResponse = sessionRewardService.getReward(userId, sessionId)

    @Transactional
    fun acknowledgeReward(userId: Long, sessionId: String): RewardAckResponse =
        sessionRewardService.acknowledgeReward(userId, sessionId)
}
