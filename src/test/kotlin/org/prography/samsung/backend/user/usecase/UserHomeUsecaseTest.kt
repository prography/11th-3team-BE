package org.prography.samsung.backend.user.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.prography.samsung.backend.common.domain.TopicType
import org.prography.samsung.backend.curriculum.dto.response.TodayTopicResponse
import org.prography.samsung.backend.curriculum.service.CurriculumService
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
    private val curriculumService: CurriculumService = mock()
    private lateinit var sut: UserHomeUsecase

    @BeforeEach
    fun setUp() {
        sut = UserHomeUsecase(
            userProfileService = UserProfileService(userProfileRepository, userCurriculumRepository, userRepository),
            sessionQueryService = sessionQueryService,
            curriculumService = curriculumService,
        )
    }

    @Test
    @DisplayName("모든 토픽 완료 시 마스터 메시지를 반환한다")
    fun shouldReturnMasterMessageWhenAllTopicsCompleted() {
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
        whenever(sessionQueryService.findCompletedTopicIdsToday(TestFixtures.USER_ID)).thenReturn(setOf(302L, 303L))
        whenever(curriculumService.getTodayTopics(TestFixtures.CURRICULUM_ID)).thenReturn(
            listOf(
                TodayTopicResponse(1, 302L, "분수란?", "개념이해", TopicType.CONCEPT, false),
                TodayTopicResponse(2, 303L, "덧셈과 뺄셈", "계산과정", TopicType.CALCULATION, false),
            ),
        )

        val result = sut.getHome(TestFixtures.USER_ID)

        assertEquals(true, result.lessonCompletedToday)
        assertEquals(TestFixtures.SESSION_ID, result.pendingRewardSessionId)
        assertEquals("선생님 덕분에 분수의 계산 마스터! 다음에 또 만나요!", result.homeMessage)
    }

    @Test
    @DisplayName("일부 토픽만 완료 시 요청 메시지를 반환한다")
    fun shouldReturnRequestMessageWhenSomeTopicsNotCompleted() {
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
                pendingRewardSessionId = null,
            ),
        )
        whenever(sessionQueryService.findCompletedTopicIdsToday(TestFixtures.USER_ID)).thenReturn(setOf(302L))
        whenever(curriculumService.getTodayTopics(TestFixtures.CURRICULUM_ID)).thenReturn(
            listOf(
                TodayTopicResponse(1, 302L, "분수란?", "개념이해", TopicType.CONCEPT, false),
                TodayTopicResponse(2, 303L, "덧셈과 뺄셈", "계산과정", TopicType.CALCULATION, false),
            ),
        )

        val result = sut.getHome(TestFixtures.USER_ID)

        assertEquals(false, result.lessonCompletedToday)
        assertEquals("쌤 오늘 학교에서 분수의 계산 배웠는데 하나도 모르겠어요 ㅠㅠ", result.homeMessage)
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
        whenever(sessionQueryService.findCompletedTopicIdsToday(TestFixtures.USER_ID)).thenReturn(emptySet())
        whenever(curriculumService.getTodayTopics(TestFixtures.CURRICULUM_ID)).thenReturn(
            listOf(
                TodayTopicResponse(1, 302L, "분수란?", "개념이해", TopicType.CONCEPT, false),
            ),
        )

        val result = sut.getHome(TestFixtures.USER_ID)

        assertEquals(false, result.lessonCompletedToday)
        assertEquals("쌤 오늘 학교에서 분수의 계산 배웠는데 하나도 모르겠어요 ㅠㅠ", result.homeMessage)
    }
}
