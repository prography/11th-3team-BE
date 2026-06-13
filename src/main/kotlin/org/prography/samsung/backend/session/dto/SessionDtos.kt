package org.prography.samsung.backend.session.dto

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.NotBlank
import org.prography.samsung.backend.common.domain.ConversationMode
import org.prography.samsung.backend.common.domain.SessionPhase
import org.prography.samsung.backend.common.dto.ActiveSessionResponse
import org.prography.samsung.backend.common.dto.HintNoteResponse
import org.prography.samsung.backend.common.dto.LessonQuestionResponse
import org.prography.samsung.backend.common.dto.TodayTopicResponse

data class SessionStartRequest(val curriculumId: Long? = null, val conversationMode: ConversationMode? = null)

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
    val conversationMode: ConversationMode,
    val currentPhase: SessionPhase,
    val topicLabel: String,
    val question: LessonQuestionResponse,
    val hintNote: HintNoteResponse,
)

data class SessionPhaseResponse(val sessionId: String, val currentPhase: SessionPhase)

data class SessionCompleteRequest(@field:NotBlank val sessionId: String)

data class SessionAbortResponse(
    val sessionId: String,
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
