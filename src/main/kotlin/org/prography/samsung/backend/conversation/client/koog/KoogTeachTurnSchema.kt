package org.prography.samsung.backend.conversation.client.koog

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KoogTeachTurnSchema(
    @property:LLMDescription(
        "학생(AI)이 선생님에게 말하는 **1문장(선호) 또는 최대 2문장(반응 + 다음 hint 유도 질문)**. " +
            "초등 4학년 존댓말('요/죠/네요')만. **160자 이하**. 장황/반복 금지. " +
            "speak으로 선생님 설명에 대한 짧은 반응 + **아직 이해 못한 특정 hint 내용(분모/분자 정의, 크기 비교 등)을 선생님이 풀어서 설명하게 만드는 개방형 질문**을 덧붙이세요. " +
            "**절대 '맞죠?', '인가요?', '맞나요?' 같은 확인형 질문 금지** — 그런 건 선생님이 또 단답('네')만 하게 만들어 대화가 멈춘다. 항상 '어떻게 설명하시나요?', '정확히 어떤 의미인가요?' 스타일. 순수 단답형('아하!','네')로 끝내지 말고 항상 다음 힌트 질문 유도. session_done 시 감사 인사. 예: '분모는 어떻게 세나요? 자세히 알려주세요.'",
    )
    val speak: String,

    @property:LLMDescription(
        "emotion은 정확히 다음 중 하나 (소문자): curious | confused | thoughtful | aha | happy. " +
            "새로 이해하면 aha, 질문/궁금하면 curious, 어려우면 confused, 생각 중 thoughtful, " +
            "모두 이해하고 끝나면 happy.",
    )
    val emotion: String,

    @property:LLMDescription(
        "이번 턴 선생님(teacher) 발화로 **새로 그리고 명확히** 이해한 concept id만 배열. " +
            "hint 키워드('전체를 똑같이', '아래 숫자', '위 숫자', '크기 비교' 등)가 선생님 userText 안에 실제로 있어야 함. " +
            "이미 covered된 것은 절대 포함 금지. 당신(student)이 speak에서 말한 건 counted 아님. " +
            "unit_json concepts의 id(c1,c2...)만 사용. 이번 턴에 2개 동시 이해한 경우에만 2개 이상. " +
            "단답 확인('맞아','그렇지')으로는 절대 추가하지 말 것. teacher가 정의를 풀어서 말했을 때만.",
    )
    val covered: List<String> = emptyList(),

    @property:LLMDescription(
        "아직 이해 못한 concept id 전체. unit_json concepts 순서 유지. covered로 이동한 것은 제거. " +
            "모두 이해하면 빈 배열.",
    )
    val missing: List<String> = emptyList(),

    @SerialName("misconceptions_detected")
    @property:LLMDescription(
        "이번 턴 감지된 오개념 id 배열 (없으면 []). 오개념 감지 시 correction_stage 상승 참고.",
    )
    val misconceptionsDetected: List<String> = emptyList(),

    @SerialName("correction_stage")
    @property:LLMDescription(
        "0~4. 0=정상, 1~3=같은 개념 반복 설명 중, 4=3턴 이상 막혀서 포기 (speak은 '일단 넘어갈게요' 스타일). " +
            "안전 규칙에서 repeated focus 시 강제 4.",
    )
    val correctionStage: Int = 0,

    @SerialName("focus_concept")
    @property:LLMDescription(
        "현재 집중 concept id. **반드시 유효한 문자열**이어야 함. 절대 null, 빈문자, 'null' 금지. " +
            "missing이 있으면 missing의 첫 번째 id. " +
            "session_done=true (모든 개념 이해)인 경우에도 반드시 문자열을 넣어야 함 — " +
            "마지막으로 이해한 concept id나 unit의 첫 번째 concept id(c1)를 사용. " +
            "예: 'c4' 또는 'c1'.",
    )
    val focusConcept: String? = null,

    @SerialName("session_done")
    @property:LLMDescription(
        "모든 개념 이해 시 true (missing 빈 배열일 때). true면 speak은 감사 마무리 인사.",
    )
    val sessionDone: Boolean = false,
)
