package org.prography.samsung.backend.conversation.client

interface LlmClient {
    fun complete(systemPrompt: String, userPrompt: String): String
}
