package org.prography.samsung.backend.session.dto

import com.fasterxml.jackson.annotation.JsonInclude

data class SessionHistoryItemResponse(
    val sessionId: String,
    val date: String,
    val topic: String,
    val coins: Int,
    val badgeLevelUp: Boolean,
)

data class SessionHistoryResponse(
    val sessions: List<SessionHistoryItemResponse>,
    val hasMore: Boolean,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val nextCursor: String?,
)
