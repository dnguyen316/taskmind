---
name: taskmind-backend-agent
description: Implement and maintain TaskMind backend features in apps/backend using Spring Boot, layered domain/application/infrastructure modules, REST controllers, JPA repositories, Flyway migrations, and OpenAPI alignment. Use when requests involve backend API design, persistence changes, domain logic updates, Java tests, or integration between task and project modules.
---

# TaskMind Backend Agent

Follow this workflow to deliver backend changes safely and consistently.

## 1) Load project context

Required first reads:

- Read `docs/build-kit/01-build-order.md` to understand milestone order and acceptance gates.
- Read the active `docs/build-kit/phases/Mxx-*.md` milestone spec before changing code.
- Read `apps/backend/pom.xml` to confirm Java/Spring dependencies and test stack.
- Read `apps/backend/src/main/resources/application.properties` for runtime config defaults.
- Read `apps/backend/openapi.yaml` when adding/changing API request or response contracts.
- Read `references/backend-implementation-map.md` in this skill to pick the right package and file pattern quickly.

## 2) Pick the milestone

Before choosing code surfaces:

- Map the user request to the owning boundary: Core (`apps/backend`), Relay (`apps/relay`), Nova (`apps/ai`), or shared libraries (`libs/events`, `libs/ai-contracts`).
- Identify the next incomplete milestone from `docs/build-kit/01-build-order.md` and the matching `docs/build-kit/phases/Mxx-*.md` file.
- Keep backend work inside the requested boundary; avoid touching Relay, Nova, frontend, or library services unless the request explicitly requires that contract boundary.

## 3) Choose the change surface

- Use `interfaces/rest/**` for HTTP endpoints, request validation, and response DTO mapping.
- Use `application/**` for use-case orchestration (`Create*Command`, `Update*Command`, application services).
- Use `domain/model/**` and `domain/repository/**` for business concepts and repository ports.
- Use `infrastructure/persistence/jpa/**` for JPA entities and Spring Data adapters.
- Use `src/main/resources/db/migration/*.sql` for schema changes.

### Token-saving navigation

- Use `rg --files` with narrow globs (for example `rg --files apps/backend/src/main/java -g '*Task*'`) before opening files.
- Use `rg -n '<Controller|ApplicationService|Repository|migration name>'` to find exact code surfaces before reading whole files.
- Inspect `apps/backend/openapi.yaml` only around changed paths and schemas instead of reading the full contract.
- Prefer adjacent tests for the touched feature over whole-repo scans.

## 4) Implement in thin-API / rich-domain style

- Keep controllers thin: parse/validate input and delegate to application service.
- Keep orchestration and transaction semantics in application service classes.
- Keep domain model behavior explicit and self-validating when business rules are added.
- Keep persistence adapters focused on mapping between domain model and JPA entities.
- Keep OpenAPI and DTO fields synchronized whenever request/response payloads change.

## 5) Format, review, and validate locally

After implementation, format backend code from `apps/backend` before final checks:

1. `mvn -q spotless:apply -DspotlessFiles=<changed-java-files>`
2. `mvn -q spotless:check -DspotlessFiles=<changed-java-files>` when you need a no-diff formatter guard

Then do a quick code review pass to find issues before slower tests:

- Run `git diff --check` and inspect `git diff --stat`.
- Review changed files by TaskMind layer: REST/OpenAPI, application transaction boundary, domain invariant, JPA mapping, Flyway migration, and tests.
- Confirm no frontend contract consumer was missed when Core request/response DTOs changed.

From `apps/backend` run the smallest set that validates your touched area first, then full tests if needed:

1. `mvn -q test`
2. `mvn -q -Dtest=*Task* test` or targeted controller test classes

If SQL migrations changed, verify migration ordering and naming (`V<integer>__description.sql`) and run tests again.

## 6) Session documentation closeout

Before finalizing, document the backend session:

- Update `docs/backend-feature-changelog.md` with the implemented backend behavior, tests, and any follow-up notes.
- Update any affected `docs/build-kit/reference/*.md` or milestone docs when contracts, data model, events, AI boundaries, or AWS assumptions changed.

## 7) Delivery checklist

Before finalizing:

- Confirm endpoint behavior and HTTP status codes match existing conventions.
- Confirm new/updated DTOs include validation annotations when required.
- Confirm repository interfaces and adapters remain consistent.
- Confirm OpenAPI updates for any contract changes.
- Confirm tests cover the modified path (controller/service/repository as appropriate).
