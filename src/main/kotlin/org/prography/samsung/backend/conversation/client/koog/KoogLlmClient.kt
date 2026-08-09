package org.prography.samsung.backend.conversation.client.koog

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.executor.model.executeStructured
import ai.koog.prompt.executor.ollama.client.OllamaModels
import ai.koog.prompt.message.MessagePart
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.prography.samsung.backend.conversation.client.LlmClient
import org.prography.samsung.backend.conversation.client.LlmTimeoutException
import org.prography.samsung.backend.conversation.config.ConversationLlmProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component

@Component
@ConditionalOnExpression("'${'$'}{conversation.llm.provider:deepseek}' != 'stub'")
class KoogLlmClient(
    private val properties: ConversationLlmProperties,
    @Autowired(required = false)
    @Qualifier("deepSeekExecutor")
    private val deepSeekExecutor: PromptExecutor?,
    @Autowired(required = false)
    @Qualifier("ollamaExecutor")
    private val ollamaExecutor: PromptExecutor?,
) : LlmClient {
    private val log = LoggerFactory.getLogger(KoogLlmClient::class.java)

    @OptIn(ExperimentalSerializationApi::class)
    private val json =
        Json {
            encodeDefaults = true
            explicitNulls = false
        }

    override fun complete(systemPrompt: String, userPrompt: String): String {
        val executor = resolveExecutor()
        val model = resolveModel()

        return try {
            runBlocking {
                withTimeout(properties.timeoutMs) {
                    val result = executor.executeStructured<KoogTeachTurnSchema>(
                        prompt = prompt("teach-turn") {
                            system(systemPrompt)
                            user(userPrompt)
                        },
                        model = model,
                    )

                    if (result.isFailure) {
                        // structured 실패 시 원인 노출 (95% → 100% 개선에 중요)
                        val ex = result.exceptionOrNull()
                        val msg = "Koog executeStructured failed for model=$model: ${ex?.message ?: ex}"
                        throw IllegalStateException(msg)
                    }

                    val structured = result.getOrThrow()

                    // 모델의 실제 추론 과정(reasoning_content)을 응답 메시지에서 추출해 로그로만 남긴다.
                    // schema/JSON에는 넣지 않으므로 reasoning이 없어도 절대 실패하지 않는다.
                    val reasoning =
                        structured.message.parts
                            .filterIsInstance<MessagePart.Reasoning>()
                            .flatMap { it.content }
                            .joinToString("\n")
                            .trim()
                    if (reasoning.isNotBlank()) {
                        log.info("Teach LLM reasoning: {}", reasoning)
                    }

                    json.encodeToString(structured.data)
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            throw LlmTimeoutException()
        } catch (e: Exception) {
            // 상위에서 retry decision을 할 수 있도록 구체 메시지 전달
            if (e is LlmTimeoutException) throw e
            throw IllegalStateException("LLM structured call failed: ${e.message}", e)
        }
    }

    private fun resolveExecutor(): PromptExecutor = when (properties.provider.lowercase()) {
        "ollama" ->
            ollamaExecutor
                ?: error("Ollama executor is not available. Set ai.koog.ollama.enabled=true.")
        else ->
            deepSeekExecutor
                ?: error(
                    "DeepSeek executor is not available. Set DEEPSEEK_API_KEY or conversation.llm.provider=ollama.",
                )
    }

    private fun resolveModel() = when (properties.provider.lowercase()) {
        "ollama" -> OllamaModels.Meta.LLAMA_3_2
        else -> resolveDeepSeekModel(properties.model)
    }

    private fun resolveDeepSeekModel(modelName: String) = when (modelName.lowercase()) {
        "deepseek-v4-pro", "deepseek_v4_pro" -> DeepSeekModels.DeepSeekV4Pro
        else -> DeepSeekModels.DeepSeekV4Flash
    }
}
