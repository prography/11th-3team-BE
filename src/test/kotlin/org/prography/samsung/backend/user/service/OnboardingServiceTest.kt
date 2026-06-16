package org.prography.samsung.backend.user.service

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
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.curriculum.service.CurriculumService
import org.prography.samsung.backend.support.TestFixtures
import org.prography.samsung.backend.user.dto.OnboardingRequest
import org.prography.samsung.backend.user.dto.UserScheduleRequest
import org.prography.samsung.backend.user.repository.UserCurriculumRepository
import org.prography.samsung.backend.user.repository.UserProfileRepository
import org.prography.samsung.backend.user.repository.UserRepository
import org.prography.samsung.backend.user.repository.UserScheduleRepository
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("OnboardingService 단위 테스트")
class OnboardingServiceTest {
    private val userRepository: UserRepository = mock()
    private val userProfileRepository: UserProfileRepository = mock()
    private val userCurriculumRepository: UserCurriculumRepository = mock()
    private val userScheduleRepository: UserScheduleRepository = mock()
    private val curriculumService: CurriculumService = mock()
    private lateinit var sut: OnboardingService

    @BeforeEach
    fun setUp() {
        sut =
            OnboardingService(
                userRepository,
                userProfileRepository,
                userCurriculumRepository,
                userScheduleRepository,
                curriculumService,
            )
    }

    @Test
    @DisplayName("온보딩 상태를 조회한다")
    fun shouldReturnOnboardingStatus() {
        val profile = TestFixtures.userProfile(onboardingCompleted = false, onboardingStep = 1)
        whenever(userProfileRepository.findById(TestFixtures.USER_ID)).thenReturn(TestFixtures.optional(profile))

        val result = sut.getStatus(TestFixtures.USER_ID)

        assertEquals(false, result.completed)
        assertEquals(1, result.step)
    }

    @Test
    @DisplayName("단원 선택 시 user_curriculums를 저장하고 step을 1로 올린다")
    fun shouldSaveCurriculumAndUpdateStep() {
        val user = TestFixtures.user()
        val curriculum = TestFixtures.curriculum()
        val profile = TestFixtures.userProfile(user = user)
        whenever(userRepository.findById(TestFixtures.USER_ID)).thenReturn(TestFixtures.optional(user))
        whenever(curriculumService.getActiveCurriculumOrThrow(TestFixtures.CURRICULUM_ID)).thenReturn(curriculum)
        whenever(userProfileRepository.findById(TestFixtures.USER_ID)).thenReturn(TestFixtures.optional(profile))
        whenever(userCurriculumRepository.findById(TestFixtures.USER_ID)).thenReturn(Optional.empty())

        val result = sut.saveCurriculum(TestFixtures.USER_ID, OnboardingRequest(TestFixtures.CURRICULUM_ID, 1))

        assertEquals(TestFixtures.CURRICULUM_ID, result.curriculumId)
        assertEquals(1, result.step)
        assertEquals(1, profile.onboardingStep)
        verify(userCurriculumRepository).save(any())
    }

    @Test
    @DisplayName("시간표 저장 시 요일·시간을 반영한다")
    fun shouldSaveScheduleWithValidatedDays() {
        val user = TestFixtures.user()
        val profile = TestFixtures.userProfile(user = user, onboardingStep = 1)
        val schedule = TestFixtures.userSchedule(user = user)
        whenever(userRepository.findById(TestFixtures.USER_ID)).thenReturn(TestFixtures.optional(user))
        whenever(userScheduleRepository.findWithDaysByUserId(TestFixtures.USER_ID)).thenReturn(schedule)
        whenever(userProfileRepository.findById(TestFixtures.USER_ID)).thenReturn(TestFixtures.optional(profile))
        whenever(userScheduleRepository.save(any())).thenAnswer { it.arguments[0] }

        val result =
            sut.saveSchedule(
                TestFixtures.USER_ID,
                UserScheduleRequest(
                    frequency = 3,
                    days = listOf("TUE", "THU", "SAT"),
                    time = "17:00",
                ),
            )

        assertEquals(3, result.frequency)
        assertEquals("17:00", result.time)
        assertEquals(3, result.days.size)
        assertEquals(2, profile.onboardingStep)
    }

    @Test
    @DisplayName("시간표 없이 온보딩 완료하면 SCHEDULE_NOT_CONFIGURED를 던진다")
    fun shouldThrowWhenCompletingWithoutSchedule() {
        val profile = TestFixtures.userProfile()
        whenever(userProfileRepository.findById(TestFixtures.USER_ID)).thenReturn(TestFixtures.optional(profile))
        whenever(userScheduleRepository.findById(TestFixtures.USER_ID)).thenReturn(Optional.empty())

        val exception =
            assertThrows(CustomException::class.java) {
                sut.completeOnboarding(TestFixtures.USER_ID)
            }

        assertEquals(DomainErrorCode.SCHEDULE_NOT_CONFIGURED, exception.errorCode)
    }

    @Test
    @DisplayName("시간표가 있으면 온보딩을 완료 처리한다")
    fun shouldCompleteOnboardingWhenScheduleExists() {
        val profile = TestFixtures.userProfile()
        val schedule = TestFixtures.userSchedule()
        whenever(userProfileRepository.findById(TestFixtures.USER_ID)).thenReturn(TestFixtures.optional(profile))
        whenever(userScheduleRepository.findById(TestFixtures.USER_ID)).thenReturn(TestFixtures.optional(schedule))

        val result = sut.completeOnboarding(TestFixtures.USER_ID)

        assertTrue(result.onboardingCompleted)
        assertTrue(profile.onboardingCompleted)
        assertEquals(2, profile.onboardingStep)
    }
}
