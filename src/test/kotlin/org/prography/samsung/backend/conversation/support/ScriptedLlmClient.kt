package org.prography.samsung.backend.conversation.support

import org.prography.samsung.backend.conversation.client.LlmClient

/**
 * 테스트 전용 LlmClient — LLM raw 응답을 시나리오별로 스크립트 주입한다.
 *
 * StubLlmClient(substring 판정 복제품)와 달리, "LLM이 특정 응답을 냈을 때 서버(guard/retry/fallback)가
 * 어떻게 처리하는가"를 결정적으로 검증하기 위한 것. 주입된 순서대로 응답을 반환하고,
 * generateTurn 이 retry 하며 넘긴 systemPrompt/userPrompt 를 캡처해 프롬프트 주입 여부를 검증한다.
 *
 * 설계: docs/ai-reference/TEACH_PIPELINE_ANALYSIS.md §7.4
 */
class ScriptedLlmClient(responses: List<String> = emptyList()) : LlmClient {
    private val queue = ArrayDeque(responses)
    val capturedSystemPrompts = mutableListOf<String>()
    val capturedUserPrompts = mutableListOf<String>()

    /** 다음 complete() 호출들이 반환할 raw 응답을 순서대로 설정한다. */
    fun script(vararg responses: String) {
        queue.clear()
        queue.addAll(responses)
    }

    override fun complete(systemPrompt: String, userPrompt: String): String {
        capturedSystemPrompts += systemPrompt
        capturedUserPrompts += userPrompt
        check(queue.isNotEmpty()) {
            "ScriptedLlmClient: complete() called ${capturedUserPrompts.size} times but script ran out of responses. " +
                "Provide enough responses for every retry attempt."
        }
        return queue.removeFirst()
    }
}
