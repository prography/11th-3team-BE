package org.prography.samsung.backend.common.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.prography.samsung.backend.common.domain.AiEmotion
import org.prography.samsung.backend.common.domain.SessionPhase
import org.prography.samsung.backend.common.domain.SessionStatus
import org.prography.samsung.backend.common.domain.TopicType

data class CurriculumChipResponse(val id: Long, val code: String, val name: String, val displayOrder: Int)

data class LevelResponse(val number: Int, val name: String)

data class CurriculumSummaryResponse(val id: Long, val name: String, val displayName: String)

data class UserScheduleResponse(
    @field:Schema(description = "주당 수업 횟수", allowableValues = ["2", "3"], example = "3")
    val frequency: Int,

    @field:ArraySchema(
        schema = Schema(
            description = "수업 요일",
            allowableValues = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"],
            example = "MON",
        ),
    )
    val days: List<String>,

    @field:Schema(
        description = "수업 시간 (KST, HH:mm 형식, 15:00~20:00 정시)",
        example = "18:00",
    )
    val time: String,
)

data class ActiveSessionResponse(
    val sessionId: String,
    @field:Schema(description = "세션 상태", allowableValues = ["STARTED", "COMPLETED", "ABORTED"], example = "STARTED")
    val status: SessionStatus,
    @field:Schema(description = "현재 수업 페이즈", allowableValues = ["INTRO", "REACTION"], example = "INTRO")
    val currentPhase: SessionPhase,
    val startedAt: String,
)

data class TodayTopicResponse(
    val sequence: Int,
    val lessonTopicId: Long,
    val title: String,
    val subtitle: String?,
    @field:Schema(description = "토픽 유형", allowableValues = ["CONCEPT", "CALCULATION"], example = "CONCEPT")
    val topicType: TopicType,
)

data class HintNoteHeaderResponse(val chapter: String, val title: String)

data class HintNoteSectionResponse(val id: String, val title: String?, val bodyHtml: String, val highlight: Boolean)

data class HintNoteResponse(val header: HintNoteHeaderResponse, val sections: List<HintNoteSectionResponse>)

data class LessonQuestionResponse(
    val bubbleText: String,
    val speak: String,
    @field:Schema(
        description = "AI 감정 상태 (소문자 문자열로 직렬화)",
        allowableValues = ["curious", "confused", "thoughtful", "aha", "happy"],
        example = "curious",
    )
    val emotion: AiEmotion,
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
