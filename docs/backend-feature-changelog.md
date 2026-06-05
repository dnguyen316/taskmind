# Backend Feature Changelog

## 2026-06-05 (Agent Session Hygiene Workflow)

### Added
- Added a backend-aware, service-boundary neutral agent session workflow for Core, Relay, Nova, and shared backend libraries.
- Documented the required closeout sequence for milestone identification, selective build-kit doc updates, backend changelog entries, Core OpenAPI sync, targeted checks, `make vibe-verify`, and skipped-check notes.
- Added a token-saving inspection workflow that starts with focused `rg` discovery and limits context loading to active guidance, owning references, identifiers, and adjacent tests before broader scans.

### Why this improves BE quality
- Keeps backend sessions traceable across services without encouraging cross-boundary changes.
- Reduces context waste during agent handoffs while preserving verification and contract-sync expectations.


## 2026-06-05 (Backend Static State Review)

### Static Backend State by Milestone
- **M01/Core foundations:** Core now has authentication REST controllers, JWT/resource-server security configuration, authenticated-user resolution, E2E bypass production guard and super-admin seeder, auth/security integration tests, and Flyway-owned auth/authz migrations. The older “auth/authz foundation pending” notes are resolved for the foundation layer; future work should focus on hardening and full parity behavior rather than re-scaffolding auth.
- **M02/Tasks + Projects:** Core now has task CRUD, task hierarchy rules, task links, release summaries, project CRUD, project membership management/authorization, and controller-level coverage for task flows, project-membership flows, security routes, and project access from task creation. The older “project endpoint tests pending” note is resolved for the currently present endpoint coverage, while any newly expanded project CRUD/archive behavior should continue to receive dedicated regression tests.
- **Current migration ceiling:** `apps/backend/src/main/resources/db/migration/V11__task_type_links_and_constraints.sql`. Future schema work must append `V12__*.sql` or later and must not edit applied migrations.

### Resolved Older Pending Items
- ✅ Auth/authz foundation is no longer pending: auth flows, JWT validation/conversion, protected route tests, E2E bypass guard, seeder, and Flyway auth tables now exist.
- ✅ Project endpoint coverage is no longer a blanket gap: project creation is exercised by membership and task authorization tests, and project membership endpoints have direct controller tests. Keep adding targeted project CRUD/archive tests when those contracts change.
- ✅ M01/M02 implementation should now be treated as an implemented foundation snapshot rather than an in-memory or unauthenticated scaffold.

### Known Remaining BE Gaps
- **M04 Scheduler:** scheduling preferences, calendar blocks, auto-scheduler, reschedule proposal persistence, and production scheduler orchestration remain to be implemented beyond deterministic planning stubs.
- **M05 Outbox/Relay:** Core outbox, Redis Streams publishing, Relay consumers, event schemas, and analytics projections are still parity gaps.
- **M06 Search/Storage:** OpenSearch-backed activity search and S3/LocalStack attachment storage are not yet represented as production-ready Core/Relay flows.
- **M07-M09 Nova facades and AI workflows:** Core-to-Nova facades, provider routing, capability contracts, agent runtime, chat/audit flows, async spec breakdown, AI feature orchestration, and Jira publish pipeline remain to be built beyond current stub endpoints.
- **M10 Integrations:** Jira Cloud, GitHub, wiki import/publish, credential handling, and sync workflows remain parity work.
- **M11 Notifications:** in-app notifications, SSE, email digests, Slack delivery, and reminders remain parity work.
- **M12 Analytics:** dashboard/report aggregation, throughput reporting, team directory projections, and Relay-backed analytics APIs remain parity work.
- **M13 Hardening/AWS:** rate limiting, observability, production profile validation, AWS managed data-plane wiring, deployment assets, and security hardening remain final parity gates.

### Docs Updated This Session
- `docs/backend-feature-changelog.md` is the session log location updated in this pass. Future backend status reviews should add the newest dated entry above this one and keep remaining gaps aligned with `docs/build-kit/01-build-order.md` rather than older scaffold notes.


## 2026-04-16 (Backend Status Review + Execution Task Plan)

### Reviewed
- Reviewed current backend implementation coverage across task and project modules, including persistence migrations, REST controllers, and application services.
- Verified feature coverage for:
  - Task CRUD, status transitions, completion check, filtering, pagination, and archive flow.
  - Project CRUD-lite (`create`, `list`, `get`, `update`) and soft-archive flow.
  - Planner/AI workflow contract stubs used by FE integration.

### Current BE Status
- ✅ Persistence baseline is established with migration-managed PostgreSQL schema for `tasks` and `projects`.
- ✅ Task APIs are feature-complete for MVP baseline and covered by integration-style controller tests.
- ✅ Project APIs are implemented end-to-end with JPA persistence and key uniqueness enforcement.
- ✅ Superseded by 2026-06-05: project endpoint coverage now exists through project membership controller tests and authenticated project creation exercises; add dedicated CRUD/archive regression tests as contracts evolve.
- ✅ Superseded by 2026-06-05: authentication/authorization foundation now exists with JWT security, auth controllers, authenticated-user resolution, E2E bypass guard/seeder, and auth/security tests.
- ⚠️ OpenAPI spec currently lags behind implemented project and planner/AI stub endpoints.

