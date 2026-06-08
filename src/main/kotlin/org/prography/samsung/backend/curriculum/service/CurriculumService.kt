package org.prography.samsung.backend.curriculum.service

import org.prography.samsung.backend.common.dto.CurriculumChipResponse
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.ErrorBaseCode
import org.prography.samsung.backend.curriculum.repository.CurriculumRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CurriculumService(private val curriculumRepository: CurriculumRepository) {
    @Transactional(readOnly = true)
    fun getActiveCurriculums(): List<CurriculumChipResponse> =
        curriculumRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc().map {
            CurriculumChipResponse(
                id = it.id,
                code = it.code,
                name = it.name,
                displayOrder = it.displayOrder,
            )
        }

    @Transactional(readOnly = true)
    fun getActiveCurriculumOrThrow(curriculumId: Long) = curriculumRepository.findByIdAndIsActiveTrue(curriculumId)
        ?: throw CustomException(ErrorBaseCode.NOT_FOUND_ENTITY)
}
