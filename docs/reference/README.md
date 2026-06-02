# Reference Docs

제품·아키텍처 의사결정과 개발 방향의 **단일 출처**입니다. Notion, 슬랙, 구두 합의와 충돌할 경우 이 디렉터리의 문서를 기준으로 합니다.

## 문서 목록

| 문서 | 경로 | 설명 |
| --- | --- | --- |
| AI 대화 루프 시스템 설계서 | [architecture/ai-conversation-loop-system-design.pdf](architecture/ai-conversation-loop-system-design.pdf) | AI 대화 루프의 시스템 설계 및 아키텍처 의사결정 |
| 개발자 기능 명세서 | [product/developer-feature-spec.html](product/developer-feature-spec.html) | 동갑내기 과외하기 화면·기능 개발 명세 (브라우저에서 열기) |

## 디렉터리 구분

| 디렉터리 | 용도 |
| --- | --- |
| `architecture/` | 시스템·도메인·AI 파이프라인 등 설계 문서 |
| `product/` | 화면·플로우·기능 명세 |

## 갱신 규칙

- 기준 문서를 수정할 때는 PR에 **변경 이유**와 **영향 범위**를 함께 기록합니다.
- 파일명은 kebab-case 영문을 사용합니다. 버전은 Git 히스토리로 관리합니다.