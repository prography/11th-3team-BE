package org.prography.samsung.backend.session.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
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

@Tag(name = "Session", description = "수업 세션 API")
@RestController
class SessionController(
    private val sessionService: SessionService,
    private val sessionHistoryService: SessionHistoryService,
) {
    @Operation(
        summary = "세션 상태 조회",
        description = "현재 진행 중인 세션, 오늘 수업 완료 여부, 미수령 보상 세션 ID를 반환합니다. 앱 진입(APP-ENTRY) 및 홈(SCR-HOME)에서 사용합니다.",
    )
    @GetMapping("/session/status")
    fun getStatus() = ApiResponseFactory.success(
        SuccessCode.OK,
        sessionService.getStatus(CurrentUserHolder.get().userId),
    )

    @Operation(
        summary = "오늘의 수업 토픽 조회",
        description = "사용자의 커리큘럼에 해당하는 오늘의 수업 토픽 목록을 반환합니다. 준비 화면(SCR-PREP) 진입 시 호출합니다.",
    )
    @GetMapping("/session/today")
    fun getToday() = ApiResponseFactory.success(
        SuccessCode.OK,
        sessionService.getToday(CurrentUserHolder.get().userId),
    )

    @Operation(
        summary = "수업 세션 시작",
        description = "새 수업 세션을 생성합니다. 이미 STARTED 상태의 세션이 있으면 resumed=true로 기존 세션을 반환합니다.\n" +
            "준비 화면(SCR-PREP)에서 [수업 시작하기] 버튼 클릭 시 호출합니다.",
    )
    @PostMapping("/session/start")
    fun start(@RequestBody(required = false) request: SessionStartRequest?) = ApiResponseFactory.success(
        SuccessCode.CREATED,
        sessionService.start(CurrentUserHolder.get().userId, request),
    )

    @Operation(
        summary = "수업(INTRO) 질문 조회",
        description = "INTRO 페이즈의 AI 말풍선 질문, 힌트노트 JSON, 토픽 라벨을 반환합니다. 수업 화면(SCR-LESSON) 진입 시 호출합니다.",
    )
    @GetMapping("/session/{sessionId}/lesson")
    fun getLesson(@PathVariable sessionId: String) = ApiResponseFactory.success(
        SuccessCode.OK,
        sessionService.getLesson(CurrentUserHolder.get().userId, sessionId),
    )

    @Operation(
        summary = "페이즈 진행 (INTRO → REACTION)",
        description = "수업 페이즈를 INTRO에서 REACTION으로 전환합니다. 수업 화면(SCR-LESSON)에서 [설명완료] 버튼 클릭 시 호출합니다.",
    )
    @PostMapping("/session/{sessionId}/advance-phase")
    fun advancePhase(@PathVariable sessionId: String) = ApiResponseFactory.success(
        SuccessCode.OK,
        sessionService.advancePhase(CurrentUserHolder.get().userId, sessionId),
    )

    @Operation(
        summary = "AI 반응(REACTION) 질문 조회",
        description = "REACTION 페이즈의 오답 말풍선, 힌트노트를 반환합니다. AI 반응 화면(SCR-REACTION) 진입 시 호출합니다.",
    )
    @GetMapping("/session/{sessionId}/reaction")
    fun getReaction(@PathVariable sessionId: String) = ApiResponseFactory.success(
        SuccessCode.OK,
        sessionService.getReaction(CurrentUserHolder.get().userId, sessionId),
    )

    @Operation(
        summary = "수업 완료 처리",
        description = "세션을 COMPLETED로 전환하고 코인·배지 정산을 수행합니다. 멱등 처리가 되어 있어 이미 완료된 세션 ID를 전달하면 기존 결과를 반환합니다.\n" +
            "AI 반응 화면(SCR-REACTION)에서 [설명 종료] 버튼 클릭 시 호출합니다.",
    )
    @PostMapping("/session/complete")
    fun complete(@Valid @RequestBody request: SessionCompleteRequest) = ApiResponseFactory.success(
        SuccessCode.OK,
        sessionService.complete(CurrentUserHolder.get().userId, request.sessionId),
    )

    @Operation(
        summary = "보상 정보 조회",
        description = "완료된 세션의 코인 보상, 배지 레벨업 여부를 반환합니다. 보상 화면(SCR-REWARD) 진입 시 호출합니다.",
    )
    @GetMapping("/session/{sessionId}/reward")
    fun getReward(@PathVariable sessionId: String) = ApiResponseFactory.success(
        SuccessCode.OK,
        sessionService.getReward(CurrentUserHolder.get().userId, sessionId),
    )

    @Operation(
        summary = "보상 수령 확인",
        description = "사용자가 보상을 확인했음을 기록합니다. 보상 화면(SCR-REWARD)에서 [확인] 버튼 클릭 시 호출합니다.",
    )
    @PostMapping("/session/{sessionId}/reward/ack")
    fun acknowledgeReward(@PathVariable sessionId: String) = ApiResponseFactory.success(
        SuccessCode.OK,
        sessionService.acknowledgeReward(CurrentUserHolder.get().userId, sessionId),
    )

    @Operation(
        summary = "수업 세션 중단",
        description = "진행 중인 세션을 ABORTED 상태로 전환합니다. 수업 중 [◀] 뒤로가기 confirm 확인 시 호출합니다.",
    )
    @PostMapping("/session/{sessionId}/abort")
    fun abort(@PathVariable sessionId: String) = ApiResponseFactory.success(
        SuccessCode.OK,
        sessionService.abort(CurrentUserHolder.get().userId, sessionId),
    )

    @Operation(
        summary = "수업 기록 조회",
        description = "완료된 수업 세션 목록을 커서 기반 페이지네이션으로 반환합니다. 수업 기록 화면(SCR-HISTORY)에서 사용합니다.",
    )
    @GetMapping("/sessions/history")
    fun getHistory(@RequestParam(required = false) cursor: String?, @RequestParam(defaultValue = "20") size: Int) =
        ApiResponseFactory.success(
            SuccessCode.OK,
            sessionHistoryService.getHistory(CurrentUserHolder.get().userId, cursor, size),
        )
}
