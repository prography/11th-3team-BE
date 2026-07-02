package org.prography.samsung.backend.session.dto.response

data class SessionHistoryItemResponse(
    val sessionId: String,
    val date: String,
    val topic: String,
    val coins: Int,
    val badgeLevelUp: Boolean,
)
