package org.prography.samsung.backend.session.dto.response

import com.fasterxml.jackson.annotation.JsonInclude

data class SessionStatusResponse(
    val lessonCompletedToday: Boolean,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val activeSession: ActiveSessionResponse?,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val pendingRewardSessionId: String?,
)
