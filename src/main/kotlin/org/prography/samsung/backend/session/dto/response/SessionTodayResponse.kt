package org.prography.samsung.backend.session.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import org.prography.samsung.backend.common.dto.ActiveSessionResponse
import org.prography.samsung.backend.curriculum.dto.response.TodayTopicResponse

data class SessionTodayResponse(
    val curriculumId: Long,
    val sessionTitle: String,
    val topics: List<TodayTopicResponse>,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val activeSession: ActiveSessionResponse?,
)
