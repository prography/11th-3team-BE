package org.prography.samsung.backend.conversation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.prography.samsung.backend.common.auth.CurrentUserHolder
import org.prography.samsung.backend.common.response.SuccessCode
import org.prography.samsung.backend.common.web.ApiResponseFactory
import org.prography.samsung.backend.conversation.dto.TeachRequest
import org.prography.samsung.backend.conversation.service.TeachService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Teach", description = "AI 대화 루프 teach API")
@RestController
class TeachController(private val teachService: TeachService) {
    @Operation(
        summary = "teach 1턴",
        description = "유저 텍스트 1턴을 받아 LLM JSON 응답을 반환합니다. ai_loop 세션 전용입니다.",
    )
    @PostMapping("/session/{sessionId}/teach")
    fun teach(@PathVariable sessionId: String, @Valid @RequestBody request: TeachRequest) = ApiResponseFactory.success(
        SuccessCode.OK,
        teachService.teach(CurrentUserHolder.get().userId, sessionId, request),
    )

    @Operation(
        summary = "teach 진행 상태",
        description = "현재 턴·진행도·sessionDone을 반환합니다.",
    )
    @GetMapping("/session/{sessionId}/teach/status")
    fun getStatus(@PathVariable sessionId: String) = ApiResponseFactory.success(
        SuccessCode.OK,
        teachService.getStatus(CurrentUserHolder.get().userId, sessionId),
    )
}
