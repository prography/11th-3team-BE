package org.prography.samsung.backend.session.usecase

import org.prography.samsung.backend.session.dto.response.SessionHistoryResponse
import org.prography.samsung.backend.session.service.SessionHistoryService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SessionHistoryUsecase(private val sessionHistoryService: SessionHistoryService) {
    @Transactional(readOnly = true)
    fun getHistory(userId: Long, cursor: String?, size: Int): SessionHistoryResponse =
        sessionHistoryService.getHistory(userId, cursor, size)
}
