package org.prography.samsung.backend.conversation.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class TeachRequest(
    @field:NotNull
    @field:Size(max = 500)
    @field:Schema(description = "유저 발화 텍스트 (최대 500자)", example = "주식이란 기업의 소유권을 나타내는 증서입니다.")
    val userText: String,
)
