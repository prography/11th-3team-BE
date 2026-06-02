# Plan Docs

기능 명세·아키텍처 문서를 바탕으로 한 **구현·모델링 계획**을 관리합니다. Reference는 기준(What), Plan은 실행 설계(How)에 가깝습니다.

## 문서 목록

| 문서 | 경로 | 설명 |
| --- | --- | --- |
| API·DB 모델링 계획 | [api-and-database-modeling-plan.md](api-and-database-modeling-plan.md) | §2 화면별 API · §3 계약 · **§4 DB(ERD·테이블·Flyway)** · §13 Figma |

## 읽는 방법

- **FE:** [§2 화면별 호출](api-and-database-modeling-plan.md#2-화면별-api-호출-명세-구현용) + [§3 API](api-and-database-modeling-plan.md#3-api-카탈로그)
- **BE:** [§4 DB](api-and-database-modeling-plan.md#4-데이터베이스) + §3 + Flyway `src/main/resources/db/migration/`
- **Figma 반영 후:** [§13 체크리스트](api-and-database-modeling-plan.md#13-figma-디자인-변경-시-수정-가이드)

## 관련 문서

- [Reference Docs](../reference/README.md) — 제품·아키텍처 기준 문서
- [Backend Docs](../README.md) — 문서 인덱스

## 갱신 규칙

- Reference 문서가 바뀌면 Plan 문서의 **영향 범위**를 함께 갱신합니다.
- 파일명은 kebab-case 영문을 사용합니다.