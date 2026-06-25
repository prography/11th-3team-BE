package org.prography.samsung.backend.session.service

import org.prography.samsung.backend.common.domain.ConversationMode
import org.prography.samsung.backend.common.domain.SessionPhase
import org.prography.samsung.backend.common.domain.SessionStatus
import org.prography.samsung.backend.common.dto.ActiveSessionResponse
import org.prography.samsung.backend.common.dto.RewardResponse
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.common.util.KstDateTimeUtils
import org.prography.samsung.backend.curriculum.entity.LessonTopic
import org.prography.samsung.backend.curriculum.repository.LessonTopicRepository
import org.prography.samsung.backend.session.SessionConstants
import org.prography.samsung.backend.session.dto.RewardAckResponse
import org.prography.samsung.backend.session.dto.SessionAbortResponse
import org.prography.samsung.backend.session.dto.SessionPhaseResponse
import org.prography.samsung.backend.session.dto.SessionStartResponse
import org.prography.samsung.backend.session.dto.SessionStatusResponse
import org.prography.samsung.backend.session.entity.SessionTopicSnapshot
import org.prography.samsung.backend.session.entity.TutoringSession
import org.prography.samsung.backend.session.repository.SessionTopicSnapshotRepository
import org.prography.samsung.backend.session.repository.TutoringSessionRepository
import org.prography.samsung.backend.user.repository.UserCurriculumRepository
import org.prography.samsung.backend.user.repository.UserProfileRepository
import org.prography.samsung.backend.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class SessionService(
    private val userRepository: UserRepository,
    private val userCurriculumRepository: UserCurriculumRepository,
    private val userProfileRepository: UserProfileRepository,
    private val lessonTopicRepository: LessonTopicRepository,
    private val tutoringSessionRepository: TutoringSessionRepository,
    private val sessionTopicSnapshotRepository: SessionTopicSnapshotRepository,
    private val sessionCompletionService: SessionCompletionService,
) {
    @Transactional(readOnly = true)
    fun getStatus(userId: Long): SessionStatusResponse {
        val today = KstDateTimeUtils.todayKst()
        val lessonCompletedToday =
            tutoringSessionRepository.existsByUserIdAndSessionDateAndStatus(
                userId,
                today,
                SessionStatus.COMPLETED,
            )
        val activeSession = tutoringSessionRepository.findByUserIdAndStatus(userId, SessionStatus.STARTED)
        val pendingReward = tutoringSessionRepository.findPendingRewardSession(userId).firstOrNull()

        return SessionStatusResponse(
            lessonCompletedToday = lessonCompletedToday,
            activeSession = activeSession?.let { toActiveSession(it) },
            pendingRewardSessionId = pendingReward?.id,
        )
    }

    @Transactional
    fun start(
        userId: Long,
        request: org.prography.samsung.backend.session.dto.SessionStartRequest?,
    ): SessionStartResponse {
        val existing = tutoringSessionRepository.findByUserIdAndStatus(userId, SessionStatus.STARTED)
        if (existing != null) {
            return SessionStartResponse(
                sessionId = existing.id,
                startedAt = KstDateTimeUtils.toOffsetDateTimeString(existing.startedAt),
                resumed = true,
            )
        }

        val user = userRepository.findById(userId).orElseThrow { CustomException(DomainErrorCode.USER_NOT_FOUND) }
        val userCurriculum =
            userCurriculumRepository.findById(userId).orElseThrow {
                CustomException(DomainErrorCode.CURRICULUM_NOT_SELECTED)
            }
        val selectedTopic = resolveSelectedLessonTopic(request?.lessonTopicId, userCurriculum.curriculum.id)
        val targetCurriculumId = selectedTopic?.curriculum?.id ?: request?.curriculumId ?: userCurriculum.curriculum.id
        if (userCurriculum.curriculum.id != targetCurriculumId) {
            throw CustomException(DomainErrorCode.CURRICULUM_MISMATCH)
        }

        val topic =
            selectedTopic
                ?: lessonTopicRepository.findAllByCurriculumIdOrderBySequenceAsc(targetCurriculumId).firstOrNull()
                ?: throw CustomException(DomainErrorCode.LESSON_TOPIC_NOT_FOUND)

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

    @Transactional
    fun advancePhase(userId: Long, sessionId: String): SessionPhaseResponse {
        val session = getStartedSession(userId, sessionId)
        if (session.currentPhase != SessionPhase.INTRO) {
            throw CustomException(DomainErrorCode.SESSION_NOT_IN_INTRO)
        }
        session.advancePhase(SessionPhase.REACTION)
        tutoringSessionRepository.save(session)
        return SessionPhaseResponse(sessionId = session.id, currentPhase = SessionPhase.REACTION)
    }

    @Transactional
    fun complete(userId: Long, sessionId: String): RewardResponse = sessionCompletionService.complete(userId, sessionId)

    @Transactional(readOnly = true)
    fun getReward(userId: Long, sessionId: String): RewardResponse {
        val session = getOwnedSession(userId, sessionId)
        if (session.status != SessionStatus.COMPLETED) {
            throw CustomException(DomainErrorCode.SESSION_NOT_COMPLETED)
        }
        val profile =
            userProfileRepository.findById(userId).orElseThrow {
                CustomException(DomainErrorCode.USER_NOT_FOUND)
            }
        return RewardResponse.from(session, profile)
    }

    @Transactional
    fun acknowledgeReward(userId: Long, sessionId: String): RewardAckResponse {
        val session = getOwnedSession(userId, sessionId)
        if (session.status != SessionStatus.COMPLETED) {
            throw CustomException(DomainErrorCode.SESSION_NOT_COMPLETED)
        }
        val now = Instant.now()
        session.acknowledgeReward(now)
        tutoringSessionRepository.save(session)
        return RewardAckResponse(
            sessionId = session.id,
            acknowledged = true,
            rewardAcknowledgedAt = KstDateTimeUtils.toOffsetDateTimeString(now),
        )
    }

    @Transactional
    fun abort(userId: Long, sessionId: String): SessionAbortResponse {
        val session = getStartedSession(userId, sessionId)
        session.abort()
        tutoringSessionRepository.save(session)
        return SessionAbortResponse(sessionId = session.id, status = SessionStatus.ABORTED)
    }

    fun findActiveSession(userId: Long): ActiveSessionResponse? =
        tutoringSessionRepository.findByUserIdAndStatus(userId, SessionStatus.STARTED)?.let { toActiveSession(it) }

    fun getStartedSessionWithSnapshot(
        userId: Long,
        sessionId: String,
        topicSequence: Int,
    ): Pair<TutoringSession, SessionTopicSnapshot> {
        val session = getStartedSession(userId, sessionId)
        val snapshot =
            sessionTopicSnapshotRepository.findBySessionIdAndSequence(sessionId, topicSequence)
                ?: throw CustomException(DomainErrorCode.LESSON_TOPIC_NOT_FOUND)
        return session to snapshot
    }

    private fun getOwnedSession(userId: Long, sessionId: String): TutoringSession =
        tutoringSessionRepository.findByUserIdAndId(userId, sessionId)
            ?: throw CustomException(DomainErrorCode.SESSION_NOT_FOUND)

    private fun resolveSelectedLessonTopic(lessonTopicId: Long?, curriculumId: Long): LessonTopic? {
        if (lessonTopicId == null) return null
        val topic =
            lessonTopicRepository.findById(lessonTopicId).orElseThrow {
                CustomException(DomainErrorCode.LESSON_TOPIC_NOT_FOUND)
            }
        if (topic.curriculum.id != curriculumId) {
            throw CustomException(DomainErrorCode.CURRICULUM_MISMATCH)
        }
        return topic
    }

    private fun getStartedSession(userId: Long, sessionId: String): TutoringSession {
        val session = getOwnedSession(userId, sessionId)
        if (session.status != SessionStatus.STARTED) {
            throw CustomException(DomainErrorCode.SESSION_NOT_STARTED)
        }
        return session
    }

    private fun toActiveSession(session: TutoringSession): ActiveSessionResponse = ActiveSessionResponse(
        sessionId = session.id,
        status = session.status,
        currentPhase = session.currentPhase ?: SessionPhase.INTRO,
        startedAt = KstDateTimeUtils.toOffsetDateTimeString(session.startedAt),
    )
}
