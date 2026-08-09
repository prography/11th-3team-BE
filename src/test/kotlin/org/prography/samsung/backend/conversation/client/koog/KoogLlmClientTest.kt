package org.prography.samsung.backend.conversation.client.koog

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.prography.samsung.backend.conversation.config.ConversationLlmProperties
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("KoogLlmClient.injectThinking 단위 테스트")
class KoogLlmClientTest {
    private val sut = KoogLlmClient(ConversationLlmProperties(), null, null)

    @Test
    @DisplayName("reasoning이 있으면 구조화 JSON에 thinking 필드로 주입한다")
    fun injectThinking_addsThinkingFieldWhenReasoningPresent() {
        val out = sut.injectThinking("""{"speak":"hi","emotion":"curious"}""", "reasoning text")
        val node = ObjectMapper().readTree(out)
        assertEquals("reasoning text", node.path("thinking").asText())
        assertEquals("hi", node.path("speak").asText())
        assertTrue(out.contains("\"thinking\""))
    }

    @Test
    @DisplayName("reasoning이 비어있으면 입력 JSON을 그대로 반환한다")
    fun injectThinking_returnsInputWhenReasoningBlank() {
        val input = """{"speak":"hi"}"""
        assertEquals(input, sut.injectThinking(input, "   "))
    }
}
