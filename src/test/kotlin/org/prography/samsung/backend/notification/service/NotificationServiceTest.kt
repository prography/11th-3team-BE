package org.prography.samsung.backend.notification.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.prography.samsung.backend.common.domain.DevicePlatform
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.notification.dto.NotificationRegisterRequest
import org.prography.samsung.backend.notification.repository.DeviceTokenRepository
import org.prography.samsung.backend.notification.repository.NotificationScheduleRepository
import org.prography.samsung.backend.support.TestFixtures
import org.prography.samsung.backend.user.repository.UserRepository
import org.prography.samsung.backend.user.repository.UserScheduleRepository

@ExtendWith(MockitoExtension::class)
@DisplayName("NotificationService 단위 테스트")
class NotificationServiceTest {
    private val userRepository: UserRepository = mock()
    private val userScheduleRepository: UserScheduleRepository = mock()
    private val deviceTokenRepository: DeviceTokenRepository = mock()
    private val notificationScheduleRepository: NotificationScheduleRepository = mock()
    private lateinit var sut: NotificationService

    @BeforeEach
    fun setUp() {
        sut =
            NotificationService(
                userRepository,
                userScheduleRepository,
                deviceTokenRepository,
                notificationScheduleRepository,
            )
    }

    @Test
    @DisplayName("시간표 없이 등록하면 SCHEDULE_NOT_CONFIGURED를 던진다")
    fun shouldThrowWhenScheduleNotConfigured() {
        whenever(userScheduleRepository.findWithDaysByUserId(TestFixtures.USER_ID)).thenReturn(null)

        val exception =
            assertThrows(CustomException::class.java) {
                sut.register(TestFixtures.USER_ID, NotificationRegisterRequest())
            }

        assertEquals(DomainErrorCode.SCHEDULE_NOT_CONFIGURED, exception.errorCode)
    }

    @Test
    @DisplayName("알림 등록 시 스케줄 수와 함께 성공 응답을 반환한다")
    fun shouldRegisterNotificationSchedules() {
        val user = TestFixtures.user()
        val schedule = TestFixtures.userSchedule(user = user)
        whenever(userScheduleRepository.findWithDaysByUserId(TestFixtures.USER_ID)).thenReturn(schedule)
        whenever(userRepository.findById(TestFixtures.USER_ID)).thenReturn(TestFixtures.optional(user))
        whenever(deviceTokenRepository.findByUserIdAndToken(TestFixtures.USER_ID, "fcm-token")).thenReturn(null)

        val result =
            sut.register(
                TestFixtures.USER_ID,
                NotificationRegisterRequest(deviceToken = "fcm-token", platform = DevicePlatform.IOS),
            )

        assertEquals(true, result.registered)
        assertEquals(3, result.scheduleCount)
        verify(notificationScheduleRepository).deleteAllByUserId(TestFixtures.USER_ID)
        verify(notificationScheduleRepository, org.mockito.kotlin.times(3)).save(any())
    }

    @Test
    @DisplayName("reschedule은 register를 호출하고 rescheduled true를 반환한다")
    fun shouldRescheduleNotifications() {
        val user = TestFixtures.user()
        val schedule = TestFixtures.userSchedule(user = user)
        whenever(userScheduleRepository.findWithDaysByUserId(TestFixtures.USER_ID)).thenReturn(schedule)
        whenever(userRepository.findById(TestFixtures.USER_ID)).thenReturn(TestFixtures.optional(user))

        val result = sut.reschedule(TestFixtures.USER_ID, NotificationRegisterRequest())

        assertEquals(true, result.rescheduled)
    }
}
