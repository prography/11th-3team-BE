package org.prography.samsung.backend.common.dto

import com.fasterxml.jackson.annotation.JsonInclude
import org.prography.samsung.backend.common.domain.SessionPhase
import org.prography.samsung.backend.common.domain.SessionStatus
import org.prography.samsung.backend.common.domain.TopicType

data class CurriculumChipResponse(val id: Long, val code: String, val name: String, val displayOrder: Int)

data class LevelResponse(val number: Int, val name: String)

data class CurriculumSummaryResponse(val id: Long, val name: String, val displayName: String)

data class UserScheduleResponse(val frequency: Int, val days: List<String>, val time: String)

data class ActiveSessionResponse(
    val sessionId: String,
    val status: SessionStatus,
    val currentPhase: SessionPhase,
    val startedAt: String,
)

data class TodayTopicResponse(
    val sequence: Int,
    val lessonTopicId: Long,
    val title: String,
    val subtitle: String?,
    val topicType: TopicType,
)

data class HintNoteHeaderResponse(val chapter: String, val title: String)

data class HintNoteSectionResponse(val id: String, val title: String?, val bodyHtml: String, val highlight: Boolean)

data class HintNoteResponse(val header: HintNoteHeaderResponse, val sections: List<HintNoteSectionResponse>)

data class LessonQuestionResponse(
    val bubbleText: String,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val displayAnswerHtml: String? = null,
)

data class RewardResponse(
    val sessionId: String,
    val coinsAwarded: Int,
    val badgeLevelUp: Boolean,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val newLevel: LevelResponse?,
    val progressPercent: Int,
    val totalCoins: Int,
)
