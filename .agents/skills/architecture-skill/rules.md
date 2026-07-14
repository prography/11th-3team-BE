# 검증 규칙 카탈로그

판정 가능한 규칙만 모았다. 각 규칙은 **탐지 패턴**(정적 분석 방법)과 **✅/❌ 예시**, **자동수정 여부**를 가진다.
권위 출처: `docs/ai-reference/DESIGN.md`, `OOP.md`, `AGENTS.md`.

레이어 순서: `controller → usecase → service → repository → entity`. `common/*`은 전 도메인 공통(의존 허용).
도메인: `conversation, curriculum, gamification, notification, session, user`.

---

## A. 레이어 의존 방향 (import 블록 파싱)

각 파일 상단 `import org.prography.samsung.backend.{domain}.{layer}.*`를 추출해 판정한다.

### A1 — controller가 repository 직접 의존 금지
controller는 usecase 또는 service만 호출한다. repository 직접 접근 금지.
- **탐지**: `*/controller/*.kt`의 import에 `.repository.` 포함
- **자동수정**: ❌ (구조 변경 — service/usecase 경유로 흐름 재설계 필요)
```kotlin
// ❌ controller에서
import org.prography.samsung.backend.session.repository.TutoringSessionRepository
// ✅ usecase/service 경유
import org.prography.samsung.backend.session.usecase.SessionLessonUsecase
```

### A2 — service가 다른 도메인의 service/repository 의존 금지 ★이 PR의 핵심
service는 자기 도메인 안에서만 동작한다. **크로스 도메인 조율은 usecase에서** 한다.
- **탐지**: `{domainX}/service/*.kt`의 import에 `{domainY}.service.` 또는 `{domainY}.repository.`
  (domainY ≠ domainX, domainY ≠ common)
- **자동수정**: ❌ (해당 호출을 usecase로 끌어올려야 함)
```kotlin
// ❌ session/service/SessionService 에서
import org.prography.samsung.backend.curriculum.repository.CurriculumRepository
// ✅ 크로스 도메인은 usecase에서 조율 (UserHomeUsecase가 sessionService + userProfileService 조합하듯)
```
> 참조 구현: `user/usecase/UserHomeUsecase` — usecase가 `userProfileService` + `sessionService`를
> 조합한다. service끼리는 서로 모른다.

### A3 — 역방향/건너뛰기 의존 금지
하위 레이어가 상위를 import 하면 위반.
- **탐지**:
  - service의 import에 `.usecase.` 또는 `.controller.`
  - repository의 import에 `.service.`/`.usecase.`/`.controller.`
  - usecase의 import에 `.controller.`
- **자동수정**: ❌

### A4 — usecase의 정당한 크로스 도메인 (위반 아님 — 오탐 방지)
usecase는 여러 도메인의 service를 의존해도 **정상**이다. A2와 혼동하지 말 것.
- usecase가 `{otherDomain}.service.*`를 import → ✅ 허용
- usecase가 `{anyDomain}.repository.*`를 직접 import → ⚠️ 플래그 (usecase는 service 경유 권장,
  단 자기 도메인 repository 직접 접근은 기존 패턴 확인 후 판단)

---

## B. 패키지 구조 / 네이밍

### B1 — 클래스 suffix와 패키지 레이어 일치
- **탐지**: 파일 경로 레이어 ↔ 클래스명 suffix 대조
  - `*Service` 클래스가 `service/` 밖에 있음 → 위반
  - `*Usecase`/`*UseCase`가 `usecase/` 밖 → 위반
  - `*Controller`가 `controller/` 밖 → 위반
  - `*Repository`가 `repository/` 밖 → 위반
- **자동수정**: ❌ (파일 이동은 import 영향 큼 — 제안만)

