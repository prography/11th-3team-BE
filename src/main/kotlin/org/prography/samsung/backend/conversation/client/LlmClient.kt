package org.prography.samsung.backend.conversation.client

interface LlmClient {
    fun complete(request: LlmRequest): LlmResult
}

data class LlmRequest(val systemPrompt: String, val userPrompt: String)

data class LlmResult(val content: String, val provider: String, val model: String)
