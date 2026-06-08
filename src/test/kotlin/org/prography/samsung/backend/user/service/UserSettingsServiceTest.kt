package org.prography.samsung.backend.user.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
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
import org.prography.samsung.backend.curriculum.service.CurriculumService
import org.prography.samsung.backend.notification.service.NotificationService
import org.prography.samsung.backend.session.repository.TutoringSessionRepository
import org.prography.samsung.backend.support.TestFixtures
import org.prography.samsung.backend.user.dto.UserSettingsRequest
import org.prography.samsung.backend.user.repository.UserCurriculumRepository
import org.prography.samsung.backend.user.repository.UserRepository
import org.prography.samsung.backend.user.repository.UserScheduleRepository

@ExtendWith(MockitoExtension::class)
@DisplayName("UserSettingsService 단위 테스트")
class UserSettingsServiceTest {
    private val userRepository: UserRepository = mock()
    private val userCurriculumRepository: UserCurriculumRepository = mock()
    private val userScheduleRepository: UserScheduleRepository = mock()
    private val tutoringSessionRepository: TutoringSessionRepository = mock()
    private val curriculumService: CurriculumService = mock()
    private val onboardingService: OnboardingService = mock()
    private val notificationService: NotificationService = mock()
    private lateinit var sut: UserSettingsService

    @BeforeEach
    fun setUp() {
        sut =
            UserSettingsService(
                userRepository,
                userCurriculumRepository,
                userScheduleRepository,
                tutoringSessionRepository,
                curriculumService,
                onboardingService,
                notificationService,
            )
    }

    @Test
    @DisplayName("설정 조회 시 단원과 시간표를 반환한다")
    fun shouldReturnCurrentSettings() {
        val userCurriculum = TestFixtures.userCurriculum()
        val schedule = TestFixtures.userSchedule()
        whenever(
            userCurriculumRepository.findById(TestFixtures.USER_ID),
        ).thenReturn(TestFixtures.optional(userCurriculum))
        whenever(userScheduleRepository.findWithDaysByUserId(TestFixtures.USER_ID)).thenReturn(schedule)
        whenever(onboardingService.toScheduleResponse(schedule))
            .thenReturn(
                org.prography.samsung.backend.common.dto.UserScheduleResponse(
                    frequency = 3,
                    days = listOf("TUE", "THU", "SAT"),
                    time = "17:00",
                ),
            )

        val result = sut.getSettings(TestFixtures.USER_ID)

        assertEquals("FRACTION_CALC", result.curriculum.code)
        assertEquals("17:00", result.schedule.time)
    }

    @Test
    @DisplayName("변경 없이 저장하면 기존 설정을 그대로 반환한다")
    fun shouldReturnCurrentSettingsWhenNoChanges() {
        val userCurriculum = TestFixtures.userCurriculum()
        val schedule = TestFixtures.userSchedule()
        whenever(
            userCurriculumRepository.findById(TestFixtures.USER_ID),
        ).thenReturn(TestFixtures.optional(userCurriculum))
        whenever(userScheduleRepository.findWithDaysByUserId(TestFixtures.USER_ID)).thenReturn(schedule)
        whenever(onboardingService.toScheduleResponse(schedule))
            .thenReturn(
                org.prography.samsung.backend.common.dto.UserScheduleResponse(
                    frequency = 3,
                    days = listOf("TUE", "THU", "SAT"),
                    time = "17:00",
                ),
            )

        val result = sut.updateSettings(TestFixtures.USER_ID, UserSettingsRequest())

        assertEquals(TestFixtures.CURRICULUM_ID, result.curriculum.id)
        verify(onboardingService, never()).saveSchedule(any(), any())
    }

    @Test
    @DisplayName("진행 중 세션이 있으면 단원 변경 시 ACTIVE_SESSION_EXISTS를 던진다")
    fun shouldThrowWhenChangingCurriculumWithActiveSession() {
        val userCurriculum = TestFixtures.userCurriculum()
        val schedule = TestFixtures.userSchedule()
        whenever(
            userCurriculumRepository.findById(TestFixtures.USER_ID),
        ).thenReturn(TestFixtures.optional(userCurriculum))
        whenever(userScheduleRepository.findWithDaysByUserId(TestFixtures.USER_ID)).thenReturn(schedule)
        whenever(onboardingService.toScheduleResponse(schedule))
            .thenReturn(
                org.prography.samsung.backend.common.dto.UserScheduleResponse(
                    frequency = 3,
                    days = listOf("TUE", "THU", "SAT"),
                    time = "17:00",
                ),
            )
        whenever(tutoringSessionRepository.findByUserIdAndStatus(TestFixtures.USER_ID, SessionStatus.STARTED))
            .thenReturn(TestFixtures.tutoringSession())

        val exception =
            assertThrows(CustomException::class.java) {
                sut.updateSettings(
                    TestFixtures.USER_ID,
                    UserSettingsRequest(curriculumId = 1L, resetProgress = true),
                )
            }

        assertEquals(DomainErrorCode.ACTIVE_SESSION_EXISTS, exception.errorCode)
    }

    @Test
    @DisplayName("시간표 변경 시 saveSchedule과 알림 reschedule을 호출한다")
    fun shouldUpdateScheduleAndRescheduleNotifications() {
        val userCurriculum = TestFixtures.userCurriculum()
        val schedule = TestFixtures.userSchedule()
        whenever(
            userCurriculumRepository.findById(TestFixtures.USER_ID),
        ).thenReturn(TestFixtures.optional(userCurriculum))
        whenever(userScheduleRepository.findWithDaysByUserId(TestFixtures.USER_ID)).thenReturn(schedule)
        whenever(onboardingService.toScheduleResponse(schedule))
            .thenReturn(
                org.prography.samsung.backend.common.dto.UserScheduleResponse(
                    frequency = 3,
                    days = listOf("TUE", "THU", "SAT"),
                    time = "17:00",
                ),
            )
            .thenReturn(
                org.prography.samsung.backend.common.dto.UserScheduleResponse(
                    frequency = 2,
                    days = listOf("MON", "WED"),
                    time = "18:00",
                ),
            )

        sut.updateSettings(
            TestFixtures.USER_ID,
            UserSettingsRequest(frequency = 2, days = listOf("MON", "WED"), time = "18:00"),
        )

        verify(onboardingService).saveSchedule(any(), any())
        verify(notificationService).rescheduleFromUserSchedule(TestFixtures.USER_ID)
    }
}
