package org.prography.samsung.backend.conversation.client.koog

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.executor.model.executeStructured
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.prography.samsung.backend.conversation.client.LlmRequest
import org.prography.samsung.backend.conversation.client.LlmResult
import org.prography.samsung.backend.conversation.client.exception.LlmClientException
import org.prography.samsung.backend.conversation.client.exception.LlmTimeoutException
import org.prography.samsung.backend.conversation.config.ConversationLlmProperties
import org.springframework.stereotype.Component
import kotlin.time.Duration.Companion.milliseconds

@Component
class KoogStructuredExecutor(private val properties: ConversationLlmProperties) {
    @OptIn(ExperimentalSerializationApi::class)
    private val json =
        Json {
            encodeDefaults = true
            explicitNulls = false
        }

    fun complete(provider: String, model: LLModel, executor: PromptExecutor, request: LlmRequest): LlmResult = try {
        runBlocking {
            withTimeout(properties.timeoutMs.milliseconds) {
                val result = executor.executeStructured<KoogTeachTurnSchema>(
                    prompt = prompt("teach-turn") {
                        system(request.systemPrompt)
                        user(request.userPrompt)
                    },
                    model = model,
                )

                if (result.isFailure) {
                    val ex = result.exceptionOrNull()
                    val msg = "Koog executeStructured failed for provider=$provider model=$model: ${ex?.message ?: ex}"
                    throw LlmClientException(msg, ex)
                }

                LlmResult(
                    content = json.encodeToString(result.getOrThrow().data),
                    provider = provider,
                    model = model.id,
                )
            }
        }
    } catch (e: TimeoutCancellationException) {
        throw LlmTimeoutException()
    } catch (e: Exception) {
        if (e is LlmTimeoutException) throw e
        throw LlmClientException("LLM structured call failed: ${e.message}", e)
    }
}
