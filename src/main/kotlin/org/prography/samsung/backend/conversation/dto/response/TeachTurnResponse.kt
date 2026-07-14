package org.prography.samsung.backend.conversation.dto.response

data class TeachTurnResponse(
    val turn: Int,
    val userText: String,
    val aiResponse: AiTurnResponse,
    val progress: TeachProgressResponse,
)
