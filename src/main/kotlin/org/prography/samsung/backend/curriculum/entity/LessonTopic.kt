package org.prography.samsung.backend.curriculum.entity

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

@Entity
@Table(name = "lesson_topics")
class LessonTopic(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_id", nullable = false)
    val curriculum: Curriculum,
    @Column(nullable = false)
    val sequence: Int,
    @Column(nullable = false, length = 200)
    val title: String,
    @Column(length = 100)
    val subtitle: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "topic_type", nullable = false, length = 20)
    val topicType: TopicType,
    @Column(name = "gnb_title", nullable = false, length = 100)
    val gnbTitle: String,
)
