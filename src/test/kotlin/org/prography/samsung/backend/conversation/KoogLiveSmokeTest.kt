package org.prography.samsung.backend.conversation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.prography.samsung.backend.conversation.client.LlmClient
import org.prography.samsung.backend.conversation.service.AiResponseValidator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * Live AI (DeepSeek via Koog) smoke tests.
 * These make real external LLM calls and are DISABLED for PR/CI.
 * Manually enable (remove @Disabled or run with specific profile) + set DEEPSEEK_API_KEY
 * when you want to verify schema compliance and response quality with actual model.
 */
@Disabled("Live AI calls disabled for PR preparation. Re-enable for manual verification only.")
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Koog + DeepSeek live smoke test")
class KoogLiveSmokeTest {
    @Autowired
    private lateinit var llmClient: LlmClient

    @Autowired
    private lateinit var aiResponseValidator: AiResponseValidator

    @Test
    @DisplayName("DeepSeek structured output이 teach 스키마를 만족한다")
    fun shouldReturnValidTeachTurnFromDeepSeek() {
        val unitJson =
            """
            {"unit_id":"frac_concept_01","topic":"분수의 개념","student_persona":"초등 4학년","concepts":[
            {"id":"c1","label":"분수는 전체를 똑같이 나눈 것 중 일부"},
            {"id":"c2","label":"분모는 아래 숫자"},
            {"id":"c3","label":"분자는 위 숫자"},
            {"id":"c4","label":"분수는 크기를 비교할 수 있다"}],"max_concepts":4}
            """.trimIndent()
        val conceptOrder = aiResponseValidator.parseConceptIdOrder(unitJson)

        val systemPrompt =
            """
            당신은 초등학생 AI 학생입니다. 선생님 설명을 듣고 JSON만 출력하세요.
            ## 단원 정보
            $unitJson
            ## JSON 스키마
            {"speak":"string","emotion":"curious","covered":["c1"],"missing":["c2","c3","c4"],"misconceptions_detected":[],"correction_stage":0,"focus_concept":"c2","session_done":false}
            """.trimIndent()

        val userPrompt =
            """
            ## 누적 이해한 개념
            없음

            ## 이번 턴 — 선생님 발화
            분수는 전체를 똑같이 나눈 거 중 일부를 나타내는 수예요.

            ## 지시
            - 선생님 설명에서 이해한 개념만 covered에 넣으세요.
            - missing은 아직 모르는 concept id 전체입니다.
            - focus_concept는 missing의 첫 번째 id로 설정하세요.
            - JSON 한 개만 출력하세요.
            """.trimIndent()

        val raw = llmClient.complete(systemPrompt, userPrompt)
        val parsed = aiResponseValidator.parseAndValidate(raw, conceptOrder)

        assertThat(parsed.speak).isNotBlank()
        assertThat(parsed.covered).contains("c1")
        assertThat(parsed.missing).contains("c2", "c3", "c4")
        assertThat(parsed.focusConcept).isEqualTo("c2")
        assertThat(parsed.sessionDone).isFalse()
    }

    @Test
    @DisplayName("4턴 teach 플로우가 개념을 순차적으로 커버한다")
    fun shouldCoverAllConceptsOverFourTurns() {
        val unitJson =
            """
            {"unit_id":"frac_concept_01","topic":"분수의 개념","student_persona":"초등 4학년","concepts":[
            {"id":"c1","label":"분수는 전체를 똑같이 나눈 것 중 일부"},
            {"id":"c2","label":"분모는 아래 숫자"},
            {"id":"c3","label":"분자는 위 숫자"},
            {"id":"c4","label":"분수는 크기를 비교할 수 있다"}],"max_concepts":4}
            """.trimIndent()
        val conceptOrder = aiResponseValidator.parseConceptIdOrder(unitJson)
        val systemPrompt =
            """
            당신은 초등학생 AI 학생입니다. 선생님 설명을 듣고 JSON만 출력하세요.
            ## 단원 정보
            $unitJson
            ## 응답 규칙
            1. speak: 1~2문장, 초등학생 말투, 존댓말.
            2. emotion: curious|confused|thoughtful|aha|happy
            3. covered: 이번 턴에 새로 이해한 concept id만.
            4. missing: 아직 모르는 concept id 전체.
            5. focus_concept: missing의 첫 번째 id.
            ## JSON 스키마
            {"speak":"string","emotion":"curious","covered":["c1"],"missing":["c2"],"misconceptions_detected":[],"correction_stage":0,"focus_concept":"c2","session_done":false}
            """.trimIndent()

        val teacherTurns =
            listOf(
                "분수는 전체를 똑같이 나눈 거 중 일부를 나타내는 수예요",
                "분모는 아래 숫자로, 전체를 똑같이 나눈 개수예요",
                "분자는 위 숫자로, 가지고 있는 조각 수예요",
                "분수는 크기를 비교할 수 있어요. 같은 분모면 분자가 큰 게 더 커요",
            )

        var accumulated = emptyList<String>()
        teacherTurns.forEachIndexed { index, userText ->
            val userP = buildString {
                appendLine("## 누적 이해한 개념")
                appendLine(if (accumulated.isEmpty()) "없음" else accumulated.joinToString(", "))
                appendLine()
                appendLine("## 이번 턴 — 선생님 발화")
                appendLine(userText)
                appendLine()
                appendLine("## 지시")
                appendLine("- 선생님 설명에서 이해한 개념만 covered에 넣으세요.")
                appendLine("- JSON 한 개만 출력하세요.")
            }
            val raw = llmClient.complete(systemPrompt, userP)
            val parsed = aiResponseValidator.parseAndValidate(raw, conceptOrder)
            accumulated = aiResponseValidator.mergeCovered(accumulated, parsed.covered)

            println("=== Turn ${index + 1} ===")
            println("teacher: $userText")
            println("student: ${parsed.speak} [${parsed.emotion.value}] covered=$accumulated")

            assertThat(parsed.speak).isNotBlank()
            assertThat(parsed.speak.length).isLessThan(200)
        }

        assertThat(accumulated).containsExactly("c1", "c2", "c3", "c4")
    }

