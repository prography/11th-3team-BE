package org.prography.samsung.backend.session.dto.response

data class SessionStartResponse(val sessionId: String, val startedAt: String, val resumed: Boolean)
