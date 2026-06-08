package org.prography.samsung.backend.session.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.prography.samsung.backend.common.domain.RewardStatus
import org.prography.samsung.backend.common.domain.SessionPhase
import org.prography.samsung.backend.common.domain.SessionStatus
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.common.response.ErrorBaseCode
import org.prography.samsung.backend.curriculum.repository.HintNoteRepository
import org.prography.samsung.backend.curriculum.repository.LessonQuestionRepository
import org.prography.samsung.backend.curriculum.repository.LessonTopicRepository
import org.prography.samsung.backend.curriculum.service.HintNoteMapper
import org.prography.samsung.backend.session.repository.SessionTopicSnapshotRepository
import org.prography.samsung.backend.session.repository.TutoringSessionRepository
import org.prography.samsung.backend.support.TestFixtures
import org.prography.samsung.backend.user.repository.UserCurriculumRepository
import org.prography.samsung.backend.user.repository.UserProfileRepository
import org.prography.samsung.backend.user.repository.UserRepository

@ExtendWith(MockitoExtension::class)
@DisplayName("SessionService 단위 테스트")
class SessionServiceTest {
    private val userRepository: UserRepository = mock()
    private val userCurriculumRepository: UserCurriculumRepository = mock()
    private val userProfileRepository: UserProfileRepository = mock()
    private val lessonTopicRepository: LessonTopicRepository = mock()
    private val lessonQuestionRepository: LessonQuestionRepository = mock()
    private val hintNoteRepository: HintNoteRepository = mock()
    private val hintNoteMapper: HintNoteMapper = mock()
    private val tutoringSessionRepository: TutoringSessionRepository = mock()
    private val sessionTopicSnapshotRepository: SessionTopicSnapshotRepository = mock()
    private val sessionCompletionService: SessionCompletionService = mock()
    private lateinit var sut: SessionService

    @BeforeEach
    fun setUp() {
        sut =
            SessionService(
                userRepository,
                userCurriculumRepository,
                userProfileRepository,
                lessonTopicRepository,
                lessonQuestionRepository,
                hintNoteRepository,
                hintNoteMapper,
                tutoringSessionRepository,
                sessionTopicSnapshotRepository,
                sessionCompletionService,
            )
    }

    @Test
    @DisplayName("진행 중 세션이 있으면 start는 resumed true로 반환한다")
    fun shouldResumeWhenActiveSessionExists() {
        val existing = TestFixtures.tutoringSession()
        whenever(tutoringSessionRepository.findByUserIdAndStatus(TestFixtures.USER_ID, SessionStatus.STARTED))
            .thenReturn(existing)

        val result = sut.start(TestFixtures.USER_ID, TestFixtures.CURRICULUM_ID)

        assertEquals(TestFixtures.SESSION_ID, result.sessionId)
        assertTrue(result.resumed)
        verify(tutoringSessionRepository, org.mockito.kotlin.never()).save(any())
    }

    @Test
    @DisplayName("curriculumId가 현재 선택과 다르면 BAD_REQUEST를 던진다")
    fun shouldThrowWhenCurriculumIdMismatchOnStart() {
        whenever(tutoringSessionRepository.findByUserIdAndStatus(TestFixtures.USER_ID, SessionStatus.STARTED))
            .thenReturn(null)
        whenever(userRepository.findById(TestFixtures.USER_ID)).thenReturn(TestFixtures.optional(TestFixtures.user()))
        whenever(userCurriculumRepository.findById(TestFixtures.USER_ID))
            .thenReturn(TestFixtures.optional(TestFixtures.userCurriculum()))

        val exception =
            assertThrows(CustomException::class.java) {
                sut.start(TestFixtures.USER_ID, 99L)
            }

        assertEquals(ErrorBaseCode.BAD_REQUEST, exception.errorCode)
    }

    @Test
    @DisplayName("INTRO 단계에서 advance-phase하면 REACTION으로 전환한다")
    fun shouldAdvancePhaseFromIntroToReaction() {
        val session = TestFixtures.tutoringSession(currentPhase = SessionPhase.INTRO)
        whenever(tutoringSessionRepository.findByUserIdAndId(TestFixtures.USER_ID, TestFixtures.SESSION_ID))
            .thenReturn(session)

        val result = sut.advancePhase(TestFixtures.USER_ID, TestFixtures.SESSION_ID)

        assertEquals(SessionPhase.REACTION, result.currentPhase)
        assertEquals(SessionPhase.REACTION, session.currentPhase)
        verify(tutoringSessionRepository).save(session)
    }

    @Test
    @DisplayName("REACTION 단계에서 advance-phase하면 SESSION_NOT_IN_INTRO를 던진다")
    fun shouldThrowWhenAdvancePhaseNotInIntro() {
        val session = TestFixtures.tutoringSession(currentPhase = SessionPhase.REACTION)
        whenever(tutoringSessionRepository.findByUserIdAndId(TestFixtures.USER_ID, TestFixtures.SESSION_ID))
            .thenReturn(session)

        val exception =
            assertThrows(CustomException::class.java) {
                sut.advancePhase(TestFixtures.USER_ID, TestFixtures.SESSION_ID)
            }

        assertEquals(DomainErrorCode.SESSION_NOT_IN_INTRO, exception.errorCode)
    }

    @Test
    @DisplayName("abort하면 세션 상태가 ABORTED가 된다")
    fun shouldAbortStartedSession() {
        val session = TestFixtures.tutoringSession()
        whenever(tutoringSessionRepository.findByUserIdAndId(TestFixtures.USER_ID, TestFixtures.SESSION_ID))
            .thenReturn(session)

        val result = sut.abort(TestFixtures.USER_ID, TestFixtures.SESSION_ID)

        assertEquals(SessionStatus.ABORTED, result.status)
        assertEquals(SessionStatus.ABORTED, session.status)
        assertEquals(null, session.currentPhase)
    }

    @Test
    @DisplayName("미수령 보상 세션이 있으면 pendingRewardSessionId를 반환한다")
    fun shouldReturnPendingRewardSessionIdInStatus() {
        val pendingSession =
            TestFixtures.tutoringSession(
                status = SessionStatus.COMPLETED,
                currentPhase = null,
            ).apply {
                rewardStatus = RewardStatus.GRANTED
            }
        whenever(
            tutoringSessionRepository.existsByUserIdAndSessionDateAndStatus(
                any(),
                any(),
                org.mockito.kotlin.eq(SessionStatus.COMPLETED),
            ),
        ).thenReturn(true)
        whenever(tutoringSessionRepository.findByUserIdAndStatus(TestFixtures.USER_ID, SessionStatus.STARTED))
            .thenReturn(null)
        whenever(tutoringSessionRepository.findPendingRewardSession(TestFixtures.USER_ID))
            .thenReturn(listOf(pendingSession))

        val result = sut.getStatus(TestFixtures.USER_ID)

        assertTrue(result.lessonCompletedToday)
        assertEquals(TestFixtures.SESSION_ID, result.pendingRewardSessionId)
    }
}
