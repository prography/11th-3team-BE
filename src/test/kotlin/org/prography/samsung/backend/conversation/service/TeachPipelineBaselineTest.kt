package org.prography.samsung.backend.conversation.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * L1 특성 테스트 (characterization test) — 현재 파이프라인의 실제 동작을 고정한다.
 *
 * 목적: docs/ai-reference/TEACH_PIPELINE_ANALYSIS.md §7 의 baseline 기준점.
 * 여기 assert된 값들은 "옳은 동작"이 아니라 "현재 동작"이다. [BASELINE-BUG] 표시된 것은
 * 개선(P1~P5) 적용 시 expected 로 반전시킬 대상이며, 테스트 diff 가 곧 before/after 증거가 된다.
 *
 * 데이터 출처: conversation_turns.json (2026-06-17 ~ 06-30) 실세션.
 * 단원 fixture: src/test/resources/teach-eval/fraction-unit.json (V7 시드와 동일한 분수 단원).
 */
@DisplayName("Teach 파이프라인 baseline 특성 테스트 (현재 동작 고정)")
class TeachPipelineBaselineTest {
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val validator = AiResponseValidator(objectMapper)
    private val guard = TeachProgressGuard(validator)

    private val fractionJson =
        this::class.java.classLoader.getResource("teach-eval/fraction-unit.json")!!.readText()
    private val conceptOrder = validator.parseConceptIdOrder(fractionJson)

    private fun delta(userText: String, covered: List<String>): List<String> =
        validator.expectedDeltaCovered(userText, covered, conceptOrder, fractionJson)

    @Nested
    @DisplayName("정상 동작 — 회귀 게이트 (개선 후에도 유지되어야 함)")
    inner class ReferencePass {
        @Test
        @DisplayName("c1 정의를 설명하면 c1 advance (f8b38857-t1)")
        fun c1_explain_advances() {
            assertEquals(listOf("c1"), delta("전체를 똑같이 나눈 것 중에 일 부분을 나타내는 수", emptyList()))
        }

        @Test
        @DisplayName("순수 affirm('그렇지')은 covered=[] (guard가 delta 비움)")
        fun affirm_never_advances() {
            assertTrue(validator.isPureAffirmation("그렇지", fractionJson))
            assertEquals(emptyList(), delta("그렇지", listOf("c1")))
        }
    }

    @Nested
    @DisplayName("P1 — substring 매칭 (BASELINE-BUG: 의미가 아니라 우연한 문자열에 의존)")
    inner class P1SubstringMatching {
        @Test
        @DisplayName("[BASELINE-BUG] 의미상 동일한 c2 페러프레이즈가 거절된다 ('나눈 것의 개수' != keyword '나눈 개수')")
        fun paraphrase_of_c2_rejected() {
            // 개선(P1) 시 이 기대값을 listOf("c2")로 반전 — 단 서버 substring으로는 영구 불가, L3에서만 통과
            assertEquals(emptyList(), delta("전체를 똑같이 나눈 것의 개수를 말해요", listOf("c1")))
        }

        @Test
        @DisplayName("[BASELINE-BUG] 비변별 keyword '아래'로 오개념 발화가 c2 advance된다 ('부모는 아래에 있는 숫자')")
        fun nondiscriminative_keyword_false_advance() {
            // 개선(P1 keywords 정비) 시 이 기대값을 emptyList()로 반전
            assertEquals(listOf("c2"), delta("부모는 아래에 있는 숫자예요", listOf("c1")))
        }

        @Test
        @DisplayName("[BASELINE-BUG] STT 오인식 발화가 서술부 keyword만으로 순차 advance (7d18f09b)")
        fun stt_misrecognition_advances_via_keywords() {
            // '굿모'(분모), '군자'(분자), '눈썹끼리'(분수끼리) 주어가 전부 오인식됐지만 keyword 매칭으로 통과
            assertEquals(listOf("c2"), delta("굿모는 전체를 똑같이 나눈 개수 아래 숫자를 말해요", listOf("c1")))
            assertEquals(listOf("c3"), delta("군자는 가지고 있는 조각의 수 위 숫자를 말해요", listOf("c1", "c2")))
            assertEquals(
                listOf("c4"),
                delta(
                    "눈썹끼리 크기 비교는 분모를 맞추고 그리고 분자의 값 중 크고 자금을 비교하면 하면 됩니다",
                    listOf("c1", "c2", "c3"),
                ),
            )
        }
    }

    @Nested
    @DisplayName("P4 — 발화 분류 (BASELINE: GARBAGE와 AFFIRM 구분 없음)")
    inner class P4Classification {
        @Test
        @DisplayName("오프토픽('소금 빵 레시피')은 GARBAGE로 분류되지만 covered=[] (087b867f-t2)")
        fun offtopic_classified_garbage() {
            assertEquals(
                AiResponseValidator.TeacherTurnKind.GARBAGE,
                validator.classifyTeacherTurn("소금 빵 레시피 알려줘", emptyList(), conceptOrder, fractionJson),
            )
            assertEquals(emptyList(), delta("소금 빵 레시피 알려줘", emptyList()))
        }
    }

