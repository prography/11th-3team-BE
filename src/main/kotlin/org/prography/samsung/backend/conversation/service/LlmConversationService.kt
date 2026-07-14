package org.prography.samsung.backend.conversation.service

import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.conversation.client.LlmClient
import org.prography.samsung.backend.conversation.client.LlmTimeoutException
import org.prography.samsung.backend.conversation.config.ConversationLlmProperties
import org.prography.samsung.backend.conversation.dto.AiTurnResponse
import org.prography.samsung.backend.conversation.entity.ConversationTurn
import org.prography.samsung.backend.conversation.entity.CurriculumUnit
import org.prography.samsung.backend.conversation.prompt.TeachPromptBuilder
import org.prography.samsung.backend.conversation.util.AiResponseValidator
import org.prography.samsung.backend.conversation.util.TeachProgressGuard
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LlmConversationService(
    private val llmClient: LlmClient,
    private val properties: ConversationLlmProperties,
    private val aiResponseValidator: AiResponseValidator,
    private val progressGuard: TeachProgressGuard,
    private val promptBuilder: TeachPromptBuilder,
) {
    private val log = LoggerFactory.getLogger(LlmConversationService::class.java)
    fun generateTurn(
        unit: CurriculumUnit,
        previousTurns: List<ConversationTurn>,
        userText: String,
        accumulatedCovered: List<String>,
        repeatedFocusCount: Int,
    ): AiTurnResponse {
        val conceptOrder = aiResponseValidator.parseConceptIdOrder(unit.unitJson)
        val systemPrompt = promptBuilder.buildSystemPrompt(unit)

        // structured + semantic 검증 실패 시 correction 피드백으로 재호출 (config에서 제어)
        val maxAttempts = (properties.maxStructuredRetries).coerceAtLeast(1)
        var lastError: String? = null

        repeat(maxAttempts) { attempt ->
            val userPrompt = promptBuilder.buildUserPrompt(
                previousTurns = previousTurns,
                userText = userText,
                accumulatedCovered = accumulatedCovered,
                conceptOrder = conceptOrder,
                previousError = lastError,
                attempt = attempt + 1,
            )

            val raw = try {
                llmClient.complete(systemPrompt, userPrompt)
            } catch (e: LlmTimeoutException) {
                log.warn("Teach LLM timeout on attempt ${attempt + 1}")
                throw CustomException(DomainErrorCode.LLM_TIMEOUT, cause = e)
            } catch (e: Exception) {
                lastError = "LLM 호출 실패: ${e.message}"
                log.warn("Teach LLM call exception on attempt ${attempt + 1}: ${e.message}")
                if (attempt == maxAttempts - 1) {
                    throw CustomException(DomainErrorCode.LLM_CALL_FAILED, cause = e)
                }
                return@repeat // 다음 attempt
            }

            val parsed = try {
                aiResponseValidator.parseAndValidate(raw, conceptOrder)
            } catch (e: Exception) {
                lastError = "파싱/검증 실패: ${e.message}. 이전 출력에서 JSON만 정확히 추출하고 모든 규칙을 지켜서 다시 출력하세요."
                log.warn("Teach LLM attempt ${attempt + 1} parse failed: ${e.message}. userText='${userText.take(60)}'")
                if (attempt == maxAttempts - 1) {
                    throw CustomException(DomainErrorCode.LLM_RESPONSE_INVALID, cause = e)
                }
                return@repeat
            }

            // semantic 추가 검증 (retry 대상)
            val semanticError = aiResponseValidator.validateSemanticRules(
                parsed,
                accumulatedCovered,
                conceptOrder,
                userText,
                unit.unitJson,
            )
            if (semanticError != null) {
                lastError = "의미 규칙 위반: $semanticError. speak는 1~2문장 140자 이하로 매우 짧게. covered 이번 턴 새로 이해한 것만."
                log.warn(
                    "Teach LLM attempt ${attempt + 1} semantic violation: $semanticError | " +
                        "speak='${parsed.speak.take(80)}'",
                )
                if (attempt == maxAttempts - 1) {
                    // Last resort: rely on prompt/semantic/guard. No speak force override here (prompt aims for LLM success).
                    return finalizeSafeResponse(
                        response = parsed,
                        unit = unit,
                        conceptOrder = conceptOrder,
                        accumulatedCovered = accumulatedCovered,
                        repeatedFocusCount = repeatedFocusCount,
                        currentUserText = userText,
                    )
                }
                return@repeat
            }

            if (attempt > 0) {
                log.info("Teach LLM recovered after ${attempt + 1} attempts")
            }
            // No unconditional speak force here — if semantic passed, use LLM's speak (prompt improvement goal).
            // Fallback only in the last-attempt semantic-fail branch for compatibility.
            return finalizeSafeResponse(
                response = parsed,
                unit = unit,
                conceptOrder = conceptOrder,
                accumulatedCovered = accumulatedCovered,
                repeatedFocusCount = repeatedFocusCount,
                currentUserText = userText,
            )
        }

        // 모든 시도 실패 시 예외 전파 (단일 모델이므로 graceful degradation 제거)
        throw CustomException(DomainErrorCode.LLM_RESPONSE_INVALID)
    }

    private fun finalizeSafeResponse(
        response: AiTurnResponse,
        unit: CurriculumUnit,
        conceptOrder: List<String>,
        accumulatedCovered: List<String>,
        repeatedFocusCount: Int,
        currentUserText: String,
    ): AiTurnResponse {
        val safe = applySafetyRules(
            response = response,
            unit = unit,
            conceptOrder = conceptOrder,
            accumulatedCovered = accumulatedCovered,
            repeatedFocusCount = repeatedFocusCount,
            currentUserText = currentUserText,
        )
        val finalFocus = if (safe.focusConcept.isBlank()) {
            aiResponseValidator.resolveFocusConcept(conceptOrder, safe.missing, explicit = null)
        } else {
            safe.focusConcept
        }
        return safe.copy(focusConcept = finalFocus)
    }

    private fun applySafetyRules(
        response: AiTurnResponse,
        unit: CurriculumUnit,
        conceptOrder: List<String>,
        accumulatedCovered: List<String>,
        repeatedFocusCount: Int,
        currentUserText: String = "",
    ): AiTurnResponse = progressGuard.normalize(
        userText = currentUserText,
        accumulatedCovered = accumulatedCovered,
        conceptOrder = conceptOrder,
        repeatedFocusCount = repeatedFocusCount,
        raw = response,
        unitJson = unit.unitJson,
    )
}
