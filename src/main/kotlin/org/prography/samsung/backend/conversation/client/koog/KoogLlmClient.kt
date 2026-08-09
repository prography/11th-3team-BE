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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.prography.samsung.backend.conversation.client.LlmClient
import org.prography.samsung.backend.conversation.client.LlmTimeoutException
import org.prography.samsung.backend.conversation.config.ConversationLlmProperties
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
                    val dataJson = json.encodeToString(structured.data)

                    // 모델의 실제 reasoning(reasoning_content)을 추출해 JSON에 thinking으로 주입 —
                    // 스키마 요청이 아니라 응답 메시지에 담긴 진짜 추론 과정을 보존한다.
                    val reasoning =
                        structured.message.parts
                            .filterIsInstance<MessagePart.Reasoning>()
                            .flatMap { it.content }
                            .joinToString("\n")
                            .trim()

                    injectThinking(dataJson, reasoning)
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

    /**
     * 모델의 reasoning(reasoning_content)이 비어있지 않으면 구조화 응답 JSON에 "thinking" 필드로 주입한다.
     * reasoning이 없으면 입력 JSON을 그대로 반환한다. (stub 등 reasoning 미지원 모델 대응)
     */
    fun injectThinking(dataJson: String, reasoning: String): String {
        if (reasoning.isBlank()) return dataJson
        val withThinking = json.parseToJsonElement(dataJson).jsonObject.toMutableMap()
        withThinking["thinking"] = JsonPrimitive(reasoning)
        return JsonObject(withThinking).toString()
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
