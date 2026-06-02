# API·데이터베이스 모델링 계획

동갑내기 과외하기 1차 프로토타입 — **화면 기준 API 호출**과 DB·계약 참조를 한 문서에 둡니다.

| 항목 | 내용 |
| --- | --- |
| 기준 문서 | [개발자 기능 명세서](../reference/product/developer-feature-spec.html) |
| 스택 | Kotlin, Spring Boot 3.5, JPA, Flyway, PostgreSQL |
| Base path | `/api/v1` (명세는 `/api` — [§12](#12-명세-충돌-및-검토-이력) 참고) |

---

## 0. 이 문서 사용법

### 0.1 역할별 읽는 순서

| 역할 | 순서 | 목표 |
| --- | --- | --- |
| **FE** | [§1 레지스트리](#1-화면-id-레지스트리-figma-앵커) → [§2 화면별 호출](#2-화면별-api-호출-명세-구현용) → [§11 TTS](#11-ttsstt-음성-연동-후순위) | “이 버튼에 무슨 API?” |
| **BE** | §2 → §3 → **[§4 DB 모델링](#4-데이터베이스)** → [§6 Phase](#6-구현-단계) | Entity·Flyway·Repository |
| **디자인 변경** | [§1](#1-화면-id-레지스트리-figma-앵커) `SCR-*`로 §2 절 찾기 → [§13 Figma](#13-figma-디자인-변경-시-수정-가이드) | 스펙 위치만 갱신 |

### 0.2 목차

| § | 제목 | 용도 |
| --- | --- | --- |
| **1** | 화면 ID 레지스트리 | Figma·명세·코드 공통 키 |
| **2** | 화면별 API 호출 | **구현 메인** — mount/클릭/API |
| **3** | API 카탈로그 | Request/Response·에러 |
| **4** | **데이터베이스** | ERD·컬럼·enum·제약·Flyway·시드·API↔DB |
| **5** | 비즈니스·상태 | 검증·세션 상태기 |
| **6** | 구현 Phase | P0~P5 |
| **7** | 패키지 구조 | Kotlin 모듈 |
| **8~9** | 팀 확인·관련 문서 | |
| **10~12** | TTS·충돌 이력 | 후순위·결정 |
| **13** | Figma 변경 가이드 | 유지보수 |

### 0.3 범위·정책 (한 줄)

| 구분 | 내용 |
| --- | --- |
| 프로토타입 API | **음성(TTS/STT) 0개** — [§11](#11-ttsstt-음성-연동-후순위) |
| 멱등 | **서버·DB만** (`session_id`, `COMPLETED` 조회) — 클라 `Idempotency-Key` 없음 |
| 당일 수업 | `COMPLETED` **다회** 허용. `lessonCompletedToday` = 팝업만 제어 |
| `complete` | **[설명 종료]** 1회. 5.0은 `GET /reward` + `POST /reward/ack` |

---

## 1. 화면 ID 레지스트리 (Figma 앵커)

디자인·명세·본 문서는 **`SCR-*` 코드**로 연결한다. Figma 프레임 이름에 `SCR-HOME` 등을 붙이면 §2 절을 바로 찾을 수 있다.

| SCR-ID | 명세 § | Figma 프레임 (권장 이름) | §2 절 | P |
| --- | --- | --- | --- | --- |
| `SCR-OB01` | 0.1 단원 선택 | `SCR-OB01 / Onboarding-Unit` | [2.1](#21-scr-ob01--01-단원-선택) | P2 |
| `SCR-OB02` | 0.2 시간표 | `SCR-OB02 / Onboarding-Schedule` | [2.2](#22-scr-ob02--02-시간표) | P2 |
| `SCR-HOME` | 1.0 홈 | `SCR-HOME / Home` | [2.3](#23-scr-home--10-홈) | P4 |
| `SCR-PREP` | 2.0 준비 | `SCR-PREP / Lesson-Prep` | [2.4](#24-scr-prep--20-준비) | P3 |
| `SCR-LESSON` | 3.0 수업·TTS | `SCR-LESSON / Lesson-Intro` | [2.5](#25-scr-lesson--30-수업-시작) | P3 |
| `SCR-REACTION` | 3.0 AI 반응 | `SCR-REACTION / Lesson-Reaction` | [2.6](#26-scr-reaction--30-ai-반응) | P3 |
| `SCR-PRAISE` | 4.0 칭찬 | `SCR-PRAISE / Praise` | [2.7](#27-scr-praise--40-칭찬) | P3 |
| `SCR-REWARD` | 5.0 보상 | `SCR-REWARD / Reward` | [2.8](#28-scr-reward--50-보상) | P3 |
| `SCR-SETTINGS` | 6.0 설정 | `SCR-SETTINGS / Settings` | [2.9](#29-scr-settings--60-설정) | P4 |
| `SCR-HISTORY` | 7.0 기록 | `SCR-HISTORY / History` | [2.10](#210-scr-history--70-수업기록) | P4 |
| `APP-ENTRY` | 앱 진입 | `APP-ENTRY / Bootstrap` | [2.0](#20-app-entry--앱-진입) | P0~P2 |

> **Figma URL / node-id:** 확정 시 §1 표에 `figma_link` 열을 추가하고 §13 체크리스트를 갱신한다.

---

## 2. 화면별 API 호출 명세 (구현용)

각 절 표 읽법:

| 열 | 의미 |
| --- | --- |
| **시점** | `mount` = 화면 진입, `click` = 사용자 액션, `local` = API 없음 |
| **API** | 비우면 호출 없음. 경로는 `/api/v1` 생략 |
| **상세** | Request/Response 필드·타입·JSON 예시 → [§3](#3-api-카탈로그). **성공 응답에 `data` 래핑 없음** |

### 2.0 `APP-ENTRY` — 앱 진입

| 시점 | 트리거 | API | BE | FE |
| --- | --- | --- | --- | --- |
| mount | 앱 cold start (토큰 있음) | `GET /user/onboarding/status` | P2 | 온보딩 vs 홈 라우팅 |
| mount | 포그라운드 복귀 (홈·수업 중) | `GET /session/status` | P3 | `pendingReward` → 5.0 |
| — | 최초 API 전 | *(내부)* user upsert | P0 | 401 전 사용자 생성 |

```mermaid
flowchart TD
    A[APP-ENTRY] --> B{onboarding/status}
    B -->|completed=false| OB01[SCR-OB01]
    B -->|completed=true| C{session/status}
    C -->|pendingRewardSessionId| RW[SCR-REWARD]
    C -->|else| H[SCR-HOME]
```

---

### 2.1 `SCR-OB01` — 0.1 단원 선택

| 시점 | 트리거 | API | Request | Response 사용처 |
| --- | --- | --- | --- | --- |
| mount | 화면 진입 | **`GET /curriculum`** | — | 칩 목록 렌더 |
| click | [다음으로] (칩 선택됨) | **`POST /user/onboarding`** | `{ curriculumId, step: 1 }` | → SCR-OB02 |
| local | [실력 진단] | — | — | 칩 deselect만 (API 없음) |
| local | 칩 선택/해제 | — | — | 클라 상태 |

**FE 구현 메모:** 진단 완료 플래그 없음 — `curriculumId` 없으면 다음 비활성.

**BE 구현 메모:** `user_curriculums` upsert.

---

### 2.2 `SCR-OB02` — 0.2 시간표

| 시점 | 트리거 | API | 순서 | 비고 |
| --- | --- | --- | --- | --- |
| click | [설정 완료 및 홈으로] | **`POST /user/schedule`** | ① | `frequency`, `days[]`, `time` |
| click | ↑ 이어서 | **`POST /notifications/register`** | ② | **schedule 저장 후**. 없으면 400 |
| click | ↑ 이어서 | **`POST /user/onboarding/complete`** | ③ | → SCR-HOME |
| local | 빈도·요일 토글 | — | — | 검증은 저장 시 서버 |

**Request 예 (`schedule`):** `{ "frequency": 3, "days": ["TUE","THU","SAT"], "time": "17:00" }`

---

### 2.3 `SCR-HOME` — 1.0 홈

| 시점 | 트리거 | API | Response 필드 → UI |
| --- | --- | --- | --- |
| mount | 화면 진입 | **`GET /user/profile`** | `level`, `totalCoins`, `curriculum`, `progressPercent`, `homeMessage` |
| mount | *(병렬 권장)* | **`GET /session/status`** | `lessonCompletedToday` → 과외 팝업, `activeSession`, `pendingRewardSessionId` |
| mount | *(대안 1-call)* | `GET /user/home` | profile + status 합본 |
| click | [과외하러가기] | — | → SCR-PREP (API 없음) |
| click | ⚙️ | — | → SCR-SETTINGS |
| click | 탭 수업기록 | — | → SCR-HISTORY |
| click | 탭 수업공간 | — | → SCR-PREP |
| local | 캐릭터 터치 (미완료) | — | 팝업 즉시 (2초 타이머 대체) |

**`lessonCompletedToday === true`:** 과외 요청 팝업 미노출. 수업공간 탭으로 **추가 수업** 가능.

---

### 2.4 `SCR-PREP` — 2.0 준비

| 시점 | 트리거 | API | 비고 |
| --- | --- | --- | --- |
| mount | 화면 진입 | **`GET /session/today`** | → [§3.5](#api-get-session-today) |
| click | [수업 시작하기] (5초 후) | **`POST /session/start`** | `sessionId` 저장 → SCR-LESSON |
| click | [◀ 돌아가기] | — | → SCR-HOME |
| local | 5초 카운트다운 | — | API 없음 |

**`start` 응답:** `{ sessionId, startedAt, resumed }` — 이후 모든 수업 API에 `{id}=sessionId`.

**에러:** `today` 실패 → FE 5초 후 홈. `start` 중복 → 409 또는 `resumed: true` ([§12 S2'](#12-명세-충돌-및-검토-이력) 진행 중 세션 재개).

---

### 2.5 `SCR-LESSON` — 3.0 수업 시작

| 시점 | 트리거 | API | 비고 |
| --- | --- | --- | --- |
| mount | 화면 진입 | **`GET /session/{sessionId}/lesson`** | 질문 말풍선, 힌트 JSON, GNB 제목 |
| **local** | 마이크 1탭 / 2탭 | **— (API 없음)** | Waveform. [§11](#11-ttsstt-음성-연동-후순위) P6+ |
| click | **[설명완료]** | **`POST /session/{sessionId}/advance-phase`** | `currentPhase` → `REACTION` |
| click | [◀] confirm 확인 | **`POST /session/{sessionId}/abort`** | → SCR-HOME |

**다음 화면:** SCR-REACTION (`advance-phase` 성공 후).

---

### 2.6 `SCR-REACTION` — 3.0 AI 반응

| 시점 | 트리거 | API | 비고 |
| --- | --- | --- | --- |
| mount | 화면 진입 | **`GET /session/{sessionId}/reaction`** | 오답 말풍선, OX 전. `phase=REACTION` 아니면 403 |
| local | [틀렸어] / [정답이야] | — | OX·힌트노트·재설명 UI |
| **local** | 재설명 마이크 1탭 | **— (API 없음)** | [§11](#11-ttsstt-음성-연동-후순위) |
| click | **[설명 종료]** | **`POST /session/complete`** | body: `{ sessionId }` — **정산 1회** |
| click | [◀] confirm | `POST …/abort` | |

**`complete` 성공 후:** → SCR-PRAISE (4.0). 실패 시 토스트 + **동일 `sessionId`로 재시도** (서버 멱등).

---

### 2.7 `SCR-PRAISE` — 4.0 칭찬

| 시점 | 트리거 | API | 비고 |
| --- | --- | --- | --- |
| mount | 화면 진입 | **—** | 정산은 이미 `complete`에서 끝 |
| click | [보상 받기] | — | → SCR-REWARD |

> 명세 4.0 “이미 완료 기록” edge case와 맞추려면 **`complete`는 반드시 이 화면 이전**(SCR-REACTION)에서 호출.

---

### 2.8 `SCR-REWARD` — 5.0 보상

| 시점 | 트리거 | API | 비고 |
| --- | --- | --- | --- |
| mount | 화면 진입 | **`GET /session/{sessionId}/reward`** | → [§3.5.2 RewardDto](#api-reward-dto) |
| mount | `reward` 404 등 | *(선택)* `POST /session/complete` | 이미 COMPLETED면 동일 응답. **일반 경로는 GET만** |
| click | **[확인]** | **`POST /session/{sessionId}/reward/ack`** | → SCR-HOME |
| local | 코인 애니메이션 | — | 서버 값은 `GET /reward` |

**재진입:** `GET /session/status` → `pendingRewardSessionId` → 이 화면 mount 시 **`GET /reward`만**.

---

### 2.9 `SCR-SETTINGS` — 6.0 설정

| 시점 | 트리거 | API | 비고 |
| --- | --- | --- | --- |
| mount | 화면 진입 | **`GET /user/settings`** | 칩·빈도·요일·시간 prefill |
| click | [저장하기] | **`PUT /user/settings`** | 변경 없으면 no-op |
| click | ↑ (시간표 변경 시) | **`POST /notifications/reschedule`** | PUT 내부 호출 가능 |
| click | [◀ 홈으로] | — | → SCR-HOME |

**단원 변경:** `resetProgress: true` + confirm. `STARTED` 세션 있으면 409 또는 abort ([§3](#3-api-카탈로그)).

---

### 2.10 `SCR-HISTORY` — 7.0 수업기록

| 시점 | 트리거 | API | 비고 |
| --- | --- | --- | --- |
| mount | 화면 진입 | **`GET /sessions/history`** | `cursor`, `size=20` |
| scroll | 하단 도달 | `GET /sessions/history?cursor=…` | `hasMore` |
| local | 빈 상태 | — | `sessions.length === 0` |

---

### 2.11 한눈에: API × 화면 매트릭스

| API | OB01 | OB02 | HOME | PREP | LESSON | REACTION | PRAISE | REWARD | SET | HIST |
| --- |:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| `GET /curriculum` | ● | | | | | | | | ● | |
| `GET /onboarding/status` | | | | | | | | | | △ |
| `POST /user/onboarding` | ● | | | | | | | | | |
| `POST /user/schedule` | | ● | | | | | | | | |
| `POST /notifications/register` | | ● | | | | | | | | |
| `POST /onboarding/complete` | | ● | | | | | | | | |
| `GET /user/profile` | | | ● | | | | | | | |
| `GET /session/status` | △ | | ● | | | | | △ | | |
| `GET /user/home` | | | ○ | | | | | | | |
| `GET /session/today` | | | | ● | | | | | | |
| `POST /session/start` | | | | ● | | | | | | |
| `GET /session/…/lesson` | | | | | ● | | | | | |
| `POST /advance-phase` | | | | | ● | | | | | |
| `GET /session/…/reaction` | | | | | | ● | | | | |
| `POST /session/complete` | | | | | | ● | | ○ | | |
| `GET /session/…/reward` | | | | | | | | ● | | |
| `POST /reward/ack` | | | | | | | | ● | | |
| `POST /session/abort` | | | | | ● | ● | | | | |
| `GET/PUT /user/settings` | | | | | | | | | ● | |
| `POST /notifications/reschedule` | | | | | | | | | ● | |
| `GET /sessions/history` | | | | | | | | | | ● |

● 필수 · △ 조건부 · ○ 선택

---

## 3. API 카탈로그

요청·응답은 **필드명(camelCase)·타입·필수 여부(R/O)**·예시 JSON**으로 고정한다. 구현 시 DTO는 본 절을 그대로 따른다.

### 3.0 공통

**Request headers**

| Header | 필수 | 설명 |
| --- | --- | --- |
| `Authorization` | O | `Bearer {accessToken}` |
| `Content-Type` | POST/PUT | `application/json` |

**성공 응답 (200)**

- **래핑 없음.** Response body가 곧 DTO(JSON object 또는 array).
- `204 No Content`: body 없음 (설정 no-op 등).

**에러 응답 (Problem Details 권장)**

```json
{
  "type": "https://api.example.com/errors/validation",
  "title": "Validation failed",
  "status": 400,
  "detail": "요일을 3개만 골라주세요.",
  "code": "SCHEDULE_DAY_COUNT_MISMATCH"
}
```

| HTTP | `code` 예시 | 상황 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 빈도·요일·시간·curriculumId |
| 401 | `UNAUTHORIZED` | 토큰 없음/만료 |
| 403 | `SESSION_PHASE_MISMATCH` | reaction 호출 시 phase≠REACTION |
| 404 | `NOT_FOUND` | session/user 없음 |
| 409 | `SESSION_ALREADY_STARTED` | 다른 STARTED 세션 존재 |
| 409 | `SESSION_NOT_STARTED` | complete 시 STARTED 아님·abort 불가 |
| 503 | `UPSTREAM_UNAVAILABLE` | (선택) 외부 연동 실패 |

**공통 enum**

| 이름 | 값 |
| --- | --- |
| `DayOfWeek` | `MON`, `TUE`, `WED`, `THU`, `FRI`, `SAT`, `SUN` |
| `TopicType` | `CONCEPT`, `CALCULATION` |
| `SessionStatus` | `STARTED`, `COMPLETED`, `ABORTED` |
| `SessionPhase` | `INTRO`, `REACTION` |
| `DevicePlatform` | `IOS`, `ANDROID` |

**공통 타입**

| 타입 | 형식 | 예시 |
| --- | --- | --- |
| `CurriculumId` | integer (int64) | `3` |
| `SessionId` | string (UUID v4) | `"550e8400-e29b-41d4-a716-446655440000"` |
| `TimeHHmm` | string `^([01][0-9]|2[0-3]):[0-5][0-9]$` | `"17:00"` |
| `DateYmd` | string `YYYY-MM-DD` (KST) | `"2026-06-02"` |
| `DateTime` | string ISO-8601 offset | `"2026-06-02T17:00:00+09:00"` |

---

### 3.1 엔드포인트 인덱스

| Method | Path | §2 사용 화면 | 상세 |
| --- | --- | --- | --- |
| GET | `/curriculum` | OB01, SETTINGS | [3.2](#32-get-curriculum) |
| GET | `/user/onboarding/status` | APP-ENTRY | [3.3](#33-onboarding) |
| POST | `/user/onboarding` | OB01 | [3.3](#33-onboarding) |
| POST | `/user/schedule` | OB02 | [3.3](#33-onboarding) |
| POST | `/user/onboarding/complete` | OB02 | [3.3](#33-onboarding) |
| GET | `/user/profile` | HOME | [3.4](#34-home) |
| GET | `/session/status` | APP-ENTRY, HOME | [3.4](#34-home) |
| GET | `/user/home` | HOME (선택) | [3.4](#34-home) |
| GET | `/session/today` | PREP | [3.5](#35-session) |
| POST | `/session/start` | PREP | [3.5](#35-session) |
| GET | `/session/{id}/lesson` | LESSON | [3.5](#35-session) |
| POST | `/session/{id}/advance-phase` | LESSON | [3.5](#35-session) |
| GET | `/session/{id}/reaction` | REACTION | [3.5](#35-session) |
| POST | `/session/complete` | REACTION | [3.5](#35-session) |
| GET | `/session/{id}/reward` | REWARD | [3.5](#35-session) |
| POST | `/session/{id}/reward/ack` | REWARD | [3.5](#35-session) |
| POST | `/session/{id}/abort` | LESSON, REACTION | [3.5](#35-session) |
| GET | `/user/settings` | SETTINGS | [3.6](#36-settings--history) |
| PUT | `/user/settings` | SETTINGS | [3.6](#36-settings--history) |
| POST | `/notifications/register` | OB02 | [3.7](#37-notifications) |
| POST | `/notifications/reschedule` | SETTINGS | [3.7](#37-notifications) |
| GET | `/sessions/history` | HISTORY | [3.6](#36-settings--history) |

### 3.2 `GET /curriculum`

| | |
| --- | --- |
| Query | 없음 |

**Response body (200):** `CurriculumChip[]`

| 필드 | 타입 | R/O | 설명 |
| --- | --- | --- | --- |
| `id` | integer | R | PK |
| `code` | string | R | `FRACTION_CALC` 등 |
| `name` | string | R | UI 칩 라벨 (예: `분수의 계산`) |
| `displayOrder` | integer | R | 정렬 |

```json
[
  {
    "id": 3,
    "code": "FRACTION_CALC",
    "name": "분수의 계산",
    "displayOrder": 3
  }
]
```

---

### 3.3 Onboarding

#### `GET /user/onboarding/status`

**Response body (200)**

| 필드 | 타입 | R/O | 설명 |
| --- | --- | --- | --- |
| `completed` | boolean | R | `true` → 홈 직행 |
| `step` | integer | R | `0` 미시작, `1` 단원 저장됨, `2` 시간표 저장됨 |

```json
{ "completed": false, "step": 1 }
```

#### `POST /user/onboarding`

**Request body**

| 필드 | 타입 | R/O | 설명 |
| --- | --- | --- | --- |
| `curriculumId` | integer | R | 선택 단원 |
| `step` | integer | R | 프로토타입: `1` 고정 |

```json
{ "curriculumId": 3, "step": 1 }
```

**Response body (200)**

| 필드 | 타입 | R/O |
| --- | --- | --- |
| `curriculumId` | integer | R |
| `step` | integer | R |

#### `POST /user/schedule`

**Request body**

| 필드 | 타입 | R/O | 설명 |
| --- | --- | --- | --- |
| `frequency` | integer | R | `2` 또는 `3` |
| `days` | `DayOfWeek[]` | R | 길이 **반드시** `frequency`와 동일 |
| `time` | `TimeHHmm` | R | `15:00`~`20:00` (1시간 단위) |

```json
{
  "frequency": 3,
  "days": ["TUE", "THU", "SAT"],
  "time": "17:00"
}
```

**Response body (200):** `UserScheduleDto`

| 필드 | 타입 | R/O |
| --- | --- | --- |
| `frequency` | integer | R |
| `days` | `DayOfWeek[]` | R |
| `time` | string | R |

**Errors:** `400` `SCHEDULE_DAY_COUNT_MISMATCH`, `400` `INVALID_LESSON_TIME`

#### `POST /user/onboarding/complete`

| | |
| --- | --- |
| Request body | 없음 (또는 `{}`) |

**Response body (200)**

| 필드 | 타입 | R/O |
| --- | --- | --- |
| `onboardingCompleted` | boolean | R | 항상 `true` |

**Errors:** `400` `SCHEDULE_NOT_CONFIGURED`

---

### 3.4 Home

#### `GET /user/profile`

**Response body (200)**

| 필드 | 타입 | R/O | 설명 |
| --- | --- | --- | --- |
| `level` | object | R | |
| `level.number` | integer | R | 배지 레벨 번호 |
| `level.name` | string | R | 예: `똑똑한 선생님` |
| `totalCoins` | integer | R | ≥ 0 |
| `curriculum` | object | R | |
| `curriculum.id` | integer | R | |
| `curriculum.name` | string | R | 단원명 |
| `curriculum.displayName` | string | R | `chapter_label` (예: `3단원 분수`) |
| `progressPercent` | integer | R | 0~100 |
| `homeMessage` | string | R | 서버 규칙 [§2.3](#23-scr-home--10-홈) |

```json
{
  "level": {
    "number": 2,
    "name": "똑똑한 선생님"
  },
  "totalCoins": 500,
  "curriculum": {
    "id": 3,
    "name": "분수의 계산",
    "displayName": "3단원 분수"
  },
  "progressPercent": 45,
  "homeMessage": "선생님 덕분에 분수 마스터! 다음에 또 만나요!"
}
```

#### `GET /session/status`

**Response body (200)**

| 필드 | 타입 | R/O | 설명 |
| --- | --- | --- | --- |
| `lessonCompletedToday` | boolean | R | 당일 `COMPLETED` ≥1 |
| `activeSession` | object \| null | R | 진행 중 `STARTED` 세션. 없으면 `null` |
| `activeSession.sessionId` | string (UUID) | R* | *object일 때 |
| `activeSession.status` | string | R* | `STARTED` |
| `activeSession.currentPhase` | `SessionPhase` | R* | |
| `activeSession.startedAt` | `DateTime` | R* | |
| `pendingRewardSessionId` | string (UUID) \| null | R | `COMPLETED`+미ACK 보상 |

```json
{
  "lessonCompletedToday": true,
  "activeSession": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "STARTED",
    "currentPhase": "REACTION",
    "startedAt": "2026-06-02T14:30:00+09:00"
  },
  "pendingRewardSessionId": null
}
```

#### `GET /user/home` (선택)

**Response body (200):** `UserProfileDto` + `SessionStatusDto` 필드를 **한 객체**에 병합.

| 필드 | 타입 | R/O |
| --- | --- | --- |
| *(profile 전 필드)* | | R |
| `lessonCompletedToday` | boolean | R |
| `activeSession` | object \| null | R |
| `pendingRewardSessionId` | string \| null | R |

---

### 3.5 Session

#### GET /session/today {#api-get-session-today}

오늘(KST) 수업 준비 화면(SCR-PREP)용. **진행 중 세션**이 있으면 함께 내려 재개·라우팅에 쓴다.

| | |
| --- | --- |
| Path params | 없음 |
| Query | 없음 |

**Response body (200)**

| 필드 | 타입 | R/O | 설명 |
| --- | --- | --- | --- |
| `curriculumId` | integer | R | 현재 선택 단원 |
| `sessionTitle` | string | R | 준비 화면 메인 타이틀 (예: `분수의 세계`) |
| `topics` | `TodayTopic[]` | R | **길이 2** (프로토타입). 순서=수업 순서 |
| `topics[].sequence` | integer | R | `1`, `2` |
| `topics[].lessonTopicId` | integer | R | 콘텐츠 PK |
| `topics[].title` | string | R | 예: `분수란?` |
| `topics[].subtitle` | string \| null | O | 카드 부제 |
| `topics[].topicType` | `TopicType` | R | `CONCEPT` \| `CALCULATION` |
| `activeSession` | object \| null | R | 없으면 `null` |
| `activeSession.sessionId` | string (UUID) | R* | |
| `activeSession.status` | `SessionStatus` | R* | `STARTED` |
| `activeSession.currentPhase` | `SessionPhase` | R* | |
| `activeSession.startedAt` | `DateTime` | R* | |

```json
{
  "curriculumId": 3,
  "sessionTitle": "분수의 세계",
  "topics": [
    {
      "sequence": 1,
      "lessonTopicId": 301,
      "title": "분수란?",
      "subtitle": "개념이해",
      "topicType": "CONCEPT"
    },
    {
      "sequence": 2,
      "lessonTopicId": 302,
      "title": "분수의 덧셈과 뺄셈",
      "subtitle": "계산과정",
      "topicType": "CALCULATION"
    }
  ],
  "activeSession": null
}
```

`activeSession`이 있는 예 (`activeSession`만 non-null):

```json
{
  "curriculumId": 3,
  "sessionTitle": "분수의 세계",
  "topics": [ … ],
  "activeSession": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "STARTED",
    "currentPhase": "INTRO",
    "startedAt": "2026-06-02T14:00:00+09:00"
  }
}
```

---

#### `POST /session/start`

**Request body**

| 필드 | 타입 | R/O | 설명 |
| --- | --- | --- | --- |
| `curriculumId` | integer | O | 생략 시 현재 `user_curriculums` |

```json
{ "curriculumId": 3 }
```

**Response body (200)**

| 필드 | 타입 | R/O | 설명 |
| --- | --- | --- | --- |
| `sessionId` | string (UUID) | R | 이후 path `{id}` |
| `startedAt` | `DateTime` | R | |
| `resumed` | boolean | R | `true`: 기존 STARTED 재개, `false`: 신규 생성 |

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "startedAt": "2026-06-02T14:30:00+09:00",
  "resumed": false
}
```

**Errors:** `409` `SESSION_ALREADY_STARTED` (재개 정책이 아닌 경우)

**Side effect:** `session_topic_snapshots` 2건 생성 (`GET /today`의 `topics`와 동일 내용).

---

#### `GET /session/{sessionId}/lesson`

| | |
| --- | --- |
| Path | `sessionId` (UUID) |

**Response body (200)**

| 필드 | 타입 | R/O | 설명 |
| --- | --- | --- | --- |
| `sessionId` | string | R | |
| `currentPhase` | `SessionPhase` | R | `INTRO` |
| `gnbTitle` | string | R | GNB (예: `3. 분수의 개념`) |
| `question` | object | R | |
| `question.bubbleText` | string | R | 말풍선 HTML/plain |
| `hintNote` | object | R | 힌트노트 JSON (§3.5.1 스키마) |

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "currentPhase": "INTRO",
  "gnbTitle": "3. 분수의 개념",
  "question": {
    "bubbleText": "선생님, 분수는 그냥 숫자랑 어떻게 달라요?"
  },
  "hintNote": {
    "header": {
      "chapter": "제 3장",
      "title": "분수의 개념"
    },
    "sections": [
      {
        "id": "q1",
        "title": "Q1. 분수란?",
        "bodyHtml": "전체를 똑같이 나눈 것 중, <strong>일부분</strong>을 나타내는 수",
        "highlight": false
      }
    ]
  }
}
```

**Errors:** `404`, `403` `SESSION_PHASE_MISMATCH` (이미 REACTION만 허용하는 경우는 lesson만 INTRO일 때 COMPLETED 등)

---

#### `POST /session/{sessionId}/advance-phase`

| | |
| --- | --- |
| Request body | 없음 (또는 `{}`) |

**Response body (200)**

| 필드 | 타입 | R/O |
| --- | --- | --- |
| `sessionId` | string | R |
| `currentPhase` | `SessionPhase` | R | `REACTION` |

**Errors:** `409` `SESSION_NOT_IN_INTRO`, `404`

---

#### `GET /session/{sessionId}/reaction`

**Response body (200)**

| 필드 | 타입 | R/O | 설명 |
| --- | --- | --- | --- |
| `sessionId` | string | R | |
| `currentPhase` | `SessionPhase` | R | `REACTION` |
| `gnbTitle` | string | R | 예: `3. 분수의 덧셈과 뺄셈` |
| `question` | object | R | |
| `question.bubbleText` | string | R | 의도적 오답 포함 가능 |
| `question.displayAnswerHtml` | string | O | 강조 오답 (예: `3/10`) |
| `hintNote` | object | R | 형광펜 대상 `highlight: true` |

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "currentPhase": "REACTION",
  "gnbTitle": "3. 분수의 덧셈과 뺄셈",
  "question": {
    "bubbleText": "아하! 그럼 2/5 더하기 1/5는 <strong>3/10</strong>이죠?",
    "displayAnswerHtml": "3/10"
  },
  "hintNote": {
    "header": {
      "chapter": "제 3장",
      "title": "분수의 덧셈과 뺄셈"
    },
    "sections": [
      {
        "id": "rule-denominator",
        "title": "핵심 규칙",
        "bodyHtml": "분모(아래)는 절대 더하지 않고 그대로 둡니다!",
        "highlight": true
      },
      {
        "id": "rule-numerator",
        "title": "",
        "bodyHtml": "분자(위)끼리만 더해야 합니다",
        "highlight": true
      }
    ]
  }
}
```

**Errors:** `403` `SESSION_PHASE_MISMATCH` (`currentPhase`≠`REACTION`)

---

#### `POST /session/complete`

**Request body**

| 필드 | 타입 | R/O | 설명 |
| --- | --- | --- | --- |
| `sessionId` | string (UUID) | R | path와 동일 값 허용 (body만 사용) |

```json
{ "sessionId": "550e8400-e29b-41d4-a716-446655440000" }
```

**Response body (200):** `RewardDto` ([§3.5.2](#352-rewarddto))

**Errors:** `409` `SESSION_NOT_STARTED`, `404`

---

#### `GET /session/{sessionId}/reward`

| | |
| --- | --- |
| Request body | 없음 |

**Response body (200):** `RewardDto` — 세션 `status=COMPLETED`일 때만.

**Errors:** `404`, `409` `SESSION_NOT_COMPLETED`

---

#### `POST /session/{sessionId}/reward/ack`

| | |
| --- | --- |
| Request body | 없음 |

**Response body (200)**

| 필드 | 타입 | R/O |
| --- | --- | --- |
| `sessionId` | string | R |
| `acknowledged` | boolean | R | `true` |
| `rewardAcknowledgedAt` | `DateTime` | R |

---

#### `POST /session/{sessionId}/abort`

| | |
| --- | --- |
| Request body | 없음 |

**Response body (200)**

| 필드 | 타입 | R/O |
| --- | --- | --- |
| `sessionId` | string | R |
| `status` | `SessionStatus` | R | `ABORTED` |

---

#### 3.5.1 `HintNoteDto` (lesson / reaction 공통)

| 필드 | 타입 | R/O |
| --- | --- | --- |
| `header.chapter` | string | R |
| `header.title` | string | R |
| `sections` | array | R |
| `sections[].id` | string | R |
| `sections[].title` | string | O |
| `sections[].bodyHtml` | string | R |
| `sections[].highlight` | boolean | R | 형광펜 대상 |

#### 3.5.2 RewardDto {#api-reward-dto}

| 필드 | 타입 | R/O | 설명 |
| --- | --- | --- | --- |
| `sessionId` | string | R | |
| `coinsAwarded` | integer | R | 이번 세션 지급 (예: 500) |
| `badgeLevelUp` | boolean | R | 이번에 레벨업했는지 |
| `newLevel` | object \| null | R | `badgeLevelUp=false` → `null` |
| `newLevel.number` | integer | R* | |
| `newLevel.name` | string | R* | |
| `progressPercent` | integer | R | 완료 후 단원 진척 0~100 |
| `totalCoins` | integer | R | 지갑 잔액 |

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "coinsAwarded": 500,
  "badgeLevelUp": true,
  "newLevel": {
    "number": 2,
    "name": "똑똑한 선생님"
  },
  "progressPercent": 45,
  "totalCoins": 500
}
```

---

### 3.6 Settings & History

#### `GET /user/settings`

**Response body (200)**

| 필드 | 타입 | R/O |
| --- | --- | --- |
| `curriculum` | `CurriculumChip` | R | 현재 선택 |
| `schedule` | `UserScheduleDto` | R | |

#### `PUT /user/settings`

**Request body**

| 필드 | 타입 | R/O |
| --- | --- | --- |
| `curriculumId` | integer | O |
| `frequency` | integer | O |
| `days` | `DayOfWeek[]` | O |
| `time` | `TimeHHmm` | O |
| `resetProgress` | boolean | O | default `false`. `true` 시 진척 0 |

**Response body (200):** 갱신된 `GET /user/settings`와 동일 구조.

**No-op:** 변경 없으면 `204 No Content` (body 없음).

#### `GET /sessions/history`

**Query**

| param | 타입 | R/O | default |
| --- | --- | --- | --- |
| `cursor` | string | O | — |
| `size` | integer | O | `20` (max 50) |

**Response body (200)**

| 필드 | 타입 | R/O |
| --- | --- | --- |
| `sessions` | array | R |
| `sessions[].sessionId` | string | R |
| `sessions[].date` | `DateYmd` | R |
| `sessions[].topic` | string | R | `primary_topic_title` |
| `sessions[].coins` | integer | R |
| `sessions[].badgeLevelUp` | boolean | R |
| `hasMore` | boolean | R |
| `nextCursor` | string \| null | R |

```json
{
  "sessions": [
    {
      "sessionId": "550e8400-e29b-41d4-a716-446655440000",
      "date": "2026-05-28",
      "topic": "3단원: 분수의 개념",
      "coins": 500,
      "badgeLevelUp": true
    }
  ],
  "hasMore": false,
  "nextCursor": null
}
```

---

### 3.7 Notifications

#### `POST /notifications/register`

**Request body**

| 필드 | 타입 | R/O |
| --- | --- | --- |
| `deviceToken` | string | O |
| `platform` | `DevicePlatform` | O |

```json
{ "deviceToken": "fcm-token-…", "platform": "IOS" }
```

**Response body (200)**

| 필드 | 타입 | R/O |
| --- | --- | --- |
| `registered` | boolean | R |
| `scheduleCount` | integer | R | 등록된 요일×시간 슬롯 수 |

**Errors:** `400` `SCHEDULE_NOT_CONFIGURED`

#### `POST /notifications/reschedule`

**Request body:** `register`와 동일 (전부 O).

**Response body (200)**

| 필드 | 타입 | R/O |
| --- | --- | --- |
| `rescheduled` | boolean | R |

---

### 3.8 명세 대비 변경

| 명세 | 본 계획 |
| --- | --- |
| `POST /api/user/reward` | `complete`에 통합 |
| 15개 경로 | §3.1 + §2.11 |

---

## 4. 데이터베이스

API §3와 1:1로 맞춘 **논리·물리 모델**이다. JPA 엔티티·Flyway SQL 작성 시 **본 절이 단일 출처**다.

### 4.0 도메인 관계 (개요)

```mermaid
flowchart TB
    subgraph master [Master]
        curriculums
        badge_levels
        lesson_topics
        lesson_questions
        hint_notes
    end
    subgraph user [User]
        users
        user_profiles
        user_curriculums
        user_schedules
        user_schedule_days
    end
    subgraph session [Session]
        tutoring_sessions
        session_topic_snapshots
        coin_ledger_entries
    end
    subgraph notify [Notification]
        device_tokens
        notification_schedules
    end
    users --> user_profiles
    users --> user_curriculums
    users --> user_schedules
    user_schedules --> user_schedule_days
    users --> tutoring_sessions
    curriculums --> lesson_topics
    curriculums --> user_curriculums
    curriculums --> tutoring_sessions
    tutoring_sessions --> session_topic_snapshots
    tutoring_sessions --> coin_ledger_entries
    badge_levels --> user_profiles
    lesson_topics --> lesson_questions
    lesson_topics --> hint_notes
    users --> device_tokens
    users --> notification_schedules
```

---

### 4.1 ERD

```mermaid
erDiagram
    users ||--|| user_profiles : has
    users ||--o| user_curriculums : selects
    users ||--|| user_schedules : has
    users ||--o{ user_schedule_days : has
    users ||--o{ tutoring_sessions : has
    users ||--o{ coin_ledger_entries : earns
    users ||--o{ device_tokens : registers
    users ||--o{ notification_schedules : has

    curriculums ||--o{ user_curriculums : chosen_by
    curriculums ||--o{ lesson_topics : contains
    curriculums ||--o{ tutoring_sessions : for

    badge_levels ||--o{ user_profiles : current_level
    lesson_topics ||--o{ lesson_questions : has
    lesson_topics ||--o{ hint_notes : has
    lesson_topics ||--o{ session_topic_snapshots : snapshots

    tutoring_sessions ||--o{ session_topic_snapshots : includes
    tutoring_sessions ||--o| coin_ledger_entries : may_generate

    user_schedules ||--o{ user_schedule_days : contains
```

**카디널리티 메모**

| 관계 | 규칙 |
| --- | --- |
| `users` ↔ `user_profiles` | 1:1 |
| `users` ↔ `user_curriculums` | 1:0..1 (현재 선택 단원) |
| `users` ↔ `user_schedules` | 1:1 |
| `tutoring_sessions` ↔ `coin_ledger_entries` | 1:0..1 (`session_id` UNIQUE) |

---

### 4.2 DB enum (PostgreSQL)

```sql
-- 예시: Flyway에서 CREATE TYPE 또는 VARCHAR + CHECK
CREATE TYPE day_of_week AS ENUM ('MON','TUE','WED','THU','FRI','SAT','SUN');
CREATE TYPE topic_type AS ENUM ('CONCEPT','CALCULATION');
CREATE TYPE session_status AS ENUM ('STARTED','COMPLETED','ABORTED');
CREATE TYPE session_phase AS ENUM ('INTRO','REACTION');
CREATE TYPE reward_status AS ENUM ('GRANTED','ACKNOWLEDGED');
CREATE TYPE coin_ledger_type AS ENUM ('SESSION_REWARD');
CREATE TYPE device_platform AS ENUM ('IOS','ANDROID');
```

---

### 4.3 테이블 정의

#### 4.3.1 마스터

**`curriculums`**

| 컬럼 | 타입 | NULL | 설명 |
| --- | --- | --- | --- |
| `id` | BIGSERIAL | PK | |
| `code` | VARCHAR(64) | UK | `FRACTION_CALC` |
| `name` | VARCHAR(100) | | 칩 라벨 |
| `chapter_label` | VARCHAR(50) | | 홈 `displayName` (예: `3단원 분수`) |
| `session_title_template` | VARCHAR(100) | | 준비 화면 `sessionTitle` (예: `분수의 세계`) |
| `display_order` | INT | | |
| `is_active` | BOOLEAN | | default true |
| `created_at` | TIMESTAMPTZ | | |

**`badge_levels`**

| 컬럼 | 타입 | NULL | 설명 |
| --- | --- | --- | --- |
| `id` | BIGSERIAL | PK | |
| `level` | INT | UK | 1, 2, … |
| `name` | VARCHAR(50) | | `똑똑한 선생님` |
| `required_completed_sessions` | INT | | 누적 완료 세션 ≥ N 시 승급 |

**`lesson_topics`**

| 컬럼 | 타입 | NULL | 설명 |
| --- | --- | --- | --- |
| `id` | BIGSERIAL | PK | |
| `curriculum_id` | BIGINT | FK | |
| `sequence` | INT | | 1, 2 (오늘 수업 2건) |
| `title` | VARCHAR(200) | | |
| `subtitle` | VARCHAR(100) | O | 준비 카드 부제 |
| `topic_type` | topic_type | | |
| `gnb_title` | VARCHAR(100) | | 수업 GNB (예: `3. 분수의 개념`) |

**`lesson_questions`**

| 컬럼 | 타입 | NULL | 설명 |
| --- | --- | --- | --- |
| `id` | BIGSERIAL | PK | |
| `lesson_topic_id` | BIGINT | FK | |
| `phase` | session_phase | | `INTRO` / `REACTION` |
| `bubble_text` | TEXT | | |
| `wrong_answer_html` | TEXT | O | REACTION 오답 강조 |

**`hint_notes`**

| 컬럼 | 타입 | NULL | 설명 |
| --- | --- | --- | --- |
| `id` | BIGSERIAL | PK | |
| `lesson_topic_id` | BIGINT | FK | |
| `phase` | session_phase | | |
| `content_json` | JSONB | | §3.5.1 `HintNoteDto` 구조 |

---

#### 4.3.2 사용자

**`users`**

| 컬럼 | 타입 | NULL | 설명 |
| --- | --- | --- | --- |
| `id` | BIGSERIAL | PK | |
| `external_id` | VARCHAR(255) | UK | 인증 subject |
| `timezone` | VARCHAR(50) | | default `Asia/Seoul` |
| `created_at` | TIMESTAMPTZ | | |

**`user_profiles`**

| 컬럼 | 타입 | NULL | 설명 |
| --- | --- | --- | --- |
| `user_id` | BIGINT | PK, FK | |
| `badge_level_id` | BIGINT | FK | |
| `total_coins` | INT | | ≥ 0, ledger와 동기 |
| `completed_session_count` | INT | | 배지·통계 |
| `onboarding_completed` | BOOLEAN | | |
| `onboarding_step` | INT | | 0, 1, 2 |
| `updated_at` | TIMESTAMPTZ | | |

**`user_curriculums`**

| 컬럼 | 타입 | NULL | 설명 |
| --- | --- | --- | --- |
| `user_id` | BIGINT | PK, FK | |
| `curriculum_id` | BIGINT | FK | |
| `progress_percent` | INT | | 0~100 |
| `updated_at` | TIMESTAMPTZ | | |

**`user_schedules`**

| 컬럼 | 타입 | NULL | 설명 |
| --- | --- | --- | --- |
| `user_id` | BIGINT | PK, FK | |
| `frequency_per_week` | INT | | 2 or 3 |
| `lesson_time` | TIME | | `17:00` |
| `updated_at` | TIMESTAMPTZ | | |

**`user_schedule_days`**

| 컬럼 | 타입 | NULL | 설명 |
| --- | --- | --- | --- |
| `id` | BIGSERIAL | PK | |
| `user_schedule_id` | BIGINT | FK | |
| `day_of_week` | day_of_week | | |
| `selected_order` | INT | | 1..N 빈도 변경 시 해제 순서 |

| 제약 | 설명 |
| --- | --- |
| UK | `(user_schedule_id, day_of_week)` |
| APP | 저장 시 `count(days) = frequency_per_week` |

---

#### 4.3.3 세션·보상

**`tutoring_sessions`**

| 컬럼 | 타입 | NULL | 설명 |
| --- | --- | --- | --- |
| `id` | UUID | PK | API `sessionId` |
| `user_id` | BIGINT | FK | |
| `curriculum_id` | BIGINT | FK | |
| `status` | session_status | | |
| `current_phase` | session_phase | O | `STARTED`일 때만 |
| `session_date` | DATE | | KST 기준 일자 |
| `started_at` | TIMESTAMPTZ | | |
| `completed_at` | TIMESTAMPTZ | O | |
| `primary_topic_title` | VARCHAR(200) | O | 기록·history `topic` |
| `coins_awarded` | INT | O | complete 시 스냅샷 |
| `badge_level_up` | BOOLEAN | O | |
| `progress_after` | INT | O | complete 후 진척도 |
| `reward_status` | reward_status | O | `COMPLETED` 후 |
| `reward_acknowledged_at` | TIMESTAMPTZ | O | |
| `created_at` | TIMESTAMPTZ | | |
| `version` | BIGINT | | 낙관적 락 (선택) |

| 인덱스 | 용도 |
| --- | --- |
| `(user_id, session_date)` | 당일 status 조회 |
| `(user_id, status)` | `activeSession` (`STARTED`) |

**`session_topic_snapshots`**

| 컬럼 | 타입 | NULL | 설명 |
| --- | --- | --- | --- |
| `id` | BIGSERIAL | PK | |
| `session_id` | UUID | FK | |
| `lesson_topic_id` | BIGINT | FK | 원본 참조 |
| `sequence` | INT | | 1, 2 |
| `title` | VARCHAR(200) | | |
| `subtitle` | VARCHAR(100) | O | |
| `topic_type` | topic_type | | |

| 제약 | |
| --- | --- |
| UK | `(session_id, sequence)` |

**`coin_ledger_entries`**

| 컬럼 | 타입 | NULL | 설명 |
| --- | --- | --- | --- |
| `id` | BIGSERIAL | PK | |
| `user_id` | BIGINT | FK | |
| `session_id` | UUID | FK, **UK** | 멱등 앵커 |
| `amount` | INT | | +500 |
| `type` | coin_ledger_type | | `SESSION_REWARD` |
| `created_at` | TIMESTAMPTZ | | |

---

#### 4.3.4 알림

**`device_tokens`**

| 컬럼 | 타입 | NULL | 설명 |
| --- | --- | --- | --- |
| `id` | BIGSERIAL | PK | |
| `user_id` | BIGINT | FK | |
| `token` | VARCHAR(512) | | |
| `platform` | device_platform | O | |
| `is_active` | BOOLEAN | | |
| `updated_at` | TIMESTAMPTZ | | |

**`notification_schedules`**

| 컬럼 | 타입 | NULL | 설명 |
| --- | --- | --- | --- |
| `id` | BIGSERIAL | PK | |
| `user_id` | BIGINT | FK | |
| `day_of_week` | day_of_week | | |
| `notify_time` | TIME | | `user_schedules.lesson_time` 복사 |
| `timezone` | VARCHAR(50) | | |
| `is_active` | BOOLEAN | | soft-delete 시 false |
| `created_at` | TIMESTAMPTZ | | |

---

### 4.4 `tutoring_sessions` 상태·비즈니스 규칙

```mermaid
stateDiagram-v2
    [*] --> STARTED: POST /session/start
    STARTED --> COMPLETED: POST /session/complete
    STARTED --> ABORTED: POST /session/abort
    ABORTED --> [*]
    COMPLETED --> [*]: reward ACK
```

| 규칙 | DB/서비스 |
| --- | --- |
| 당일 다회 완료 | `session_date`당 `COMPLETED` **여러 행** 허용 |
| `lessonCompletedToday` | EXISTS `COMPLETED` WHERE `user_id` AND `session_date`=today |
| `activeSession` | `status=STARTED` 1건 (정책: 재개 우선, 신규 start 409) |
| `pendingReward` | `COMPLETED` AND `reward_status=GRANTED` AND `reward_acknowledged_at IS NULL` |
| `complete` 멱등 | `status=COMPLETED` → INSERT 스킵, 컬럼 스냅샷으로 응답 |
| 코인 | `complete` TX: ledger INSERT + `user_profiles.total_coins` += amount |
| 진척도 | `user_curriculums.progress_percent` 갱신 (프로토: +45, cap 100) |
| `primary_topic_title` | `session_topic_snapshots` WHERE `sequence=1` |

**`current_phase` 전이**

| from | to | 트리거 |
| --- | --- | --- |
| `INTRO` | `REACTION` | `POST /advance-phase` |
| — | — | OX·마이크: DB 미저장 (클라) |

---

### 4.5 Flyway 마이그레이션

| 버전 | 파일 | 내용 |
| --- | --- | --- |
| V1 | `V1__create_master_tables.sql` | curriculums, badge_levels, enum |
| V2 | `V2__create_content_tables.sql` | lesson_* + **분수 시드** |
| V3 | `V3__create_user_tables.sql` | users, profiles, curriculum, schedule |
| V4 | `V4__create_session_tables.sql` | tutoring_sessions, snapshots, coin_ledger |
| V5 | `V5__create_notification_tables.sql` | device_tokens, notification_schedules |

`spring.jpa.hibernate.ddl-auto: validate` — 스키마는 Flyway만 변경.

---

### 4.6 시드 데이터 (프로토타입)

| 대상 | 내용 |
| --- | --- |
| `curriculums` | 명세 8단원 + `chapter_label` |
| `badge_levels` | Lv.1(0회), Lv.2(1회), … |
| `lesson_topics` ×2 | 분수 단원 — `GET /session/today`와 동일 |
| `lesson_questions` | SCR-LESSON INTRO, SCR-REACTION |
| `hint_notes` | JSONB — §3.5.1/3.5 reaction 예시 |
| 보상 | `coins_awarded = 500` (서비스 상수 또는 `reward_rules` 추후) |

---

### 4.7 API ↔ DB 매핑 (빠른 참조)

| API | 주요 읽기/쓰기 테이블 |
| --- | --- |
| `GET /curriculum` | `curriculums` |
| `POST /user/onboarding` | `user_curriculums`, `user_profiles.onboarding_step` |
| `POST /user/schedule` | `user_schedules`, `user_schedule_days` |
| `GET /user/profile` | `user_profiles`, `badge_levels`, `user_curriculums`, `curriculums` |
| `GET /session/status` | `tutoring_sessions` (today, STARTED, pending reward) |
| `GET /session/today` | `user_curriculums`, `curriculums`, `lesson_topics`, `tutoring_sessions`(STARTED) |
| `POST /session/start` | `tutoring_sessions`, `session_topic_snapshots` |
| `GET …/lesson` | `lesson_questions`, `hint_notes`, `lesson_topics` |
| `POST /advance-phase` | `tutoring_sessions.current_phase` |
| `GET …/reaction` |同上 `phase=REACTION` |
| `POST /session/complete` | `tutoring_sessions`, `coin_ledger_entries`, `user_profiles`, `user_curriculums` |
| `POST /reward/ack` | `tutoring_sessions.reward_*` |
| `GET /sessions/history` | `tutoring_sessions` WHERE `COMPLETED` |
| `PUT /user/settings` | `user_curriculums`, `user_schedules*`, `notification_schedules` |

---

### 4.8 JPA 패키지 ↔ 테이블 (구현)

| 패키지 | 엔티티 예 |
| --- | --- |
| `curriculum` | `Curriculum`, `LessonTopic`, `LessonQuestion`, `HintNote` |
| `user` | `User`, `UserProfile`, `UserCurriculum`, `UserSchedule`, `UserScheduleDay` |
| `session` | `TutoringSession`, `SessionTopicSnapshot`, `CoinLedgerEntry` |
| `notification` | `DeviceToken`, `NotificationSchedule` |
| `gamification` | `BadgeLevel` (또는 curriculum 패키지) |

---

## 5. 비즈니스·상태

### 5.1 세션 플로우 (전체)

```mermaid
sequenceDiagram
    participant FE
    participant BE
    FE->>BE: GET /session/today
    FE->>BE: POST /session/start
    FE->>BE: GET /lesson
    Note over FE: 마이크 local
    FE->>BE: POST /advance-phase
    FE->>BE: GET /reaction
    Note over FE: OX local
    FE->>BE: POST /complete
    FE->>BE: GET /reward
    FE->>BE: POST /reward/ack
```

### 5.2 서버 주도 멱등성

클라 `Idempotency-Key` **미사용**. `complete`는 `session_id` + `COMPLETED` + ledger UNIQUE로 처리 ([§0.3](#03-범위정책-한-줄)).

### 5.3 검증

- schedule: `days.length === frequency` at save.
- time: `15:00`~`20:00` 정각.
- `session_date`: Asia/Seoul.

---

## 6. 구현 단계

| Phase | 산출 | §2 화면 |
| --- | --- | --- |
| P0 | auth, users | APP-ENTRY |
| P1 | curriculum, lesson 시드 | (데이터) |
| P2 | onboarding APIs | OB01, OB02 |
| P3 | session APIs | PREP~REWARD |
| P4 | home, settings, history | HOME, SETTINGS, HISTORY |
| P5 | notifications | OB02, SETTINGS |
| **P6+** | TTS/STT | [§11](#11-ttsstt-음성-연동-후순위) |

---

## 7. 패키지 구조

```
session/   # today, start, lesson, reaction, complete, reward, ack, abort
user/      # profile, onboarding, settings
curriculum/
notification/
```

`SessionCompletionService` — complete 단일 `@Transactional`.

---

## 8. 팀 확인 (잔여)

| # | 항목 | 채택 |
| --- | --- | --- |
| 1 | Auth | JWT + user upsert |
| 2 | 진행 중 `start` | 재개 우선 / 신규 409 |
| 3 | `/api` vs `/api/v1` | v1 |
| 4 | AI/STT | P6+ |

---

## 9. 관련 문서

- [개발자 기능 명세서](../reference/product/developer-feature-spec.html)
- [AI 대화 루프 설계서](../reference/architecture/ai-conversation-loop-system-design.pdf)

---

## 10. (구 §10~11 통합 참고)

상세 도메인 다이어그램·검토 이력 전문은 Git 히스토리 또는 명세 PR 참고. 결정 ID S1~S11은 [§12](#12-명세-충돌-및-검토-이력) 유지.

---

## 11. TTS/STT·음성 연동 (후순위)

| 구분 | 내용 |
| --- | --- |
| 프로토타입 API | **0개** |
| **호출 위치 (나중)** | SCR-LESSON 마이크 1·2탭, SCR-REACTION 재설명 마이크 → [§2.5](#25-scr-lesson--30-수업-시작), [§2.6](#26-scr-reaction--30-ai-반응) `local` 행을 API 행으로 **교체** |
| 예상 API (초안) | `POST …/speech/start`, `stop`, `retry` — [§13](#13-figma-디자인-변경-시-수정-가이드) |

P0~P5: §2.5~2.6의 `local`만 구현하면 E2E 완료.

---

## 12. 명세 충돌 및 검토 이력

| ID | 채택 요약 |
| --- | --- |
| S1 | 당일 `COMPLETED` 다회 |
| S2 | `complete`=@REACTION [설명 종료], 5.0=GET reward |
| S11 | Idempotency-Key 미채택 |
| S4 | reward→complete 통합 |
| S5 | `pendingRewardSessionId` |
| S7 | topic snapshots + phase |

---

## 13. Figma·디자인 변경 시 수정 가이드

### 13.1 원칙

1. **화면 단위는 `SCR-*`를 바꾸지 않는다.** 레이아웃만 바뀌면 §2 해당 절만 수정.
2. **새 화면**이 생기면 §1에 행 추가 → §2에 절 추가 → §3.1 인덱스 추가.
3. **버튼이 API를 호출**하게 바뀌면 §2 표에 `click` 행 추가/수정.
4. **음성** 연동은 §11만 확장하고 §2.5~2.6의 `local`→`click` 전환.

### 13.2 체크리스트 (디자인 핸드오프 시)

- [ ] Figma 프레임명 = §1 `SCR-*` 와 일치?
- [ ] 각 Interactive 컴포넌트가 §2 `click` 행과 1:1?
- [ ] 신규 모달/바텀시트 → mount API 필요 여부 §2에 반영?
- [ ] §2.11 매트릭스 열/행 갱신?
- [ ] §3 Request/Response 필드 변경 시 §2 “Response 사용처” 갱신?
- [ ] DB 컬럼 필요 시 §4 + Flyway 버전 추가?

### 13.3 수정 위치 빠른 찾기

| 바꾸는 것 | 수정 절 |
| --- | --- |
| 홈 헤더·코인·레벨 | [§2.3](#23-scr-home--10-홈), [§3.4](#34-home) |
| 수업 준비 카운트다운 | [§2.4](#24-scr-prep--20-준비) `local` |
| 마이크·Waveform | [§2.5](#25-scr-lesson--30-수업-시작), [§11](#11-ttsstt-음성-연동-후순위) |
| OX·재설명 | [§2.6](#26-scr-reaction--30-ai-반응) |
| 보상 금액·배지 | [§2.8](#28-scr-reward--50-보상), `complete` 트랜잭션, `badge_levels` 시드 |
| 설정 칩·시간표 | [§2.9](#29-scr-settings--60-설정) |
| 기록 카드 | [§2.10](#210-scr-history--70-수업기록) |

### 13.4 Figma 링크 기록 (TODO)

| SCR-ID | Figma URL | node-id | 최종 동기화일 |
| --- | --- | --- | --- |
| SCR-HOME | *TBD* | | |
| SCR-LESSON | *TBD* | | |
| … | | | |

디자인 URL 확정 시 위 표만 채우면 코드·문서 추적이 유지된다.