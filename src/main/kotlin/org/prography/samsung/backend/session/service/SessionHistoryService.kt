package org.prography.samsung.backend.session.service

import org.prography.samsung.backend.common.util.KstDateTimeUtils
import org.prography.samsung.backend.session.dto.SessionHistoryItemResponse
import org.prography.samsung.backend.session.dto.SessionHistoryResponse
import org.prography.samsung.backend.session.repository.TutoringSessionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.Base64

@Service
class SessionHistoryService(private val tutoringSessionRepository: TutoringSessionRepository) {
    @Transactional(readOnly = true)
    fun getHistory(userId: Long, cursor: String?, size: Int): SessionHistoryResponse {
        val pageSize = size.coerceIn(1, 50)
        val (cursorCompletedAt, cursorSessionId) = decodeCursor(cursor)

        val sessions =
            tutoringSessionRepository.findCompletedHistory(
                userId = userId,
                cursorCompletedAt = cursorCompletedAt,
                cursorSessionId = cursorSessionId,
                pageable = PageRequest.of(0, pageSize + 1),
            )

        val hasMore = sessions.size > pageSize
        val page = if (hasMore) sessions.take(pageSize) else sessions
        val nextCursor =
            if (hasMore) {
                val last = page.last()
                encodeCursor(last.completedAt!!, last.id)
            } else {
                null
            }

        return SessionHistoryResponse(
            sessions =
            page.map {
                SessionHistoryItemResponse(
                    sessionId = it.id,
                    date = KstDateTimeUtils.toDateString(it.completedAt!!),
                    topic = it.primaryTopicTitle ?: "",
                    coins = it.coinsAwarded ?: 0,
                    badgeLevelUp = it.badgeLevelUp ?: false,
                )
            },
            hasMore = hasMore,
            nextCursor = nextCursor,
        )
    }

    private fun encodeCursor(completedAt: Instant, sessionId: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString("$completedAt|$sessionId".toByteArray())

    private fun decodeCursor(cursor: String?): Pair<Instant?, String?> {
        if (cursor.isNullOrBlank()) return null to null
        val decoded = String(Base64.getUrlDecoder().decode(cursor))
        val parts = decoded.split("|", limit = 2)
        if (parts.size != 2) return null to null
        return Instant.parse(parts[0]) to parts[1]
    }
}
