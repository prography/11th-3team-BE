package org.prography.samsung.backend.common.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object KstDateTimeUtils {
    val KST: ZoneId = ZoneId.of("Asia/Seoul")
    private val ISO_OFFSET_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun todayKst(): LocalDate = LocalDate.now(KST)

    fun toOffsetDateTimeString(instant: Instant): String = instant.atZone(KST).format(ISO_OFFSET_FORMATTER)

    fun toDateString(instant: Instant): String = instant.atZone(KST).toLocalDate().format(DATE_FORMATTER)

    fun toTimeString(time: LocalTime): String = time.format(DateTimeFormatter.ofPattern("HH:mm"))
}
