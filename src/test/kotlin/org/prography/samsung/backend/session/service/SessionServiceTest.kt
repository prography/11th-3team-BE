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
import org.prography.samsung.backend.common.domain.SessionPhase
import org.prography.samsung.backend.common.domain.SessionStatus
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.curriculum.service.CurriculumService
import org.prography.samsung.backend.session.dto.request.SessionStartRequest
import org.prography.samsung.backend.session.repository.SessionTopicSnapshotRepository
import org.prography.samsung.backend.session.repository.TutoringSessionRepository
import org.prography.samsung.backend.support.TestFixtures

@ExtendWith(MockitoExtension::class)
@DisplayName("SessionLifecycleService 단위 테스트")
class SessionServiceTest {
    private val curriculumService: CurriculumService = mock()
    private val sessionQueryService: SessionQueryService = mock()
    private val tutoringSessionRepository: TutoringSessionRepository = mock()
    private val sessionTopicSnapshotRepository: SessionTopicSnapshotRepository = mock()
    private lateinit var sut: SessionLifecycleService

    @BeforeEach
    fun setUp() {
        sut =
            SessionLifecycleService(
                curriculumService = curriculumService,
                sessionQueryService = sessionQueryService,
                tutoringSessionRepository = tutoringSessionRepository,
                sessionTopicSnapshotRepository = sessionTopicSnapshotRepository,
            )
    }

    @Test
    @DisplayName("진행 중 세션이 있으면 start는 resumed true로 반환한다")
    fun shouldResumeWhenActiveSessionExists() {
        val existing = TestFixtures.tutoringSession()
        whenever(tutoringSessionRepository.findByUserIdAndStatus(TestFixtures.USER_ID, SessionStatus.STARTED))
            .thenReturn(existing)

        val result = sut.start(
            TestFixtures.user(),
            TestFixtures.userCurriculum(),
            SessionStartRequest(curriculumId = TestFixtures.CURRICULUM_ID),
        )

        assertEquals(TestFixtures.SESSION_ID, result.sessionId)
        assertTrue(result.resumed)
        verify(tutoringSessionRepository, org.mockito.kotlin.never()).save(any())
    }

    @Test
    @DisplayName("lessonTopicId가 현재 커리큘럼과 다르면 BAD_REQUEST를 던진다")
    fun shouldThrowWhenLessonTopicIdMismatchOnStart() {
        val otherCurriculum = TestFixtures.curriculum(id = 99L)
        val otherTopic = TestFixtures.lessonTopic(curriculum = otherCurriculum, sequence = 1)
        whenever(tutoringSessionRepository.findByUserIdAndStatus(TestFixtures.USER_ID, SessionStatus.STARTED))
            .thenReturn(null)
        whenever(curriculumService.getLessonTopicById(otherTopic.id)).thenReturn(otherTopic)

        val exception =
            assertThrows(CustomException::class.java) {
                sut.start(
                    TestFixtures.user(),
                    TestFixtures.userCurriculum(),
                    SessionStartRequest(lessonTopicId = otherTopic.id),
                )
            }

        assertEquals(DomainErrorCode.CURRICULUM_MISMATCH, exception.errorCode)
    }

    @Test
    @DisplayName("INTRO 단계에서 advance-phase하면 REACTION으로 전환한다")
    fun shouldAdvancePhaseFromIntroToReaction() {
        val session = TestFixtures.tutoringSession(currentPhase = SessionPhase.INTRO)
        whenever(sessionQueryService.getStartedSession(TestFixtures.USER_ID, TestFixtures.SESSION_ID))
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
        whenever(sessionQueryService.getStartedSession(TestFixtures.USER_ID, TestFixtures.SESSION_ID))
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
        whenever(sessionQueryService.getStartedSession(TestFixtures.USER_ID, TestFixtures.SESSION_ID))
            .thenReturn(session)

        val result = sut.abort(TestFixtures.USER_ID, TestFixtures.SESSION_ID)

        assertEquals(SessionStatus.ABORTED, result.status)
        assertEquals(SessionStatus.ABORTED, session.status)
        assertEquals(null, session.currentPhase)
    }
}
