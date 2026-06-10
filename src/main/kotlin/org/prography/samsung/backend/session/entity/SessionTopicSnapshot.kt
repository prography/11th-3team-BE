package org.prography.samsung.backend.session.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.prography.samsung.backend.common.domain.TopicType
import org.prography.samsung.backend.common.entity.BaseEntity
import org.prography.samsung.backend.curriculum.entity.LessonTopic

@Entity
@Table(name = "session_topic_snapshots")
class SessionTopicSnapshot(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    val session: TutoringSession,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_topic_id", nullable = false)
    val lessonTopic: LessonTopic,

    @Column(nullable = false)
    val sequence: Int,

    @Column(nullable = false, length = 200)
    val title: String,

    @Column(length = 100)
    val subtitle: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "topic_type", nullable = false, length = 20)
    val topicType: TopicType,
) : BaseEntity()
