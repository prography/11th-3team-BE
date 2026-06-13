package org.prography.samsung.backend.conversation.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.prography.samsung.backend.common.domain.ConversationMode
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.common.response.ErrorBaseCode
import org.prography.samsung.backend.conversation.config.ConversationLlmProperties
import org.prography.samsung.backend.conversation.dto.TeachProgressResponse
import org.prography.samsung.backend.conversation.dto.TeachRequest
import org.prography.samsung.backend.conversation.dto.TeachStatusResponse
import org.prography.samsung.backend.conversation.dto.TeachTurnResponse
import org.prography.samsung.backend.conversation.entity.ConversationTurn
import org.prography.samsung.backend.conversation.repository.ConversationTurnRepository
import org.prography.samsung.backend.conversation.repository.CurriculumUnitRepository
import org.prography.samsung.backend.session.entity.TutoringSession
import org.prography.samsung.backend.session.repository.TutoringSessionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TeachService(
    private val tutoringSessionRepository: TutoringSessionRepository,
    private val conversationTurnRepository: ConversationTurnRepository,
    private val curriculumUnitRepository: CurriculumUnitRepository,
    private val llmConversationService: LlmConversationService,
    private val aiResponseValidator: AiResponseValidator,
    private val properties: ConversationLlmProperties,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun teach(userId: Long, sessionId: String, request: TeachRequest): TeachTurnResponse {
        val session = getAiLoopSession(userId, sessionId)
        val userText = request.userText.trim()
        if (userText.isBlank()) {
            throw CustomException(DomainErrorCode.TEACH_EMPTY_USER_TEXT)
        }
        if (session.turnCount >= properties.maxTurnsPerSession) {
            throw CustomException(DomainErrorCode.TEACH_TURN_LIMIT_EXCEEDED)
        }
        if (conversationTurnRepository.countBySessionId(sessionId) >= properties.maxLlmCallsPerSession) {
            throw CustomException(DomainErrorCode.TEACH_TURN_LIMIT_EXCEEDED)
        }

        val unit =
            curriculumUnitRepository.findFirstByCurriculumIdOrderByIdAsc(session.curriculum.id)
                ?: throw CustomException(ErrorBaseCode.NOT_FOUND_ENTITY)

        val previousTurns = conversationTurnRepository.findAllBySessionIdOrderByTurnNumberAsc(sessionId)
        val accumulatedCovered = session.getCoveredConceptList(objectMapper)
        val repeatedFocusCount = countRepeatedFocus(previousTurns)

        val aiResponse =
            llmConversationService.generateTurn(
                unit = unit,
                previousTurns = previousTurns,
                userText = userText,
                accumulatedCovered = accumulatedCovered,
                repeatedFocusCount = repeatedFocusCount,
            )

        val turnNumber = session.turnCount + 1
        conversationTurnRepository.save(
            ConversationTurn(
                session = session,
                turnNumber = turnNumber,
                userText = userText,
                aiResponseJson = aiResponseValidator.toJson(aiResponse),
            ),
        )

        session.recordTurn(aiResponse.covered, objectMapper)
        tutoringSessionRepository.save(session)

        val total = aiResponseValidator.totalConcepts(unit.unitJson)
        return TeachTurnResponse(
            turn = turnNumber,
            userText = userText,
            aiResponse = aiResponse,
            progress = TeachProgressResponse(
                coveredCount = aiResponse.covered.size,
                total = total,
            ),
        )
    }

    @Transactional(readOnly = true)
    fun getStatus(userId: Long, sessionId: String): TeachStatusResponse {
        val session = getAiLoopSession(userId, sessionId)
        val unit =
            curriculumUnitRepository.findFirstByCurriculumIdOrderByIdAsc(session.curriculum.id)
                ?: throw CustomException(ErrorBaseCode.NOT_FOUND_ENTITY)
        val lastTurn = conversationTurnRepository.findTopBySessionIdOrderByTurnNumberDesc(sessionId)
        val covered = session.getCoveredConceptList(objectMapper)
        val total = aiResponseValidator.totalConcepts(unit.unitJson)
        val sessionDone = lastTurn?.let { aiResponseValidator.fromJson(it.aiResponseJson).sessionDone } ?: false

        return TeachStatusResponse(
            turn = session.turnCount,
            sessionDone = sessionDone,
            progress = TeachProgressResponse(coveredCount = covered.size, total = total),
            lastAiResponse = lastTurn?.let { aiResponseValidator.fromJson(it.aiResponseJson) },
        )
    }

    private fun getAiLoopSession(userId: Long, sessionId: String): TutoringSession {
        val session =
            tutoringSessionRepository.findByUserIdAndId(userId, sessionId)
                ?: throw CustomException(ErrorBaseCode.NOT_FOUND_ENTITY)
        if (session.conversationMode != ConversationMode.AI_LOOP) {
            throw CustomException(DomainErrorCode.TEACH_SESSION_NOT_AI_LOOP)
        }
        if (session.status != org.prography.samsung.backend.common.domain.SessionStatus.STARTED) {
            throw CustomException(DomainErrorCode.SESSION_NOT_STARTED)
        }
        return session
    }

    private fun countRepeatedFocus(previousTurns: List<ConversationTurn>): Int {
        if (previousTurns.size < 2) return 0
        val lastFocus = aiResponseValidator.fromJson(previousTurns.last().aiResponseJson).focusConcept
        return previousTurns
            .takeLast(3)
            .count { aiResponseValidator.fromJson(it.aiResponseJson).focusConcept == lastFocus }
    }
}
