# 세션 관리 및 오류 진단 문서

> 기준일: 2026-08-09
>
> 범위: `tutoring_sessions`의 생성·재개·페이즈 전환·AI 대화·완료·보상·중단·이력 조회와 이 흐름에 연결된 오류 계약

## 1. 문서 목적

이 문서는 현재 구현을 기준으로 세션 API의 정상 흐름과 오류 발생 조건을 정리한다. 특히 운영 로그에 나타나는 400, 403, 404, 409, 500 응답을 “인증 문제”, “세션 소유권 문제”, “현재 상태 문제”, “페이즈/모드 계약 문제”, “서버 결함”으로 빠르게 구분하는 데 사용한다.

런타임의 기준은 다음 코드와 마이그레이션이다.

- `session/controller/SessionController.kt`
- `session/service/SessionService.kt`
- `session/service/SessionCompletionService.kt`
- `conversation/service/TeachService.kt`
- `common/response/DomainErrorCode.kt`
- `src/main/resources/db/migration/V4__create_session_tables.sql`

기존 Wiki의 세션 완료 결정은 단일 트랜잭션과 순차 재호출 멱등성의 배경으로 참고하되, 현재 코드와 불일치하는 토픽 시퀀스 설명은 이 문서의 현재 구현 분석을 우선한다.

## 2. 현재 상태 모델

### 2.1 상태와 페이즈

```text
POST /session/start
  └─ 새 세션: STARTED + INTRO
       ├─ GET /session/{id}/lesson       정상
       ├─ POST /session/{id}/advance-phase
       │    └─ STARTED + REACTION
       │         ├─ GET /session/{id}/reaction 정상
       │         └─ POST /session/complete      현재 구현상 정상
       └─ POST /session/{id}/abort
            └─ ABORTED + currentPhase=null

POST /session/complete
  └─ COMPLETED + currentPhase=null + rewardStatus=GRANTED
       ├─ GET /session/{id}/reward       정상
       └─ POST /session/{id}/reward/ack  ACKNOWLEDGED
```

| 필드 | 현재 값 | 의미 | 주의점 |
|---|---|---|---|
| `status` | `STARTED`, `COMPLETED`, `ABORTED` | 세션 생명주기 | `STARTED` 하나만 존재한다는 DB 제약이 없다. |
| `currentPhase` | `INTRO`, `REACTION` | 화면/API 진입 단계 | DB 컬럼은 nullable이다. `STARTED + null`을 방어하는 불변식이 없다. |
| `conversationMode` | `static`, `ai_loop` | 대화 처리 경로 | `teach`만 `ai_loop`를 강제하고, lesson/reaction/advance는 모드를 검사하지 않는다. |
| `rewardStatus` | `GRANTED`, `ACKNOWLEDGED` | 보상 확인 여부 | 완료 시 `GRANTED`, 확인 시 `ACKNOWLEDGED`로 바뀐다. |

### 2.2 세션 시작과 재개

`SessionService.start()`는 먼저 사용자에게 `STARTED` 세션이 있는지 조회한다.

- 있으면 요청의 `lessonTopicId`, `curriculumId`, `conversationMode`를 다시 검증하거나 적용하지 않고 기존 세션을 `resumed=true`로 반환한다.
- 없으면 현재 선택 커리큘럼과 요청 토픽을 검증한 뒤 `STARTED + INTRO` 세션과 `sequence=1` 스냅샷을 생성한다.
- 따라서 HTTP 201만으로 “새 세션이 생성됐다”고 판단하면 안 된다. 응답의 `data.resumed`와 `sessionId`를 반드시 사용해야 한다.
- 재개 응답에는 `currentPhase`가 없으므로, 재개된 세션이 이미 `REACTION`인 경우 클라이언트가 `/lesson`을 바로 호출하면 40310을 받을 수 있다.

### 2.3 모드별 흐름

| 모드 | 시작 요청 | 허용된 대화 API | 현재 구현상 주의점 |
|---|---|---|---|
| `static` | `conversationMode` 생략 또는 `static` | `/lesson` → `/advance-phase` → `/reaction` | `/teach` 호출 시 40320 |
| `ai_loop` | `conversationMode=ai_loop` | `/lesson` → `/teach`, `/teach/status` | teach는 `STARTED`와 모드만 검사하며 `currentPhase`나 `sessionDone`은 검사하지 않는다. |

## 3. API 계약과 오류 매트릭스

