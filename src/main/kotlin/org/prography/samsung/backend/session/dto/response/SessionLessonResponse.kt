package org.prography.samsung.backend.session.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.prography.samsung.backend.common.domain.ConversationMode
import org.prography.samsung.backend.common.domain.SessionPhase
import org.prography.samsung.backend.curriculum.dto.response.HintNoteResponse
import org.prography.samsung.backend.curriculum.dto.response.LessonQuestionResponse

data class SessionLessonResponse(
    val sessionId: String,
    @field:Schema(
        description = "대화 모드 (소문자 문자열로 직렬화)",
        allowableValues = ["static", "ai_loop"],
        example = "static",
    )
    val conversationMode: ConversationMode,
    @field:Schema(description = "현재 수업 페이즈", allowableValues = ["INTRO", "REACTION"], example = "INTRO")
    val currentPhase: SessionPhase,
    val topicLabel: String,
    val question: LessonQuestionResponse,
    val hintNote: HintNoteResponse,
)
