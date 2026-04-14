# Backend Feature Changelog

This changelog tracks backend feature progress against the core implementation plan.

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
