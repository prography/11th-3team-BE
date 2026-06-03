# AGENTS.md

## Purpose
This file is a short map for agents working in the Prography Samsung Backend. Do not treat it as the full source of truth. Keep `AGENTS.md` brief, put durable knowledge in repo docs, and update those docs when behavior or architecture changes.

## Read Order
1. Read this file for repo-wide rules and navigation.
2. Read `CLAUDE.md` for quick commands and coding rules.
3. Read local docs in `docs/` as needed:
   - `docs/ai-reference/DESIGN.md` and `docs/ai-reference/design-docs/` for engineering principles
   - `docs/ai-reference/RELIABILITY.md` when touching DB migrations, health checks, or deployment
   - `docs/ai-reference/SECURITY.md` when touching auth, secrets, or exposed endpoints
   - `docs/ai-reference/QUALITY_SCORE.md` for current codebase quality snapshot
   - `docs/ai-reference/PLANS.md` for documentation completion gate rules

## Repo Facts
- Stack: Kotlin, JDK 25, Spring Boot 3.5, Gradle wrapper, ktlint.
- App root: `src/main/kotlin/org/prography/samsung/backend`.
- Tests: `src/test/kotlin/...`, JUnit 5 + Spring Boot Test.
- Runtime config: `src/main/resources/application.yaml`, test: `application-test.yaml`.
- Local services: PostgreSQL via Docker (test uses H2 in-memory).
- DB migrations: Flyway — `src/main/resources/db/migration/`.
- Packaging: `./gradlew clean bootJar` or Docker image pushed to GHCR.
- API actuator: `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`.

## Architecture Rules
- Keep packages domain-oriented under `org.prography.samsung.backend`.
- Keep controllers thin. Put business rules in `service` or `usecase`.
- Keep DTO naming explicit: `*Request` inbound, `*Response` outbound, `*Command` service-layer transfer.
- Prefer constructor injection and follow existing Spring/Lombok/Kotlin patterns.
- Use `CustomException` + domain-specific `*ErrorCode` — never raw `RuntimeException`.

## Working Rules
- Make the smallest coherent change that solves the task.
- Add or update tests only when the user explicitly asks for test code.
- Do not commit secrets, `.env`, AWS credentials, or machine-local values.
- Preserve user changes already present in the worktree unless explicitly told otherwise.
- Use `./gradlew test` for verification when the change touches runtime logic.
- Run `./gradlew ktlintCheck` before committing — ktlint is enforced.
- Update `AGENTS.md` when repo-wide working rules, workflows, or agent expectations change.

## Git & Branch Convention
- Commit format: `type: message` — types: `feat`, `fix`, `chore`, `build`, `style`, `refactor`, `docs`, `test`, `ci`.
- Branch format: `{type}/#issue` (e.g., `feat/#12`). Main integration branch: `main`.

## CI/CD
- CI: runs on PR to `develop` or `main` — `./gradlew clean build` with `SPRING_PROFILES_ACTIVE=local`.
- CD: runs on push to `main` — builds bootJar, pushes Docker image to GHCR, deploys to EC2 via AWS SSM.
- Deploy script: `script/deploy.sh` — handles pull, run, health check, and rollback.

## Useful Commands
- `./gradlew bootRun`
- `./gradlew test`
- `./gradlew clean bootJar`
- `./gradlew ktlintCheck`
- `./gradlew ktlintFormat`
