package org.prography.samsung.backend.session.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.prography.samsung.backend.common.domain.SessionStatus
import org.prography.samsung.backend.session.repository.TutoringSessionRepository
import org.prography.samsung.backend.support.TestFixtures
import org.springframework.data.domain.Pageable
import java.time.Instant
import java.util.Base64

@ExtendWith(MockitoExtension::class)
@DisplayName("SessionHistoryService 단위 테스트")
class SessionHistoryServiceTest {
    private val tutoringSessionRepository: TutoringSessionRepository = mock()
    private lateinit var sut: SessionHistoryService

    @BeforeEach
    fun setUp() {
        sut = SessionHistoryService(tutoringSessionRepository)
    }

    @Test
    @DisplayName("완료 세션 목록을 반환한다")
    fun shouldReturnCompletedSessionHistory() {
        val completedAt = Instant.parse("2026-06-08T06:00:00Z")
        val session =
            TestFixtures.tutoringSession(
                status = SessionStatus.COMPLETED,
                currentPhase = null,
                coinsAwarded = 500,
                badgeLevelUp = true,
            ).apply {
                this.completedAt = completedAt
                primaryTopicTitle = "분수란?"
            }
        whenever(
            tutoringSessionRepository.findCompletedHistory(
                userId = eq(TestFixtures.USER_ID),
                cursorCompletedAt = isNull(),
                cursorSessionId = isNull(),
                pageable = any<Pageable>(),
            ),
        ).thenReturn(listOf(session))

        val result = sut.getHistory(TestFixtures.USER_ID, cursor = null, size = 20)

        assertEquals(1, result.sessions.size)
        assertEquals(500, result.sessions[0].coins)
        assertTrue(result.sessions[0].badgeLevelUp)
        assertFalse(result.hasMore)
        assertNull(result.nextCursor)
    }

    @Test
    @DisplayName("size보다 많으면 hasMore와 nextCursor를 반환한다")
    fun shouldReturnHasMoreAndNextCursorWhenMoreSessionsExist() {
        val completedAt = Instant.parse("2026-06-08T06:00:00Z")
        val sessions =
            (1..3).map { index ->
                TestFixtures.tutoringSession(
                    id = "session-$index",
                    status = SessionStatus.COMPLETED,
                    currentPhase = null,
                    coinsAwarded = 500,
                ).apply {
                    this.completedAt = completedAt.minusSeconds(index.toLong())
                    primaryTopicTitle = "topic-$index"
                }
            }
        whenever(
            tutoringSessionRepository.findCompletedHistory(
                userId = eq(TestFixtures.USER_ID),
                cursorCompletedAt = isNull(),
                cursorSessionId = isNull(),
                pageable = any<Pageable>(),
            ),
        ).thenReturn(sessions)

        val result = sut.getHistory(TestFixtures.USER_ID, cursor = null, size = 2)

        assertEquals(2, result.sessions.size)
        assertTrue(result.hasMore)
        assertNotNull(result.nextCursor)
    }

    @Test
    @DisplayName("cursor를 디코딩하여 페이징 조회에 사용한다")
    fun shouldDecodeCursorForPagination() {
        val completedAt = Instant.parse("2026-06-08T06:00:00Z")
        val cursor =
            Base64.getUrlEncoder().withoutPadding()
                .encodeToString("$completedAt|${TestFixtures.SESSION_ID}".toByteArray())
        whenever(
            tutoringSessionRepository.findCompletedHistory(
                userId = eq(TestFixtures.USER_ID),
                cursorCompletedAt = eq(completedAt),
                cursorSessionId = eq(TestFixtures.SESSION_ID),
                pageable = any<Pageable>(),
            ),
        ).thenReturn(emptyList())

        val result = sut.getHistory(TestFixtures.USER_ID, cursor = cursor, size = 20)

        assertTrue(result.sessions.isEmpty())
        assertFalse(result.hasMore)
    }
}
