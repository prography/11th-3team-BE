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
   - `docs/ai-reference/ERROR_CODES.md` when adding or modifying error codes

## Repo Facts
- Stack: Kotlin, JDK 25, Spring Boot 3.5, Gradle wrapper, ktlint.
- App root: `src/main/kotlin/org/prography/samsung/backend`.
- Tests: `src/test/kotlin/...`, JUnit 5 + Spring Boot Test.
- Runtime config: `src/main/resources/application.yaml`, test: `application-test.yaml`.
- Local services: PostgreSQL via Docker (test uses H2 in-memory).
- DB migrations: Flyway — `src/main/resources/db/migration/`.
- Packaging: `./gradlew clean bootJar` or Docker image pushed to GHCR.
- API actuator: `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`.
- Swagger UI: `/swagger-ui/index.html`, OpenAPI JSON: `/v3/api-docs`.

## Architecture Rules
- Keep packages domain-oriented under `org.prography.samsung.backend`.
- Keep controllers thin. Put business rules in `service` or `usecase`.
- Entry point is always the usecase: for domains that have a `usecase/` layer, controllers call only usecases (never a service directly), even for pass-through calls. The pass-through usecase is an accepted trade-off of the "controller entry point is always the usecase" rule.
- The usecase owns the transaction boundary: `@Transactional` (including `readOnly = true`) lives on the usecase. Services carry no `@Transactional` and merely participate in the usecase's boundary. This keeps the rollback-only marker traceable to one place. (Applies to domains with a `usecase/` layer — currently `session`, `user`.)
- Keep DTO naming explicit: `*Request` inbound, `*Response` outbound, `*Command` service-layer transfer.
- Prefer constructor injection and follow existing Spring/Lombok/Kotlin patterns.
- Use `CustomException` + `DomainErrorCode` in service layer — never raw `RuntimeException`, and never throw `ErrorBaseCode` directly from services.
- All entity state changes go through entity methods (e.g. `session.complete(...)`, `session.abort()`, `profile.applySessionReward(...)`) — never set fields directly from a service.
- All `@Entity` classes must extend `BaseEntity` (provides `createdAt`, `updatedAt`).
- Auth: controllers use `@CurrentUser` to get `userId: Long` injected by `DeviceUserAuthFilter` + `CurrentUserHolder`. Never read `Authorization` header in controllers.
- New domain error codes go in `DomainErrorCode` with a unique code per entry
- `ErrorBaseCode` is reserved for infra/framework-level errors handled in `GlobalExceptionHandler` or auth filters only.

## Working Rules
- Make the smallest coherent change that solves the task.
- Add or update tests only when the user explicitly asks for test code.
- Do not commit secrets, `.env`, AWS credentials, or machine-local values.
- Preserve user changes already present in the worktree unless explicitly told otherwise.
- Use `./gradlew test` for verification when the change touches runtime logic.
- Run `./gradlew ktlintCheck` before committing — ktlint is enforced.
- Update `AGENTS.md` when repo-wide working rules, workflows, or agent expectations change.

## Completion Gate
A task is not done until all three pass:
1. `./gradlew test` — tests green
2. `./gradlew ktlintCheck` — lint clean
3. Docs sync: update `AGENTS.md` or `docs/ai-reference/` if behavior or architecture changed.

> If any gate fails, fix it before reporting completion. See `docs/ai-reference/PLANS.md` for full gate details.

## Automation Patterns (Skill & Hook)
When a workflow repeats, promote it rather than re-explaining each session:
- **Skill** — repeated multi-step workflows (e.g., PR creation, DB migration, session flow testing) → `.claude/skills/{name}/SKILL.md`
- **Hook (Stop)** — enforce completion gate deterministically (tests, lint, docs) → `.claude/settings.json` Stop hook
- **Hook (PreToolUse)** — block dangerous commands before execution

Rule: if you've explained the same procedure twice in a session, make it a skill. If a check is mechanical (pass/fail), make it a hook.

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
