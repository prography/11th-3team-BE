package org.prography.samsung.backend.user.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.prography.samsung.backend.common.auth.CurrentUserHolder
import org.prography.samsung.backend.common.response.SuccessCode
import org.prography.samsung.backend.common.web.ApiResponseFactory
import org.prography.samsung.backend.user.dto.OnboardingRequest
import org.prography.samsung.backend.user.dto.UserScheduleRequest
import org.prography.samsung.backend.user.service.OnboardingService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Onboarding", description = "온보딩 API")
@RestController
class OnboardingController(private val onboardingService: OnboardingService) {
    @Operation(
        summary = "온보딩 완료 상태 조회",
        description = "온보딩 완료 여부와 현재 단계를 반환합니다. 앱 진입(APP-ENTRY) 시 홈 또는 온보딩 화면으로 라우팅 판단에 사용합니다.",
    )
    @GetMapping("/user/onboarding/status")
    fun getStatus() = ApiResponseFactory.success(
        SuccessCode.OK,
        onboardingService.getStatus(CurrentUserHolder.get().userId),
    )

    @Operation(
        summary = "온보딩 커리큘럼 선택 저장",
        description = "단원 선택 화면(SCR-OB01)에서 선택한 커리큘럼을 저장합니다. user_curriculums를 upsert합니다.",
    )
    @PostMapping("/user/onboarding")
    fun saveCurriculum(@Valid @RequestBody request: OnboardingRequest) = ApiResponseFactory.success(
        SuccessCode.OK,
        onboardingService.saveCurriculum(CurrentUserHolder.get().userId, request),
    )

    @Operation(
        summary = "온보딩 과외 스케줄 저장",
        description = "시간표 화면(SCR-OB02)에서 설정한 빈도·요일·시간을 저장합니다. 기존 스케줄이 있으면 덮어씁니다.",
    )
    @PostMapping("/user/schedule")
    fun saveSchedule(@Valid @RequestBody request: UserScheduleRequest) = ApiResponseFactory.success(
        SuccessCode.OK,
        onboardingService.saveSchedule(CurrentUserHolder.get().userId, request),
    )

    @Operation(
        summary = "온보딩 완료",
        description = "온보딩을 완료 처리합니다. 스케줄 미등록 상태에서 호출하면 400을 반환합니다. 시간표 화면(SCR-OB02)에서 [설정 완료 및 홈으로 가기] 마지막 단계에 호출합니다.",
    )
    @PostMapping("/user/onboarding/complete")
    fun completeOnboarding() = ApiResponseFactory.success(
        SuccessCode.OK,
        onboardingService.completeOnboarding(CurrentUserHolder.get().userId),
    )
}