공통 응답은 `{ "code": businessCode, "message": ..., "data": ... }` 형식이다. `GlobalExceptionHandler`는 `CustomException`의 HTTP 상태와 business code를 그대로 응답한다.

### 3.1 세션 API

| API | 정상 조건 | 주요 오류 | 클라이언트 주의 |
|---|---|---|---|
| `GET /session/status` | 인증된 사용자 | 보통 200 | `activeSession.currentPhase`로 재개 화면을 결정한다. 단, 현재 구현은 null phase를 INTRO로 보정한다. |
| `GET /session/today` | 사용자 커리큘럼 존재 | 40080 | 토픽 목록과 활성 세션을 함께 받는다. 활성 세션의 토픽과 현재 커리큘럼 토픽을 혼동하지 않는다. |
| `POST /session/start` | 사용자·커리큘럼·토픽이 유효 | 40080, 40092, 40410, 40440, 40010/40020 | 활성 세션이 있으면 요청 파라미터는 무시되고 `resumed=true`가 된다. |
| `GET /session/{id}/lesson` | 소유 세션이 `STARTED + INTRO`, 스냅샷/INTRO 콘텐츠 존재 | 40310, 40420, 40440, 40930 | `REACTION` 세션에서 호출하면 40310이다. |
| `POST /session/{id}/advance-phase` | 소유 세션이 `STARTED + INTRO` | 40420, 40930, 40940 | 중복 클릭·재시도 시 두 번째 호출은 40940이다. |
| `GET /session/{id}/reaction` | 소유 세션이 `STARTED + REACTION`, 콘텐츠 존재 | 40310, 40420, 40440, 40930 | `INTRO`에서 호출하면 40310이다. |
| `POST /session/complete` | 요청 body의 sessionId가 유효 | 40010/40020, 40080, 40420, 40440, 40930 | 현재 구현은 phase, `ai_loop`의 `sessionDone`을 검증하지 않는다. |
| `GET /session/{id}/reward` | 소유 세션이 `COMPLETED` | 40410, 40420, 40950 | 완료 전 조회는 40950이다. |
| `POST /session/{id}/reward/ack` | 소유 세션이 `COMPLETED` | 40420, 40410, 40950 | 이미 ACKNOWLEDGED여도 별도 거부 없이 다시 성공한다. |
| `POST /session/{id}/abort` | 소유 세션이 `STARTED` | 40420, 40930 | 완료/중단 세션을 다시 중단할 수 없다. |
| `GET /sessions/history` | 인증된 사용자, 유효한 cursor | 50000 가능 | 잘못된 Base64/Instant cursor가 400이 아니라 예외로 전파될 수 있다. `size`는 1~50으로 보정된다. |
| `POST /session/{id}/teach` | 소유 세션이 `STARTED + ai_loop`, 입력 1~500자 | 40010/40020, 40090, 40320, 40420, 40430, 40930, 40970 | LLM 호출은 DB 트랜잭션 안에서 실행된다. 중복 요청용 idempotency key가 없다. |
| `GET /session/{id}/teach/status` | 소유 세션이 `STARTED + ai_loop` | 40320, 40420, 40430, 40930 | 완료된 세션의 teach 상태는 조회할 수 없다. |

### 3.2 오류 코드별 의미

#### 400 — 요청 또는 선행 설정 오류

| 코드 | 이름 | 발생 지점 | 조치 |
|---:|---|---|---|
| 40000 | `BAD_REQUEST` | 공통 기본 오류 | 요청 계약을 확인한다. |
| 40010 | `MISSING_PARAM` | `@Valid` 검증 실패 | 필수 body/필드를 확인한다. `SessionCompleteRequest.sessionId`, `TeachRequest.userText` 등이 대상이다. |
| 40020 | `NOT_READABLE` | JSON/body 역직렬화 실패 | enum 값, 타입, JSON 문법, Content-Type을 확인한다. |
| 40040 | `INVALID_DEVICE_USER_ID` | 인증 필터의 Bearer UUID 검증 실패 | Authorization Bearer 값이 UUID인지 확인한다. 인증 실패지만 현재 코드상 400이다. |
| 40050 | `SCHEDULE_DAY_COUNT_MISMATCH` | 온보딩/설정의 요일 수와 frequency 불일치 | frequency와 선택 요일 수를 맞춘다. |
| 40060 | `INVALID_LESSON_TIME` | 시간 파싱/허용 시간 검증 실패 | 허용된 정각을 사용한다. 현재 `ScheduleValidator`는 frequency 값 오류도 이 코드로 반환해 메시지가 실제 원인과 어긋날 수 있다. |
| 40070 | `SCHEDULE_NOT_CONFIGURED` | 설정 조회 또는 온보딩 완료 전 | 시간표 설정을 먼저 완료한다. 세션 시작 자체보다 온보딩 선행 조건이다. |
| 40080 | `CURRICULUM_NOT_SELECTED` | `/session/today`, `/session/start`, 완료 보상 정산 | 사용자 커리큘럼을 먼저 선택한다. |
| 40090 | `TEACH_EMPTY_USER_TEXT` | 공백 제거 후 teach 입력이 빈 문자열 | 빈 발화를 보내지 않는다. |
| 40091 | `SCHEDULE_DUPLICATE_DAY` | 시간표 요일 중복 | 중복 요일을 제거한다. |
| 40092 | `CURRICULUM_MISMATCH` | start의 curriculum/topic이 사용자 선택과 다름 | 현재 선택 커리큘럼에 속한 토픽만 요청한다. |

