package org.prography.samsung.backend.common.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class AiEmotion(val value: String) {
    CURIOUS("curious"),
    CONFUSED("confused"),
    THOUGHTFUL("thoughtful"),
    AHA("aha"),
    HAPPY("happy"),
    ;

    @JsonValue
    fun toJson(): String = value

    companion object {
        @JsonCreator
        fun fromValue(raw: String): AiEmotion = entries.firstOrNull { it.value == raw }
            ?: CURIOUS
    }
}
