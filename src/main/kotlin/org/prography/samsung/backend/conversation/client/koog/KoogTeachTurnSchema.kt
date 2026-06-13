package org.prography.samsung.backend.conversation.client.koog

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KoogTeachTurnSchema(
    @property:LLMDescription(
        "학생(AI)이 선생님에게 말하는 **정확히 1문장 (선호) 또는 최대 2문장**. " +
            "초등 4학년 존댓말('요/죠/네요')만. **140자 이하, 매우 짧고 간결하게**. " +
            "장황/반복/긴 설명 절대 금지. '아하!', '음...', '그럼...' + 핵심만. " +
            "session_done=true여도 1~2문장 감사 인사로 끝. 좋은 예: '아하! 그럼 분모는 아래 숫자인 거네요?'",
    )
    val speak: String,

    @property:LLMDescription(
        "emotion은 정확히 다음 중 하나 (소문자): curious | confused | thoughtful | aha | happy. " +
            "새로 이해하면 aha, 질문/궁금하면 curious, 어려우면 confused, 생각 중 thoughtful, " +
            "모두 이해하고 끝나면 happy.",
    )
    val emotion: String,

    @property:LLMDescription(
        "이번 턴 선생님 발화로 **새로** 이해한 concept id만 배열. 이미 covered된 것은 절대 포함 금지. " +
            "unit_json concepts의 id(c1,c2...)만 사용. 이번 턴에 2개 동시 이해한 경우에만 2개 이상.",
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
