package org.prography.samsung.backend.conversation.client.koog

import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaModels
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
    havingValue = "ollama",
)
class KoogOllamaLlmClient(
    private val properties: ConversationLlmProperties,
    private val koogStructuredExecutor: KoogStructuredExecutor,
    @param:Autowired(required = false)
    @param:Qualifier("ollamaExecutor")
    private val ollamaExecutor: PromptExecutor?,
) : LlmClient {
    override fun complete(request: LlmRequest): LlmResult {
        val executor = ollamaExecutor
            ?: throw LlmClientException("Ollama executor is not available. Set ai.koog.ollama.enabled=true.")

        return koogStructuredExecutor.complete(
            provider = PROVIDER,
            model = KoogOllamaModelResolver.resolve(properties.model),
            executor = executor,
            request = request,
        )
    }

    private companion object {
        const val PROVIDER = "ollama"
    }
}

private object KoogOllamaModelResolver {
    private val modelsByAlias = mapOf(
        "llama-3.2" to OllamaModels.Meta.LLAMA_3_2,
        "llama_3_2" to OllamaModels.Meta.LLAMA_3_2,
        "llama3.2" to OllamaModels.Meta.LLAMA_3_2,
        "meta-llama-3.2" to OllamaModels.Meta.LLAMA_3_2,
    )

    fun resolve(modelName: String) = modelsByAlias[modelName.lowercase()] ?: OllamaModels.Meta.LLAMA_3_2
}
