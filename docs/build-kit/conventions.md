# Conventions

Patterns every milestone must follow. These mirror the reference implementation; deviating
breaks parity and the review gates.

Use this file together with the root [`AGENTS.md`](../../AGENTS.md), the architecture
summary in [`00-overview.md`](00-overview.md), and the milestone sequence in
[`01-build-order.md`](01-build-order.md). `AGENTS.md` defines the non-negotiable agent
rules, `00-overview.md` defines ownership boundaries, `01-build-order.md` defines when
work happens, and this file defines how code should be shaped.

## Backend: DDD four-layer feature modules

Core, Relay, and Nova all use **Java 17 + Spring Boot 3.3.5** and organize code by
**feature module**: each feature is an independent vertical slice under
`com.taskmind.<service>.<feature>`. Every module has four layers; **keep each change in the
layer that owns the concern**:

```text
<feature>/
  interfaces/rest/                 # thin controllers: validate input, map DTOs, delegate
  interfaces/rest/dto/             # request/response records
  application/                     # use-case orchestration + transactions
                                   #   Create*Command, Update*Command, *ApplicationService
  domain/model/                    # business entities (self-validating) + enums
  domain/                          # domain rules/services (for example, *Rules)
  domain/repository/               # repository ports (interfaces)
  infrastructure/persistence/jpa/  # *JpaEntity + Spring Data adapters implementing ports
```

Rules:

- **Controllers contain no business logic.** They validate, map DTOs to command/domain
  objects, and call an application service.
- **Application services own transactions** (`@Transactional`) and orchestration. They
  depend on **domain repository ports**, never on Spring Data interfaces directly.
- **Domain entities are self-validating** and free of framework annotations where possible.
- **Use explicit Java local variable types in backend code.** Do not introduce `var` in service code; explicit types make refactors, reviews, and cross-layer contract changes easier to reason about.
- Persistence concerns live in `*JpaEntity` mappers.
- **Infrastructure adapters** implement the domain ports and translate between `*JpaEntity`
  and the domain model. The Spring Data interface (`SpringData*JpaRepository`) is wrapped by
  a `Jpa*Repository` adapter that implements the domain port.

The **task** module is the reference example of the full pattern (see
[`phases/M02-tasks-projects.md`](phases/M02-tasks-projects.md)). Build it first and mirror
its shape everywhere else.

### Naming

| Thing | Pattern | Example |
|-------|---------|---------|
| Controller | `<Feature>Controller` | `TaskController` |
| App service | `<Feature>ApplicationService` | `TaskApplicationService` |
| Command | `Create<Feature>Command` / `Update<Feature>Command` | `CreateTaskCommand` |
| Domain entity | `<Feature>` | `Task` |
| Domain rules | `<Feature><Concern>Rules` | `TaskHierarchyRules` |
| Repository port | `<Feature>Repository` | `TaskRepository` |
| JPA entity | `<Feature>JpaEntity` | `TaskJpaEntity` |
| JPA adapter | `Jpa<Feature>Repository` | `JpaTaskRepository` |
| Spring Data | `SpringData<Feature>JpaRepository` | `SpringDataTaskJpaRepository` |
| Request DTO | `Create<Feature>Request` / `Update<Feature>Request` | `CreateTaskRequest` |
| Response DTO | `<Feature>Response` | `TaskResponse` |

## Persistence & migrations

- **Flyway owns the schema**; Hibernate is `ddl-auto=validate`. A broken migration breaks
  the build because tests run Flyway against H2 in PostgreSQL mode.
- Migrations live in `apps/<service>/src/main/resources/db/migration/`, named
  `V<n>__snake_case_description.sql`. **Append the next integer. Never edit an applied
  migration**; add a new one instead.
- Every mutable entity has an optimistic-lock `@Version` column.
- Soft-delete via `deleted_at` where the reference model uses it: tasks, projects,
  memberships, and scheduled blocks.
- Core owns the **public** schema; Nova owns the **ai** schema; Relay projects into the
  **analytics** schema, which is created by a Core migration and written by Relay.

## TDD for Java (RED -> GREEN -> REFACTOR)

1. **RED** - write a failing test first: controller slice, application service, or domain
   rule test describing the desired behavior.
2. **GREEN** - implement the minimum to pass.
3. **REFACTOR** - clean up while keeping tests green.

Tests run under the `test` profile against **H2 in PostgreSQL mode**. Flyway still runs,
the E2E auth bypass is enabled, and Elasticsearch/OpenSearch autoconfig is excluded.

```bash
mvn -q test                                      # all tests in module
mvn -q -Dtest=TaskControllerTest test           # one test class
mvn -q -Dtest='*Task*' test                     # pattern
mvn -q -Dtest=TaskControllerTest#name test      # single method
```

Aim for: a controller test (request -> response + status), an application-service test
(orchestration + error/conflict paths), and domain-rule unit tests for any non-trivial
invariant.

## Security

- Core is a stateless JWT resource server. `SecurityConfig` plus an authorization rules
  class define public versus protected routes. Errors are RFC-7807 `ProblemDetail`.
- `/internal/**` routes are guarded by a separate `@Order(0)` chain requiring the
  `X-Service-Token` header.
- E2E bypass is profile-gated (`local`/`test`/dedicated `e2e` only) and must **fail startup** in
  `prod`.

## REST + OpenAPI

- Public Core endpoints are documented in `apps/backend/openapi.yaml` (OpenAPI 3.0.3).
  **Update it in the same change** that alters a request/response shape.
- `/internal/**` and some auth/scheduler/AI-BFF routes are intentionally not in the public
  OpenAPI; see [`reference/api-contract.md`](reference/api-contract.md).

## Frontend (Vue 3 + TS)

- Organize by **feature** under `src/features/<feature>/{pages,components,composables,api}`.
  Keep pages thin; put reactive logic in `composables/`; server calls in `api/`; shared
  enums/constants centralized.
- **All HTTP goes through `src/lib/apiClient.ts`** (axios): injects the bearer token, and on
  a `401` for a protected route transparently refreshes via `/v1/auth/token/refresh`
  (single-flight), retries once, then fires session-expired. Base URL = `VITE_API_BASE_URL`
  (default `http://localhost:8080`). The frontend talks **only to Core**.
- Routing guards use `meta.requiresAuth` / `meta.public`; `stores/auth.ts` holds the
  session and `ensureInitialized()` runs before each navigation.
- UI is **Ant Design Vue 4**; state is **Pinia 3**; rich text is **Tiptap 3**; forms use
  vee-validate + yup; dates use dayjs.
- “Lint” for the frontend = `npm run typecheck` + `npm run build` (no ESLint/unit-test
  runner).
- Use **strict TypeScript**: no `any` in new code, prefer typed API responses, and narrow at
  the API boundary.

## Code-comment discipline

Comments explain **non-obvious intent, trade-offs, or constraints** - never narrate what
the code already says. Do not leave “explain the change” comments.

## Verification gate (every milestone)

```bash
make vibe-verify  # mvn test (900s cap) + FE typecheck (180s cap); exit 124 = timeout
```

For UI milestones, add a browser E2E pass on `localhost:5173` with the superadmin bypass.
