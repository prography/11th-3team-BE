# Plan Docs

기능 명세·아키텍처 문서를 바탕으로 한 **구현·모델링 계획**을 관리합니다. Reference는 기준(What), Plan은 실행 설계(How)에 가깝습니다.

## 문서 목록

| 문서 | 경로 | 설명 |
| --- | --- | --- |
| API·DB 모델링 계획 | [api-and-database-modeling-plan.md](api-and-database-modeling-plan.md) | §2 화면별 API · §3 계약 · **§4 DB** · §13 Figma · **§14 Open Questions** |
| **방향 A** — 텍스트 전용 BE + 클라 음성 | [teach-text-only-implementation.md](teach-text-only-implementation.md) | STT/TTS는 FE, BE는 `userText` → LLM JSON만 |
| **방향 B** — Gemini Voice API | [gemini-voice-api-implementation.md](gemini-voice-api-implementation.md) | Live API(B-1) 또는 Text LLM + Gemini TTS(B-2) |

## AI 대화 루프 — 방향 비교

| | 방향 A | 방향 B (B-2 권장) | 방향 B (B-1 Live) |
| --- | --- | --- | --- |
| BE 입력/출력 | 텍스트 / JSON | 텍스트 / JSON (+ TTS 선택) | 토큰·상태 sync / JSON 저장 |
| 음성 STT | FE 임의 | FE 임의 | Gemini Live |
| 음성 TTS | FE 임의 | Gemini TTS | Gemini Live |
| 설계서 JSON 호환 | 높음 | 높음 | Function calling 필요 |
| BE 복잡도 | 낮음 | 낮음~중 | 중~높음 |

## 읽는 방법

- **FE:** [§2 화면별 호출](api-and-database-modeling-plan.md#2-화면별-api-호출-명세-구현용) + [§3 API](api-and-database-modeling-plan.md#3-api-카탈로그)
- **BE:** [§4 DB](api-and-database-modeling-plan.md#4-데이터베이스) + §3 + Flyway `src/main/resources/db/migration/`
- **Figma 반영 후:** [§13 체크리스트](api-and-database-modeling-plan.md#13-figma-디자인-변경-시-수정-가이드)
- **학습 플로우 기획 확인:** [§14 Open Questions](api-and-database-modeling-plan.md#14-open-questions-메인-학습-플로우) (6건)
- **AI teach·음성 구현:** [teach-text-only-implementation.md](teach-text-only-implementation.md) 또는 [gemini-voice-api-implementation.md](gemini-voice-api-implementation.md)

## 관련 문서

- [Reference Docs](../reference/README.md) — 제품·아키텍처 기준 문서
- [AI 대화 루프 설계서](../reference/architecture/ai-conversation-loop-system-design.pdf)
- [Backend Docs](../README.md) — 문서 인덱스

## 갱신 규칙

- Reference 문서가 바뀌면 Plan 문서의 **영향 범위**를 함께 갱신합니다.
- 파일명은 kebab-case 영문을 사용합니다.