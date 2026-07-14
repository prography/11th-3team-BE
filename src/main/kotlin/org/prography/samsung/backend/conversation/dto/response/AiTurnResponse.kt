package org.prography.samsung.backend.conversation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.prography.samsung.backend.common.domain.AiEmotion

data class AiTurnResponse(
    val speak: String,
    @field:Schema(
        description = "AI 감정 상태 (소문자 문자열로 직렬화)",
        allowableValues = ["curious", "confused", "thoughtful", "aha", "happy"],
        example = "curious",
    )
    val emotion: AiEmotion,
    val covered: List<String>,
    val missing: List<String>,
    val misconceptionsDetected: List<String>,
    val correctionStage: Int,
    val focusConcept: String,
    val sessionDone: Boolean,
)
