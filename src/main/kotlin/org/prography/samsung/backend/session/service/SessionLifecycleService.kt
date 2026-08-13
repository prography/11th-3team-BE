package org.prography.samsung.backend.session.service

import org.prography.samsung.backend.common.domain.ConversationMode
import org.prography.samsung.backend.common.domain.SessionPhase
import org.prography.samsung.backend.common.domain.SessionStatus
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.common.util.KstDateTimeUtils
import org.prography.samsung.backend.curriculum.entity.LessonTopic
import org.prography.samsung.backend.curriculum.service.CurriculumService
import org.prography.samsung.backend.session.SessionConstants
import org.prography.samsung.backend.session.dto.request.SessionStartRequest
import org.prography.samsung.backend.session.dto.response.SessionAbortResponse
import org.prography.samsung.backend.session.dto.response.SessionPhaseResponse
import org.prography.samsung.backend.session.dto.response.SessionStartResponse
import org.prography.samsung.backend.session.entity.SessionTopicSnapshot
import org.prography.samsung.backend.session.entity.TutoringSession
import org.prography.samsung.backend.session.repository.SessionTopicSnapshotRepository
import org.prography.samsung.backend.session.repository.TutoringSessionRepository
import org.prography.samsung.backend.user.entity.User
import org.prography.samsung.backend.user.entity.UserCurriculum
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class SessionLifecycleService(
    private val curriculumService: CurriculumService,
    private val sessionQueryService: SessionQueryService,
    private val tutoringSessionRepository: TutoringSessionRepository,
    private val sessionTopicSnapshotRepository: SessionTopicSnapshotRepository,
) {
    fun start(user: User, userCurriculum: UserCurriculum, request: SessionStartRequest?): SessionStartResponse {
        val existing = tutoringSessionRepository.findByUserIdAndStatus(user.id, SessionStatus.STARTED)
        if (existing != null) {
            return SessionStartResponse(
                sessionId = existing.id,
                startedAt = KstDateTimeUtils.toOffsetDateTimeString(existing.startedAt),
                resumed = true,
            )
        }

        val selectedTopic = resolveSelectedLessonTopic(request?.lessonTopicId, userCurriculum.curriculum.id)
        val targetCurriculumId = selectedTopic?.curriculum?.id ?: request?.curriculumId ?: userCurriculum.curriculum.id
        if (userCurriculum.curriculum.id != targetCurriculumId) {
            throw CustomException(DomainErrorCode.CURRICULUM_MISMATCH)
        }

        val topic = selectedTopic ?: curriculumService.getFirstLessonTopic(targetCurriculumId)

        val now = Instant.now()
        val conversationMode = request?.conversationMode ?: ConversationMode.STATIC
        val session =
            tutoringSessionRepository.save(
                TutoringSession(
                    id = UUID.randomUUID().toString(),
                    user = user,
                    curriculum = userCurriculum.curriculum,
                    lessonTopic = topic,
                    status = SessionStatus.STARTED,
                    currentPhase = SessionPhase.INTRO,
                    conversationMode = conversationMode,
                    sessionDate = KstDateTimeUtils.todayKst(),
                    startedAt = now,
                ),
            )

        sessionTopicSnapshotRepository.save(
            SessionTopicSnapshot(
                session = session,
                lessonTopic = topic,
                sequence = SessionConstants.SNAPSHOT_SEQUENCE,
                title = topic.title,
                subtitle = topic.subtitle,
                topicType = topic.topicType,
            ),
        )

        return SessionStartResponse(
            sessionId = session.id,
            startedAt = KstDateTimeUtils.toOffsetDateTimeString(session.startedAt),
            resumed = false,
        )
    }

    fun advancePhase(userId: Long, sessionId: String): SessionPhaseResponse {
        val session = sessionQueryService.getStartedSession(userId, sessionId)
        if (session.currentPhase != SessionPhase.INTRO) {
            throw CustomException(DomainErrorCode.SESSION_NOT_IN_INTRO)
        }
        session.advancePhase(SessionPhase.REACTION)
        tutoringSessionRepository.save(session)
        return SessionPhaseResponse(sessionId = session.id, currentPhase = SessionPhase.REACTION)
    }

    fun abort(userId: Long, sessionId: String): SessionAbortResponse {
        val session = sessionQueryService.getStartedSession(userId, sessionId)
        session.abort()
        tutoringSessionRepository.save(session)
        return SessionAbortResponse(sessionId = session.id, status = SessionStatus.ABORTED)
    }

    private fun resolveSelectedLessonTopic(lessonTopicId: Long?, curriculumId: Long): LessonTopic? {
        if (lessonTopicId == null) return null
        val topic = curriculumService.getLessonTopicById(lessonTopicId)
        if (topic.curriculum.id != curriculumId) {
            throw CustomException(DomainErrorCode.CURRICULUM_MISMATCH)
        }
        return topic
    }
}