    @Test
    @DisplayName("다양한 상황(정상/오개념/모호/중복/완료)에서 스키마 + 품질 준수 검증")
    fun diverseSituationsSchemaCompliance() {
        val unitJson =
            """
            {"unit_id":"frac_concept_01","topic":"분수의 개념","student_persona":"초등 4학년","concepts":[
            {"id":"c1","label":"분수는 전체를 똑같이 나눈 것 중 일부"},
            {"id":"c2","label":"분모는 아래 숫자"},
            {"id":"c3","label":"분자는 위 숫자"},
            {"id":"c4","label":"분수는 크기를 비교할 수 있다"}],"max_concepts":4}
            """.trimIndent()
        val conceptOrder = aiResponseValidator.parseConceptIdOrder(unitJson)
        val systemPrompt =
            """
            당신은 초등학생 AI 학생입니다. 선생님 설명을 듣고 JSON만 출력하세요.
            ## 단원 정보
            $unitJson
            ## 응답 규칙 (중요)
            - speak: 정확히 1~2문장, 초등학생 존댓말("요/죠/네요"). 180자 이내.
            - emotion: curious|confused|thoughtful|aha|happy 중 정확히 하나.
            - covered: 이번 턴에 새로 이해한 concept id만 (이미 누적된 것은 절대 반복 금지).
            - missing: 아직 모르는 전체 (순서 유지).
            - focus_concept: missing의 첫 번째.
            - session_done: missing이 비면 true.
            ## JSON 스키마
            {"speak":"string","emotion":"curious","covered":["c1"],"missing":["c2","c3","c4"],"misconceptions_detected":[],"correction_stage":0,"focus_concept":"c2","session_done":false}
            """.trimIndent()

        // 다양한 상황: (누적 covered, 이번 선생님 설명)
        val scenarios = listOf(
            emptyList<String>() to "분수는 전체를 똑같이 나눈 거 중 일부를 나타내는 수예요", // 정상 c1
            listOf("c1") to "분모는 전체를 똑같이 나눈 개수, 아래 숫자예요", // 정상 c2
            listOf("c1", "c2") to "그럼 분모끼리 더하면 되는 거예요?", // 오개념 (m1 유도)
            listOf("c1", "c2") to "음... 잘 모르겠어요. 다시 설명해 주세요", // 모호/부족
            listOf("c1", "c2") to "분자는 위에 있는 숫자고, 조각 수를 나타내요", // 정상 c3
            listOf("c1", "c2", "c3") to "분수는 크기를 비교할 수 있어요. 분모 같으면 분자 큰 게 커요", // 정상 c4 + 완료 유도
            listOf("c1") to "분수는 뭐예요?", // 매우 짧고 모호
            listOf("c1", "c2") to "분모는 아래고 분자는 위고 크기 비교도 가능하대요 선생님", // 한 번에 여러 개념
        )

        scenarios.forEachIndexed { index, (accumulated, userText) ->
            val userP = buildString {
                appendLine("## 누적 이해한 개념")
                appendLine(if (accumulated.isEmpty()) "없음" else accumulated.joinToString(", "))
                appendLine()
                appendLine("## 이번 턴 — 선생님 발화")
                appendLine(userText)
                appendLine()
                appendLine("## 지시")
                appendLine("- 이번 설명으로 새로 이해한 것만 covered에 넣으세요.")
                appendLine("- JSON 한 개만 출력하세요. 다른 텍스트 금지.")
            }
            val raw = llmClient.complete(systemPrompt, userP)
            val parsed = aiResponseValidator.parseAndValidate(raw, conceptOrder)

            println("=== Diverse Scenario ${index + 1} ===")
            println("accumulated before: $accumulated")
            println("teacher: $userText")
            println("student speak: ${parsed.speak}")
            println("emotion: ${parsed.emotion.value} | covered: ${parsed.covered} | missing: ${parsed.missing}")
            println(
                "focus: ${parsed.focusConcept} | done: ${parsed.sessionDone} | correction: ${parsed.correctionStage}",
            )
            println("raw length: ${raw.length}")

            assertThat(parsed.speak).isNotBlank()
            assertThat(parsed.speak.length).isLessThan(220)
            assertThat(parsed.emotion.value).isIn("curious", "confused", "thoughtful", "aha", "happy")
            // covered는 누적에 없던 새 것만이어야 함 (대략)
            parsed.covered.forEach { id ->
                assertThat(accumulated).doesNotContain(id)
            }
        }
    }

    // Helper prompts kept minimal inside tests to satisfy ktlint function-signature in this live-only test file.

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun overrideForLiveKoog(registry: DynamicPropertyRegistry) {
            registry.add("conversation.llm.provider") { "deepseek" }
            registry.add("ai.koog.deepseek.enabled") { "true" }
            registry.add("ai.koog.deepseek.api-key") { System.getenv("DEEPSEEK_API_KEY") }
            registry.add("conversation.llm.timeout-ms") { "30000" }
        }
    }
}
