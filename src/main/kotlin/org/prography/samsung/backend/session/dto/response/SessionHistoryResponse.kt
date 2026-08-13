package org.prography.samsung.backend.session.dto.response

import com.fasterxml.jackson.annotation.JsonInclude

data class SessionHistoryResponse(
    val sessions: List<SessionHistoryItemResponse>,
    val hasMore: Boolean,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val nextCursor: String?,
)