#### 403 — 인증이 아니라 세션 계약 위반일 수 있음

| 코드 | 이름 | 실제 조건 | 오해하기 쉬운 점 |
|---:|---|---|---|
| 40300 | `FORBIDDEN` | 공통 기본 권한 오류 | 로그에 userId가 채워진 뒤 403이면 먼저 도메인 code를 확인한다. |
| 40310 | `SESSION_PHASE_MISMATCH` | 요청 API의 기대 phase와 `currentPhase`가 다름 | `/lesson`은 INTRO, `/reaction`은 REACTION만 허용한다. 기존 운영 로그의 반복 403은 이 코드일 가능성이 높다. |
| 40320 | `TEACH_SESSION_NOT_AI_LOOP` | STATIC 세션에서 `/teach` 호출 | start 응답의 실제 `conversationMode`를 확인한다. 활성 세션 재개 시 요청 모드가 무시될 수 있다. |

#### 404 — 세션/콘텐츠 식별자 또는 소유권 문제

| 코드 | 이름 | 조건 |
|---:|---|---|
| 40410 | `USER_NOT_FOUND` | 사용자/프로필/커리큘럼 정산 데이터가 없음 |
| 40420 | `SESSION_NOT_FOUND` | sessionId가 없거나 현재 userId 소유가 아님 |
| 40430 | `CURRICULUM_NOT_FOUND` | AI loop 세션에서 연결된 curriculum unit을 찾지 못함 |
| 40440 | `LESSON_TOPIC_NOT_FOUND` | 선택 토픽, 세션 스냅샷, phase별 질문 또는 힌트 노트가 없음 |

`findByUserIdAndId(userId, sessionId)`를 사용하므로 다른 사용자의 sessionId를 호출해도 40420으로 보인다. 이것은 IDOR를 피하기 위한 의도된 동작이다.

#### 409 — 상태 충돌 또는 작업 한도

| 코드 | 이름 | 발생 조건 | 조치 |
|---:|---|---|---|
| 40920 | `SESSION_ALREADY_STARTED` | 현재 구현에서 실제 사용 지점이 확인되지 않음 | start는 409가 아니라 기존 세션을 201/resumed=true로 반환한다. 클라이언트가 이 코드를 전제로 하면 안 된다. |
| 40930 | `SESSION_NOT_STARTED` | 세션이 COMPLETED/ABORTED이거나 STARTED가 아님 | status API로 활성 세션을 다시 확인한다. |
| 40940 | `SESSION_NOT_IN_INTRO` | REACTION에서 advance-phase 재호출 | 중복 클릭을 성공으로 간주하거나 현재 phase에 맞게 화면을 복구한다. |
| 40950 | `SESSION_NOT_COMPLETED` | 완료 전 reward 조회/확인 | complete 성공 후 reward 화면으로 이동한다. |
| 40960 | `ACTIVE_SESSION_EXISTS` | 활성 세션 중 커리큘럼 변경 | 먼저 abort 또는 complete 후 설정을 변경한다. |
| 40970 | `TEACH_TURN_LIMIT_EXCEEDED` | 최대 턴 또는 최대 LLM 호출 수 초과 | 추가 teach를 중단하고 완료/종료 흐름으로 이동한다. |

#### 500 — 현재 도메인 오류로 변환되지 않는 위험

