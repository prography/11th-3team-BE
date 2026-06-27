# Teach 커리큘럼 맥락 스키마 비의존화 계획

| 항목 | 내용 |
| --- | --- |
| 버전 | v0.1 · 다음 스프린트 검토용 |
| 상태 | **계획만 확정 — 코드 미적용** |
| 선행 작업 | `feat/fix-teach-affirm-covered` (프롬프트 일반화, `formatLessonConcepts`, V9/V10 마이그레이션) |
| 연관 문서 | [teach-text-only-implementation.md](teach-text-only-implementation.md), [ai-conversation-loop-system-design.pdf](../reference/architecture/ai-conversation-loop-system-design.pdf) |
| 대상 | 백엔드 · 커리큘럼 생성 파이프라인(향후) |

---

## 1. 배경과 문제

### 1.1 현재 상태 (2026-06 기준)

`curriculum_units.unit_json`에 단원 데이터를 저장하고, teach 런타임에서 다음을 **JSON 트리 구조에 의존**해 수행한다.

- `parseConceptIdOrder` — `concepts[].id`
- `formatLessonConcepts` — `concepts[].name|label`, `key_points|keywords`, `description`
- `termsForConcept` / `expectedDeltaCovered` — affirm·explain 판별용 문자열 매칭
- `TeachProgressGuard` — 위 판별 결과로 delta `covered` 보정

분수 시드(`label` + `keywords`)와 사회 JSON(`name` + `key_points`)은 **알려진 변형만** 흡수 가능하다.

### 1.2 요구사항 (제품 관점)

> **단원 맥락은 생성할 때마다 형태를 알 수 없다.**  
> 매번 다른 JSON 스키마·필드명·중첩 구조가 나올 수 있으므로, teach 백엔드가 **고정 JSON 포맷을 전제하면 안 된다.**

`formatLessonConcepts`는 “raw JSON을 프롬프트에 붙이지 않기” 위한 중간 단계였으나, 동시에 **암묵적 스키마 계약**을 만들었다. 이 계약은 생성 파이프라인과 충돌한다.

### 1.3 유지해야 하는 것

| 유지 | 이유 |
| --- | --- |
| teach **응답 JSON 스키마** (`speak`, `covered`, `missing`, …) | API·FE 계약, 변경 없음 |
| **제네릭 teach 지시문** (affirm / explain / speak 규칙) | 커리큘럼 무관 재사용 |
| 세션 **진행 상태** (`covered_concepts`, turn log) | 학습 이력 |
| LLM이 **선생님 역할 유도** (짧은 학생, `선생님,` + 개방형 질문) | 제품 목표 |

### 1.4 버려야/약화해야 하는 것

| 제거·약화 대상 | 이유 |
| --- | --- |
| 런타임 `unit_json` 트리 파싱 (`formatLessonConcepts`, `termsForConcept`, …) | 스키마 미지 |
| 서버 측 key_point **부분 문자열 매칭**으로 explain 판별 | 필드명·문구가 생성마다 다름 |
| 프롬프트에 “이 JSON 필드만 써라” 뉘앙스 | 생성 맥락 불명 |

---

## 2. 목표 (성공 기준)

| # | 기준 | 검증 |
| --- | --- | --- |
| 1 | `unit_json` **원본 형태에 제약 없이** DB 저장 가능 | 임의 JSON/텍스트 fixture ingest 테스트 |
| 2 | teach **시스템·유저 프롬프트 본문**은 커리큘럼별 필드명을 언급하지 않음 | 프롬프트 스냅샷 테스트 |
| 3 | 런타임이 `concepts[].key_points` 등 **고정 경로를 파싱하지 않음** | grep/아키텍처 리뷰 + 단위 테스트 |
| 4 | LLM 응답은 기존 `AiTurnResponse` 스키마로 검증 | `parseAndValidate` + semantic 규칙 |
| 5 | affirm 시 `covered=[]`, explain 시 **정확히 1개 advance** (가능한 범위) | 시나리오 테스트 + harness |
| 6 | 기존 Flyway **이미 적용 마이그레이션 수정 없음** | V7 체크섬 불변 |

---

## 3. 제안 아키텍처: 3-레이어 분리

생성 맥락(자유)과 teach 런타임(안정)을 **저장 시점에 분리**한다.