### Task Plan (Next Execution Slice)
1. ✅ Superseded by 2026-06-05: project endpoint coverage now exists for membership flows and authenticated project creation paths; continue adding targeted CRUD/archive tests when contracts change.
2. Expand `openapi.yaml` to include project endpoints and planner/AI stub contracts.
3. ✅ Superseded by 2026-06-05: JWT auth middleware and authenticated-user scoping foundation now exist.
4. Add service-level validation and observability hooks (structured logs + request correlation IDs) for new planner/AI paths.

### Why this improves BE quality
- Creates an explicit implementation snapshot for team alignment before deeper AI orchestration work.
- Reduces delivery risk by making backend gaps visible and sequencing immediate execution priorities.

## 2026-04-16 (Planner + AI Workflow API Stubs for FE Integration)

### Added
- Added `PlanningController` with FE-aligned MVP endpoints for upcoming UI surfaces:
  - `POST /v1/ai/capture`
  - `POST /v1/ai/goals/{goalId}/breakdown`
  - `POST /v1/planner/daily/generate`
  - `POST /v1/planner/reschedule/proposals`
  - `POST /v1/review/weekly/generate`
- Added deterministic scaffold responses for capture drafts, goal breakdown, daily planning, reschedule proposals, and weekly review output shapes.

### Why this improves BE quality
- Unblocks frontend integration work by exposing contract-shaped endpoints ahead of deeper orchestration implementation.
- Preserves deterministic behavior while enabling iterative replacement with production AI/rules engines.

This changelog tracks backend feature progress against the core implementation plan.

## 2026-04-15 (PostgreSQL Persistence Foundation)

### Added
- Added Flyway migration `V1__create_tasks_table.sql` to provision the `tasks` table with integrity constraints and indexes.
- Added JPA-backed persistence adapter (`JpaTaskRepository`) and Spring Data repository wiring for `TaskRepository`.
- Added runtime PostgreSQL wiring and test-profile H2 configuration to keep local tests deterministic.

### Changed
- Replaced in-memory task repository implementation with persistent JPA/Flyway-backed infrastructure.
- Updated backend test suite to run under `test` profile so migrations and persistence are validated in CI-style runs.

### Why this improves BE quality
- Aligns the backend with Phase 0 plan goals by moving from transient memory storage to migration-managed persistence.
- Makes schema evolution explicit, reviewable, and repeatable across environments.

## 2026-04-15 (OpenAPI Contract Draft)

### Added
- Added initial OpenAPI 3.0 contract draft at `apps/backend/openapi.yaml` for current Task APIs:
  - `POST /v1/tasks`
  - `GET /v1/tasks`
  - `PATCH /v1/tasks/{id}`
  - `PATCH /v1/tasks/{id}/status`
  - `GET /v1/tasks/{id}/completion`
  - `PATCH /v1/tasks/{id}/archive`
- Included shared schema components for domain enums and DTOs (`TaskStatus`, `TaskSource`, `EnergyLevel`, create/update payloads).

### Why this improves BE quality
- Gives FE and BE a single contract artifact to align on request/response shapes.
- Reduces integration ambiguity before broader AI/planner endpoints are implemented.

## 2026-04-14

### Added
- Initial task domain scaffold (`Task`, `TaskStatus`, `TaskSource`, `EnergyLevel`) for BE MVP fields.
- Task CRUD baseline endpoints:
  - `POST /v1/tasks`
  - `GET /v1/tasks`
  - `PATCH /v1/tasks/{id}`
- Task status and completion tracking endpoints:
  - `PATCH /v1/tasks/{id}/status`
  - `GET /v1/tasks/{id}/completion`
- API validation constraints for task create/update payloads.
- MockMvc backend tests covering create, invalid input, update, status transition, completion check, and user filtering.

### Current Status
- ✅ BE development started for Phase 0 foundation.
- ✅ Task status update/check-completed flow is available.
- ⚠️ Persistence is still in-memory and must be migrated to PostgreSQL in the next milestone.
- ⚠️ Authentication/authorization middleware still pending.

### Next Up
1. Add PostgreSQL migrations and JPA repositories.
2. Add JWT authentication middleware and per-user scoping.
3. Add OpenAPI spec for v1 endpoints.

## 2026-04-14 (DDD Refactor)

### Changed
- Refactored backend task module into DDD-inspired layers for team scalability:
  - `domain` (entities/value enums + repository port)
  - `application` (use-case orchestration)
  - `infrastructure` (in-memory repository adapter)
  - `interfaces/rest` (controller + transport DTOs)
- Preserved API compatibility for existing task endpoints while improving folder boundaries for teamwork.

### Teamwork Benefit
- Clear ownership boundaries across layers reduce merge conflicts and improve onboarding for parallel backend work.

## 2026-04-14 (Task API Iteration)

### Added
- Task listing enhancements on `GET /v1/tasks`:
  - `status` filter
  - `overdueOnly` filter (based on due date and active status)
  - `page` and `size` pagination controls
- Safe archive API:
  - `PATCH /v1/tasks/{id}/archive` marks task status as `ARCHIVED` without destructive deletion.

### Changed
- Application service task listing now supports composable filtering + pagination.
- MockMvc contract tests expanded to verify status filtering, overdue filtering, pagination, and archive flow.

### Why this improves BE quality
- Improves scalability readiness by avoiding unbounded list responses.
- Enables safer lifecycle handling aligned with “no silent destructive edits” by archiving instead of hard deleting.