`GlobalExceptionHandler`는 `DataAccessException`과 예상하지 못한 예외를 모두 50000으로 응답한다. 다음 조건은 현재 구현상 409나 400으로 명확히 변환되지 않을 수 있다.

- 동시에 여러 `STARTED` 세션이 생성되어 단일 결과를 기대하는 repository query가 여러 행을 반환하는 경우
- `@Version` 충돌이 발생한 경우
- 잘못된 history cursor가 `Base64.getUrlDecoder()` 또는 `Instant.parse()`에서 실패하는 경우
- `STARTED` 세션의 `currentPhase`가 null이거나 완료 데이터가 불완전한 경우
- 완료 중 프로필/커리큘럼/ledger 정합성 문제가 발생하는 경우

## 4. 운영 로그 진단 절차

### 4.1 403이 반복될 때

1. access log의 HTTP status만 보지 말고 응답 body의 `code`를 확인한다.
2. `40310`이면 DB에서 다음 값을 확인한다.

```sql
SELECT id, user_id, status, current_phase, conversation_mode,
       lesson_topic_id, started_at, completed_at, version
FROM tutoring_sessions
WHERE id = '<sessionId>';
```

3. `current_phase=REACTION`이면 `/lesson` 대신 `/reaction`으로 이동한다.
4. `current_phase=null`이면 상태 데이터 불변식 위반이다. `/session/status`가 null을 INTRO로 숨길 수 있으므로 DB 값을 직접 확인한다.
5. `40320`이면 start 응답 또는 status API의 실제 `conversationMode`가 `ai_loop`인지 확인한다.

### 4.2 400이 발생할 때

- 40080: onboarding/curriculum 선택 여부 확인
- 40092: 요청의 curriculumId와 lessonTopicId가 현재 선택 커리큘럼에 속하는지 확인
- 40040: Bearer 값이 UUID인지 확인
- 40010/40020: body 누락·JSON 타입·enum 직렬화·필드 validation 확인
- 40090: `userText.trim()` 결과가 빈 문자열인지 확인

### 4.3 409가 발생할 때

409는 대개 인증이나 sessionId 오타가 아니라 “현재 상태에서 작업 순서가 맞지 않음”을 의미한다. 클라이언트가 무조건 재시도하면 같은 오류를 반복할 수 있으므로 `GET /session/status`로 현재 세션과 phase를 갱신한 뒤 분기한다.

## 5. 위험 레지스터

등급은 현재 장애 가능성과 데이터 손실/비즈니스 우회 가능성을 함께 고려했다.

