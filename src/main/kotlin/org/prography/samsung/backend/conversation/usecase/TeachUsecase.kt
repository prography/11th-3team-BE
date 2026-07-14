package org.prography.samsung.backend.conversation.usecase

import com.fasterxml.jackson.databind.ObjectMapper
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.conversation.config.ConversationLlmProperties
import org.prography.samsung.backend.conversation.dto.request.TeachRequest
import org.prography.samsung.backend.conversation.dto.response.TeachProgressResponse
import org.prography.samsung.backend.conversation.dto.response.TeachStatusResponse
import org.prography.samsung.backend.conversation.dto.response.TeachTurnResponse
import org.prography.samsung.backend.conversation.service.ConversationTurnService
import org.prography.samsung.backend.conversation.service.LlmConversationService
import org.prography.samsung.backend.conversation.util.AiResponseValidator
import org.prography.samsung.backend.curriculum.service.CurriculumService
import org.prography.samsung.backend.session.SessionConstants
import org.prography.samsung.backend.session.service.SessionQueryService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TeachUsecase(
    private val sessionQueryService: SessionQueryService,
    private val curriculumService: CurriculumService,
    private val conversationTurnService: ConversationTurnService,
    private val llmConversationService: LlmConversationService,
    private val aiResponseValidator: AiResponseValidator,
    private val properties: ConversationLlmProperties,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun teach(userId: Long, sessionId: String, request: TeachRequest): TeachTurnResponse {
        val session = sessionQueryService.getStartedAiLoopSession(userId, sessionId)
        val userText = request.userText.trim()
        if (userText.isBlank()) {
            throw CustomException(DomainErrorCode.TEACH_EMPTY_USER_TEXT)
        }
        if (session.turnCount >= properties.maxTurnsPerSession) {
            throw CustomException(DomainErrorCode.TEACH_TURN_LIMIT_EXCEEDED)
        }
        if (conversationTurnService.countBySessionId(sessionId) >= properties.maxLlmCallsPerSession) {
            throw CustomException(DomainErrorCode.TEACH_TURN_LIMIT_EXCEEDED)
        }

        val unit = curriculumService.resolveCurriculumUnit(
            lessonTopic = session.lessonTopic,
            curriculumId = session.curriculum.id,
            sequence = SessionConstants.SNAPSHOT_SEQUENCE,
        )
        val previousTurns = conversationTurnService.getPreviousTurns(sessionId)
        val accumulatedCovered = session.getCoveredConceptList(objectMapper)
        val repeatedFocusCount = conversationTurnService.countRepeatedFocus(previousTurns)

        val aiResponse =
            llmConversationService.generateTurn(
                unit = unit,
                previousTurns = previousTurns,
                userText = userText,
                accumulatedCovered = accumulatedCovered,
                repeatedFocusCount = repeatedFocusCount,
            )

        val turnNumber = session.turnCount + 1
        conversationTurnService.saveTurn(
            session = session,
            turnNumber = turnNumber,
            userText = userText,
            aiResponse = aiResponse,
        )

        val mergedCovered = aiResponseValidator.mergeCovered(accumulatedCovered, aiResponse.covered)
        session.recordTurn(mergedCovered, objectMapper)

        val total = aiResponseValidator.totalConcepts(unit.unitJson)
        return TeachTurnResponse(
            turn = turnNumber,
            userText = userText,
            aiResponse = aiResponse,
            progress = TeachProgressResponse(
                coveredCount = mergedCovered.size,
                total = total,
            ),
        )
    }

    @Transactional(readOnly = true)
    fun getStatus(userId: Long, sessionId: String): TeachStatusResponse {
        val session = sessionQueryService.getStartedAiLoopSession(userId, sessionId)
        val unit = curriculumService.resolveCurriculumUnit(
            lessonTopic = session.lessonTopic,
            curriculumId = session.curriculum.id,
            sequence = SessionConstants.SNAPSHOT_SEQUENCE,
        )
        val covered = session.getCoveredConceptList(objectMapper)
        val lastAiResponse = conversationTurnService.getLastAiResponse(sessionId)
        val total = aiResponseValidator.totalConcepts(unit.unitJson)

        return TeachStatusResponse(
            turn = session.turnCount,
            sessionDone = lastAiResponse?.sessionDone ?: false,
            progress = TeachProgressResponse(coveredCount = covered.size, total = total),
            lastAiResponse = lastAiResponse,
        )
    }
}
