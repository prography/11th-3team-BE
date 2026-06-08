package org.prography.samsung.backend.common.domain

enum class DayOfWeekCode {
    MON,
    TUE,
    WED,
    THU,
    FRI,
    SAT,
    SUN,
    ;

    companion object {
        fun fromString(value: String): DayOfWeekCode = entries.firstOrNull { it.name == value }
            ?: throw IllegalArgumentException("Invalid day of week: $value")
    }
}
