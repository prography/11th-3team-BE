package org.prography.samsung.backend.session.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import org.prography.samsung.backend.common.dto.ActiveSessionResponse

data class SessionStatusResponse(
    val lessonCompletedToday: Boolean,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val activeSession: ActiveSessionResponse?,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val pendingRewardSessionId: String?,
)
