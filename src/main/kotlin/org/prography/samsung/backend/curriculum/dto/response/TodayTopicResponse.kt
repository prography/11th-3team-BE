package org.prography.samsung.backend.curriculum.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.prography.samsung.backend.common.domain.TopicType

data class TodayTopicResponse(
    val sequence: Int,
    val lessonTopicId: Long,
    val title: String,
    val subtitle: String?,
    @field:Schema(description = "토픽 유형", allowableValues = ["CONCEPT", "CALCULATION"], example = "CONCEPT")
    val topicType: TopicType,
)
