package org.prography.samsung.backend.common.util

import org.prography.samsung.backend.common.domain.DayOfWeekCode
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.common.response.ErrorBaseCode
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object ScheduleValidator {
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
    private val ALLOWED_TIMES =
        (15..20).map { hour -> LocalTime.of(hour, 0) }

    fun parseDays(days: List<String>): List<DayOfWeekCode> = days.map { DayOfWeekCode.fromString(it) }

    fun validateSchedule(frequency: Int, days: List<DayOfWeekCode>, time: String) {
        if (frequency !in setOf(2, 3)) {
            throw CustomException(DomainErrorCode.INVALID_LESSON_TIME)
        }
        if (days.size != frequency) {
            throw CustomException(DomainErrorCode.SCHEDULE_DAY_COUNT_MISMATCH)
        }
        if (days.distinct().size != days.size) {
            throw CustomException(ErrorBaseCode.BAD_REQUEST)
        }

        val parsedTime =
            runCatching { LocalTime.parse(time, TIME_FORMATTER) }
                .getOrElse { throw CustomException(DomainErrorCode.INVALID_LESSON_TIME) }

        if (parsedTime !in ALLOWED_TIMES) {
            throw CustomException(DomainErrorCode.INVALID_LESSON_TIME)
        }
    }
}
