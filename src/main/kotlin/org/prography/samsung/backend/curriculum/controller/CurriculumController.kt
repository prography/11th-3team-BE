package org.prography.samsung.backend.curriculum.controller

import org.prography.samsung.backend.common.response.SuccessCode
import org.prography.samsung.backend.common.web.ApiResponseFactory
import org.prography.samsung.backend.curriculum.service.CurriculumService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CurriculumController(private val curriculumService: CurriculumService) {
    @GetMapping("/curriculum")
    fun getCurriculums() = ApiResponseFactory.success(SuccessCode.OK, curriculumService.getActiveCurriculums())
}
