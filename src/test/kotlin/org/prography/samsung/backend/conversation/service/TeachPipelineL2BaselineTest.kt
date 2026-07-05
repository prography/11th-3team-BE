package org.prography.samsung.backend.conversation.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.prography.samsung.backend.conversation.entity.CurriculumUnit
import org.prography.samsung.backend.conversation.support.ScriptedLlmClient
import org.prography.samsung.backend.curriculum.entity.Curriculum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles

/**
 * L2 특성 테스트 — Spring 컨텍스트에서 LlmConversationService.generateTurn() 의 실제 경로를 고정한다.
 *
 * ScriptedLlmClient 를 @Primary 로 주입해 LLM raw 응답을 직접 스크립트하고, retry 소진 후
 * fallback 이 규칙 위반 speak 를 어떻게 처리하는지(§3.6 구조적 구멍) 재현한다.
 * StubLlmClient(substring 복제품)로는 재현 불가능한 서버 로직 경로가 대상.
 *
 * 설계: docs/ai-reference/TEACH_PIPELINE_ANALYSIS.md §7.4
 * baseline 기준점: 여기 assert 된 값은 "현재 동작"이며 개선(P5) 시 반전 대상.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Teach 파이프라인 L2 baseline — generateTurn fallback 경로 (현재 동작 고정)")
class TeachPipelineL2BaselineTest {
    @TestConfiguration
    class ScriptedClientConfig {
        @Bean
        @Primary
        fun scriptedLlmClient(): ScriptedLlmClient = ScriptedLlmClient()
    }

    @Autowired
    private lateinit var service: LlmConversationService

    @Autowired
    private lateinit var scriptedLlmClient: ScriptedLlmClient

    private val fractionJson =
        this::class.java.classLoader.getResource("teach-eval/fraction-unit.json")!!.readText()

    // generateTurn 은 unitJson + systemPromptTemplate 만 사용. persist 하지 않으므로 최소 엔티티로 충분.
    private val unit = CurriculumUnit(
        unitId = "frac_concept_01",
        curriculum = Curriculum(
            code = "TEST_FRAC",
            name = "분수",
            chapterLabel = "1단원",
            sessionTitleTemplate = "분수 수업",
            displayOrder = 1,
        ),
        unitJson = fractionJson,
        systemPromptTemplate = "당신은 초등학생 AI 학생입니다.\n## 단원 정보\n{{lesson_concepts}}\n## 응답 규칙\nJSON만 출력하세요.",
    )

    @BeforeEach
    fun resetCaptures() {
        scriptedLlmClient.capturedSystemPrompts.clear()
        scriptedLlmClient.capturedUserPrompts.clear()
    }

    @Test
    @DisplayName("[BASELINE-BUG] affirm에 정답 유출+확인형 speak를 3회 반복 주입하면 fallback이 그대로 반환 (e0a86f4f-t2)")
    fun answer_leak_speak_returned_verbatim_after_retries_exhausted() {
        // user='응'(affirm)인데 LLM이 covered=[c2] + 정답 유출 speak 를 냄 → semantic 위반 → retry.
        // maxStructuredRetries(=3)회 모두 위반 응답 → 마지막 attempt fallback 이 speak 를 어떻게 다루는지 고정.
        val violating =
            """{"speak":"네, 선생님! 그럼 분모가 아래 숫자 맞죠?","emotion":"curious","covered":["c2"],""" +
                """"missing":["c2","c3","c4"],"misconceptions_detected":[],"correction_stage":0,""" +
                """"focus_concept":"c2","session_done":false}"""
        scriptedLlmClient.script(violating, violating, violating)

        val result = service.generateTurn(
            unit = unit,
            previousTurns = emptyList(),
            userText = "응",
            accumulatedCovered = listOf("c1"),
            repeatedFocusCount = 0,
        )

        // guard가 affirm이므로 covered는 비워짐(정상). 그러나 speak는 필터 없이 원문 유지 — 정답 유출이 유저에게 도달.
        assertThat(result.covered).isEmpty()
        assertThat(result.speak).isEqualTo("네, 선생님! 그럼 분모가 아래 숫자 맞죠?")
    }

    @Test
    @DisplayName("[BASELINE-BUG] 시스템 프롬프트에 misconceptions pattern이 주입되지 않는다 (P2)")
    fun misconceptions_not_injected_into_system_prompt() {
        val valid =
            """{"speak":"선생님, 분모는 정확히 어떻게 설명하시나요?","emotion":"curious","covered":[],""" +
                """"missing":["c2","c3","c4"],"misconceptions_detected":[],"correction_stage":0,""" +
                """"focus_concept":"c2","session_done":false}"""
        scriptedLlmClient.script(valid)

        service.generateTurn(
            unit = unit,
            previousTurns = emptyList(),
            userText = "그렇지",
            accumulatedCovered = listOf("c1"),
            repeatedFocusCount = 0,
        )

        val systemPrompt = scriptedLlmClient.capturedSystemPrompts.first()
        // fixture에 정의된 오개념(m1)의 pattern/correction 이 프롬프트에 없음 → LLM은 오개념 목록을 못 봄
        assertThat(systemPrompt).doesNotContain("분모끼리 더한다")
        assertThat(systemPrompt).doesNotContain("더하지 않아요")
    }

    @Test
    @DisplayName("[BASELINE-BUG] user 프롬프트에 repeatedFocusCount(막힌 턴 수)가 주입되지 않는다 (P3)")
    fun repeated_focus_count_not_injected_into_user_prompt() {
        val valid =
            """{"speak":"선생님, 분모는 정확히 어떻게 설명하시나요?","emotion":"confused","covered":[],""" +
                """"missing":["c2","c3","c4"],"misconceptions_detected":[],"correction_stage":0,""" +
                """"focus_concept":"c2","session_done":false}"""
        scriptedLlmClient.script(valid)

        service.generateTurn(
            unit = unit,
            previousTurns = emptyList(),
            userText = "잘 모르겠어요 혹시 부모에 대해서 알려 줄 수 있나요",
            accumulatedCovered = listOf("c1"),
            repeatedFocusCount = 3, // 막혀있음에도
        )

        val userPrompt = scriptedLlmClient.capturedUserPrompts.first()
        // repeatedFocusCount 를 프롬프트에 전달하는 코드가 없어 LLM은 유저가 3턴째 막혀있는지 모름
        assertThat(userPrompt).doesNotContain("repeatedFocusCount")
        assertThat(userPrompt).doesNotContain("3턴")
    }
}
