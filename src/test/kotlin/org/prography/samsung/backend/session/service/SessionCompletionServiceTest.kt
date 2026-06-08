package org.prography.samsung.backend.session.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.prography.samsung.backend.common.domain.SessionStatus
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.common.response.ErrorBaseCode
import org.prography.samsung.backend.gamification.repository.BadgeLevelRepository
import org.prography.samsung.backend.session.SessionConstants
import org.prography.samsung.backend.session.repository.CoinLedgerEntryRepository
import org.prography.samsung.backend.session.repository.SessionTopicSnapshotRepository
import org.prography.samsung.backend.session.repository.TutoringSessionRepository
import org.prography.samsung.backend.support.TestFixtures
import org.prography.samsung.backend.user.repository.UserCurriculumRepository
import org.prography.samsung.backend.user.repository.UserProfileRepository

@ExtendWith(MockitoExtension::class)
@DisplayName("SessionCompletionService 단위 테스트")
class SessionCompletionServiceTest {
    private val tutoringSessionRepository: TutoringSessionRepository = mock()
    private val sessionTopicSnapshotRepository: SessionTopicSnapshotRepository = mock()
    private val coinLedgerEntryRepository: CoinLedgerEntryRepository = mock()
    private val userProfileRepository: UserProfileRepository = mock()
    private val userCurriculumRepository: UserCurriculumRepository = mock()
    private val badgeLevelRepository: BadgeLevelRepository = mock()
    private lateinit var sut: SessionCompletionService

    @BeforeEach
    fun setUp() {
        sut =
            SessionCompletionService(
                tutoringSessionRepository,
                sessionTopicSnapshotRepository,
                coinLedgerEntryRepository,
                userProfileRepository,
                userCurriculumRepository,
                badgeLevelRepository,
            )
    }

    @Test
    @DisplayName("STARTED 세션 완료 시 코인·진척도·배지를 갱신한다")
    fun shouldCompleteStartedSessionAndAwardRewards() {
        val user = TestFixtures.user()
        val session = TestFixtures.tutoringSession(user = user)
        val profile = TestFixtures.userProfile(user = user, completedSessionCount = 0)
        val userCurriculum = TestFixtures.userCurriculum(user = user, progressPercent = 0)
        val snapshot = TestFixtures.sessionTopicSnapshot(session = session, sequence = 1)
        val nextBadge = TestFixtures.badgeLevel(2, "똑똑한 선생님", 1)

        whenever(tutoringSessionRepository.findByUserIdAndId(TestFixtures.USER_ID, TestFixtures.SESSION_ID))
            .thenReturn(session)
        whenever(userProfileRepository.findById(TestFixtures.USER_ID)).thenReturn(TestFixtures.optional(profile))
        whenever(
            userCurriculumRepository.findById(TestFixtures.USER_ID),
        ).thenReturn(TestFixtures.optional(userCurriculum))
        whenever(sessionTopicSnapshotRepository.findBySessionIdAndSequence(TestFixtures.SESSION_ID, 1))
            .thenReturn(snapshot)
        whenever(
            badgeLevelRepository.findTopByRequiredCompletedSessionsLessThanEqualOrderByLevelDesc(1),
        ).thenReturn(nextBadge)
        whenever(coinLedgerEntryRepository.existsBySessionId(TestFixtures.SESSION_ID)).thenReturn(false)

        val result = sut.complete(TestFixtures.USER_ID, TestFixtures.SESSION_ID)

        assertEquals(500, result.coinsAwarded)
        assertEquals(45, result.progressPercent)
        assertEquals(500, result.totalCoins)
        assertTrue(result.badgeLevelUp)
        assertEquals(2, result.newLevel?.number)
        assertEquals(SessionStatus.COMPLETED, session.status)
        verify(coinLedgerEntryRepository).save(any())
    }

