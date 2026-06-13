package org.prography.samsung.backend.conversation.client

import org.prography.samsung.backend.conversation.service.AiResponseValidator
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["conversation.llm.provider"], havingValue = "stub")
class StubLlmClient(private val aiResponseValidator: AiResponseValidator) : LlmClient {
    override fun complete(systemPrompt: String, userPrompt: String): String {
        val conceptOrder = aiResponseValidator.parseConceptIdOrder(extractUnitJson(systemPrompt))
        val covered =
            when {
                matchesConceptKeywords(userPrompt, listOf("크기", "비교")) -> conceptOrder
                matchesConceptKeywords(userPrompt, listOf("분자")) -> conceptOrder.take(3)
                matchesConceptKeywords(userPrompt, listOf("분모")) -> conceptOrder.take(2)
                matchesConceptKeywords(userPrompt, listOf("똑같이", "일부")) -> conceptOrder.take(1)
                else -> emptyList()
            }
        val missing = aiResponseValidator.resolveMissing(conceptOrder, covered)
        val sessionDone = missing.isEmpty()
        val speak =
            when {
                sessionDone -> "아하! 이제 분수가 뭔지 완전히 이해했어요. 고마워요 선생님!"
                covered.size >= 2 -> "그럼 분자는 위에 있는 숫자인 거죠?"
                covered.isNotEmpty() -> "아하! 그럼 분모는 나눈 개수구나?"
                else -> "음... 분수가 뭐예요? 좀 더 쉽게 설명해 주세요."
            }
        val emotion =
            when {
                sessionDone -> "happy"
                covered.isNotEmpty() -> "aha"
                else -> "curious"
            }
        val focusConcept = aiResponseValidator.resolveFocusConcept(conceptOrder, missing, explicit = null)

        return buildString {
            appendLine("{")
            appendLine("  \"speak\": \"$speak\",")
            appendLine("  \"emotion\": \"$emotion\",")
            appendLine("  \"covered\": ${toJsonArray(covered)},")
            appendLine("  \"missing\": ${toJsonArray(missing)},")
            appendLine("  \"misconceptions_detected\": [],")
            appendLine("  \"correction_stage\": 0,")
            appendLine("  \"focus_concept\": \"$focusConcept\",")
            appendLine("  \"session_done\": $sessionDone")
            appendLine("}")
        }
    }

    private fun extractUnitJson(systemPrompt: String): String = systemPrompt.lineSequence()
        .first { it.contains("\"unit_id\"") && it.contains("\"concepts\"") }
        .trim()

    private fun matchesConceptKeywords(text: String, keywords: List<String>): Boolean =
        keywords.any { text.contains(it) }

    private fun toJsonArray(values: List<String>): String =
        values.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
}
