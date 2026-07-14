package org.prography.samsung.backend.conversation.dto.response

import com.fasterxml.jackson.annotation.JsonInclude

data class TeachStatusResponse(
    val turn: Int,
    val sessionDone: Boolean,
    val progress: TeachProgressResponse,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val lastAiResponse: AiTurnResponse?,
)
