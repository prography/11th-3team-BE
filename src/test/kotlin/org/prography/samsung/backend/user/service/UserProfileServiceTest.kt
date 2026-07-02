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
import org.prography.samsung.backend.support.TestFixtures
import org.prography.samsung.backend.user.repository.UserCurriculumRepository
import org.prography.samsung.backend.user.repository.UserProfileRepository
import org.prography.samsung.backend.user.repository.UserRepository

@ExtendWith(MockitoExtension::class)
@DisplayName("UserProfileService 단위 테스트")
class UserProfileServiceTest {
    private val userProfileRepository: UserProfileRepository = mock()
    private val userCurriculumRepository: UserCurriculumRepository = mock()
    private val userRepository: UserRepository = mock()
    private lateinit var sut: UserProfileService

    @BeforeEach
    fun setUp() {
        sut = UserProfileService(userProfileRepository, userCurriculumRepository, userRepository)
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

        val result = sut.getUserProfileResponse(TestFixtures.USER_ID)

        assertEquals(1, result.level.number)
        assertEquals("분수의 계산", result.curriculum.name)
        assertEquals("쌤 오늘 학교에서 분수의 계산 배웠는데 하나도 모르겠어요 ㅠㅠ", result.homeMessage)
    }

    @Test
    @DisplayName("홈 메시지는 항상 요청 메시지를 반환한다")
    fun shouldReturnRequestMessageInProfile() {
        val profile = TestFixtures.userProfile()
        val userCurriculum = TestFixtures.userCurriculum()
        whenever(userProfileRepository.findById(TestFixtures.USER_ID)).thenReturn(TestFixtures.optional(profile))
        whenever(
            userCurriculumRepository.findById(TestFixtures.USER_ID),
        ).thenReturn(TestFixtures.optional(userCurriculum))

        val result = sut.getUserProfileResponse(TestFixtures.USER_ID)

        assertFalse(result.homeMessage.contains("마스터"))
    }
}
