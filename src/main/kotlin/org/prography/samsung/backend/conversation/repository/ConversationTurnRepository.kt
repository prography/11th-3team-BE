package org.prography.samsung.backend.conversation.repository

import org.prography.samsung.backend.conversation.entity.ConversationTurn
import org.springframework.data.jpa.repository.JpaRepository

interface ConversationTurnRepository : JpaRepository<ConversationTurn, Long> {
    fun findAllBySessionIdOrderByTurnNumberAsc(sessionId: String): List<ConversationTurn>

    fun findTopBySessionIdOrderByTurnNumberDesc(sessionId: String): ConversationTurn?

    fun countBySessionId(sessionId: String): Long
}
