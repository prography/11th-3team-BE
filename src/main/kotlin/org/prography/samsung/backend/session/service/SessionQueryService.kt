package org.prography.samsung.backend.session.service

import org.prography.samsung.backend.common.domain.SessionPhase
import org.prography.samsung.backend.common.domain.SessionStatus
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.common.util.KstDateTimeUtils
import org.prography.samsung.backend.session.dto.response.ActiveSessionResponse
import org.prography.samsung.backend.session.dto.response.SessionStatusResponse
import org.prography.samsung.backend.session.entity.SessionTopicSnapshot
import org.prography.samsung.backend.session.entity.TutoringSession
import org.prography.samsung.backend.session.repository.SessionTopicSnapshotRepository
import org.prography.samsung.backend.session.repository.TutoringSessionRepository
import org.springframework.stereotype.Service

@Service
class SessionQueryService(
    private val tutoringSessionRepository: TutoringSessionRepository,
    private val sessionTopicSnapshotRepository: SessionTopicSnapshotRepository,
) {
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

    fun findCompletedTopicIdsToday(userId: Long): Set<Long> {
        val today = KstDateTimeUtils.todayKst()
        return tutoringSessionRepository.findCompletedTopicIdsByUserIdAndSessionDate(userId, today)
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

    fun getOwnedSession(userId: Long, sessionId: String): TutoringSession =
        tutoringSessionRepository.findByUserIdAndId(userId, sessionId)
            ?: throw CustomException(DomainErrorCode.SESSION_NOT_FOUND)

    fun getStartedSession(userId: Long, sessionId: String): TutoringSession {
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
