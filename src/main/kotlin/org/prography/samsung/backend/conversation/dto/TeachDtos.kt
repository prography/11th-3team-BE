package org.prography.samsung.backend.conversation.dto

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.prography.samsung.backend.common.domain.AiEmotion

data class TeachRequest(
    @field:NotNull
    @field:Size(max = 500)
    val userText: String,
)

data class AiTurnResponse(
    val speak: String,
    val emotion: AiEmotion,
    val covered: List<String>,
    val missing: List<String>,
    val misconceptionsDetected: List<String>,
    val correctionStage: Int,
    val focusConcept: String,
    val sessionDone: Boolean,
)

data class TeachProgressResponse(val coveredCount: Int, val total: Int)

data class TeachTurnResponse(
    val turn: Int,
    val userText: String,
    val aiResponse: AiTurnResponse,
    val progress: TeachProgressResponse,
)

data class TeachStatusResponse(
    val turn: Int,
    val sessionDone: Boolean,
    val progress: TeachProgressResponse,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val lastAiResponse: AiTurnResponse?,
)
