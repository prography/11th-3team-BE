package org.prography.samsung.backend.curriculum.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.prography.samsung.backend.common.response.SuccessCode
import org.prography.samsung.backend.common.web.ApiResponseFactory
import org.prography.samsung.backend.curriculum.service.CurriculumService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Curriculum", description = "커리큘럼 API")
@RestController
class CurriculumController(private val curriculumService: CurriculumService) {
    @Operation(
        summary = "커리큘럼 목록 조회",
        description = "활성화된 커리큘럼 목록을 반환합니다. 온보딩 단원 선택 화면(SCR-OB01)에서 칩 목록 렌더에 사용합니다.",
    )
    @GetMapping("/curriculum")
    fun getCurriculums() = ApiResponseFactory.success(SuccessCode.OK, curriculumService.getActiveCurriculums())
}
