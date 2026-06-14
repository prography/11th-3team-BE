package org.prography.samsung.backend.conversation.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.prography.samsung.backend.common.domain.AiEmotion

data class TeachRequest(
    @field:NotNull
    @field:Size(max = 500)
    @field:Schema(description = "유저 발화 텍스트 (최대 500자)", example = "주식이란 기업의 소유권을 나타내는 증서입니다.")
    val userText: String,
)

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