    @Test
    @DisplayName("이미 COMPLETED인 세션은 멱등하게 기존 보상을 반환한다")
    fun shouldReturnExistingRewardWhenSessionAlreadyCompleted() {
        val profile = TestFixtures.userProfile(totalCoins = 500)
        val session =
            TestFixtures.tutoringSession(
                status = SessionStatus.COMPLETED,
                currentPhase = null,
                coinsAwarded = 500,
                badgeLevelUp = true,
                progressAfter = 45,
            )

        whenever(tutoringSessionRepository.findByUserIdAndId(TestFixtures.USER_ID, TestFixtures.SESSION_ID))
            .thenReturn(session)
        whenever(userProfileRepository.findById(TestFixtures.USER_ID)).thenReturn(TestFixtures.optional(profile))

        val result = sut.complete(TestFixtures.USER_ID, TestFixtures.SESSION_ID)

        assertEquals(500, result.coinsAwarded)
        verify(userProfileRepository, never()).save(any())
        verify(coinLedgerEntryRepository, never()).save(any())
    }

    @Test
    @DisplayName("ABORTED 세션이면 SESSION_NOT_STARTED를 던진다")
    fun shouldThrowWhenSessionIsNotStarted() {
        val session = TestFixtures.tutoringSession(status = SessionStatus.ABORTED, currentPhase = null)
        whenever(tutoringSessionRepository.findByUserIdAndId(TestFixtures.USER_ID, TestFixtures.SESSION_ID))
            .thenReturn(session)

        val exception =
            assertThrows(CustomException::class.java) {
                sut.complete(TestFixtures.USER_ID, TestFixtures.SESSION_ID)
            }

        assertEquals(DomainErrorCode.SESSION_NOT_STARTED, exception.errorCode)
    }

    @Test
    @DisplayName("세션이 없으면 NOT_FOUND_ENTITY를 던진다")
    fun shouldThrowWhenSessionNotFound() {
        whenever(tutoringSessionRepository.findByUserIdAndId(TestFixtures.USER_ID, TestFixtures.SESSION_ID))
            .thenReturn(null)

        val exception =
            assertThrows(CustomException::class.java) {
                sut.complete(TestFixtures.USER_ID, TestFixtures.SESSION_ID)
            }

        assertEquals(ErrorBaseCode.NOT_FOUND_ENTITY, exception.errorCode)
    }

    @Test
    @DisplayName("진척도는 100을 초과하지 않는다")
    fun shouldCapProgressPercentAt100() {
        val user = TestFixtures.user()
        val session = TestFixtures.tutoringSession(user = user)
        val profile = TestFixtures.userProfile(user = user, completedSessionCount = 5)
        val userCurriculum = TestFixtures.userCurriculum(user = user, progressPercent = 80)
        val snapshot = TestFixtures.sessionTopicSnapshot(session = session)

        whenever(tutoringSessionRepository.findByUserIdAndId(TestFixtures.USER_ID, TestFixtures.SESSION_ID))
            .thenReturn(session)
        whenever(userProfileRepository.findById(TestFixtures.USER_ID)).thenReturn(TestFixtures.optional(profile))
        whenever(
            userCurriculumRepository.findById(TestFixtures.USER_ID),
        ).thenReturn(TestFixtures.optional(userCurriculum))
        whenever(sessionTopicSnapshotRepository.findBySessionIdAndSequence(TestFixtures.SESSION_ID, 1))
            .thenReturn(snapshot)
        whenever(
            badgeLevelRepository.findTopByRequiredCompletedSessionsLessThanEqualOrderByLevelDesc(6),
        ).thenReturn(profile.badgeLevel)
        whenever(coinLedgerEntryRepository.existsBySessionId(TestFixtures.SESSION_ID)).thenReturn(true)

        val result = sut.complete(TestFixtures.USER_ID, TestFixtures.SESSION_ID)

        assertEquals(100, result.progressPercent)
        assertEquals(100, userCurriculum.progressPercent)
        assertFalse(result.badgeLevelUp)
        assertEquals(SessionConstants.COINS_PER_SESSION, result.coinsAwarded)
    }
}