### B2 — DTO 패키지 분리 & 네이밍
이 PR이 `SharedDtos`/`UserDtos`/`SessionDtos`를 도메인별 `dto/request`·`dto/response`로 분리했다.
- **탐지**:
  - `*Request` 클래스가 `dto/request/` 밖 → 위반
  - `*Response` 클래스가 `dto/response/` 밖 → 위반
  - `dto/` 안에 `*Request`/`*Response`/`*Command` 외 네이밍의 전송 객체 → ⚠️ 플래그
- **자동수정**: ✅ (request/response 하위 패키지로 파일 이동 + package 선언 수정 제안)
> 단, `*Command`(service 레이어 전송)는 위치 규칙이 느슨하다 — 플래그만.

---

## C. 컨벤션 (grep 패턴)

### C1 — raw 예외 금지
- **탐지**: 변경 `.kt`에서 `throw RuntimeException` / `throw IllegalStateException` /
  `throw IllegalArgumentException` / service·usecase에서 `throw .*(ErrorBaseCode\.`
- **올바름**: `throw CustomException(DomainErrorCode.SOME_CODE)`
- **자동수정**: ✅ (CustomException 치환 — 단 적절한 DomainErrorCode 선택은 사용자 확인)
> `ErrorBaseCode`는 인프라/프레임워크 레벨(GlobalExceptionHandler·auth filter) 전용. service throw 금지.

### C2 — @Entity는 BaseEntity 상속 필수
- **탐지**: `@Entity` 선언된 클래스의 헤더에 `: BaseEntity()` 없음
- **자동수정**: ✅ (`: BaseEntity()` 추가 + import 추가)
```kotlin
// ✅
@Entity
class SomeEntity(...) : BaseEntity()
```

### C3 — service에 JPA 인프라 직접 주입 금지
service는 "무엇을(비즈니스)"만 안다. "어떻게 DB에 반영하는가"는 repository.
- **탐지**: `*/service/*.kt` 또는 `*/usecase/*.kt` 생성자 파라미터에
  `EntityManager` / `JdbcTemplate` / `DataSource`
- **자동수정**: ❌ (saveAndFlush 등으로 대체 — 설계 판단 필요, OOP.md §1 참조)

### C4 — service에서 엔티티 필드 직접 대입 금지
상태 변경은 엔티티 메서드로 캡슐화.
- **탐지** (휴리스틱): service/usecase에서 `<entityVar>.<field> =` 대입 패턴
  (`val`/`var` 선언 제외, 단순 프로퍼티 set 대입). 컬렉션 `.clear()`/`.add()` 직접 조작도 플래그.
- **자동수정**: ❌ (엔티티 메서드명 설계 필요 — 플래그 + 제안만)
```kotlin
// ❌ profile.onboardingCompleted = true
// ✅ profile.completeOnboarding()
```

### C5 — controller에서 Authorization 헤더 직접 접근 금지
인증 사용자는 `@CurrentUser userId: Long`로 받는다.
- **탐지**: `*/controller/*.kt`에서 `@RequestHeader.*Authorization` 또는
  `request.getHeader("Authorization")` 류
- **자동수정**: ❌ (`@CurrentUser`로 시그니처 변경 — 제안만)

### C6 — 외부 의존성 포트/어댑터 추상화 (LLM 등)
새 외부 벤더 연동 시 `.claude/skills/architecture/ports-and-adapters.md` 기준 적용.
- **탐지** (휴리스틱): service가 벤더 SDK 타입/예외를 직접 import하거나, 한 클래스 안
  `when (provider)` 분기로 벤더 선택 → 추상화 누수 의심
- **자동수정**: ❌ (포트/어댑터 분리는 설계 — 플래그 + 해당 서브문서로 안내)

---

## D. 진입점 & 트랜잭션 경계 (import + grep) ★PR #30 합의 규칙

핵심 규칙: **Controller → Usecase → Service, Controller는 usecase만 호출하고, `@Transactional`은
usecase가 소유한다.** service는 트랜잭션 경계를 선언하지 않고 usecase의 경계에 참여만 한다.
이렇게 하면 rollback-only 마킹 지점이 한 곳(usecase)으로 고정되어 "왜 커밋이 안 되지?" 디버깅이 쉬워진다.

