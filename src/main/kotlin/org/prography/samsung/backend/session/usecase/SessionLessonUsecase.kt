package org.prography.samsung.backend.session.usecase

import org.prography.samsung.backend.common.domain.SessionPhase
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.curriculum.service.CurriculumService
import org.prography.samsung.backend.session.SessionConstants
import org.prography.samsung.backend.session.dto.response.SessionLessonResponse
import org.prography.samsung.backend.session.dto.response.SessionStatusResponse
import org.prography.samsung.backend.session.dto.response.SessionTodayResponse
import org.prography.samsung.backend.session.service.SessionQueryService
import org.prography.samsung.backend.user.service.UserProfileService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SessionLessonUsecase(
    private val sessionQueryService: SessionQueryService,
    private val curriculumService: CurriculumService,
    private val userProfileService: UserProfileService,
) {
    @Transactional(readOnly = true)
    fun getToday(userId: Long): SessionTodayResponse {
        val userCurriculum = userProfileService.getUserCurriculum(userId)
        val curriculum = userCurriculum.curriculum
        val completedTopicIds = sessionQueryService.findCompletedTopicIdsToday(userId)
        val topics = curriculumService.getTodayTopics(curriculum.id, completedTopicIds)
        val activeSession = sessionQueryService.findActiveSession(userId)
        return SessionTodayResponse(
            curriculumId = curriculum.id,
            sessionTitle = curriculum.sessionTitleTemplate,
            topics = topics,
            activeSession = activeSession,
        )
    }

    @Transactional(readOnly = true)
    fun getLesson(userId: Long, sessionId: String): SessionLessonResponse =
        buildPhaseResponse(userId, sessionId, SessionPhase.INTRO, SessionConstants.SNAPSHOT_SEQUENCE)

    @Transactional(readOnly = true)
    fun getReaction(userId: Long, sessionId: String): SessionLessonResponse =
        buildPhaseResponse(userId, sessionId, SessionPhase.REACTION, SessionConstants.SNAPSHOT_SEQUENCE)

    @Transactional(readOnly = true)
    fun getStatus(userId: Long): SessionStatusResponse = sessionQueryService.getStatus(userId)

    private fun buildPhaseResponse(
        userId: Long,
        sessionId: String,
        expectedPhase: SessionPhase,
        topicSequence: Int,
    ): SessionLessonResponse {
        val (session, snapshot) = sessionQueryService.getStartedSessionWithSnapshot(userId, sessionId, topicSequence)
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
