package org.prography.samsung.backend.session.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.prography.samsung.backend.common.domain.SessionPhase

data class SessionPhaseResponse(
    val sessionId: String,
    @field:Schema(description = "현재 수업 페이즈", allowableValues = ["INTRO", "REACTION"], example = "REACTION")
    val currentPhase: SessionPhase,
)
