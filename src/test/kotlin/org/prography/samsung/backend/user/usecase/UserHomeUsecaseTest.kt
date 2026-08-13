package org.prography.samsung.backend.user.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.prography.samsung.backend.session.dto.response.SessionStatusResponse
import org.prography.samsung.backend.session.service.SessionQueryService
import org.prography.samsung.backend.support.TestFixtures
import org.prography.samsung.backend.user.repository.UserCurriculumRepository
import org.prography.samsung.backend.user.repository.UserProfileRepository
import org.prography.samsung.backend.user.repository.UserRepository
import org.prography.samsung.backend.user.service.UserProfileService

@ExtendWith(MockitoExtension::class)
@DisplayName("UserHomeUsecase 단위 테스트")
class UserHomeUsecaseTest {
    private val userProfileRepository: UserProfileRepository = mock()
    private val userCurriculumRepository: UserCurriculumRepository = mock()
    private val userRepository: UserRepository = mock()
    private val sessionQueryService: SessionQueryService = mock()
    private lateinit var sut: UserHomeUsecase

    @BeforeEach
    fun setUp() {
        sut = UserHomeUsecase(
            userProfileService = UserProfileService(userProfileRepository, userCurriculumRepository, userRepository),
            sessionQueryService = sessionQueryService,
        )
    }

    @Test
    @DisplayName("수업 완료 시 마스터 메시지를 반환한다")
    fun shouldReturnMasterMessageWhenLessonCompleted() {
        val profile = TestFixtures.userProfile()
        val userCurriculum = TestFixtures.userCurriculum()
        whenever(userProfileRepository.findById(TestFixtures.USER_ID)).thenReturn(TestFixtures.optional(profile))
        whenever(
            userCurriculumRepository.findById(TestFixtures.USER_ID),
        ).thenReturn(TestFixtures.optional(userCurriculum))
        whenever(sessionQueryService.getStatus(TestFixtures.USER_ID)).thenReturn(
            SessionStatusResponse(
                lessonCompletedToday = true,
                activeSession = null,
                pendingRewardSessionId = TestFixtures.SESSION_ID,
            ),
        )

        val result = sut.getHome(TestFixtures.USER_ID)

        assertEquals(true, result.lessonCompletedToday)
        assertEquals(TestFixtures.SESSION_ID, result.pendingRewardSessionId)
        assertEquals("선생님 덕분에 분수의 계산 마스터! 다음에 또 만나요!", result.homeMessage)
    }

    @Test
    @DisplayName("수업 미완료 시 요청 메시지를 반환한다")
    fun shouldReturnRequestMessageWhenLessonNotCompleted() {
        val profile = TestFixtures.userProfile()
        val userCurriculum = TestFixtures.userCurriculum()
        whenever(userProfileRepository.findById(TestFixtures.USER_ID)).thenReturn(TestFixtures.optional(profile))
        whenever(
            userCurriculumRepository.findById(TestFixtures.USER_ID),
        ).thenReturn(TestFixtures.optional(userCurriculum))
        whenever(sessionQueryService.getStatus(TestFixtures.USER_ID)).thenReturn(
            SessionStatusResponse(
                lessonCompletedToday = false,
                activeSession = null,
                pendingRewardSessionId = null,
            ),
        )

        val result = sut.getHome(TestFixtures.USER_ID)

        assertEquals(false, result.lessonCompletedToday)
        assertEquals("쌤 오늘 학교에서 분수의 계산 배웠는데 하나도 모르겠어요 ㅠㅠ", result.homeMessage)
    }
}
