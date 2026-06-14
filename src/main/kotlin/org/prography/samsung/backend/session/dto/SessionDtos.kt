package org.prography.samsung.backend.session.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import org.prography.samsung.backend.common.domain.ConversationMode
import org.prography.samsung.backend.common.domain.SessionPhase
import org.prography.samsung.backend.common.dto.ActiveSessionResponse
import org.prography.samsung.backend.common.dto.HintNoteResponse
import org.prography.samsung.backend.common.dto.LessonQuestionResponse
import org.prography.samsung.backend.common.dto.TodayTopicResponse

data class SessionStartRequest(
    @field:Schema(description = "커리큘럼 ID (생략 시 현재 커리큘럼 사용)", example = "1")
    val curriculumId: Long? = null,

    @field:Schema(
        description = "대화 모드. 생략 시 static",
        allowableValues = ["static", "ai_loop"],
        example = "static",
    )
    val conversationMode: ConversationMode? = null,
)

data class SessionStartResponse(val sessionId: String, val startedAt: String, val resumed: Boolean)

data class SessionTodayResponse(
    val curriculumId: Long,
    val sessionTitle: String,
    val topics: List<TodayTopicResponse>,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val activeSession: ActiveSessionResponse?,
)

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

data class SessionPhaseResponse(
    val sessionId: String,
    @field:Schema(description = "현재 수업 페이즈", allowableValues = ["INTRO", "REACTION"], example = "REACTION")
    val currentPhase: SessionPhase,
)

data class SessionCompleteRequest(@field:NotBlank val sessionId: String)

data class SessionAbortResponse(
    val sessionId: String,
    @field:Schema(description = "세션 상태", allowableValues = ["STARTED", "COMPLETED", "ABORTED"], example = "ABORTED")
    val status: org.prography.samsung.backend.common.domain.SessionStatus,
)

data class RewardAckResponse(val sessionId: String, val acknowledged: Boolean, val rewardAcknowledgedAt: String)

data class SessionStatusResponse(
    val lessonCompletedToday: Boolean,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val activeSession: ActiveSessionResponse?,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val pendingRewardSessionId: String?,
)
