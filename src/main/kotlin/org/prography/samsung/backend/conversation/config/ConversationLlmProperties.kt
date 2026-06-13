package org.prography.samsung.backend.conversation.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "conversation.llm")
data class ConversationLlmProperties(
    val provider: String = "deepseek",
    val model: String = "deepseek-v4-flash",
    val timeoutMs: Long = 8000,
    val maxTurnsPerSession: Int = 10,
    val maxLlmCallsPerSession: Int = 15,
    val contextTurns: Int = 5,
    /** structured + semantic 검증 실패 시 최대 재시도 횟수 (1 = 재시도 없음) */
    val maxStructuredRetries: Int = 3,
)
