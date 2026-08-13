package org.prography.samsung.backend.session.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.prography.samsung.backend.session.dto.request.SessionStartRequest
import org.prography.samsung.backend.session.dto.response.SessionStartResponse
import org.prography.samsung.backend.session.service.SessionLifecycleService
import org.prography.samsung.backend.support.TestFixtures
import org.prography.samsung.backend.user.repository.UserCurriculumRepository
import org.prography.samsung.backend.user.repository.UserProfileRepository
import org.prography.samsung.backend.user.repository.UserRepository
import org.prography.samsung.backend.user.service.UserProfileService

@ExtendWith(MockitoExtension::class)
@DisplayName("SessionLifeCycleUsecase 단위 테스트")
class SessionLifeCycleUsecaseTest {
    private val sessionLifecycleService: SessionLifecycleService = mock()
    private val userProfileRepository: UserProfileRepository = mock()
    private val userCurriculumRepository: UserCurriculumRepository = mock()
    private val userRepository: UserRepository = mock()
    private lateinit var sut: SessionLifeCycleUsecase

    @BeforeEach
    fun setUp() {
        sut =
            SessionLifeCycleUsecase(
                sessionLifecycleService = sessionLifecycleService,
                userProfileService =
                UserProfileService(
                    userProfileRepository,
                    userCurriculumRepository,
                    userRepository,
                ),
            )
    }

    @Test
    @DisplayName("현재 커리큘럼의 lessonTopicId가 있으면 오래된 curriculumId도 세션 시작을 위임한다")
    fun shouldDelegateStartWhenLessonTopicIdExistsWithStaleCurriculumId() {
        val user = TestFixtures.user()
        val userCurriculum = TestFixtures.userCurriculum(user = user)
        val request = SessionStartRequest(lessonTopicId = 301L, curriculumId = 99L)
        val expected =
            SessionStartResponse(
                sessionId = TestFixtures.SESSION_ID,
                startedAt = "2026-06-08T14:00:00+09:00",
                resumed = false,
            )
        whenever(userRepository.findById(TestFixtures.USER_ID)).thenReturn(TestFixtures.optional(user))
        whenever(userCurriculumRepository.findById(TestFixtures.USER_ID)).thenReturn(
            TestFixtures.optional(userCurriculum),
        )
        whenever(sessionLifecycleService.start(user, userCurriculum, request)).thenReturn(expected)

        val result = sut.start(TestFixtures.USER_ID, request)

        assertEquals(expected, result)
    }
}