| ID | 등급 | 상태 | 근거 | 영향 |
|---|---|---|---|---|
| R1 | 높음 | 확인됨 | `start()`가 활성 세션 조회 후 생성하지만 `V4`에 사용자별 `STARTED` partial unique 제약이 없다. | 동시 start로 여러 활성 세션이 생기고, 이후 단일 결과 query가 500 또는 비결정적 결과가 될 수 있다. |
| R2 | 높음 | 확인됨 | 기존 STARTED 세션을 재개할 때 요청 topic/curriculum/mode를 무시하고 `resumed=true`만 반환한다. | 사용자가 선택한 모드와 실제 세션 모드가 달라져 40320이 발생하거나, 이미 REACTION인 세션에 `/lesson`을 호출해 40310이 발생한다. |
| R3 | 높음 | 확인됨 | `complete()`는 STARTED 여부만 검사하고 `currentPhase=REACTION` 또는 AI loop `sessionDone`을 검사하지 않는다. | INTRO에서 조기 완료하거나, AI loop 대화가 끝나지 않았는데 보상·진척도를 지급할 수 있다. |
| R4 | 높음 | 확인됨 | `teach()`는 외부 LLM 호출을 포함하지만 요청 idempotency key가 없고, duplicate turn은 `(session_id, turn_number)` 유니크 제약에 의존한다. | 재시도는 새 턴으로 중복 저장될 수 있고, 동시 요청은 unique/optimistic lock 예외로 500이 될 수 있다. |
| R5 | 중간 | 확인됨 | `TutoringSession`에 `@Version`은 있지만 충돌 예외를 도메인 409로 변환하지 않는다. advance/abort/teach/complete 경합은 generic 500으로 노출될 수 있다. | 사용자는 실제로는 “이미 다른 요청이 반영됨”인데 서버 오류로 인식하고 재시도 폭주할 수 있다. |
| R6 | 중간 | 확인됨 | `currentPhase` DB 컬럼과 엔티티 필드가 nullable이며 `toActiveSession()`은 null을 INTRO로 보정한다. | status API는 INTRO라고 말하지만 lesson API는 40310을 반환하는 모순된 계약이 생긴다. |
| R7 | 중간 | 확인됨 | start는 세션을 만들고 스냅샷만 생성한다. 질문·힌트·AI curriculum unit 존재 검증은 lesson/teach 시점까지 늦춰져 있다. | start는 201인데 화면 진입에서 40440/40430이 발생한다. 운영에서는 생성 성공과 콘텐츠 준비 실패를 분리해 추적해야 한다. |
| R8 | 중간 | 확인됨 | history cursor decode가 입력 예외를 잡지 않고 generic handler로 보낸다. | 사용자가 cursor를 변조하거나 잘린 cursor를 재사용하면 400 대신 500이 발생한다. |
| R9 | 중간 | 확인됨 | static/ai_loop에 대한 phase와 mode 조합 검사가 endpoint마다 다르다. teach만 AI loop를 강제하고 lesson/reaction/advance는 mode를 검사하지 않는다. | 클라이언트가 잘못된 조합을 호출해도 어느 API는 진행되고 어느 API는 40320을 반환해 계약 이해가 어렵다. |
| R10 | 중간 | 요구사항 확인 필요 | 현재 구현과 기존 결정은 당일 여러 COMPLETED 세션을 허용하고 `lessonCompletedToday`는 화면용 플래그로만 사용한다. | “하루 한 수업”이 제품 요구라면 중복 수업/중복 보상이 허용되는 정책 결함이다. “재수업 허용”이 요구라면 현재 동작을 API 문서에 명시해야 한다. |
| R11 | 낮음 | 확인됨 | `SESSION_ALREADY_STARTED(40920)`가 선언되어 있으나 start의 실제 경로에서는 사용되지 않는다. | API 문서나 클라이언트가 40920을 기대하면 실제 201/resumed 동작과 어긋난다. |
| R12 | 낮음 | 확인됨 | 기존 Wiki 결정은 INTRO/REACTION 토픽 시퀀스를 1/2로 설명하지만 현재 `SNAPSHOT_SEQUENCE=1`만 생성·조회한다. | 운영자와 개발자가 오래된 설계 문서를 보고 잘못된 DB 조회나 수정 작업을 할 수 있다. |
| R13 | 낮음 | 확인됨 | `SessionCompletionService`의 이미 COMPLETED 분기에서 `userProfileRepository.findById(userId).orElseThrow()`를 사용한다. | 데이터가 일부 유실된 legacy 세션이면 표준 CustomException이 아닌 generic 500이 된다. |
| R14 | 중간 | 관측성 부족 | access log는 endpoint/status/duration을 남기지만 business code, session phase, mode, transition 결과는 남기지 않는다. | 동일한 403/409의 원인을 로그만으로 구분하기 어렵고, 현재 제공된 로그처럼 반복 403을 응답 body/DB와 다시 대조해야 한다. |

## 6. 우선순위별 개선 방향

### P0 — 장애를 정확히 분류

- `RequestLoggingFilter` 완료 로그 또는 별도 도메인 로그에 `businessCode`, `sessionId`, `status`, `currentPhase`, `conversationMode`, `resumed`를 포함한다.
- 40310/40320/40930/40940/40970을 endpoint별 metric counter로 집계한다.
- 운영 DB에서 `STARTED` 중복과 `STARTED + current_phase IS NULL`을 점검한다.

### P1 — 상태와 동시성 불변식 고정

- PostgreSQL에 사용자당 `status='STARTED'` partial unique index를 추가하고, 경합으로 발생한 unique violation을 기존 세션 재조회 또는 명시적 409로 변환한다.
- `STARTED`인 세션의 `current_phase`를 NOT NULL로 보장한다. 기존 null 데이터를 먼저 정리한 뒤 제약을 추가해야 한다.
- 상태 전이를 `expectedStatus/expectedPhase` 조건부 update 또는 명시적 lock으로 보호하고, optimistic lock 충돌을 재시도 가능한 409 계약으로 변환한다.

### P1 — 완료 조건을 명시

- static: `STARTED + REACTION`에서만 complete 허용
- ai_loop: 제품 규칙에 따라 `sessionDone=true` 또는 명시적 종료 버튼을 완료 조건으로 정의
- 완료 중복 요청은 현재의 순차 멱등성뿐 아니라 동시 요청 결과도 검증한다.

