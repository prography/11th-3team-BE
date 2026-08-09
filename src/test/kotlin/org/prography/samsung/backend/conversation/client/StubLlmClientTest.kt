package org.prography.samsung.backend.conversation.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.prography.samsung.backend.conversation.service.AiResponseValidator
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("StubLlmClient — curriculum-agnostic stub responses")
class StubLlmClientTest {
    private val validator = AiResponseValidator(jacksonObjectMapper())
    private val sut = StubLlmClient(validator)

    private val systemPrompt =
        """
        ## 수업 개념
        - [c1] 문화유산의 의미
          • 조상들이 물려준 소중한 것
        - [c2] 문화유산 조사 방법
          • 직접 찾아가는 답사

        ## 응답 규칙
        1. JSON only
        """.trimIndent()

    private fun userPrompt(userText: String, accumulated: List<String> = emptyList()): String = buildString {
        appendLine("## 제공된 개념 ID 목록")
        appendLine("c1, c2")
        appendLine()
        appendLine("## 누적 이해한 개념 (이전 턴까지)")
        appendLine(if (accumulated.isEmpty()) "없음" else accumulated.joinToString(", "))
        appendLine()
        appendLine("## 이번 턴 — 선생님 발화")
        appendLine(userText)
    }

    @Test
    fun affirm_produces_empty_delta_covered() {
        val raw = sut.complete(systemPrompt, userPrompt("그렇지"))
        val node = ObjectMapper().readTree(raw)
        assertEquals("[]", node.path("covered").toString())
        assertTrue(node.path("speak").asText().startsWith("선생님,"))
        assertTrue(node.path("thinking").asText().isNotBlank())
    }

    @Test
    fun explain_with_social_key_point_advances_exactly_one() {
        val raw = sut.complete(systemPrompt, userPrompt("조상들이 물려준 소중한 것"))
        val node = ObjectMapper().readTree(raw)
        assertEquals("""["c1"]""", node.path("covered").toString())
        assertEquals("c2", node.path("focus_concept").asText())
    }
}
