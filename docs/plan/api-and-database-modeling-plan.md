# API·데이터베이스 모델링 계획

동갑내기 과외하기 1차 프로토타입 — **화면 기준 API 호출**과 DB·계약 참조를 한 문서에 둡니다.

| 항목 | 내용 |
| --- | --- |
| 기준 문서 | [개발자 기능 명세서](../reference/product/developer-feature-spec.html) (디자인 핸드오프 반영, 2026-06-06) |
| 디자인 참조 | UT 프로토타입·컴포넌트 시트 — [디자인 vs 명세 차이](../reference/product/design-vs-feature-spec-diff.md) |
| 스택 | Kotlin, Spring Boot 3.5, JPA, Flyway, PostgreSQL |
| URL 경로 | 루트 경로 — `/api` prefix·버저닝 없음 |
| 구현 원칙 | **UI** = 디자인 핸드오프 · **로직·API·DB** = 명세 + 본 문서 |

---

## 0. 이 문서 사용법

### 0.1 역할별 읽는 순서

| 역할 | 순서 | 목표 |
| --- | --- | --- |
| **FE** | [§0.4](#04-디자인ui-vs-api-ssot) → [§1](#1-화면-id-레지스트리-figma-앵커) → [§2](#2-화면별-api-호출-명세-구현용) → [§11 TTS](#11-ttsstt-음성-연동-후순위) | UI 토큰 + “이 버튼에 무슨 API?” |
| **BE** | §2 → §3 → **[§4 DB 모델링](#4-데이터베이스)** → [§6 Phase](#6-구현-단계) | Entity·Flyway·Repository |
| **디자인** | [§0.4](#04-디자인ui-vs-api-ssot) → [§13](#13-figma-디자인-변경-시-수정-가이드) | 비주얼·카피는 디자인, API는 §2·§3만 |
| **디자인 변경** | [§1](#1-화면-id-레지스트리-figma-앵커) `SCR-*`로 §2 절 찾기 → [§13](#13-figma-디자인-변경-시-수정-가이드) | UI 메모·스펙 위치 갱신 |

### 0.2 목차

| § | 제목 | 용도 |
| --- | --- | --- |
| **0.3~0.4** | 범위·정책 / 디자인 SSOT | API 정책·FE 토큰 |
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
| **14** | **Open Questions** | **메인 학습 플로우** 기획 확인 (결정 전) |

### 0.3 범위·정책 (한 줄)

| 구분 | 내용 |
| --- | --- |
| 프로토타입 API | **음성(TTS/STT) 0개** — [§11](#11-ttsstt-음성-연동-후순위) |
| 인증 (P0) | 로그인 없음 — 앱이 기기별 **UUID v4** 발급 → `Authorization: Bearer {deviceUserId}` ([§3.0](#30-공통)) |
| 멱등 | **서버·DB만** (`session_id`, `COMPLETED` 조회) — 클라 `Idempotency-Key` 없음 |
| 당일 수업 | `COMPLETED` **다회** 허용. `lessonCompletedToday` = 팝업만 제어 |
| `complete` | **[설명 종료]** 1회. 5.0은 `GET /reward` + `POST /reward/ack` |

### 0.4 디자인·UI vs API SSOT

| 구분 | 단일 출처 (SSOT) | 본 문서 반영 위치 |
| --- | --- | --- |
| 비주얼·컴포넌트·레이아웃 | UT 프로토타입 + `컴포넌트.svg` | §2 `FE 구현 메모`, [§13](#13-figma-디자인-변경-시-수정-가이드) |
| 화면 카피·버튼 라벨 | 디자인 우선, 명세가 동작 정의 | §2 각 절 UI 메모 |
| 동작·상태기·API·DB | 명세 + **본 문서** | §2~§4 |
| 화면 추적 키 | `SCR-*` (Figma·코드·문서 공통) | §1 |

**디자인 토큰 (FE 테마 변수)**

| 토큰 | 값 | 용도 |
| --- | --- | --- |
| CTA Primary | `#FFD700` 그라데이션 | 다음으로, 수업 시작, 과외하러가기, 보상 받기 |
| Confirm | `#49CA34` | 설명완료, 설명 종료, 확인, 설정 저장 |
| Chip / Tab Active | `#FF9438` | 커리큘럼 칩, 요일, 하단 탭 언더라인 |
| Navy | `#1A3A5C` / `#0F2444` | GNB, AI 말풍선 테두리 |
| 말풍선 | AI 흰+네이비 / 유저 노란 / 결과 그린 | 수업·반응 |
| 퀘스트북 | `#8b7355` | 힌트노트 접힘 탭 |
| 폰트 | Paperlogy 8~32px | 앱 전역 |

> API Request/`data` 필드명·타입은 **디자인과 무관**하게 §3를 따른다. 성공·실패 envelope(`code`, `message`, `data`)는 [§3.0](#30-공통). UI만 바뀌고 API가 같으면 §2의 `FE 구현 메모`만 갱신한다.

---

## 1. 화면 ID 레지스트리 (Figma 앵커)

디자인·명세·본 문서는 **`SCR-*` 코드**로 연결한다. Figma 프레임 이름에 `SCR-HOME` 등을 붙이면 §2 절을 바로 찾을 수 있다.

| SCR-ID | 명세 § | Figma 프레임 (권장 이름) | §2 절 | P |
| --- | --- | --- | --- | --- |
| `SCR-SPLASH` | 앱 스플래시 | `SCR-SPLASH / Splash` | [2.0](#20-scr-splash--앱-스플래시) | P0 |
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
| `APP-ENTRY` | 앱 진입 라우팅 | `APP-ENTRY / Bootstrap` | [2.0.1](#201-app-entry--앱-진입-라우팅) | P0~P2 |

> **Figma URL / node-id:** 확정 시 §1 표에 `figma_link` 열을 추가하고 §13 체크리스트를 갱신한다.

---

## 2. 화면별 API 호출 명세 (구현용)

각 절 표 읽법:

| 열 | 의미 |
| --- | --- |
| **시점** | `mount` = 화면 진입, `click` = 사용자 액션, `local` = API 없음 |
| **API** | 비우면 호출 없음. 경로는 §3.1 Path 그대로 (prefix 없음) |
| **상세** | Request/Response 필드·타입·JSON 예시 → [§3](#3-api-카탈로그). 성공·실패 모두 **`ApiResponse` envelope** (`code`, `message`, `data`) |

### 2.0 `SCR-SPLASH` — 앱 스플래시

| 시점 | 트리거 | API | BE | FE |
| --- | --- | --- | --- | --- |
| mount | 앱 cold start | — | — | 중앙 "동갑내기 과외하기" 로고 (정적) |
| local | 1~1.5초 경과 | — | — | → [§2.0.1](#201-app-entry--앱-진입-라우팅) 라우팅 |

**FE 구현 메모:** API 호출 없음. 디자인 SSOT: `SCR-SPLASH` 일러스트/로고. **앱 최초 실행 시 `deviceUserId`(UUID v4) 생성·로컬 저장** (iOS Keychain / Android EncryptedSharedPreferences) — [§3.0 인증](#30-공통). 스플래시 중 네트워크 프리페치는 선택.

---

### 2.0.1 `APP-ENTRY` — 앱 진입 라우팅

`SCR-SPLASH` 이후 또는 딥링크·포그라운드 복귀 시 실행.

| 시점 | 트리거 | API | BE | FE |
| --- | --- | --- | --- | --- |
| local | 스플래시 중 | — | — | `deviceUserId` 없으면 생성·저장 ([§3.0](#30-공통)) |
| mount | 스플래시 종료 | `GET /user/onboarding/status` | P2 | `Authorization: Bearer {deviceUserId}` — 온보딩 vs 홈 라우팅 |
| mount | 포그라운드 복귀 (홈·수업 중) | `GET /session/status` | P3 | `pendingReward` → 5.0 |
| — | 첫 API 요청 시 (서버) | *(내부)* user upsert | P0 | Bearer UUID → `users.external_id` 조회·없으면 생성 |

```mermaid
flowchart TD
    S[SCR-SPLASH 1~1.5s] --> A[APP-ENTRY]
    A --> B{onboarding/status}
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

**FE 구현 메모 (디자인):** 단계 표기 `1단계: 커리큘럼 선택`. 진단 버튼 `실력 진단` (Secondary — 흰 배경+네이비 테두리). CTA `다음으로` (골드). 칩 선택색 `#FF9438`. 기본 선택 `분수의 계산`. 진단 완료 플래그 없음 — `curriculumId` 없으면 다음 비활성.

**BE 구현 메모:** `user_curriculums` upsert.

---

### 2.2 `SCR-OB02` — 0.2 시간표

| 시점 | 트리거 | API | 순서 | 비고 |
| --- | --- | --- | --- | --- |
| click | [설정 완료 및 홈으로 가기] | **`POST /user/schedule`** | ① | `frequency`, `days[]`, `time` |
| click | ↑ 이어서 | **`POST /notifications/register`** | ② | **schedule 저장 후**. 없으면 400 |
| click | ↑ 이어서 | **`POST /user/onboarding/complete`** | ③ | → SCR-HOME |
| local | 빈도·요일 토글 | — | — | 검증은 저장 시 서버 |

**Request 예 (`schedule`):** `{ "frequency": 3, "days": ["TUE","THU","SAT"], "time": "17:00" }`

**FE 구현 메모 (디자인):** 단계 표기 `2단계: 과외 스케줄`. 완료 CTA 골드 `설정 완료 및 홈으로 가기`. 기본값 빈도=주3회, 요일=화·목·토, 시간=오후 5시. 요일·칩 활성색 `#FF9438`.

---

### 2.3 `SCR-HOME` — 1.0 홈

| 시점 | 트리거 | API | `data` 필드 → UI |
| --- | --- | --- | --- |
| mount | 화면 진입 | **`GET /user/profile`** | `data.level`, `data.totalCoins`, `data.curriculum`, `data.progressPercent`, `data.homeMessage` |
| mount | *(병렬 권장)* | **`GET /session/status`** | `data.lessonCompletedToday` → 과외 팝업, `data.activeSession`, `data.pendingRewardSessionId` |
| mount | *(대안 1-call)* | `GET /user/home` | profile + status 합본 |
| click | [과외하러가기] | — | → SCR-PREP (API 없음) |
| click | ⚙️ | — | → SCR-SETTINGS |
| click | 탭 수업기록 | — | → SCR-HISTORY |
| click | 탭 수업공간 | — | → SCR-PREP |
| local | 캐릭터 터치 (미완료) | — | 팝업 즉시 (2초 타이머 대체) |

**`lessonCompletedToday === true`:** 과외 요청 팝업 미노출. 수업공간 탭으로 **추가 수업** 가능.

**FE 구현 메모 (디자인):** 배경 일러스트(언덕·하늘). 진행률 바 그린 `#49CA34`. 탭 활성 오렌지 `#FF9438` + 하단 언더라인. CTA 골드 `과외하러가기`. 캐릭터·말풍선은 일러스트 SSOT. **동작**은 명세 그대로: 2초 후 과외 팝업, 캐릭터 터치 시 즉시 팝업.

---

### 2.4 `SCR-PREP` — 2.0 준비

| 시점 | 트리거 | API | 비고 |
| --- | --- | --- | --- |
| mount | 화면 진입 | **`GET /session/today`** | → [§3.5](#api-get-session-today) |
| click | [수업 시작하기] (5초 후) | **`POST /session/start`** | `sessionId` 저장 → SCR-LESSON |
| click | [◀ 돌아가기] | — | → SCR-HOME (**confirm 없음**) |
| local | 5초 카운트다운 | — | API 없음 |

**`start` 응답:** `data: { sessionId, startedAt, resumed }` (`code: 201`) — 이후 모든 수업 API에 `{id}=sessionId`.

**FE 구현 메모 (디자인):** 헤더 `◀ 돌아가기`만 (GNB `내 수업공간`은 수업 화면용). 배지 `오늘의 수업` + 타이틀 `sessionTitle`(예: `분수의 세계`) + 토픽 카드 2건. 안내 문구 `💡 설명할 내용을… (약 5초)`. CTA 골드 `수업 시작하기` — 5초 전 `disabled` + `(N)` 카운트다운.

**에러:** `today` 실패 → FE 5초 후 홈. `start` 중복 → 409 또는 `resumed: true` ([§12 S2'](#12-명세-충돌-및-검토-이력) 진행 중 세션 재개).

---

### 2.5 `SCR-LESSON` — 3.0 수업 시작

| 시점 | 트리거 | API | 비고 |
| --- | --- | --- | --- |
| mount | 화면 진입 | **`GET /session/{sessionId}/lesson`** | 질문 말풍선, 힌트 JSON, `topicLabel` |
| **local** | 마이크 1탭 / 2탭 | **— (API 없음)** | Waveform. [§11](#11-ttsstt-음성-연동-후순위) P6+ |
| click | **[설명완료]** | **`POST /session/{sessionId}/advance-phase`** | `currentPhase` → `REACTION` |
| click | [◀] confirm 확인 | **`POST /session/{sessionId}/abort`** | → SCR-HOME |

**FE 구현 메모 (디자인):** GNB 중앙 고정 **`내 수업공간`** (좌 ◀ / 우 ⚙️). API `topicLabel`은 힌트노트 헤더·콘텐츠 컨텍스트용 — GNB 바에 표시하지 않음. AI 말풍선 흰+네이비, 유저 말풍선 노란. 힌트노트 접힘 탭: 갈색 퀘스트북 `힌트노트`. `설명완료` 버튼 그린 `#49CA34`. ◀ 이탈 시 confirm 팝업 (디자인 컴포넌트).

**다음 화면:** SCR-REACTION (`advance-phase` 성공 후).

---

### 2.6 `SCR-REACTION` — 3.0 AI 반응

| 시점 | 트리거 | API | 비고 |
| --- | --- | --- | --- |
| mount | 화면 진입 | **`GET /session/{sessionId}/reaction`** | 오답 말풍선, OX 전. `phase=REACTION` 아니면 403 |
| local | [틀렸어!] / [맞았어!] | — | OX·힌트노트·재설명 UI |
| **local** | 재설명 마이크 1탭 | **— (API 없음)** | [§11](#11-ttsstt-음성-연동-후순위) |
| click | **[설명 종료]** | **`POST /session/complete`** | body: `{ sessionId }` — **정산 1회** |
| click | [◀] confirm | `POST …/abort` | |

**`complete` 성공 후:** → SCR-PRAISE (4.0). 실패 시 토스트 + **동일 `sessionId`로 재시도** (서버 멱등).

**FE 구현 메모 (디자인):** GNB `내 수업공간` (LESSON과 동일). OX 라벨 `틀렸어!` / `맞았어!` — `[맞았어!]` 클릭 차단, `[틀렸어!]`만 분기. 결과 말풍선 그린. `설명 종료` 그린 CTA. 형광펜 300ms 순차·힌트노트 자동 펼침은 클라 only.

---

### 2.7 `SCR-PRAISE` — 4.0 칭찬

| 시점 | 트리거 | API | 비고 |
| --- | --- | --- | --- |
| mount | 화면 진입 | **—** | 정산은 이미 `complete`에서 끝 |
| click | [보상 받기] | — | → SCR-REWARD |

> 명세 4.0 “이미 완료 기록” edge case와 맞추려면 **`complete`는 반드시 이 화면 이전**(SCR-REACTION)에서 호출.

**FE 구현 메모 (디자인):** 배경 컨페티 일러스트. CTA 골드 `보상 받기`. 하단 탭 숨김.

---

### 2.8 `SCR-REWARD` — 5.0 보상

| 시점 | 트리거 | API | 비고 |
| --- | --- | --- | --- |
| mount | 화면 진입 | **`GET /session/{sessionId}/reward`** | → [§3.5.2 RewardDto](#api-reward-dto) |
| mount | `reward` 404 등 | *(선택)* `POST /session/complete` | 이미 COMPLETED면 동일 응답. **일반 경로는 GET만** |
| click | **[확인]** | **`POST /session/{sessionId}/reward/ack`** | → SCR-HOME |
| local | 코인 애니메이션 | — | 서버 값은 `GET /reward` 응답의 `data` |

**재진입:** `GET /session/status` → `pendingRewardSessionId` → 이 화면 mount 시 **`GET /reward`만**.

**FE 구현 메모 (디자인):** 밝은 톤 + 학부모·봉투 일러스트. 코인 `+500` 카운트업. `확인` 그린 `#49CA34`. 배지 레벨업 **조건부** 노출 (`badgeLevelUp`).

---

### 2.9 `SCR-SETTINGS` — 6.0 설정

| 시점 | 트리거 | API | 비고 |
| --- | --- | --- | --- |
| mount | 화면 진입 | **`GET /user/settings`** | 칩·빈도·요일·시간 prefill |
| click | [설정 저장] | **`PUT /user/settings`** | 변경 없으면 no-op |
| click | ↑ (시간표 변경 시) | **`POST /notifications/reschedule`** | PUT 내부 호출 가능 |
| click | [◀ 홈으로] | — | → SCR-HOME |

**단원 변경:** `resetProgress: true` + confirm. `STARTED` 세션 있으면 409 또는 abort ([§3](#3-api-카탈로그)).

**FE 구현 메모 (디자인):** 타이틀 `설정` + 서브 `단원과 과외 시간을 변경할 수 있어요`. 저장 CTA 그린 `설정 저장`. 칩·요일 활성색 `#FF9438`.

---

### 2.10 `SCR-HISTORY` — 7.0 수업기록

| 시점 | 트리거 | API | 비고 |
| --- | --- | --- | --- |
| mount | 화면 진입 | **`GET /sessions/history`** | `cursor`, `size=20` |
| scroll | 하단 도달 | `GET /sessions/history?cursor=…` | `hasMore` |
| local | 빈 상태 | — | `sessions.length === 0` |

**FE 구현 메모 (디자인):** 타이틀 `수업 기록`. 빈 상태 일러스트 + 안내 문구. 카드에 `+500c`·배지 레벨업 뱃지. 하단 탭 3개, 활성 오렌지.

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

§3.1 Path가 **전체 URL 경로**이다 (`/api` prefix·버저닝 없음). 예: `GET /curriculum`.

요청·응답은 **필드명(camelCase)·타입·필수 여부(R/O)**·예시 JSON**으로 고정한다. 구현 시 DTO는 본 절을 그대로 따른다.

> **응답 envelope:** 성공·실패 모두 `ApiResponse<T>` (`code`, `message`, `data`)로 내려간다. 본 절의 **Response `data`** 표·JSON 예시는 **`data` 필드** 구조를 뜻한다. 구현 SSOT: `common/response/ApiResponse.kt`, `SuccessCode`, `ErrorBaseCode`.

### 3.0 공통

**Request headers**

| Header | 필수 | 설명 |
| --- | --- | --- |
| `Authorization` | O | `Bearer {deviceUserId}` — 프로토타입: 기기 UUID v4. 추후 로그인 시 JWT로 **동일 헤더**만 교체 |
| `Content-Type` | POST/PUT | `application/json` |

**인증 (P0 — 기기 UUID, 로그인 없음)**

| 역할 | 규칙 |
| --- | --- |
| **FE (Android/iOS 공통)** | 앱 최초 실행 시 `deviceUserId` = UUID v4 생성 → 로컬 영구 저장. 이후 **모든 API**에 `Authorization: Bearer {deviceUserId}` |
| **FE 저장소** | iOS Keychain / Android EncryptedSharedPreferences (크로스플랫폼: secure storage 라이브러리) |
| **BE** | Bearer 값을 `users.external_id`(UK)로 조회. 없으면 `users` + `user_profiles` **upsert** 후 요청 처리 |
| **내부 PK** | API·FK는 `users.id`(BIGSERIAL). Bearer UUID는 식별자만 담당 |

```
Authorization: Bearer 550e8400-e29b-41d4-a716-446655440000
```

```mermaid
sequenceDiagram
    participant App as FE
    participant Store as 로컬 저장소
    participant BE as BE
    participant DB as users

    App->>Store: deviceUserId 없으면 UUID v4 생성·저장
    App->>BE: API + Authorization Bearer deviceUserId
    BE->>DB: external_id 조회
    alt 신규 기기
        BE->>DB: users + user_profiles INSERT
    end
    BE->>App: ApiResponse
```

| 구분 | `deviceUserId` (Bearer) | `deviceToken` (FCM) |
| --- | --- | --- |
| 용도 | **사용자 식별** | 푸시 알림 |
| 저장 (FE) | Keychain / EncryptedSharedPreferences | FCM SDK |
| 저장 (BE) | `users.external_id` | `device_tokens.token` |

**프로토타입 한계 (허용):** 앱 삭제·재설치 → 새 UUID → 새 사용자(진행도 초기화). 기기 변경 시 데이터 이전 없음.

**추후 로그인:** `Authorization: Bearer {jwt}` — 헤더·FE 인터셉터 구조 유지, 토큰 발급 API만 추가. `external_id`는 OAuth `sub` 등으로 매핑 확장.

**인증 오류**

| HTTP | `code` | 상황 |
| --- | --- | --- |
| 401 | `40110` | `Authorization` 없음·Bearer 형식 아님 |
| 401 | `40000` | UUID 형식 불일치 (도메인 검증) |

**응답 envelope (`ApiResponse<T>`)**

| 필드 | 타입 | R/O | 설명 |
| --- | --- | --- | --- |
| `code` | integer | R | 비즈니스 결과 코드. 성공 `200`/`201`/`202`, 실패 `40xxx`~`50xxx` ([`ErrorBaseCode`](../../src/main/kotlin/org/prography/samsung/backend/common/response/ErrorBaseCode.kt)) |
| `message` | string | R | 사용자·클라이언트용 메시지. 성공 시 `SuccessCode.message`, 실패 시 `ApiCode.message` 또는 `CustomException` override |
| `data` | object \| array \| null | O | **성공 시** DTO payload. `null`이면 JSON에서 **필드 생략** (`@JsonInclude(NON_NULL)`). **실패 시** 생략 |

**성공 응답 예 (`SuccessCode`)**

```json
{
  "code": 200,
  "message": "요청이 성공했습니다.",
  "data": { }
}
```

| `code` | HTTP | 용도 |
| --- | --- | --- |
| `200` | 200 OK | 조회·갱신·일반 성공 (기본) |
| `201` | 201 Created | 리소스 생성 (`POST /session/start` 등) |
| `202` | 202 Accepted | 비동기 접수 (후순위) |

- 배열 응답: `data`가 **배열** (`GET /curriculum` → `data: CurriculumChip[]`).
- body 없는 성공: `data` 생략 또는 `null` (예: 멱등 no-op). **`204 No Content` 미사용** — envelope 일관성 유지.

**에러 응답 예 (`ErrorBaseCode` / 도메인 `ApiCode`)**

```json
{
  "code": 40010,
  "message": "요일을 3개만 골라주세요."
}
```

| HTTP | `code` | `ErrorBaseCode` / 도메인 | 상황 |
| --- | --- | --- | --- |
| 400 | `40000` | `BAD_REQUEST` | 일반 검증 실패 |
| 400 | `40010` | `MISSING_PARAM` | 필수 필드·요일 개수 불일치 (`SCHEDULE_DAY_COUNT_MISMATCH`) |
| 400 | `40020` | `NOT_READABLE` | JSON 파싱 오류 |
| 401 | `40100` | `EXPIRED_TOKEN` | 토큰 만료 |
| 401 | `40110` | `UNAUTHORIZED` | 토큰 없음·인증 실패 |
| 403 | `40300` | `FORBIDDEN` | phase 불일치 (`SESSION_PHASE_MISMATCH`) |
| 404 | `40410` | `NOT_FOUND_ENTITY` | session/user 없음 |
| 409 | `40900` | `CONFLICT` | `SESSION_ALREADY_STARTED`, `SESSION_NOT_STARTED` |
| 500 | `50000` | `INTERNAL_SERVER_ERROR` | 서버 오류 |
| 503 | *(TBD)* | 도메인 `ApiCode` 추가 | (선택) 외부 연동 실패 |

> 도메인 전용 코드(`SCHEDULE_DAY_COUNT_MISMATCH` 등)는 `ApiCode` 구현 enum으로 추가하고, 응답 `code`는 **정수**로 내려간다. `CustomException(errorCode, message?)`로 `message`만 상황별 override 가능.

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

**Response `data` (200):** `CurriculumChip[]`

| 필드 | 타입 | R/O | 설명 |
| --- | --- | --- | --- |
| `id` | integer | R | PK |
| `code` | string | R | `FRACTION_CALC` 등 |
| `name` | string | R | UI 칩 라벨 (예: `분수의 계산`) |
| `displayOrder` | integer | R | 정렬 |

```json
{
  "code": 200,
  "message": "요청이 성공했습니다.",
  "data": [
    {
      "id": 3,
      "code": "FRACTION_CALC",
      "name": "분수의 계산",
      "displayOrder": 3
    }
  ]
}
```

---

### 3.3 Onboarding

#### `GET /user/onboarding/status`

**Response `data` (200)**

| 필드 | 타입 | R/O | 설명 |
| --- | --- | --- | --- |
| `completed` | boolean | R | `true` → 홈 직행 |
| `step` | integer | R | `0` 미시작, `1` 단원 저장됨, `2` 시간표 저장됨 |

```json
{
  "code": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "completed": false,
    "step": 1
  }
}
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

**Response `data` (200)**

| 필드 | 타입 | R/O |
| --- | --- | --- |
| `curriculumId` | integer | R |
| `step` | integer | R |

```json
{
  "code": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "curriculumId": 3,
    "step": 1
  }
}
```

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

**Response `data` (200):** `UserScheduleDto`

| 필드 | 타입 | R/O |
| --- | --- | --- |
| `frequency` | integer | R |
| `days` | `DayOfWeek[]` | R |
| `time` | string | R |

```json
{
  "code": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "frequency": 3,
    "days": ["TUE", "THU", "SAT"],
    "time": "17:00"
  }
}
```

**Errors:** `400` `SCHEDULE_DAY_COUNT_MISMATCH`, `400` `INVALID_LESSON_TIME`

#### `POST /user/onboarding/complete`

| | |
| --- | --- |
| Request body | 없음 (또는 `{}`) |

**Response `data` (200)**

| 필드 | 타입 | R/O |
| --- | --- | --- |
| `onboardingCompleted` | boolean | R | 항상 `true` |

```json
{
  "code": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "onboardingCompleted": true
  }
}
```

**Errors:** `400` `SCHEDULE_NOT_CONFIGURED`

---

### 3.4 Home

#### `GET /user/profile`

**Response `data` (200)**

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
  "code": 200,
  "message": "요청이 성공했습니다.",
  "data": {
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
}
```

#### `GET /session/status`

**Response `data` (200)**

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
  "code": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "lessonCompletedToday": true,
    "activeSession": {
      "sessionId": "550e8400-e29b-41d4-a716-446655440000",
      "status": "STARTED",
      "currentPhase": "REACTION",
      "startedAt": "2026-06-02T14:30:00+09:00"
    },
    "pendingRewardSessionId": null
  }
}
```

#### `GET /user/home` (선택)

**Response `data` (200):** `UserProfileDto` + `SessionStatusDto` 필드를 **한 객체**에 병합.

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

**Response `data` (200)**

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
  "code": 200,
  "message": "요청이 성공했습니다.",
  "data": {
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
}
```

`activeSession`이 있는 예 (`data.activeSession`만 non-null):

```json
{
  "code": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "curriculumId": 3,
    "sessionTitle": "분수의 세계",
    "topics": [ "…" ],
    "activeSession": {
      "sessionId": "550e8400-e29b-41d4-a716-446655440000",
      "status": "STARTED",
      "currentPhase": "INTRO",
      "startedAt": "2026-06-02T14:00:00+09:00"
    }
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

**Response `data` (201)**

| 필드 | 타입 | R/O | 설명 |
| --- | --- | --- | --- |
| `sessionId` | string (UUID) | R | 이후 path `{id}` |
| `startedAt` | `DateTime` | R | |
| `resumed` | boolean | R | `true`: 기존 STARTED 재개, `false`: 신규 생성 |

```json
{
  "code": 201,
  "message": "요청이 성공했습니다.",
  "data": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "startedAt": "2026-06-02T14:30:00+09:00",
    "resumed": false
  }
}
```

**Errors:** `409` `SESSION_ALREADY_STARTED` (재개 정책이 아닌 경우)

**Side effect:** `session_topic_snapshots` 2건 생성 (`GET /today`의 `topics`와 동일 내용).

---

#### `GET /session/{sessionId}/lesson`

| | |
| --- | --- |
| Path | `sessionId` (UUID) |

**Response `data` (200)**

| 필드 | 타입 | R/O | 설명 |
| --- | --- | --- | --- |
| `sessionId` | string | R | |
| `currentPhase` | `SessionPhase` | R | `INTRO` |
| `topicLabel` | string | R | 힌트노트·콘텐츠 컨텍스트 (예: `3. 분수의 개념`). **FE GNB 바는 고정 `내 수업공간`** ([§2.5](#25-scr-lesson--30-수업-시작)) |
| `question` | object | R | |
| `question.bubbleText` | string | R | 말풍선 HTML/plain |
| `hintNote` | object | R | 힌트노트 JSON (§3.5.1 스키마) |

```json
{
  "code": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "currentPhase": "INTRO",
    "topicLabel": "3. 분수의 개념",
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
}
```

**Errors:** `404`, `403` `SESSION_PHASE_MISMATCH` (이미 REACTION만 허용하는 경우는 lesson만 INTRO일 때 COMPLETED 등)

---

#### `POST /session/{sessionId}/advance-phase`

| | |
| --- | --- |
| Request body | 없음 (또는 `{}`) |

**Response `data` (200)**

| 필드 | 타입 | R/O |
| --- | --- | --- |
| `sessionId` | string | R |
| `currentPhase` | `SessionPhase` | R | `REACTION` |

```json
{
  "code": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "currentPhase": "REACTION"
  }
}
```

**Errors:** `409` `SESSION_NOT_IN_INTRO`, `404`

---

#### `GET /session/{sessionId}/reaction`

**Response `data` (200)**

| 필드 | 타입 | R/O | 설명 |
| --- | --- | --- | --- |
| `sessionId` | string | R | |
| `currentPhase` | `SessionPhase` | R | `REACTION` |
| `topicLabel` | string | R | 힌트노트·콘텐츠 컨텍스트 (예: `3. 분수의 덧셈과 뺄셈`). GNB 바는 `내 수업공간` 고정 |
| `question` | object | R | |
| `question.bubbleText` | string | R | 의도적 오답 포함 가능 |
| `question.displayAnswerHtml` | string | O | 강조 오답 (예: `3/10`) |
| `hintNote` | object | R | 형광펜 대상 `highlight: true` |

```json
{
  "code": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "currentPhase": "REACTION",
    "topicLabel": "3. 분수의 덧셈과 뺄셈",
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

**Response `data` (200):** `RewardDto` ([§3.5.2](#352-rewarddto))

**Errors:** `409` `SESSION_NOT_STARTED`, `404`

---

#### `GET /session/{sessionId}/reward`

| | |
| --- | --- |
| Request body | 없음 |

**Response `data` (200):** `RewardDto` — 세션 `status=COMPLETED`일 때만.

**Errors:** `404`, `409` `SESSION_NOT_COMPLETED`

---

#### `POST /session/{sessionId}/reward/ack`

| | |
| --- | --- |
| Request body | 없음 |

**Response `data` (200)**

| 필드 | 타입 | R/O |
| --- | --- | --- |
| `sessionId` | string | R |
| `acknowledged` | boolean | R | `true` |
| `rewardAcknowledgedAt` | `DateTime` | R |

```json
{
  "code": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "acknowledged": true,
    "rewardAcknowledgedAt": "2026-06-02T15:00:00+09:00"
  }
}
```

---

#### `POST /session/{sessionId}/abort`

| | |
| --- | --- |
| Request body | 없음 |

**Response `data` (200)**

| 필드 | 타입 | R/O |
| --- | --- | --- |
| `sessionId` | string | R |
| `status` | `SessionStatus` | R | `ABORTED` |

```json
{
  "code": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "ABORTED"
  }
}
```

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
  "code": 200,
  "message": "요청이 성공했습니다.",
  "data": {
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
}
```

---

### 3.6 Settings & History

#### `GET /user/settings`

**Response `data` (200)**

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

**Response `data` (200):** 갱신된 `GET /user/settings`와 동일 구조 (`data`에 설정 DTO).

**No-op:** 변경 없어도 `200` + 기존 설정을 `data`에 반환 (`ApiResponse` envelope 유지).

#### `GET /sessions/history`

**Query**

| param | 타입 | R/O | default |
| --- | --- | --- | --- |
| `cursor` | string | O | — |
| `size` | integer | O | `20` (max 50) |

**Response `data` (200)**

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
  "code": 200,
  "message": "요청이 성공했습니다.",
  "data": {
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

**Response `data` (200)**

| 필드 | 타입 | R/O |
| --- | --- | --- |
| `registered` | boolean | R |
| `scheduleCount` | integer | R | 등록된 요일×시간 슬롯 수 |

```json
{
  "code": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "registered": true,
    "scheduleCount": 3
  }
}
```

**Errors:** `400` `SCHEDULE_NOT_CONFIGURED`

#### `POST /notifications/reschedule`

**Request body:** `register`와 동일 (전부 O).

**Response `data` (200)**

| 필드 | 타입 | R/O |
| --- | --- | --- |
| `rescheduled` | boolean | R |

```json
{
  "code": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "rescheduled": true
  }
}
```

---

### 3.8 명세 대비 변경

| 명세 | 본 계획 |
| --- | --- |
| `POST /user/reward` | `complete`에 통합 |
| 15개 경로 | §3.1 + §2.11 |
| 응답 `gnbTitle` (구 와이어프레임) | `topicLabel` — GNB UI는 디자인 고정 `내 수업공간` ([§0.4](#04-디자인ui-vs-api-ssot)) |
| 명세·와이어프레임의 flat JSON | `ApiResponse` envelope (`code`, `message`, `data`) — [§3.0](#30-공통) |
| 로그인·회원 API 없음 | P0: 기기 UUID Bearer 인증 — [§3.0 인증](#30-공통) |

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
| `gnb_title` | VARCHAR(100) | | API `topicLabel` / 힌트노트 헤더 (예: `3. 분수의 개념`). FE GNB 바 문구와 무관 |

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
| `external_id` | VARCHAR(255) | UK | P0: `deviceUserId`(UUID v4, Bearer). 추후: 로그인 subject (`sub`) |
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
| P0 | device UUID 인증 필터, users upsert | SCR-SPLASH, APP-ENTRY |
| P1 | curriculum, lesson 시드 | (데이터) |
| P2 | onboarding APIs | OB01, OB02 |
| P3 | session APIs | PREP~REWARD |
| P4 | home, settings, history | HOME, SETTINGS, HISTORY |
| P5 | notifications | OB02, SETTINGS |
| **P6+** | TTS/STT | [§11](#11-ttsstt-음성-연동-후순위) |

---

## 7. 패키지 구조

```
common/response/  # ApiResponse, SuccessCode, ErrorBaseCode, ApiCode
common/exception/ # CustomException
common/auth/      # Bearer UUID 필터, CurrentUser, UserUpsertService
session/          # today, start, lesson, reaction, complete, reward, ack, abort
user/             # profile, onboarding, settings
curriculum/
notification/
```

`DeviceUserAuthFilter` — `Authorization: Bearer {uuid}` 추출 → `users.external_id` upsert → 요청 컨텍스트에 `userId` 주입. 컨트롤러는 `ApiResponse.onSuccess(SuccessCode, data)` / `CustomException`으로 [§3.0](#30-공통) envelope를 반환한다. `SessionCompletionService` — complete 단일 `@Transactional`.

---

## 8. 팀 확인 (잔여)

아래는 **이미 합의된 전제**다. **메인 학습 플로우**에서 아직 열린 항목만 [§14](#14-open-questions-메인-학습-플로우) (6건).

| # | 항목 | 채택 |
| --- | --- | --- |
| 1 | Auth (P0) | `Authorization: Bearer {deviceUserId}` — 기기 UUID + user upsert. 추후 JWT 동일 헤더 |
| 2 | 진행 중 `start` | 재개 우선 / 신규 409 |
| 3 | URL 경로 | 루트 경로 — `/api` prefix·버저닝 없음 |
| 4 | AI/STT | P6+ |

---

## 14. Open Questions (메인 학습 플로우)

**범위:** 홈 진입 → 준비 → 수업(INTRO) → AI 반응(REACTION) → 칭찬 → 보상 → 홈 복귀 ([§2.3](#23-scr-home--10-홈)~[§2.8](#28-scr-reward--50-보상), [§5.1](#51-세션-플로우-전체)).  
온보딩·설정·푸시·Figma·API 네이밍 등은 **본 절에서 다루지 않는다** (별도 기획/§8 전제).

기획 확인 후 **결정** 열을 채우고 §2·§3·§12를 갱신한다.

```mermaid
flowchart LR
  SPLASH[SCR-SPLASH] --> HOME[SCR-HOME]
  HOME --> PREP[SCR-PREP]
  PREP --> LESSON[SCR-LESSON INTRO]
  LESSON --> REACTION[SCR-REACTION]
  REACTION --> PRAISE[SCR-PRAISE]
  PRAISE --> REWARD[SCR-REWARD]
  REWARD --> HOME
```

### 14.1 핵심 확인 (6건)

| ID | 단계 | 확인할 것 | 명세 vs 계획 (요약) | 결정 (TBD) |
| --- | --- | --- | --- | --- |
| **MQ-1** | REACTION → 4.0 → 5.0 | **`POST /session/complete`를 어느 화면에서 호출하는가?** 그 시점에 코인·진척·`COMPLETED`가 확정되는가? | **2026-06-06 명세 갱신 후 일치:** REACTION [설명 종료] → `complete`, 5.0 → `GET /reward` + `ack` (§12 S2) | **합의** — 명세·계획 동일 |
| **MQ-2** | 4.0 / 5.0 / APP-ENTRY | **앱 강종·재진입** 시 칭찬·보상·홈 중 어디까지 다시 보여주는가? | 명세 4.0: 완료 기록 후 강종 → **칭찬/보상 재노출 안 함**, 홈 직행. 명세 5.0: 미수령 시 **보상 재노출 또는** 홈에서 코인 자동 반영. 계획: `pendingRewardSessionId` → REWARD | |
| **MQ-3** | PREP / APP-ENTRY | **`POST /session/start` 시점**과 **재진입 화면** | 계획: 준비 [수업 시작하기]에서 `start`. `activeSession` 있을 때 **PREP / LESSON / REACTION** 중 어디로 보낼지 명세에 없음. §8: 재개 우선 — FE 라우팅 규칙만 미정 | |
| **MQ-4** | PREP → LESSON → REACTION | **준비 2토픽과 수업 phase 매핑** | `GET /session/today`는 토픽 2건. 계획 가정: seq1→`GET /lesson`(INTRO), seq2→`GET /reaction`. 명세에 **고정 매핑 문구 없음**. 당일 2회차 수업 시 `today` 내용 **동일/변경**도 함께 확정 필요 | |
| **MQ-5** | HOME → PREP (당일 2회차) | **당일 첫 수업 완료 후** 추가 수업 | 명세: `lessonCompleted` 시 팝업만 숨기고 **수업공간→준비 가능**. 계획: `lessonCompletedToday` + `COMPLETED` 다회. 2회차 `start`는 **신규 UUID**인지, `today`/`topics` 동일인지 | |
| **MQ-6** | LESSON / REACTION | **`abort` 이후** 같은 날 다시 수업 | 명세: confirm 후 홈. 계획: `ABORTED`만 기록. **진척·코인·`lessonCompletedToday` 불변**인지, PREP에서 새 `start` 허용인지 | |

### 14.2 계획서에 이미 적어 둔 전제 (질문 아님)

구현 시 아래를 **기본값**으로 두고, MQ-1~2에서 기획이 다르게 정하면 이 블록을 §12 결정으로 옮긴다.

| 전제 | 내용 |
| --- | --- |
| 음성 | 마이크·OX·힌트노트 펼침은 **클라 only** — 서버 phase는 `INTRO` / `REACTION`만 ([§11](#11-ttsstt-음성-연동-후순위)) |
| REACTION UI | [설명 종료]는 명세상 **재설명(로컬) 후** 노출 — 서버는 `phase=REACTION`만 검증, 마이크 완료 여부는 검증 안 함 |
| 보상 금액 | 프로토 **500코인 고정** (`complete` 트랜잭션) |
| 진척·배지 | `complete` 시 `progressPercent` **+45 cap 100**, 배지는 `completed_session_count` 기준 ([§4.4](#44-tutoring_sessions-상태비즈니스-규칙)) — **2회차·레벨업 팝업 조건**은 MQ-1·5와 함께 확정 |
| `start` 중복 | 동일 `STARTED` 재호출 → **`resumed: true`** (§8 #2). 409는 **재개 정책이 아닐 때**만 |

### 14.3 결정 기록

| MQ-ID | 결정 요약 | 날짜 | §12 ID |
| --- | --- | --- | --- |
| MQ-1 | `complete` = SCR-REACTION [설명 종료]. 5.0 = GET reward + ack | 2026-06-06 | S2 |

> **MQ-2** 정리 후 칭찬(4.0) 재노출 vs `pendingRewardSessionId` 우선순위를 §2.7·§2.8에 확정 문구로 반영한다.

---

## 9. 관련 문서

- [개발자 기능 명세서](../reference/product/developer-feature-spec.html) — 디자인 핸드오프 반영 (2026-06-06)
- [디자인 vs 명세 차이 (이력)](../reference/product/design-vs-feature-spec-diff.md)
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
| S12 | API `gnbTitle` → `topicLabel` (FE GNB `내 수업공간` 고정, 2026-06-06) |
| S13 | `SCR-SPLASH` 추가 — API 없음, APP-ENTRY 전 표시 |
| S14 | 응답 envelope `ApiResponse` (`code`, `message`, `data`) — `ErrorBaseCode` 정수 코드, DTO는 `data`에 래핑 |
| S15 | P0 인증: `Authorization: Bearer {deviceUserId}` — FE 기기 UUID, BE `users.external_id` upsert. 추후 JWT 동일 헤더 |
| S16 | URL `/api` prefix·버저닝 없음 (루트 경로). Request/Response JSON **camelCase** (DB snake_case 분리) |

---

## 13. Figma·디자인 변경 시 수정 가이드

### 13.0 2026-06-06 디자인 핸드오프 반영 요약

| 변경 | API·DB 영향 | 문서 반영 |
| --- | --- | --- |
| `SCR-SPLASH` 추가 | 없음 (클라 only) | §1, §2.0 |
| GNB 수업 화면 = `내 수업공간` | `gnbTitle` → API `topicLabel` (DB `gnb_title` 유지) | §2.5, §2.6, §3.5 |
| PREP 뒤로가기 confirm 제거 | 없음 | §2.4 |
| OX 카피 `틀렸어!` / `맞았어!` | 없음 | §2.6 |
| 설정 저장 `설정 저장` (그린) | 없음 | §2.9 |
| 디자인 토큰 (골드·오렌지·네이비·그린) | 없음 | §0.4, §2 `FE 구현 메모` |
| 온보딩 단계 카피 `N단계: …` | 없음 | §2.1, §2.2 |

상세 UI diff: [design-vs-feature-spec-diff.md](../reference/product/design-vs-feature-spec-diff.md)

### 13.1 원칙

1. **화면 단위는 `SCR-*`를 바꾸지 않는다.** 레이아웃·카피만 바뀌면 §2 `FE 구현 메모`와 [§0.4](#04-디자인ui-vs-api-ssot) 토큰만 수정.
2. **새 화면**이 생기면 §1에 행 추가 → §2에 절 추가 → (API 있으면) §3.1 인덱스 추가.
3. **버튼이 API를 호출**하게 바뀌면 §2 표에 `click` 행 추가/수정.
4. **비주얼만 변경** (색·폰트·일러스트) → API·DB·§3 **수정하지 않음**.
5. **음성** 연동은 §11만 확장하고 §2.5~2.6의 `local`→`click` 전환.

### 13.2 체크리스트 (디자인 핸드오프 시)

- [x] `SCR-SPLASH` §1·§2 반영 (2026-06-06)
- [x] 디자인 토큰 §0.4 정리
- [x] 화면별 `FE 구현 메모` §2 반영
- [ ] Figma 프레임명 = §1 `SCR-*` 와 일치?
- [ ] 각 Interactive 컴포넌트가 §2 `click` 행과 1:1?
- [ ] 신규 모달/바텀시트 → mount API 필요 여부 §2에 반영?
- [ ] §2.11 매트릭스 열/행 갱신?
- [ ] §3 Request/Response 필드 변경 시 §2 “Response 사용처” 갱신?
- [ ] DB 컬럼 필요 시 §4 + Flyway 버전 추가?

### 13.3 수정 위치 빠른 찾기

| 바꾸는 것 | 수정 절 |
| --- | --- |
| 스플래시·앱 진입 | [§2.0](#20-scr-splash--앱-스플래시), [§2.0.1](#201-app-entry--앱-진입-라우팅) |
| 디자인 토큰·테마 | [§0.4](#04-디자인ui-vs-api-ssot) |
| 홈 헤더·코인·레벨·일러스트 | [§2.3](#23-scr-home--10-홈), [§3.4](#34-home) |
| 수업 준비 카운트다운·뒤로가기 | [§2.4](#24-scr-prep--20-준비) |
| GNB `내 수업공간`·퀘스트북 | [§2.5](#25-scr-lesson--30-수업-시작), [§3.5](#35-session) `topicLabel` |
| 마이크·Waveform | [§2.5](#25-scr-lesson--30-수업-시작), [§11](#11-ttsstt-음성-연동-후순위) |
| OX·재설명·맞았어 차단 | [§2.6](#26-scr-reaction--30-ai-반응) |
| 보상 금액·배지·일러스트 | [§2.8](#28-scr-reward--50-보상), `complete` 트랜잭션, `badge_levels` 시드 |
| 설정 칩·시간표 | [§2.9](#29-scr-settings--60-설정) |
| 기록 카드 | [§2.10](#210-scr-history--70-수업기록) |

### 13.4 Figma 링크 기록 (TODO)

| SCR-ID | Figma URL | node-id | 최종 동기화일 |
| --- | --- | --- | --- |
| SCR-SPLASH | *TBD* | | 2026-06-06 (SVG 핸드오프) |
| SCR-HOME | *TBD* | | 2026-06-06 |
| SCR-LESSON | *TBD* | | 2026-06-06 |
| SCR-REACTION | *TBD* | | 2026-06-06 |
| … | | | |

디자인 URL 확정 시 위 표만 채우면 코드·문서 추적이 유지된다.