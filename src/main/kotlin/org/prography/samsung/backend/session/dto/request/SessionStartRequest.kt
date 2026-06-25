package org.prography.samsung.backend.session.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import org.prography.samsung.backend.common.domain.ConversationMode

data class SessionStartRequest(
    @field:Schema(description = "커리큘럼 ID (생략 시 현재 커리큘럼 사용)", example = "1")
    val curriculumId: Long? = null,

    @field:Schema(
        description = "대화 모드. 생략 시 static",
        allowableValues = ["static", "ai_loop"],
        example = "static",
    )
    val conversationMode: ConversationMode? = null,
)
