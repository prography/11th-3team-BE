package org.prography.samsung.backend.common.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class ConversationMode(val value: String) {
    STATIC("static"),
    AI_LOOP("ai_loop"),
    ;

    @JsonValue
    fun toJson(): String = value

    companion object {
        @JsonCreator
        fun fromValue(raw: String): ConversationMode = entries.firstOrNull { it.value == raw }
            ?: throw IllegalArgumentException("Unknown conversation mode: $raw")
    }
}
