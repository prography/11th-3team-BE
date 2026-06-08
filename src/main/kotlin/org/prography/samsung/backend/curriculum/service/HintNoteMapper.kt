package org.prography.samsung.backend.curriculum.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.prography.samsung.backend.common.dto.HintNoteResponse
import org.springframework.stereotype.Component

@Component
class HintNoteMapper(private val objectMapper: ObjectMapper) {
    fun toResponse(contentJson: String): HintNoteResponse =
        objectMapper.readValue(contentJson, HintNoteResponse::class.java)
}