```
┌──────────────────────────────────────────────────────────────┐
│  생성 파이프라인 (형태 자유 — BE teach가 가정하지 않음)         │
│  · LLM/에디터가 만든 JSON·텍스트·메타 혼합 산출물             │
└──────────────────────────┬───────────────────────────────────┘
                           │ ingest (저장 시 1회)
┌──────────────────────────▼───────────────────────────────────┐
│  DB curriculum_units                                          │
│  · unit_json        TEXT  — raw 보관 (감사·재생성·버전용)      │
│  · lesson_context   TEXT  — 프롬프트에 넣을 **자유 텍스트**    │
│  · concept_order    TEXT  — JSON 배열 ["c1","c2",...]        │
│  · system_prompt_template — 변경 없음 ({{lesson_context}})   │
└──────────────────────────┬───────────────────────────────────┘
                           │ teach 런타임
┌──────────────────────────▼───────────────────────────────────┐
│  LlmConversationService                                       │
│  · system: template + lesson_context (opaque)                 │
│  · user: concept_order + generic rules + turn state           │
│  · 검증: concept_order ⊆ id만, JSON 스키마, semantic 규칙      │
│  · **unit_json 트리 파싱 없음**                                │
└──────────────────────────────────────────────────────────────┘
```

### 3.1 필드 책임

| 필드 | 누가 채우나 | teach가 쓰는 방식 |
| --- | --- | --- |
| `unit_json` | 생성 파이프라인 (임의) | **런타임 미사용** (보관만). 필요 시 admin/export |
| `lesson_context` | 생성 시 **프롬프트용 요약 텍스트**로 함께 산출 | system prompt `{{lesson_context}}` 치환 |
| `concept_order` | 생성 시 id 순서만 명시 (배열) | user prompt ID 목록 + validator 허용 id 집합 |
| `system_prompt_template` | 운영 시드 / 마이그레이션 | 제네릭 teach 템플릿 (커리큘럼 무관) |

### 3.2 `lesson_context` 예시 (형식 자유, 텍스트만)

```text
이번 단원에서 학생이 이해해야 할 것:
1. (c1) 문화유산 — 조상이 남긴 소중한 것, 유형/무형 구분
2. (c2) 조사 방법 — 답사, 면담, 자료 검색
...
선생님이 설명할 때 자주 쓰일 표현: "조상들이 물려준", "직접 찾아가는 답사"
```

생성마다 문장·목록·표 형식이 달라도 된다. **서버는 파싱하지 않고** LLM에 전달만 한다.

### 3.3 진행 판별 (affirm / explain) 전략 변경

**현재:** 서버가 `key_points` 부분 문자열 매칭 → explain  
**제안:** 2단계 혼합

1. **1차: LLM 분류** — 프롬프트 규칙 + `lesson_context` 맥락으로 `covered` 출력
2. **2차: 서버 semantic 검증** (스키마 유지)
   - `covered` ⊆ `concept_order`, 중복·stale id 금지
   - affirm + `covered` 비어 있지 않음 → retry
   - `covered` 개수 > 1 (단일 advance 원칙) → retry
   - **문자열 매칭 제거** (`termsForConcept`, `userTextExplainsConcept` deprecate)

`TeachProgressGuard`는 delta `covered` 정규화와 focus/missing 재계산만 담당하고, **JSON에서 용어 추출하지 않음**.

---

## 4. 비목표 (이번 계획에서 하지 않음)

- `unit_json` 스키마 표준화·JSON Schema 강제
- 생성 파이프라인 자체 구현 (별도 프로젝트/스프린트)
- teach 응답 API 스키마 변경
- 실시간 LLM으로 ingest 시 `lesson_context` 자동 생성 (선택 과제, Phase 3)
- 기존 `docs/curriculum/*.json` 파일 형식 변경

---

## 5. 구현 단계 (PR 단위 제안)

### Phase 1 — 데이터 모델 (Flyway V11+)

- [ ] `curriculum_units`에 `lesson_context TEXT NOT NULL DEFAULT ''`, `concept_order TEXT NOT NULL DEFAULT '[]'` 추가
- [ ] 기존 행 백필: `concept_order` ← `unit_json`에서 **1회** 추출 (마이그레이션 스크립트만, 런타임 파싱 아님)
- [ ] 기존 행 백필: `lesson_context` ← `formatLessonConcepts` 결과를 **초기값**으로 저장 (이후 수동·생성 파이프라인으로 교체)
- [ ] `system_prompt_template` 플레이스홀더: `{{lesson_concepts}}` → `{{lesson_context}}` (신규 행·UPDATE 마이그레이션)

### Phase 2 — 런타임 decouple

