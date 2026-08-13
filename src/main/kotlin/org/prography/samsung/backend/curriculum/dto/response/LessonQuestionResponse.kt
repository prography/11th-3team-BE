package org.prography.samsung.backend.curriculum.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.prography.samsung.backend.common.domain.AiEmotion

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