> **적용 범위**: `usecase/` 레이어가 있는 도메인만. 현재 `conversation`, `session`, `user`.
> usecase가 아직 없는 도메인(`curriculum`, `notification` 등)은 이 그룹 검증에서 제외한다
> — controller가 service를 직접 호출하고 service가 `@Transactional`을 갖는 게 정상이다.
> 판정 전에 해당 도메인에 `.../{domain}/usecase/` 디렉토리가 존재하는지 먼저 확인한다.

### D1 — controller가 service 직접 호출 금지
usecase 레이어가 있는 도메인에서 controller는 **usecase만** 주입/호출한다. 패스스루(단순 위임)라도
service를 직접 부르지 않고 패스스루 usecase를 거친다 — 진입 경로를 하나로 고정하기 위함.
- **탐지**: `{domain}/controller/*.kt`의 import·생성자에 `{domain}.service.` (단, `{domain}/usecase/` 존재 시)
- **자동수정**: ❌ (패스스루 usecase 신설 + controller 배선 변경 — 구조 변경)
```kotlin
// ❌ usecase 있는 user 도메인인데 controller가 service 직접 주입
class UserController(private val userProfileService: UserProfileService)
// ✅ usecase 경유 (패스스루라도)
class UserController(private val userProfileUsecase: UserProfileUsecase)
```
> 참조: `UserProfileUsecase`(getProfile/getSettings/updateSettings),
> `OnboardingUsecase`(getStatus/saveCurriculum/...) — 단순 위임도 usecase가 감싼다.

### D2 — usecase 있는 도메인의 service에 @Transactional 잔존 금지
service는 트랜잭션 경계를 선언하지 않는다. `@Transactional(readOnly = true)`도 마찬가지.
- **탐지**: `{domain}/service/*.kt`에 `@Transactional` (단, `{domain}/usecase/` 존재 시)
- **자동수정**: ✅ (해당 어노테이션 줄 + 미사용 import 제거. 단 **같은 경계를 담당하는 usecase
  진입 메서드에 `@Transactional`이 있는지 D3로 함께 확인** 후 제거 — 무경계 상태 방지)
```kotlin
// ❌ session/service/SessionQueryService
@Transactional(readOnly = true)
fun getStatus(userId: Long): SessionStatusResponse
// ✅ 경계는 usecase가 소유, service는 무경계
fun getStatus(userId: Long): SessionStatusResponse
```

### D3 — usecase 진입(public) 메서드에 @Transactional 필수
controller가 호출하는 usecase의 public 메서드는 트랜잭션 경계를 소유해야 한다.
읽기 전용 흐름은 `@Transactional(readOnly = true)`, 쓰기는 `@Transactional`.
- **탐지**: `{domain}/usecase/*.kt`의 public `fun` 중 `@Transactional`(또는 readOnly) 미부착
  (private helper 제외)
- **자동수정**: ✅ (쓰기/읽기 판단은 흐름 확인 필요 — readOnly 여부는 사용자 확인 후 부착)
> D2·D3는 짝이다: service에서 걷어낸 경계가 usecase에 반드시 존재해야 무경계 구멍이 안 생긴다.
> 검증 시 "service에서 제거된 트랜잭션이 상위 usecase 진입점에 있는가"를 교차 확인한다.

---

## 휴리스틱 (단정하지 않고 플래그만)

| 항목 | 신호 | 행동 |
|------|------|------|
| controller가 thin하지 않음 | controller 메서드에 `if`/`when`/`for` 다수, 라인 수 큼 | "비즈니스 로직 의심" 플래그 |
| usecase 누락 의심 | controller가 동일 흐름에서 service 2개+ 직접 조합 | "usecase 도입 검토" 플래그 |

> 휴리스틱은 위반으로 단정하지 않는다. 리포트에 "⚠️ 검토 권장"으로만 표기한다.
