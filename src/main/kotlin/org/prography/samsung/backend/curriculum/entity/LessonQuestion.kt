package org.prography.samsung.backend.curriculum.entity

import jakarta.persistence.Column
import jakarta.persistence.Convert
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
import org.prography.samsung.backend.common.domain.AiEmotion
import org.prography.samsung.backend.common.domain.AiEmotionConverter
import org.prography.samsung.backend.common.domain.SessionPhase
import org.prography.samsung.backend.common.entity.BaseEntity

@Entity
@Table(name = "lesson_questions")
class LessonQuestion(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_topic_id", nullable = false)
    val lessonTopic: LessonTopic,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val phase: SessionPhase,

    @Column(name = "bubble_text", nullable = false, columnDefinition = "TEXT")
    val bubbleText: String,

    @Column(name = "wrong_answer_html", columnDefinition = "TEXT")
    val wrongAnswerHtml: String? = null,

    @Convert(converter = AiEmotionConverter::class)
    @Column(nullable = false, length = 20)
    val emotion: AiEmotion = AiEmotion.CURIOUS,
) : BaseEntity()
