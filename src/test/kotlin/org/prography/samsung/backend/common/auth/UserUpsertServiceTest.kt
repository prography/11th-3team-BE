package org.prography.samsung.backend.common.auth

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
import org.prography.samsung.backend.gamification.repository.BadgeLevelRepository
import org.prography.samsung.backend.support.TestFixtures
import org.prography.samsung.backend.user.entity.User
import org.prography.samsung.backend.user.repository.UserProfileRepository
import org.prography.samsung.backend.user.repository.UserRepository

@ExtendWith(MockitoExtension::class)
@DisplayName("UserUpsertService 단위 테스트")
class UserUpsertServiceTest {
    private val userRepository: UserRepository = mock()
    private val userProfileRepository: UserProfileRepository = mock()
    private val badgeLevelRepository: BadgeLevelRepository = mock()
    private lateinit var sut: UserUpsertService

    @BeforeEach
    fun setUp() {
        sut = UserUpsertService(userRepository, userProfileRepository, badgeLevelRepository)
    }

    @Test
    @DisplayName("기존 externalId가 있으면 새로 생성하지 않고 반환한다")
    fun shouldReturnExistingUserWithoutCreatingProfile() {
        val existingUser = TestFixtures.user(externalId = "device-id")
        whenever(userRepository.findByExternalId("device-id")).thenReturn(existingUser)

        val result = sut.upsertByExternalId("device-id")

        assertEquals(TestFixtures.USER_ID, result.userId)
        assertEquals("device-id", result.externalId)
        verify(userRepository, never()).save(any<User>())
        verify(userProfileRepository, never()).save(any())
    }

    @Test
    @DisplayName("신규 externalId면 user와 profile을 생성한다")
    fun shouldCreateUserAndProfileForNewExternalId() {
        val savedUser = TestFixtures.user()
        val defaultBadge = TestFixtures.badgeLevel(1, "새싹 선생님", 0)
        whenever(userRepository.findByExternalId("new-device")).thenReturn(null)
        whenever(userRepository.save(any<User>())).thenReturn(savedUser)
        whenever(badgeLevelRepository.findByLevel(1)).thenReturn(defaultBadge)

        val result = sut.upsertByExternalId("new-device")

        assertEquals(TestFixtures.USER_ID, result.userId)
        verify(userProfileRepository).save(any())
    }

    @Test
    @DisplayName("배지 시드가 없으면 IllegalStateException을 던진다")
    fun shouldThrowWhenBadgeSeedMissing() {
        whenever(userRepository.findByExternalId("new-device")).thenReturn(null)
        whenever(userRepository.save(any<User>())).thenReturn(TestFixtures.user())
        whenever(badgeLevelRepository.findByLevel(1)).thenReturn(null)
        whenever(badgeLevelRepository.findAll()).thenReturn(emptyList())

        assertThrows(IllegalStateException::class.java) {
            sut.upsertByExternalId("new-device")
        }
    }
}
