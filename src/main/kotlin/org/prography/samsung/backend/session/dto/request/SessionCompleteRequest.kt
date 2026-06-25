package org.prography.samsung.backend.session.dto.request

import jakarta.validation.constraints.NotBlank

data class SessionCompleteRequest(@field:NotBlank val sessionId: String)
