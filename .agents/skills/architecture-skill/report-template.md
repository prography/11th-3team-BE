# 리포트 출력 포맷

검증 결과는 아래 형식으로 출력한다. 위반이 없으면 "통과" 섹션만 낸다.

```
## 🏛 아키텍처 검증 리포트
대상: <브랜치/범위>  |  변경 .kt 파일: <N>개

### ❌ 위반 (<count>건)

[A2] session/service/SessionService.kt:7
  무엇이: service가 curriculum 도메인 repository를 직접 import
  왜:     크로스 도메인 의존은 usecase에서 조율해야 함 (service끼리 서로 모름)
  방향:   해당 조회를 SessionLessonUsecase로 끌어올리고 SessionService는 자기 도메인만 다루도록
  자동수정: ❌ 구조 변경 필요

[C2] curriculum/entity/HintNote.kt:12
  무엇이: @Entity인데 BaseEntity 미상속
  왜:     모든 엔티티는 createdAt/updatedAt 자동관리를 위해 BaseEntity 상속 필수
  방향:   `: BaseEntity()` 추가
  자동수정: ✅

[D2] session/service/SessionQueryService.kt:22
  무엇이: usecase 있는 도메인의 service에 @Transactional(readOnly) 잔존
  왜:     트랜잭션 경계는 usecase가 소유해야 rollback-only 마킹 추적이 한 곳으로 고정됨
  방향:   service의 @Transactional 제거 (상위 usecase 진입점에 경계 있는지 D3로 교차 확인)
  자동수정: ✅

### ⚠️ 검토 권장 (휴리스틱 — 단정 아님)

[thin?] user/controller/UserController.kt:30
  controller 메서드에 분기/조합 로직 다수 — usecase 도입 검토

### ✅ 통과
- A. 레이어 의존 방향: controller→repository 직접 의존 없음, 역방향 의존 없음
- B. 패키지/네이밍: DTO request/response 분리 정상, suffix-패키지 일치
- C. 컨벤션: raw 예외 없음, JPA 인프라 주입 없음, @CurrentUser 사용
- D. 진입점·트랜잭션: controller→usecase 경유, service에 @Transactional 없음, usecase 진입점이 경계 소유

### 🔧 자동수정 제안
수정 가능한 위반 <M>건이 있습니다 (C2 ×1, C1 ×2).
적용할까요? (적용 후 ./gradlew ktlintCheck 권장)
```

## 규칙
- 위반은 **규칙ID 오름차순**(A→B→C)으로 정렬. 같은 규칙은 파일 경로순.
- "방향"은 추상적 훈계가 아니라 **구체적 다음 행동**으로 적는다.
- 위반 0건이면: `✅ 아키텍처 준수 — 위반 없음` + 통과 그룹 요약.
- 자동수정 제안 섹션은 **자동수정 O 항목이 1건 이상일 때만** 출력한다.
- 사용자가 승인하면 Edit으로 적용하고, 적용한 항목 목록 + `./gradlew ktlintCheck` 실행을 권한다.