### P2 — 재개 API 계약 정리

- start 재개 응답에 `currentPhase`, `conversationMode`, `lessonTopicId`를 포함한다.
- 요청 파라미터를 무시할 경우 API 문서에 “활성 세션이 있으면 모든 start 선택값을 무시한다”고 명시한다.
- 또는 요청과 기존 세션이 충돌하면 409를 반환해 사용자가 명시적으로 abort/continue를 선택하게 한다.

### P2 — 입력·콘텐츠·이력 오류 정리

- start 시 필요한 phase별 질문/힌트와 AI loop curriculum unit을 사전 검증하거나, 콘텐츠 준비 실패를 별도 business code로 구분한다.
- history cursor의 Base64/Instant 파싱 오류를 400 business code로 변환한다.
- `ScheduleValidator`에서 frequency 오류와 lesson time 오류를 별도 코드로 분리하거나 현재 40060 동작을 문서화한다.

### P3 — 재현 테스트 보강

- 동일 user의 동시 `/session/start`가 활성 세션 하나만 남기는지 검증한다.
- 기존 세션이 REACTION일 때 start 재개 응답과 `/lesson` 호출 결과를 검증한다.
- INTRO에서 complete, `ai_loop`에서 teach 전 complete, 잘못된 mode/phase 조합을 검증한다.
- teach 동일 요청 재시도와 동시 요청의 저장 결과를 검증한다.
- advance/abort/complete/teach의 경합과 optimistic lock 응답을 검증한다.
- null phase active session, malformed cursor, missing content/unit을 검증한다.

## 7. 현재 테스트 커버리지와 공백

현재 확인된 테스트는 다음 정상/순차 오류를 보호한다.

- `SessionApiIntegrationTest`: start resume, full static flow, INTRO에서 reaction 조회 시 40310, abort, 완료 전 reward 40950
- `SessionServiceTest`: curriculum/topic mismatch 40092, INTRO→REACTION 전환, REACTION에서 중복 advance 40940, abort, pending reward
- `SessionCompletionServiceTest`: STARTED 완료, 순차 중복 complete, ABORTED 완료 거부, progress 100 cap
- `TeachApiIntegrationTest`: static에서 teach 40320, 빈 userText 40090

반면 다음은 현재 테스트로 고정되어 있지 않거나 일부 `@Disabled` 상태다.

- 동시 start로 인한 활성 세션 중복
- 재개 시 요청 모드/토픽 무시
- INTRO 조기 완료 및 AI loop 미완료 완료
- teach 네트워크 재시도/동시 요청/LLM 호출 중 트랜잭션 유지
- malformed history cursor
- `STARTED + currentPhase=null`
- start 성공 후 phase별 콘텐츠 누락
- optimistic lock/DataAccessException의 HTTP 계약

## 8. 현재 운영 로그 사례와의 연결

다음 패턴은 “인증 실패”보다 “재개된 세션의 phase 불일치”를 우선 의심해야 한다.

```text
POST /session/start                 201
GET  /session/{same-session}/lesson 403 반복
```

신규 세션이라면 start에서 `currentPhase=INTRO`로 저장하므로 즉시 lesson 호출은 phase 검사에서 403이 되지 않는다. 따라서 위 패턴은 보통 다음 중 하나다.

1. start가 새 세션이 아니라 이미 REACTION인 세션을 `resumed=true`로 반환했다.
2. 클라이언트가 start 응답의 새 `sessionId`가 아닌 이전 sessionId를 계속 사용했다.
3. DB에 `STARTED + currentPhase=null` 또는 기타 불변식 위반 데이터가 있다.

최종 판정은 응답 body의 `code=40310`과 위 SQL 조회 결과를 함께 확인해야 한다.

## 9. 문서 유지 규칙

- 상태 전이, 오류 코드, 보상 멱등성, DB 제약이 바뀌면 이 문서를 같은 변경에서 갱신한다.
- 오류 코드 이름만 추가하고 실제 endpoint 테스트를 추가하지 않는 변경은 완료로 보지 않는다.
- 기존 Wiki의 설계 결정과 현재 코드가 충돌하면 현재 코드를 먼저 검증하고, 결정 문서에 `stale` 또는 변경 이유를 남긴다.
- 운영 장애 분석 시 HTTP status만 기록하지 말고 `code`와 세션 상태를 함께 기록한다.
