package org.prography.samsung.backend.conversation.client.koog

import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.model.PromptExecutor
import org.prography.samsung.backend.conversation.client.LlmClient
import org.prography.samsung.backend.conversation.client.LlmRequest
import org.prography.samsung.backend.conversation.client.LlmResult
import org.prography.samsung.backend.conversation.client.exception.LlmClientException
import org.prography.samsung.backend.conversation.config.ConversationLlmProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    name = ["conversation.llm.provider"],
    havingValue = "deepseek",
    matchIfMissing = true,
)
class KoogDeepSeekLlmClient(
    private val properties: ConversationLlmProperties,
    private val koogStructuredExecutor: KoogStructuredExecutor,
    @param:Autowired(required = false)
    @param:Qualifier("deepSeekExecutor")
    private val deepSeekExecutor: PromptExecutor?,
) : LlmClient {
    override fun complete(request: LlmRequest): LlmResult {
        val executor = deepSeekExecutor
            ?: throw LlmClientException(
                "DeepSeek executor is not available. Set DEEPSEEK_API_KEY or conversation.llm.provider=ollama.",
            )

        return koogStructuredExecutor.complete(
            provider = PROVIDER,
            model = KoogDeepSeekModelResolver.resolve(properties.model),
            executor = executor,
            request = request,
        )
    }

    private companion object {
        const val PROVIDER = "deepseek"
    }
}

private object KoogDeepSeekModelResolver {
    fun resolve(modelName: String) = when (modelName.lowercase()) {
        "deepseek-v4-pro", "deepseek_v4_pro" -> DeepSeekModels.DeepSeekV4Pro
        else -> DeepSeekModels.DeepSeekV4Flash
    }
}
