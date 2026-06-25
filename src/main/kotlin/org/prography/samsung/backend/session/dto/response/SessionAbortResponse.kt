package org.prography.samsung.backend.session.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.prography.samsung.backend.common.domain.SessionStatus

data class SessionAbortResponse(
    val sessionId: String,
    @field:Schema(description = "세션 상태", allowableValues = ["STARTED", "COMPLETED", "ABORTED"], example = "ABORTED")
    val status: SessionStatus,
)
