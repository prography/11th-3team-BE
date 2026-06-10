package org.prography.samsung.backend.curriculum.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.ErrorBaseCode
import org.prography.samsung.backend.curriculum.repository.CurriculumRepository
import org.prography.samsung.backend.support.TestFixtures

@ExtendWith(MockitoExtension::class)
@DisplayName("CurriculumService 단위 테스트")
class CurriculumServiceTest {
    private val curriculumRepository: CurriculumRepository = mock()
    private lateinit var sut: CurriculumService

    @BeforeEach
    fun setUp() {
        sut = CurriculumService(curriculumRepository)
    }

    @Test
    @DisplayName("활성 커리큘럼 목록을 DTO로 변환해 반환한다")
    fun shouldReturnActiveCurriculumChips() {
        val curriculum = TestFixtures.curriculum()
        whenever(curriculumRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(listOf(curriculum))

        val result = sut.getActiveCurriculums()

        assertEquals(1, result.size)
        assertEquals(TestFixtures.CURRICULUM_ID, result[0].id)
        assertEquals("FRACTION_CALC", result[0].code)
        assertEquals("분수의 계산", result[0].name)
    }

    @Test
    @DisplayName("존재하지 않는 커리큘럼이면 NOT_FOUND_ENTITY를 던진다")
    fun shouldThrowWhenCurriculumNotFound() {
        whenever(curriculumRepository.findByIdAndIsActiveTrue(99L)).thenReturn(null)

        val exception =
            assertThrows(CustomException::class.java) {
                sut.getActiveCurriculumOrThrow(99L)
            }

        assertEquals(ErrorBaseCode.NOT_FOUND_ENTITY, exception.errorCode)
    }
}
