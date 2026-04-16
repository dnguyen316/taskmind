---
name: taskmind-backend-agent
description: Implement and maintain TaskMind backend features in apps/backend using Spring Boot, layered domain/application/infrastructure modules, REST controllers, JPA repositories, Flyway migrations, and OpenAPI alignment. Use when requests involve backend API design, persistence changes, domain logic updates, Java tests, or integration between task and project modules.
---

# TaskMind Backend Agent

Follow this workflow to deliver backend changes safely and consistently.

## 1) Load project context

- Read `apps/backend/pom.xml` to confirm Java/Spring dependencies and test stack.
- Read `apps/backend/src/main/resources/application.properties` for runtime config defaults.
- Read `apps/backend/openapi.yaml` when adding/changing API request or response contracts.
- Read `references/backend-implementation-map.md` in this skill to pick the right package and file pattern quickly.

## 2) Choose the change surface

- Use `interfaces/rest/**` for HTTP endpoints, request validation, and response DTO mapping.
- Use `application/**` for use-case orchestration (`Create*Command`, `Update*Command`, application services).
- Use `domain/model/**` and `domain/repository/**` for business concepts and repository ports.
- Use `infrastructure/persistence/jpa/**` for JPA entities and Spring Data adapters.
- Use `src/main/resources/db/migration/*.sql` for schema changes.

## 3) Implement in thin-API / rich-domain style

- Keep controllers thin: parse/validate input and delegate to application service.
- Keep orchestration and transaction semantics in application service classes.
- Keep domain model behavior explicit and self-validating when business rules are added.
- Keep persistence adapters focused on mapping between domain model and JPA entities.
- Keep OpenAPI and DTO fields synchronized whenever request/response payloads change.

## 4) Validate locally

From `apps/backend` run the smallest set that validates your touched area first, then full tests if needed:

1. `mvn -q test`
2. `mvn -q -Dtest=*Task* test` or targeted controller test classes

If SQL migrations changed, verify migration ordering and naming (`V<integer>__description.sql`) and run tests again.

## 5) Delivery checklist

Before finalizing:

- Confirm endpoint behavior and HTTP status codes match existing conventions.
- Confirm new/updated DTOs include validation annotations when required.
- Confirm repository interfaces and adapters remain consistent.
- Confirm OpenAPI updates for any contract changes.
- Confirm tests cover the modified path (controller/service/repository as appropriate).