- [ ] `LlmConversationService.buildSystemPrompt`: `lesson_context` + `concept_order` 엔티티 필드 사용
- [ ] `buildUserPrompt`: `concept_order`만 주입 (이미 유사, 유지)
- [ ] `validateSemanticRules`: `unitJson` 파라미터 제거, 문자열 매칭 규칙 제거
- [ ] `TeachProgressGuard`: `expectedDeltaCovered(unitJson)` 제거, LLM `covered` + affirm guard만
- [ ] `formatLessonConcepts`, `termsForConcept`, `extractConceptTerms` — **deprecated** 후 제거 (또는 ingest 전용 패키지로 이동)
- [ ] `StubLlmClient`: `lesson_context` 기반 최소 heuristic 또는 고정 시나리오 fixture

### Phase 3 — 생성·운영 연동

- [ ] 커리큘럼 등록 API/스크립트: `unit_json` + `lesson_context` + `concept_order` 세트 저장
- [ ] `docs/curriculum` ingest 가이드: raw JSON과 teach용 텍스트를 **함께** 제출하는 규칙
- [ ] 프롬프트 개선 harness: JSON 구조 대신 `lesson_context` 품질 루프

### Phase 4 — 검증·정리

- [ ] `LlmConversationPromptTest`: `lesson_context` / `concept_order` 기반 assertion
- [ ] `TeachSemanticFlowTest`: 서버 문자열 매칭 제거 후 LLM mock 또는 stub 시나리오로 대체
- [ ] `AGENTS.md` / teach 관련 ai-reference 한 줄 링크

---

## 6. 마이그레이션·호환

| 환경 | 처리 |
| --- | --- |
| 이미 V7~V10 적용 DB | V11 컬럼 추가 + 백필만. **V7 파일 수정 금지** |
| 신규 로컬/CI | Flyway 전체 순서 적용 |
| `unit_json` only 레거시 행 | `lesson_context` 빈 경우 system prompt에 “맥락 없음” + `concept_order`만으로 동작 (품질 저하 허용, 에러 아님) |

---

## 7. 리스크와 완화

| 리스크 | 완화 |
| --- | --- |
| LLM만 믿으면 explain/affirm 오판 증가 | semantic retry + `TeachProgressGuard` delta 규칙 + harness 루프 |
| `lesson_context` 품질 편차 | 생성 파이프라인에서 id·key phrase를 텍스트로 명시하도록 가이드 (스키마 강제 아님) |
| `concept_order`와 LLM `covered` 불일치 | `parseAndValidate` id 집합 검증 유지 |
| 이중 저장 (`unit_json` + `lesson_context`) | `unit_json`은 source/archive, teach는 `lesson_context`만 — 역할 문서화 |

---

## 8. 현재 코드와의 매핑 (제거·대체 예정)

| 현재 | 계획 후 |
| --- | --- |
| `formatLessonConcepts(unitJson)` | ingest/백필 1회용 → 런타임 제거 |
| `parseConceptIdOrder(unitJson)` | `concept_order` 컬럼 읽기 |
| `termsForConcept` / `userTextExplainsConcept` | 제거 (LLM 분류) |
| `validateSemanticRules(..., unitJson)` | `unitJson` 인자 제거 |
| `buildSystemPrompt`의 `{{unit_json}}` 치환 | `{{lesson_context}}` 치환 |

---

## 9. 오픈 질문

1. `lesson_context` 최대 길이(토큰) 상한? — 컬럼 TEXT + prompt trim 정책 필요 여부
2. `concept_order`를 LLM이 매 턴 출력하는 `missing` 순서와 어떻게 동기화할지 — 현행 `conceptOrder` 순서 유지로 충분한지
3. 생성 파이프라인이 BE repo 안에 들어오는지, 외부 도구인지 — ingest API 범위
4. 분수 등 레거시 `keywords` 단원도 `lesson_context` 수동 작성 vs 자동 백필만으로 운영할지

---

## 10. 결론

**teach 백엔드는 “단원 JSON의 모양”을 알지 못해도 된다.**  
대신 **저장 시점에** teach가 필요한 최소 정보를 명시적으로 둔다.

- **자유 보관:** `unit_json` (raw)
- **LLM 전달:** `lesson_context` (텍스트, 형식 자유)
- **기계 검증:** `concept_order` (id 배열만)

이 계획은 코드 변경 없이 다음 스프린트 구현 입력으로 남긴다. 구현 시 본 문서의 Phase 1부터 순서대로 PR을 쪼갠다.