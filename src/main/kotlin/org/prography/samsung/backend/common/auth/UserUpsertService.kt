package org.prography.samsung.backend.common.auth

import org.prography.samsung.backend.gamification.repository.BadgeLevelRepository
import org.prography.samsung.backend.user.entity.User
import org.prography.samsung.backend.user.entity.UserProfile
import org.prography.samsung.backend.user.repository.UserProfileRepository
import org.prography.samsung.backend.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserUpsertService(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
    private val badgeLevelRepository: BadgeLevelRepository,
) {
    @Transactional
    fun upsertByExternalId(externalId: String): CurrentUser {
        val existing = userRepository.findByExternalId(externalId)
        if (existing != null) {
            return CurrentUser(existing.id, existing.externalId)
        }

        val user = userRepository.save(User(externalId = externalId))
        val defaultBadge =
            badgeLevelRepository.findByLevel(1)
                ?: badgeLevelRepository.findAll().minByOrNull { it.level }
                ?: throw IllegalStateException("Badge level seed is missing")
        userProfileRepository.save(
            UserProfile(
                user = user,
                badgeLevel = defaultBadge,
            ),
        )
        return CurrentUser(user.id, user.externalId)
    }
}
