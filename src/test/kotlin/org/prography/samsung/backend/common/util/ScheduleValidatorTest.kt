package org.prography.samsung.backend.common.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.prography.samsung.backend.common.domain.DayOfWeekCode
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.common.response.ErrorBaseCode

@DisplayName("ScheduleValidator 단위 테스트")
class ScheduleValidatorTest {
    @Test
    @DisplayName("요일 문자열을 DayOfWeekCode로 파싱한다")
    fun shouldParseDayOfWeekCodes() {
        val result = ScheduleValidator.parseDays(listOf("TUE", "THU"))

        assertEquals(listOf(DayOfWeekCode.TUE, DayOfWeekCode.THU), result)
    }

    @ParameterizedTest
    @CsvSource("15:00", "17:00", "20:00")
    @DisplayName("허용된 정각 시간이면 검증을 통과한다")
    fun shouldPassValidationForAllowedTimes(time: String) {
        ScheduleValidator.validateSchedule(
            frequency = 2,
            days = listOf(DayOfWeekCode.TUE, DayOfWeekCode.THU),
            time = time,
        )
    }

    @Test
    @DisplayName("요일 개수가 frequency와 다르면 SCHEDULE_DAY_COUNT_MISMATCH를 던진다")
    fun shouldThrowWhenDayCountMismatch() {
        val exception =
            assertThrows(CustomException::class.java) {
                ScheduleValidator.validateSchedule(
                    frequency = 3,
                    days = listOf(DayOfWeekCode.TUE, DayOfWeekCode.THU),
                    time = "17:00",
                )
            }

        assertEquals(DomainErrorCode.SCHEDULE_DAY_COUNT_MISMATCH, exception.errorCode)
    }

    @Test
    @DisplayName("중복 요일이면 BAD_REQUEST를 던진다")
    fun shouldThrowWhenDuplicateDays() {
        val exception =
            assertThrows(CustomException::class.java) {
                ScheduleValidator.validateSchedule(
                    frequency = 2,
                    days = listOf(DayOfWeekCode.TUE, DayOfWeekCode.TUE),
                    time = "17:00",
                )
            }

        assertEquals(ErrorBaseCode.BAD_REQUEST, exception.errorCode)
    }

    @ParameterizedTest
    @CsvSource("14:00", "21:00", "17:30")
    @DisplayName("허용되지 않은 시간이면 INVALID_LESSON_TIME을 던진다")
    fun shouldThrowWhenLessonTimeInvalid(time: String) {
        val exception =
            assertThrows(CustomException::class.java) {
                ScheduleValidator.validateSchedule(
                    frequency = 2,
                    days = listOf(DayOfWeekCode.TUE, DayOfWeekCode.THU),
                    time = time,
                )
            }

        assertEquals(DomainErrorCode.INVALID_LESSON_TIME, exception.errorCode)
    }
}
