# Backend

Prography Samsung Backend 애플리케이션입니다.

## 기술 스택

- Kotlin
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Boot Actuator
- Flyway
- H2 Database
- Gradle Kotlin DSL
- Java 25

## 프로젝트 구조

```text
.
├── build.gradle.kts
├── settings.gradle.kts
├── docs/
│   ├── README.md
│   ├── health-check.md
│   └── reference/
│       ├── architecture/
│       └── product/
└── src/
    ├── main/
    │   ├── kotlin/
    │   └── resources/
    └── test/
        └── kotlin/
```

## 실행 방법

```bash
./gradlew bootRun
```

기본 포트는 Spring Boot 기본값인 `8080`입니다.

## 테스트

```bash
./gradlew test
```

## 문서

프로젝트 문서는 `docs/` 하위에서 관리합니다.

- [문서 인덱스](docs/README.md)
