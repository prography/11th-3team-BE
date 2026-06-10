package org.prography.samsung.backend.session.controller

import jakarta.validation.Valid
import org.prography.samsung.backend.common.auth.CurrentUserHolder
import org.prography.samsung.backend.common.response.SuccessCode
import org.prography.samsung.backend.common.web.ApiResponseFactory
import org.prography.samsung.backend.session.dto.SessionCompleteRequest
import org.prography.samsung.backend.session.dto.SessionStartRequest
import org.prography.samsung.backend.session.service.SessionHistoryService
import org.prography.samsung.backend.session.service.SessionService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class SessionController(
    private val sessionService: SessionService,
    private val sessionHistoryService: SessionHistoryService,
) {
    @GetMapping("/session/status")
    fun getStatus() = ApiResponseFactory.success(
        SuccessCode.OK,
        sessionService.getStatus(CurrentUserHolder.get().userId),
    )

    @GetMapping("/session/today")
    fun getToday() = ApiResponseFactory.success(
        SuccessCode.OK,
        sessionService.getToday(CurrentUserHolder.get().userId),
    )

    @PostMapping("/session/start")
    fun start(@RequestBody(required = false) request: SessionStartRequest?) = ApiResponseFactory.success(
        SuccessCode.CREATED,
        sessionService.start(CurrentUserHolder.get().userId, request?.curriculumId),
    )

    @GetMapping("/session/{sessionId}/lesson")
    fun getLesson(@PathVariable sessionId: String) = ApiResponseFactory.success(
        SuccessCode.OK,
        sessionService.getLesson(CurrentUserHolder.get().userId, sessionId),
    )

    @PostMapping("/session/{sessionId}/advance-phase")
    fun advancePhase(@PathVariable sessionId: String) = ApiResponseFactory.success(
        SuccessCode.OK,
        sessionService.advancePhase(CurrentUserHolder.get().userId, sessionId),
    )

    @GetMapping("/session/{sessionId}/reaction")
    fun getReaction(@PathVariable sessionId: String) = ApiResponseFactory.success(
        SuccessCode.OK,
        sessionService.getReaction(CurrentUserHolder.get().userId, sessionId),
    )

    @PostMapping("/session/complete")
    fun complete(@Valid @RequestBody request: SessionCompleteRequest) = ApiResponseFactory.success(
        SuccessCode.OK,
        sessionService.complete(CurrentUserHolder.get().userId, request.sessionId),
    )

    @GetMapping("/session/{sessionId}/reward")
    fun getReward(@PathVariable sessionId: String) = ApiResponseFactory.success(
        SuccessCode.OK,
        sessionService.getReward(CurrentUserHolder.get().userId, sessionId),
    )

    @PostMapping("/session/{sessionId}/reward/ack")
    fun acknowledgeReward(@PathVariable sessionId: String) = ApiResponseFactory.success(
        SuccessCode.OK,
        sessionService.acknowledgeReward(CurrentUserHolder.get().userId, sessionId),
    )

    @PostMapping("/session/{sessionId}/abort")
    fun abort(@PathVariable sessionId: String) = ApiResponseFactory.success(
        SuccessCode.OK,
        sessionService.abort(CurrentUserHolder.get().userId, sessionId),
    )

    @GetMapping("/sessions/history")
    fun getHistory(@RequestParam(required = false) cursor: String?, @RequestParam(defaultValue = "20") size: Int) =
        ApiResponseFactory.success(
            SuccessCode.OK,
            sessionHistoryService.getHistory(CurrentUserHolder.get().userId, cursor, size),
        )
}
