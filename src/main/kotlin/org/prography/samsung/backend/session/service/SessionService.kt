package org.prography.samsung.backend.session.service

import org.prography.samsung.backend.common.domain.SessionPhase
import org.prography.samsung.backend.common.domain.SessionStatus
import org.prography.samsung.backend.common.dto.ActiveSessionResponse
import org.prography.samsung.backend.common.dto.LessonQuestionResponse
import org.prography.samsung.backend.common.dto.RewardResponse
import org.prography.samsung.backend.common.dto.TodayTopicResponse
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.common.response.ErrorBaseCode
import org.prography.samsung.backend.common.util.KstDateTimeUtils
import org.prography.samsung.backend.curriculum.repository.HintNoteRepository
import org.prography.samsung.backend.curriculum.repository.LessonQuestionRepository
import org.prography.samsung.backend.curriculum.repository.LessonTopicRepository
import org.prography.samsung.backend.curriculum.service.HintNoteMapper
import org.prography.samsung.backend.session.SessionConstants
import org.prography.samsung.backend.session.dto.RewardAckResponse
import org.prography.samsung.backend.session.dto.SessionAbortResponse
import org.prography.samsung.backend.session.dto.SessionLessonResponse
import org.prography.samsung.backend.session.dto.SessionPhaseResponse
import org.prography.samsung.backend.session.dto.SessionStartResponse
import org.prography.samsung.backend.session.dto.SessionStatusResponse
import org.prography.samsung.backend.session.dto.SessionTodayResponse
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
    private val lessonQuestionRepository: LessonQuestionRepository,
    private val hintNoteRepository: HintNoteRepository,
    private val hintNoteMapper: HintNoteMapper,
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
        val pendingReward =
            tutoringSessionRepository.findPendingRewardSession(userId).firstOrNull()

        return SessionStatusResponse(
            lessonCompletedToday = lessonCompletedToday,
            activeSession = activeSession?.let { toActiveSession(it) },
            pendingRewardSessionId = pendingReward?.id,
        )
    }

    @Transactional(readOnly = true)
    fun getToday(userId: Long): SessionTodayResponse {
        val userCurriculum =
            userCurriculumRepository.findById(userId).orElseThrow {
                CustomException(DomainErrorCode.CURRICULUM_NOT_SELECTED)
            }
        val curriculum = userCurriculum.curriculum
        val topics =
            lessonTopicRepository.findAllByCurriculumIdOrderBySequenceAsc(curriculum.id).map {
                TodayTopicResponse(
                    sequence = it.sequence,
                    lessonTopicId = it.id,
                    title = it.title,
                    subtitle = it.subtitle,
                    topicType = it.topicType,
                )
            }
        val activeSession = tutoringSessionRepository.findByUserIdAndStatus(userId, SessionStatus.STARTED)

        return SessionTodayResponse(
            curriculumId = curriculum.id,
            sessionTitle = curriculum.sessionTitleTemplate,
            topics = topics,
            activeSession = activeSession?.let { toActiveSession(it) },
        )
    }

    @Transactional
    fun start(userId: Long, curriculumId: Long?): SessionStartResponse {
        val existing = tutoringSessionRepository.findByUserIdAndStatus(userId, SessionStatus.STARTED)
        if (existing != null) {
            return SessionStartResponse(
                sessionId = existing.id,
                startedAt = KstDateTimeUtils.toOffsetDateTimeString(existing.startedAt),
                resumed = true,
            )
        }

        val user = userRepository.findById(userId).orElseThrow { CustomException(ErrorBaseCode.NOT_FOUND_ENTITY) }
        val userCurriculum =
            userCurriculumRepository.findById(userId).orElseThrow {
                CustomException(DomainErrorCode.CURRICULUM_NOT_SELECTED)
            }
        val targetCurriculumId = curriculumId ?: userCurriculum.curriculum.id
        if (userCurriculum.curriculum.id != targetCurriculumId) {
            throw CustomException(ErrorBaseCode.BAD_REQUEST)
        }

        val topics = lessonTopicRepository.findAllByCurriculumIdOrderBySequenceAsc(targetCurriculumId)
        val now = Instant.now()
        val session =
            tutoringSessionRepository.save(
                TutoringSession(
                    id = UUID.randomUUID().toString(),
                    user = user,
                    curriculum = userCurriculum.curriculum,
                    status = SessionStatus.STARTED,
                    currentPhase = SessionPhase.INTRO,
                    sessionDate = KstDateTimeUtils.todayKst(),
                    startedAt = now,
                ),
            )

        topics.forEach { topic ->
            sessionTopicSnapshotRepository.save(
                SessionTopicSnapshot(
                    session = session,
                    lessonTopic = topic,
                    sequence = topic.sequence,
                    title = topic.title,
                    subtitle = topic.subtitle,
                    topicType = topic.topicType,
                ),
            )
        }

        return SessionStartResponse(
            sessionId = session.id,
            startedAt = KstDateTimeUtils.toOffsetDateTimeString(session.startedAt),
            resumed = false,
        )
    }

    @Transactional(readOnly = true)
    fun getLesson(userId: Long, sessionId: String): SessionLessonResponse =
        buildPhaseResponse(userId, sessionId, SessionPhase.INTRO, SessionConstants.INTRO_TOPIC_SEQUENCE)

    @Transactional(readOnly = true)
    fun getReaction(userId: Long, sessionId: String): SessionLessonResponse =
        buildPhaseResponse(userId, sessionId, SessionPhase.REACTION, SessionConstants.REACTION_TOPIC_SEQUENCE)

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
                CustomException(ErrorBaseCode.NOT_FOUND_ENTITY)
            }
        return sessionCompletionService.toRewardResponse(session, profile)
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

    private fun buildPhaseResponse(
        userId: Long,
        sessionId: String,
        expectedPhase: SessionPhase,
        topicSequence: Int,
    ): SessionLessonResponse {
        val session = getStartedSession(userId, sessionId)
        if (session.currentPhase != expectedPhase) {
            throw CustomException(DomainErrorCode.SESSION_PHASE_MISMATCH)
        }

        val snapshot =
            sessionTopicSnapshotRepository.findBySessionIdAndSequence(sessionId, topicSequence)
                ?: throw CustomException(ErrorBaseCode.NOT_FOUND_ENTITY)
        val question =
            lessonQuestionRepository.findByLessonTopicIdAndPhase(snapshot.lessonTopic.id, expectedPhase)
                ?: throw CustomException(ErrorBaseCode.NOT_FOUND_ENTITY)
        val hintNote =
            hintNoteRepository.findByLessonTopicIdAndPhase(snapshot.lessonTopic.id, expectedPhase)
                ?: throw CustomException(ErrorBaseCode.NOT_FOUND_ENTITY)

        return SessionLessonResponse(
            sessionId = session.id,
            currentPhase = expectedPhase,
            topicLabel = snapshot.lessonTopic.gnbTitle,
            question =
            LessonQuestionResponse(
                bubbleText = question.bubbleText,
                displayAnswerHtml = question.wrongAnswerHtml,
            ),
            hintNote = hintNoteMapper.toResponse(hintNote.contentJson),
        )
    }

    private fun getOwnedSession(userId: Long, sessionId: String): TutoringSession =
        tutoringSessionRepository.findByUserIdAndId(userId, sessionId)
            ?: throw CustomException(ErrorBaseCode.NOT_FOUND_ENTITY)

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
