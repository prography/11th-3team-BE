package org.prography.samsung.backend.conversation.service

import org.prography.samsung.backend.conversation.dto.response.AiTurnResponse
import org.prography.samsung.backend.conversation.entity.ConversationTurn
import org.prography.samsung.backend.conversation.repository.ConversationTurnRepository
import org.prography.samsung.backend.conversation.util.AiResponseValidator
import org.prography.samsung.backend.session.entity.TutoringSession
import org.springframework.stereotype.Service

@Service
class ConversationTurnService(
    private val conversationTurnRepository: ConversationTurnRepository,
    private val aiResponseValidator: AiResponseValidator,
) {
    fun countBySessionId(sessionId: String): Long = conversationTurnRepository.countBySessionId(sessionId)

    fun getPreviousTurns(sessionId: String): List<ConversationTurn> =
        conversationTurnRepository.findAllBySessionIdOrderByTurnNumberAsc(sessionId)

    fun getLastAiResponse(sessionId: String): AiTurnResponse? =
        conversationTurnRepository.findTopBySessionIdOrderByTurnNumberDesc(sessionId)
            ?.let { aiResponseValidator.fromJson(it.aiResponseJson) }

    fun saveTurn(
        session: TutoringSession,
        turnNumber: Int,
        userText: String,
        aiResponse: AiTurnResponse,
    ): ConversationTurn = conversationTurnRepository.save(
        ConversationTurn(
            session = session,
            turnNumber = turnNumber,
            userText = userText,
            aiResponseJson = aiResponseValidator.toJson(aiResponse),
        ),
    )

    fun countRepeatedFocus(previousTurns: List<ConversationTurn>): Int {
        if (previousTurns.size < 2) return 0
        val lastFocus = aiResponseValidator.fromJson(previousTurns.last().aiResponseJson).focusConcept
        return previousTurns
            .takeLast(3)
            .count { aiResponseValidator.fromJson(it.aiResponseJson).focusConcept == lastFocus }
    }
}
