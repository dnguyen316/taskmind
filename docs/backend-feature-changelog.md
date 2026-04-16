# Backend Feature Changelog

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
- ⚠️ Project endpoint test coverage is not yet present in the test suite.
- ⚠️ Authentication/authorization remains pending for per-user enforcement.
- ⚠️ OpenAPI spec currently lags behind implemented project and planner/AI stub endpoints.

### Task Plan (Next Execution Slice)
1. Add `ProjectController` integration tests for create/list/get/update/archive + conflict cases.
2. Expand `openapi.yaml` to include project endpoints and planner/AI stub contracts.
3. Introduce JWT auth middleware and enforce owner/user scoping across task/project queries.
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
