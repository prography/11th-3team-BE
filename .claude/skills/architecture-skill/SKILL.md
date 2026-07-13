---
name: architecture-skill
description: 변경분(PR diff)이 이 백엔드의 계층 구조(controller → usecase → service → repository → entity)와 트랜잭션 경계 규칙(진입점은 항상 usecase, @Transactional은 usecase 소유), 패키지/DTO/에러 컨벤션을 준수하는지 정적으로 검증한다. PR 리뷰, 머지 전 점검, 리팩토링 검증 시 사용. 위반을 리포트하고 수정 가능한 항목은 일괄 제안한다.
---

# Architecture Verification Skill

변경된 코드가 프로젝트 아키텍처를 준수하는지 **정적 분석(grep / import 파싱)** 으로 검증한다.
규칙을 새로 정의하지 않는다 — `docs/ai-reference/DESIGN.md`, `OOP.md`, `AGENTS.md`,
그리고 `.claude/skills/architecture/`(작성-전 가이드)의 규칙을 **판정 가능한 형태로** 적용한다.

> 이 스킬은 **검증(verifying)** 전용이다. 코드를 작성하기 *전에* 규칙을 적용하려면
> `architecture` 스킬을 사용한다. 두 스킬은 같은 규칙을 공유하되 방향이 반대다.

## 언제 쓰나
- PR/브랜치를 머지하기 전 아키텍처 준수 점검
- 리팩토링(레이어 분리, usecase 도입 등)이 의도대로 됐는지 확인
- "이 PR이 우리 구조 잘 따르나?" 류 요청

## 검증 대상 (스코프)
기본은 **변경분만**: `git diff main...HEAD`.
- 인자 없음 → 현재 브랜치 vs `main` diff
- 인자로 브랜치/커밋 범위 지정 가능 (예: `feat/123`, `HEAD~3..HEAD`)
- `*.kt` 파일만 대상. 삭제된 파일은 제외.

## 검증 절차

### Step 1 — 변경 파일 수집 + 레이어 라벨링
```bash
git diff --name-only --diff-filter=d main...HEAD -- '*.kt'
```
각 파일을 **패키지 경로**로 `(도메인, 레이어)` 라벨링한다:
```
.../{domain}/controller/X.kt  → (domain, controller)
.../{domain}/usecase/X.kt     → (domain, usecase)
.../{domain}/service/X.kt     → (domain, service)
.../{domain}/repository/X.kt  → (domain, repository)
.../{domain}/entity/X.kt      → (domain, entity)
.../{domain}/dto/...          → (domain, dto)
.../common/...                → (공통, 레이어 검증 제외 — 모든 도메인이 의존 허용)
```

### Step 2 — 정적 규칙 검증
각 변경 파일에 대해 `rules.md`의 규칙을 적용한다. 핵심은 **import 블록 파싱**이다
(이 프로젝트는 본문 FQCN을 금지하므로 import가 의존성의 단일 출처 — DESIGN.md Import 규칙).

검증 그룹:
- **A. 레이어 의존 방향** (import 파싱): controller→repository 직접 의존, service→타 도메인
  service/repository 의존, 역방향 의존.
- **B. 패키지/네이밍**: 클래스 suffix와 패키지 레이어 불일치, DTO 위치/네이밍.
- **C. 컨벤션** (grep): raw 예외, BaseEntity 미상속, service의 JPA 인프라 주입,
  controller의 Authorization 직접 접근, service의 엔티티 필드 직접 대입.
- **D. 진입점 & 트랜잭션 경계** (import + grep): usecase 있는 도메인에서 controller가
  service 직접 호출, service의 `@Transactional` 잔존, usecase 진입 메서드의 `@Transactional` 누락.

규칙별 탐지 패턴과 ✅/❌ 예시는 **`rules.md`** 참조.

### Step 3 — 리포트
`report-template.md` 포맷으로 출력한다.
- 위반: `[규칙ID] 파일:라인 — 무엇이 / 왜 위반 / 수정 방향`
- 통과한 규칙 그룹 요약
- 위반 0건이면 "✅ 아키텍처 준수" 명시

### Step 4 — 수정 일괄 제안
리포트 직후, **자동수정 가능한 위반**을 모아 한 번에 제안하고 사용자 승인 시 적용한다.

| 자동수정 O (기계적) | 자동수정 X (구조 변경 — 제안만) |
|---|---|
| BaseEntity 상속 추가 (C2) | controller→repository 의존 (A: usecase/service 경유 필요) |
| raw RuntimeException → CustomException (C1) | service→타 도메인 의존 (A: usecase로 끌어올려야 함) |
| DTO 파일을 dto/request·response로 이동 (B) | service의 JPA 인프라 주입 제거 (C3: saveAndFlush 등 설계 판단) |
| service의 `@Transactional` 제거 (D2) | controller→service 직접 호출 (D1: 패스스루 usecase 신설 필요) |
| usecase 진입 메서드에 `@Transactional` 추가 (D3) | |

승인 흐름: 리포트 → "수정 가능한 N건을 적용할까요?" → 승인 시 Edit 적용 → `./gradlew ktlintCheck` 권고.

## 한계 (정직하게 명시)
- "controller가 thin한가", "service가 단일 책임인가" 같은 **의미적 판정**은 정적 분석으로
  단정할 수 없다. 분기/루프 수, 라인 수 휴리스틱으로 **플래그만** 하고 단정하지 않는다.
- 리플렉션·동적 빈 조회로 숨은 의존은 import에 안 잡힌다. 그래프(MCP)가 필요하면 별도로 안내한다.
