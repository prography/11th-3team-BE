package org.prography.samsung.backend.conversation.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.prography.samsung.backend.common.domain.AiEmotion

@DisplayName("AiResponseValidator 단위 테스트")
class AiResponseValidatorTest {
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val sut = AiResponseValidator(objectMapper)

    private val unitJson =
        """
        {"concepts":[{"id":"c1"},{"id":"c2"}],"max_concepts":2}
        """.trimIndent()

    @Test
    @DisplayName("유효한 JSON을 파싱한다")
    fun shouldParseValidJson() {
        val raw =
            """
            {"speak":"아하!","emotion":"aha","covered":["c1"],"missing":["c2"],
            "misconceptions_detected":[],"correction_stage":0,"focus_concept":"c2","session_done":false}
            """.trimIndent()

        val result = sut.parseAndValidate(raw, sut.parseConceptIdOrder(unitJson))

        assertEquals("아하!", result.speak)
        assertEquals(AiEmotion.AHA, result.emotion)
        assertEquals(listOf("c1"), result.covered)
    }

    @Test
    @DisplayName("마크다운 펜스 안 JSON을 추출한다")
    fun shouldExtractJsonFromMarkdownFence() {
        val raw =
            """
            ```json
            {"speak":"테스트","emotion":"curious","covered":[],"missing":["c1","c2"],
            "misconceptions_detected":[],"correction_stage":0,"focus_concept":"c1","session_done":false}
            ```
            """.trimIndent()

        val result = sut.parseAndValidate(raw, sut.parseConceptIdOrder(unitJson))

        assertEquals("테스트", result.speak)
    }

    @Test
    @DisplayName("금지 어휘를 필터링한다")
    fun shouldFilterForbiddenWords() {
        val raw =
            """
            {"speak":"씨발 이게 뭐예요","emotion":"confused","covered":[],"missing":["c1","c2"],
            "misconceptions_detected":[],"correction_stage":0,"focus_concept":"c1","session_done":false}
            """.trimIndent()

        val result = sut.parseAndValidate(raw, sut.parseConceptIdOrder(unitJson))

        assertEquals("*** 이게 뭐예요", result.speak)
    }

    @Test
    @DisplayName("잘못된 concept id면 예외를 던진다")
    fun shouldRejectInvalidConceptId() {
        val raw =
            """
            {"speak":"테스트","emotion":"curious","covered":["c9"],"missing":["c2"],
            "misconceptions_detected":[],"correction_stage":0,"focus_concept":"c2","session_done":false}
            """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            sut.parseAndValidate(raw, sut.parseConceptIdOrder(unitJson))
        }
    }
}
