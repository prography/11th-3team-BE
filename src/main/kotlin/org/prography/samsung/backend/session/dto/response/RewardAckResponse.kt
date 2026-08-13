package org.prography.samsung.backend.session.dto.response

data class RewardAckResponse(val sessionId: String, val acknowledged: Boolean, val rewardAcknowledgedAt: String)
