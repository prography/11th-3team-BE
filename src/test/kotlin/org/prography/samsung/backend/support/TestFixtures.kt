package org.prography.samsung.backend.support

import org.prography.samsung.backend.common.domain.DayOfWeekCode
import org.prography.samsung.backend.common.domain.SessionPhase
import org.prography.samsung.backend.common.domain.SessionStatus
import org.prography.samsung.backend.common.domain.TopicType
import org.prography.samsung.backend.curriculum.entity.Curriculum
import org.prography.samsung.backend.curriculum.entity.LessonTopic
import org.prography.samsung.backend.gamification.entity.BadgeLevel
import org.prography.samsung.backend.session.entity.SessionTopicSnapshot
import org.prography.samsung.backend.session.entity.TutoringSession
import org.prography.samsung.backend.user.entity.User
import org.prography.samsung.backend.user.entity.UserCurriculum
import org.prography.samsung.backend.user.entity.UserProfile
import org.prography.samsung.backend.user.entity.UserSchedule
import org.prography.samsung.backend.user.entity.UserScheduleDay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional

object TestFixtures {
    const val USER_ID = 1L
    const val SESSION_ID = "550e8400-e29b-41d4-a716-446655440000"
    const val CURRICULUM_ID = 3L

    fun user(id: Long = USER_ID, externalId: String = "device-user-id"): User = User(id = id, externalId = externalId)

    fun curriculum(id: Long = CURRICULUM_ID): Curriculum = Curriculum(
        code = "FRACTION_CALC",
        name = "분수의 계산",
        chapterLabel = "3단원 분수",
        sessionTitleTemplate = "분수의 세계",
        displayOrder = 3,
    ).also { curriculum ->
        val idField = Curriculum::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(curriculum, id)
    }

    fun badgeLevel(level: Int, name: String, required: Int): BadgeLevel =
        BadgeLevel(level = level, name = name, requiredCompletedSessions = required)

    fun userProfile(
        user: User = user(),
        badgeLevel: BadgeLevel = badgeLevel(1, "새싹 선생님", 0),
        totalCoins: Int = 0,
        completedSessionCount: Int = 0,
        onboardingCompleted: Boolean = false,
        onboardingStep: Int = 0,
    ): UserProfile = UserProfile(
        user = user,
        badgeLevel = badgeLevel,
        totalCoins = totalCoins,
        completedSessionCount = completedSessionCount,
        onboardingCompleted = onboardingCompleted,
        onboardingStep = onboardingStep,
    )

    fun userCurriculum(
        user: User = user(),
        curriculum: Curriculum = curriculum(),
        progressPercent: Int = 0,
    ): UserCurriculum = UserCurriculum(
        user = user,
        curriculum = curriculum,
        progressPercent = progressPercent,
    )

    fun userSchedule(
        user: User = user(),
        frequency: Int = 3,
        lessonTime: LocalTime = LocalTime.of(17, 0),
        days: List<DayOfWeekCode> = listOf(DayOfWeekCode.TUE, DayOfWeekCode.THU, DayOfWeekCode.SAT),
    ): UserSchedule {
        val schedule = UserSchedule(user = user, frequencyPerWeek = frequency, lessonTime = lessonTime)
        days.forEachIndexed { index, day ->
            schedule.days.add(UserScheduleDay(userSchedule = schedule, dayOfWeek = day, selectedOrder = index + 1))
        }
        return schedule
    }

    fun tutoringSession(
        id: String = SESSION_ID,
        user: User = user(),
        curriculum: Curriculum = curriculum(),
        status: SessionStatus = SessionStatus.STARTED,
        currentPhase: SessionPhase? = SessionPhase.INTRO,
        coinsAwarded: Int? = null,
        badgeLevelUp: Boolean? = null,
        progressAfter: Int? = null,
    ): TutoringSession = TutoringSession(
        id = id,
        user = user,
        curriculum = curriculum,
        status = status,
        currentPhase = currentPhase,
        sessionDate = LocalDate.of(2026, 6, 8),
        startedAt = Instant.parse("2026-06-08T05:00:00Z"),
        coinsAwarded = coinsAwarded,
        badgeLevelUp = badgeLevelUp,
        progressAfter = progressAfter,
    )

    fun lessonTopic(
        curriculum: Curriculum = curriculum(),
        sequence: Int = 1,
        gnbTitle: String = "3. 분수의 개념",
    ): LessonTopic {
        val topic =
            LessonTopic(
                curriculum = curriculum,
                sequence = sequence,
                title = "분수란?",
                subtitle = "개념이해",
                topicType = TopicType.CONCEPT,
                gnbTitle = gnbTitle,
            )
        val idField = LessonTopic::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(topic, 301L + sequence)
        return topic
    }

    fun sessionTopicSnapshot(
        session: TutoringSession = tutoringSession(),
        lessonTopic: LessonTopic = lessonTopic(),
        sequence: Int = 1,
    ): SessionTopicSnapshot = SessionTopicSnapshot(
        session = session,
        lessonTopic = lessonTopic,
        sequence = sequence,
        title = lessonTopic.title,
        subtitle = lessonTopic.subtitle,
        topicType = lessonTopic.topicType,
    )

    fun <T : Any> optional(value: T): Optional<T> = Optional.of(value)
}
