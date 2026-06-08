package org.prography.samsung.backend.user.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.prography.samsung.backend.session.dto.SessionStatusResponse
import org.prography.samsung.backend.session.service.SessionService
import org.prography.samsung.backend.support.TestFixtures
import org.prography.samsung.backend.user.repository.UserCurriculumRepository
import org.prography.samsung.backend.user.repository.UserProfileRepository

@ExtendWith(MockitoExtension::class)
@DisplayName("UserProfileService 단위 테스트")
class UserProfileServiceTest {
    private val userProfileRepository: UserProfileRepository = mock()
    private val userCurriculumRepository: UserCurriculumRepository = mock()
    private val sessionService: SessionService = mock()
    private lateinit var sut: UserProfileService

    @BeforeEach
    fun setUp() {
        sut = UserProfileService(userProfileRepository, userCurriculumRepository, sessionService)
    }

    @Test
    @DisplayName("프로필 조회 시 레벨·단원·홈 메시지를 반환한다")
    fun shouldReturnProfileWithHomeMessage() {
        val profile = TestFixtures.userProfile()
        val userCurriculum = TestFixtures.userCurriculum()
        whenever(userProfileRepository.findById(TestFixtures.USER_ID)).thenReturn(TestFixtures.optional(profile))
        whenever(
            userCurriculumRepository.findById(TestFixtures.USER_ID),
        ).thenReturn(TestFixtures.optional(userCurriculum))
        whenever(sessionService.getStatus(TestFixtures.USER_ID))
            .thenReturn(
                SessionStatusResponse(
                    lessonCompletedToday = false,
                    activeSession = null,
                    pendingRewardSessionId = null,
                ),
            )

        val result = sut.getProfile(TestFixtures.USER_ID)

        assertEquals(1, result.level.number)
        assertEquals("분수의 계산", result.curriculum.name)
        assertEquals("쌤 오늘 학교에서 분수의 계산 배웠는데 하나도 모르겠어요 ㅠㅠ", result.homeMessage)
    }

    @Test
    @DisplayName("홈 조회 시 프로필과 세션 상태를 병합한다")
    fun shouldReturnCombinedHomeResponse() {
        val profile = TestFixtures.userProfile()
        val userCurriculum = TestFixtures.userCurriculum()
        val sessionStatus =
            SessionStatusResponse(
                lessonCompletedToday = true,
                activeSession = null,
                pendingRewardSessionId = TestFixtures.SESSION_ID,
            )
        whenever(userProfileRepository.findById(TestFixtures.USER_ID)).thenReturn(TestFixtures.optional(profile))
        whenever(
            userCurriculumRepository.findById(TestFixtures.USER_ID),
        ).thenReturn(TestFixtures.optional(userCurriculum))
        whenever(sessionService.getStatus(TestFixtures.USER_ID)).thenReturn(sessionStatus)

        val result = sut.getHome(TestFixtures.USER_ID)

        assertEquals(true, result.lessonCompletedToday)
        assertEquals(TestFixtures.SESSION_ID, result.pendingRewardSessionId)
        assertEquals("선생님 덕분에 분수의 계산 마스터! 다음에 또 만나요!", result.homeMessage)
    }

    @Test
    @DisplayName("당일 수업 미완료 시 요청 메시지를 반환한다")
    fun shouldReturnRequestMessageWhenLessonNotCompletedToday() {
        val profile = TestFixtures.userProfile()
        val userCurriculum = TestFixtures.userCurriculum()
        whenever(userProfileRepository.findById(TestFixtures.USER_ID)).thenReturn(TestFixtures.optional(profile))
        whenever(
            userCurriculumRepository.findById(TestFixtures.USER_ID),
        ).thenReturn(TestFixtures.optional(userCurriculum))
        whenever(sessionService.getStatus(TestFixtures.USER_ID))
            .thenReturn(
                SessionStatusResponse(
                    lessonCompletedToday = false,
                    activeSession = null,
                    pendingRewardSessionId = null,
                ),
            )

        val result = sut.getProfile(TestFixtures.USER_ID)

        assertFalse(result.homeMessage.contains("마스터"))
    }
}
