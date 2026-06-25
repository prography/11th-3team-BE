package org.prography.samsung.backend.common.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.prography.samsung.backend.common.domain.SessionPhase
import org.prography.samsung.backend.common.domain.SessionStatus

data class ActiveSessionResponse(
    val sessionId: String,
    @field:Schema(description = "세션 상태", allowableValues = ["STARTED", "COMPLETED", "ABORTED"], example = "STARTED")
    val status: SessionStatus,
    @field:Schema(description = "현재 수업 페이즈", allowableValues = ["INTRO", "REACTION"], example = "INTRO")
    val currentPhase: SessionPhase,
    val startedAt: String,
)
