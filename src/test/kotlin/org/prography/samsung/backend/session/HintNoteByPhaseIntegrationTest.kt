package org.prography.samsung.backend.session

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.prography.samsung.backend.support.IntegrationTestSupport
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@DisplayName("힌트 노트 phase별 제공 통합 테스트")
class HintNoteByPhaseIntegrationTest : IntegrationTestSupport() {
    @Test
    @DisplayName("분수 개념 토픽은 INTRO와 REACTION에서 서로 다른 힌트 노트를 반환한다")
    fun shouldReturnDifferentHintNotesPerPhaseForConceptTopic() {
        val deviceId = newDeviceId()
        completeOnboarding(deviceId)
        val sessionId = startSession(deviceId, topicIdAt(deviceId, CONCEPT_TOPIC_INDEX))

        expectApiSuccess(get("/session/$sessionId/lesson", deviceId))
            .andExpect(jsonPath("$.data.hintNote.header.title").value("분수의 개념"))
            .andExpect(jsonPath("$.data.hintNote.sections[0].title").value("Q1. 분수란?"))
            .andExpect(jsonPath("$.data.hintNote.sections[0].highlight").value(false))

        expectApiSuccess(post("/session/$sessionId/advance-phase", deviceId))

        expectApiSuccess(get("/session/$sessionId/reaction", deviceId))
            .andExpect(jsonPath("$.data.hintNote.header.title").value("분수의 개념"))
            .andExpect(jsonPath("$.data.hintNote.sections[0].title").value("오개념 정정"))
            .andExpect(jsonPath("$.data.hintNote.sections[0].highlight").value(true))
    }

    @Test
    @DisplayName("분수 덧셈 토픽은 INTRO와 REACTION에서 서로 다른 힌트 노트를 반환한다")
    fun shouldReturnDifferentHintNotesPerPhaseForCalculationTopic() {
        val deviceId = newDeviceId()
        completeOnboarding(deviceId)
        val sessionId = startSession(deviceId, topicIdAt(deviceId, CALCULATION_TOPIC_INDEX))

        expectApiSuccess(get("/session/$sessionId/lesson", deviceId))
            .andExpect(jsonPath("$.data.hintNote.header.title").value("분수의 덧셈과 뺄셈"))
            .andExpect(jsonPath("$.data.hintNote.sections[0].title").value("핵심 규칙"))
            .andExpect(jsonPath("$.data.hintNote.sections[0].highlight").value(false))

        expectApiSuccess(post("/session/$sessionId/advance-phase", deviceId))

        expectApiSuccess(get("/session/$sessionId/reaction", deviceId))
            .andExpect(jsonPath("$.data.hintNote.header.title").value("분수의 덧셈과 뺄셈"))
            .andExpect(jsonPath("$.data.hintNote.sections.length()").value(2))
            .andExpect(jsonPath("$.data.hintNote.sections[0].highlight").value(true))
    }

    @Test
    @DisplayName("두 토픽은 같은 phase에서 서로 다른 힌트 노트를 반환한다")
    fun shouldReturnTopicSpecificHintNoteForSamePhase() {
        val deviceId = newDeviceId()
        completeOnboarding(deviceId)

        val conceptHint = hintNoteTitleOf(deviceId, CONCEPT_TOPIC_INDEX)
        val calculationHint = hintNoteTitleOf(deviceId, CALCULATION_TOPIC_INDEX)

        assert(conceptHint == "분수의 개념") { "개념 토픽 힌트가 예상과 다르다: $conceptHint" }
        assert(calculationHint == "분수의 덧셈과 뺄셈") { "계산 토픽 힌트가 예상과 다르다: $calculationHint" }
    }

    private fun hintNoteTitleOf(deviceId: String, topicIndex: Int): String {
        val sessionId = startSession(deviceId, topicIdAt(deviceId, topicIndex))
        val body =
            expectApiSuccess(get("/session/$sessionId/lesson", deviceId))
                .andReturn()
                .response
                .contentAsString
        abortSession(deviceId, sessionId)
        return objectMapper.readTree(body)
            .path("data")
            .path("hintNote")
            .path("header")
            .path("title")
            .asText()
    }

    private fun abortSession(deviceId: String, sessionId: String) {
        expectApiSuccess(post("/session/$sessionId/abort", deviceId))
    }

    private fun topicIdAt(deviceId: String, index: Int): Long {
        val body =
            expectApiSuccess(get("/session/today", deviceId))
                .andReturn()
                .response
                .contentAsString
        return objectMapper.readTree(body)
            .path("data")
            .path("topics")[index]
            .path("lessonTopicId")
            .asLong()
    }

    companion object {
        private const val CONCEPT_TOPIC_INDEX = 0
        private const val CALCULATION_TOPIC_INDEX = 1
    }
}
