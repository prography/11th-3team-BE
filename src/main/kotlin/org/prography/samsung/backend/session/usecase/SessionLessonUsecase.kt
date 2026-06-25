package org.prography.samsung.backend.session.usecase

import org.prography.samsung.backend.common.domain.SessionPhase
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.curriculum.service.CurriculumService
import org.prography.samsung.backend.session.SessionConstants
import org.prography.samsung.backend.session.dto.response.SessionLessonResponse
import org.prography.samsung.backend.session.dto.response.SessionPhaseResponse
import org.prography.samsung.backend.session.dto.response.SessionTodayResponse
import org.prography.samsung.backend.session.service.SessionService
import org.prography.samsung.backend.user.service.UserProfileService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SessionLessonUsecase(
    private val sessionService: SessionService,
    private val curriculumService: CurriculumService,
    private val userProfileService: UserProfileService,
) {
    @Transactional(readOnly = true)
    fun getToday(userId: Long): SessionTodayResponse {
        val userCurriculum = userProfileService.getUserCurriculum(userId)
        val curriculum = userCurriculum.curriculum
        val topics = curriculumService.getTodayTopics(curriculum.id)
        val activeSession = sessionService.findActiveSession(userId)
        return SessionTodayResponse(
            curriculumId = curriculum.id,
            sessionTitle = curriculum.sessionTitleTemplate,
            topics = topics,
            activeSession = activeSession,
        )
    }

    @Transactional(readOnly = true)
    fun getLesson(userId: Long, sessionId: String): SessionLessonResponse =
        buildPhaseResponse(userId, sessionId, SessionPhase.INTRO, SessionConstants.INTRO_TOPIC_SEQUENCE)

    @Transactional(readOnly = true)
    fun getReaction(userId: Long, sessionId: String): SessionLessonResponse =
        buildPhaseResponse(userId, sessionId, SessionPhase.REACTION, SessionConstants.REACTION_TOPIC_SEQUENCE)

    @Transactional
    fun advancePhase(userId: Long, sessionId: String): SessionPhaseResponse =
        sessionService.advancePhase(userId, sessionId)

    private fun buildPhaseResponse(
        userId: Long,
        sessionId: String,
        expectedPhase: SessionPhase,
        topicSequence: Int,
    ): SessionLessonResponse {
        val (session, snapshot) = sessionService.getStartedSessionWithSnapshot(userId, sessionId, topicSequence)
        if (session.currentPhase != expectedPhase) {
            throw CustomException(DomainErrorCode.SESSION_PHASE_MISMATCH)
        }
        val content = curriculumService.getLessonContent(snapshot.lessonTopic.id, expectedPhase)
        return SessionLessonResponse(
            sessionId = session.id,
            conversationMode = session.conversationMode,
            currentPhase = expectedPhase,
            topicLabel = snapshot.lessonTopic.gnbTitle,
            question = content.question,
            hintNote = content.hintNote,
        )
    }
}