    @Nested
    @DisplayName("P2 — 오개념 (BASELINE-BUG: misconceptions 정의는 있으나 프롬프트 미주입)")
    inner class P2Misconceptions {
        @Test
        @DisplayName("[BASELINE-BUG] misconceptions 필드가 formatLessonConcepts 출력에 포함되지 않는다")
        fun misconceptions_not_in_formatted_concepts() {
            val formatted = validator.formatLessonConcepts(fractionJson)
            // fixture에 정의된 오개념 pattern/correction 이 프롬프트용 포맷 문자열에 전혀 없음
            assertTrue(formatted.contains("분모")) // concepts 는 포함됨을 확인 (포맷 자체는 동작)
            assertTrue(
                !formatted.contains("분모끼리 더한다") && !formatted.contains("더하지 않아요"),
                "BASELINE: misconceptions pattern/correction 이 프롬프트에 주입되지 않아야 함(현재 동작). " +
                    "개선(P2) 시 이 assert를 반전",
            )
        }
    }

    @Nested
    @DisplayName("P3 — correction_stage (BASELINE-BUG: 0→4 점프, 중간 단계 없음)")
    inner class P3CorrectionStage {
        private fun rawStuck(stage: Int) = org.prography.samsung.backend.conversation.dto.AiTurnResponse(
            speak = "선생님, 분모는 정확히 어떻게 설명하시나요?",
            emotion = org.prography.samsung.backend.common.domain.AiEmotion.CONFUSED,
            covered = emptyList(),
            missing = listOf("c2", "c3", "c4"),
            misconceptionsDetected = emptyList(),
            correctionStage = stage,
            focusConcept = "c2",
            sessionDone = false,
        )

        @Test
        @DisplayName("[BASELINE-BUG] repeatedFocusCount>=3 이면 중간 단계 없이 stage=4로 점프")
        fun jumps_straight_to_stage_four() {
            val normalized = guard.normalize(
                userText = "잘 모르겠어요 혹시 부모에 대해서 알려 줄 수 있나요",
                accumulatedCovered = listOf("c1"),
                conceptOrder = conceptOrder,
                repeatedFocusCount = 3,
                raw = rawStuck(0),
                unitJson = fractionJson,
            )
            // 1~3 단계를 거치지 않고 바로 4. 그리고 stage 값에 따른 speak/힌트 변화도 없음(§3.4)
            assertEquals(4, normalized.correctionStage)
            assertEquals(emptyList(), normalized.covered)
        }

        @Test
        @DisplayName("BASELINE: 막힌 유저에게 힌트가 제공되지 않고 동일 유도 질문만 반환 (c1ab2035-t4)")
        fun no_hint_for_stuck_user() {
            val normalized = guard.normalize(
                userText = "잘 모르겠어요 혹시 부모에 대해서 알려 줄 수 있나요",
                accumulatedCovered = listOf("c1"),
                conceptOrder = conceptOrder,
                repeatedFocusCount = 3,
                raw = rawStuck(4),
                unitJson = fractionJson,
            )
            // 도움 요청이지만 covered 진행 없음, speak 는 LLM 원문 유지(힌트 주입 로직 없음)
            assertEquals(emptyList(), normalized.covered)
            assertEquals("c2", normalized.focusConcept)
        }
    }

    @Nested
    @DisplayName("P5 — fallback speak (BASELINE-BUG: 위반 speak를 필터 없이 통과)")
    inner class P5FallbackSpeak {
        @Test
        @DisplayName("[BASELINE-BUG] affirm fallback은 '?' 존재만 검사 — 확인형 '맞죠?'가 통과된다 (e0a86f4f-t2)")
        fun closed_question_passes_affirm_fallback() {
            val raw = org.prography.samsung.backend.conversation.dto.AiTurnResponse(
                speak = "네, 선생님! 그럼 분모가 아래 숫자 맞죠?",
                emotion = org.prography.samsung.backend.common.domain.AiEmotion.CURIOUS,
                covered = emptyList(),
                missing = listOf("c2", "c3", "c4"),
                misconceptionsDetected = emptyList(),
                correctionStage = 0,
                focusConcept = "c2",
                sessionDone = false,
            )
            val normalized = guard.normalize(
                userText = "응",
                accumulatedCovered = listOf("c1"),
                conceptOrder = conceptOrder,
                repeatedFocusCount = 0,
                raw = raw,
                unitJson = fractionJson,
            )
            // '?'를 포함하므로 guard의 affirm fallback(정답 유출+확인형)을 그대로 통과
            assertEquals("네, 선생님! 그럼 분모가 아래 숫자 맞죠?", normalized.speak)
        }
    }
}
