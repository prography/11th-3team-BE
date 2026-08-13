package org.prography.samsung.backend.curriculum.service

import org.prography.samsung.backend.common.domain.SessionPhase
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.curriculum.dto.response.CurriculumChipResponse
import org.prography.samsung.backend.curriculum.dto.response.LessonContent
import org.prography.samsung.backend.curriculum.dto.response.LessonQuestionResponse
import org.prography.samsung.backend.curriculum.dto.response.TodayTopicResponse
import org.prography.samsung.backend.curriculum.entity.LessonTopic
import org.prography.samsung.backend.curriculum.repository.CurriculumRepository
import org.prography.samsung.backend.curriculum.repository.HintNoteRepository
import org.prography.samsung.backend.curriculum.repository.LessonQuestionRepository
import org.prography.samsung.backend.curriculum.repository.LessonTopicRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CurriculumService(
    private val curriculumRepository: CurriculumRepository,
    private val lessonTopicRepository: LessonTopicRepository,
    private val lessonQuestionRepository: LessonQuestionRepository,
    private val hintNoteRepository: HintNoteRepository,
    private val hintNoteMapper: HintNoteMapper,
) {
    @Transactional(readOnly = true)
    fun getActiveCurriculums(): List<CurriculumChipResponse> =
        curriculumRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc().map {
            CurriculumChipResponse(
                id = it.id,
                code = it.code,
                name = it.name,
                displayOrder = it.displayOrder,
            )
        }

    @Transactional(readOnly = true)
    fun getActiveCurriculumOrThrow(curriculumId: Long) = curriculumRepository.findByIdAndIsActiveTrue(curriculumId)
        ?: throw CustomException(DomainErrorCode.CURRICULUM_NOT_FOUND)

    @Transactional(readOnly = true)
    fun getLessonTopicById(lessonTopicId: Long): LessonTopic =
        lessonTopicRepository.findById(lessonTopicId).orElseThrow {
            CustomException(DomainErrorCode.LESSON_TOPIC_NOT_FOUND)
        }

    @Transactional(readOnly = true)
    fun getFirstLessonTopic(curriculumId: Long): LessonTopic =
        lessonTopicRepository.findAllByCurriculumIdOrderBySequenceAsc(curriculumId).firstOrNull()
            ?: throw CustomException(DomainErrorCode.LESSON_TOPIC_NOT_FOUND)

    @Transactional(readOnly = true)
    fun getTodayTopics(curriculumId: Long): List<TodayTopicResponse> =
        lessonTopicRepository.findAllByCurriculumIdOrderBySequenceAsc(curriculumId).map {
            TodayTopicResponse(
                sequence = it.sequence,
                lessonTopicId = it.id,
                title = it.title,
                subtitle = it.subtitle,
                topicType = it.topicType,
            )
        }

    @Transactional(readOnly = true)
    fun getLessonContent(lessonTopicId: Long, phase: SessionPhase): LessonContent {
        val question = lessonQuestionRepository.findByLessonTopicIdAndPhase(lessonTopicId, phase)
            ?: throw CustomException(DomainErrorCode.LESSON_TOPIC_NOT_FOUND)
        val hintNote = hintNoteRepository.findByLessonTopicIdAndPhase(lessonTopicId, phase)
            ?: throw CustomException(DomainErrorCode.HINT_NOTE_NOT_FOUND)
        return LessonContent(
            question = LessonQuestionResponse(
                bubbleText = question.bubbleText,
                speak = question.bubbleText,
                emotion = question.emotion,
                displayAnswerHtml = question.wrongAnswerHtml,
            ),
            hintNote = hintNoteMapper.toResponse(hintNote.contentJson),
        )
    }
}
