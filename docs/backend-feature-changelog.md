## 2026-06-17 (M09 Core Spec Breakdown Facade Slice)

### Added

- Added the Core `specbreakdown` module with draft, async job status/command, review, materialization, template, and draft attachment REST surfaces.
- Added Flyway-owned Core tables for spec breakdown drafts, jobs, templates, attachments, and Jira/Scrum task metadata columns.
- Wired Core spec jobs through the existing Nova capability facade and emit spec breakdown completion/failure outbox events while using existing task creation events for materialized work.
- Synchronized `apps/backend/openapi.yaml` with the new public Core spec-breakdown request/response shapes.

### Remaining gaps

- The worker is a synchronous facade slice rather than a durable scheduled queue with retry/concurrency enforcement.
- Draft attachments and templates expose the endpoint contracts but need repository-backed persistence/storage integration beyond in-memory controller state.
- Materialization records task hierarchy via existing task creation, but task-level Jira/Scrum field persistence needs a follow-up mapper from draft metadata into the new task columns.

### Verification notes

- Targeted backend verification: `mvn -q -pl apps/backend -am -Dtest=SpecBreakdownControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`.
- Applicable skills: none. Delegated agents: none.

## 2026-06-16 (Daily Planner Cross-User Authorization)

### Changed

- Daily planner requests keep the explicit `userId` contract, but non-privileged callers now receive `403 Forbidden` when requesting a plan for any user other than the authenticated principal.
- ADMIN and MANAGER callers may continue to generate daily plans for another user.
- Documented the daily planner endpoint and authorization behavior in the Core OpenAPI contract.

### Verification notes

- Targeted backend verification: `mvn -q -pl apps/backend -am -Dtest=PlanningControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`.
- Full gate: `make vibe-verify`.
- Applicable skills: none. Delegated agents: none.

## 2026-06-16 (M08 Nova Chat Streaming Facade)

### Added

- Added Core `POST /v1/nova/chat/stream` as an authenticated `text/event-stream` facade for Nova chat turns.
- Extended the Core Nova client with a deterministic SSE fallback backed by the existing non-streaming Nova chat response for local Nova deployments that do not expose streaming.
- Documented the streaming endpoint and `NovaChatStreamChunk` event payload in the Core OpenAPI contract.
- Added MockMvc coverage for authenticated SSE access and Nova failure problem-details mapping.

### Verification notes

- Targeted backend verification: `mvn -q -pl apps/backend -am -Dtest=NovaFacadeControllerTest,RestNovaClientTest -Dsurefire.failIfNoSpecifiedTests=false test`.
- Applicable skills: none. Delegated agents: none.

# Backend Feature Changelog

## 2026-06-16 (M09 Nova Spec Capabilities)

### Added

- Added typed Nova capability registrations for `spec-outline`, `spec-enrich`, `spec-breakdown`, `spec-breakdown-section`, `spec-merge`, and `spec-suggest-links` to begin the M09 spec breakdown pipeline.
- Extended the deterministic mock provider with reviewable Epic → Story → Subtask, enrichment, section, merge, and link-suggestion outputs for targeted Nova tests.
- Added focused typed-capability coverage for M09 required inputs and output shape metadata.

### Verification Notes

- Advanced primary milestone: M09 spec breakdown pipeline, Nova capability foundation slice.
- Core OpenAPI was not changed because this pass added internal Nova capability behavior only; Core `/v1/spec-breakdown/**` facades remain follow-up work.
- Applicable skills: `taskmind-backend-feature`. Delegated agents: none.

## 2026-06-15 (M03 Task Detail OpenAPI Contract)

### Changed

- Synchronized the Core OpenAPI contract with the existing authenticated `GET /v1/tasks/{id}` task detail endpoint.

### Verification Notes

- Advanced primary milestone: M03 task detail frontend/Core integration.
- Runtime backend code was not changed because the endpoint already existed in `TaskController`; this pass documented the contract for frontend consumption.
- Applicable skills: none. Delegated agents: none.

## 2026-06-14

- Fixed project creation so non-privileged users can create projects without supplying `ownerUserId`; Core now derives ownership from the authenticated requester while preserving privileged owner assignment.

## 2026-06-14 (Local Frontend CORS)

### Changed

- Added a Core CORS configuration source for `/v1/**` and `/api/**` so allowed browser origins can be configured without opening production by default.
- Enabled the local profile to allow the Vite frontend origin `http://localhost:5173`, with `TASKMIND_CORS_ALLOWED_ORIGINS` available for local override/cross-device testing.
- Added focused security coverage for local frontend preflight requests.

### Verification Notes

- Primary milestone: local development support for Core/frontend integration.
- Core OpenAPI was not changed because no request or response schema changed.

# Backend Feature Changelog

## 2026-06-14 (M08 Capture Acceptance Path)

### Added

- Added authenticated Core endpoints for accepting and rejecting AI capture drafts under `/v1/ai/capture/accept` and `/v1/ai/capture/reject`.
- Accepted capture drafts now create real requester-scoped tasks through `TaskApplicationService` and emit `ai.suggestion_accepted` funnel events.
- Rejected capture drafts now emit `ai.suggestion_rejected` funnel events with the provided rejection reason.
- Updated the Core OpenAPI contract with capture accept/reject paths and schemas.
- Extended `PlanningControllerTest` coverage for successful accept, authenticated requester scoping, and accept/reject event publication.

### Verification notes

- Targeted backend verification: `mvn -q -pl apps/backend -am -Dtest=PlanningControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`.
- Applicable skills: none. Delegated agents: none.

## 2026-06-14 (M08 Nova Typed Capabilities)

### Changed

- Replaced M08 Nova placeholder capability registrations with concrete typed capability classes for capture, describe task, autocomplete task, translate task, weekly review, goal breakdown, project brief, duration estimate, rationale phrase, and dashboard insights.
- Added required-input validation and strict output schema metadata for the typed Nova capabilities while keeping provider routing and deterministic mock outputs inside Nova.
- Added focused Nova coverage for each typed capability input validation path and deterministic output shape.

### Verification Notes

- Advanced primary milestone: M08 AI features, Nova capability implementation slice.
- Core OpenAPI was not changed because this pass kept existing Core facade contracts and made Nova normal mock-provider responses match those expected shapes.
- Applicable skills: none. Delegated agents: none.

## 2026-06-14 (AI outbox funnel events)

### Changed

- Replaced the no-op Core AI event publisher with an outbox-backed publisher for `ai.capture_submitted`, `ai.suggestion_accepted`, and `ai.suggestion_rejected`.
- Added Relay AI funnel projection handling for daily capture, accepted suggestion, and rejected suggestion counts.
- Added backend and Relay tests covering capture outbox writes and AI funnel projection ingestion.

# Backend Feature Changelog

## 2026-06-14 (M08 Core AI Facade Utilities)

### Added

- Added a Core AI facade application service with deterministic local fallbacks for capture, task describe, task autocomplete, and task translate flows.
- Wired `/v1/ai/capture`, `/v1/ai/tasks/describe`, `/v1/ai/tasks/describe/autocomplete`, and `/v1/ai/tasks/translate` through the Core-to-Nova capability client while preserving local fallback behavior when Nova is unavailable or returns placeholder output.
- Registered Nova placeholder capability IDs for the new M08 task utility capabilities and synchronized the Core OpenAPI contract for the added AI utility endpoints.

### Verification Notes

- Advanced primary milestone: M08 AI features, Core facade utility slice.
- No frontend UI implementation or browser E2E was completed in this backend-focused pass.

## 2026-06-14 (Agent Workflow Token Optimization)

### Added

- Added compact AI lifecycle templates, an agent memory index, and local Codex skills for backend, frontend, and static QA workflows so Codex Cloud sessions can start from focused context instead of reloading broad build-kit documentation.
- Extended the session workflow with read-only QA rules, skill discovery guidance, and memory-first token-saving inspection.
- Added a token-retrospective cadence that turns token reports into workflow, memory, or skill improvements when provider counts are available.

### Verification Notes

- Documentation and agent-workflow-only change; no product runtime behavior, Core OpenAPI contract, frontend UI, or browser E2E was affected.
- Applicable skills: none. Delegated agents: none.

## 2026-06-12 (M07 Core Nova Facade)

### Added

- Added the Core-side Nova facade for authenticated `/v1/nova/chat`, `/v1/nova/capabilities`, and `/v1/nova/runs/{runId}` endpoints backed by the shared `taskmind-ai-contracts` DTOs.
- Added environment-backed Core Nova client configuration and server-to-server `X-Service-Token` forwarding for calls to Nova internal endpoints without moving prompts or provider logic into Core.
- Updated the Core OpenAPI contract and shared AI capability-list DTOs for the new facade shapes.
- Added focused Core coverage for facade authentication, deterministic stubbed Nova responses, service-token forwarding, and Nova error mapping.
- Stabilized scheduler duplicate-generation coverage so the full verification gate remains date-independent after June 2026.

### Verification Notes

- Advanced primary milestone: M07 Nova AI, Core facade slice.
- No frontend UI implementation or browser E2E was required for this backend-only slice.
- Applicable skills: none. Delegated agents: none.

## 2026-06-12 (M07 Nova Service Foundation)

### Added

- Added Nova service-token protection for internal AI routes while keeping `/api/health` public.
- Added the Flyway-owned `ai.ai_runs` audit schema, deterministic mock provider, provider router, capability registry/runtime, chat session handling with Redis-ready storage and in-memory test fallback, and internal Nova REST endpoints for chat, capability execution, audit lookup, and capability discovery.
- Added focused Nova coverage for internal route security, mock-provider determinism, capability execution, chat continuation behavior, and audit writes.

### Verification Notes

- Advanced primary milestone: M07 Nova AI.
- Core AI facades and OpenAPI synchronization remain follow-up work for the Core slice of M07; this pass scoped changes to `apps/ai` plus Nova reference docs.
- No frontend UI implementation or browser E2E was required for this Nova foundation slice.
- Applicable skills: none. Delegated agents: none.

## 2026-06-07 (M06 Relay Activity Search Ingest Semantics)

### Changed

- Clarified Relay activity indexing as part of the ingest atomic unit: indexing failures now write DLQ/metrics evidence and roll back event-store/projection writes so `analytics.event_store` dedupe cannot make a missing OpenSearch document unretryable.
- Added focused Relay coverage proving a task event whose first indexing attempt fails can be retried and indexed with the same event id.

### Verification Notes

- Advanced primary milestone: M06 Search & Storage on AWS.
- No frontend UI implementation or browser E2E was required for this Relay backend fix.
- No Codex skill or delegated sub-agent was used.

## 2026-06-06 (M04 Scheduler Status Clarification)

### Status correction

- M04 backend scheduler progress should be read as an advanced Core slice, not full milestone completion: Core now has the scheduler-owned contract/module path advanced through scheduling preferences, scheduled block APIs, deterministic schedule generation/reschedule proposal behavior, and OpenAPI contract sync.
- Full M04 remains open until the `/calendar` frontend UI is verified end-to-end in a browser and the repository-wide `make vibe-verify` gate is green in the same closeout context.
- Do not mark M04 complete from backend-only evidence; calendar UI implementation plus browser proof are required by the milestone definition of done.

### Verification notes

- Exact backend/Core command to use for targeted M04 traceability: `cd apps/backend && mvn -q -Dtest=SchedulerControllerTest,AutoSchedulerTest,SchedulerCommandServiceTest test`.
- Exact repository gate run for this traceability update: `make vibe-verify` (passed on 2026-06-06).
- Browser E2E outcome: skipped/not proven for the backend/Core slice because this container has no browser binary, no Playwright/Cypress E2E dependency, no Docker runtime, and this pass did not run the `/calendar` browser flow on `localhost:5173` with `superadmin@taskmind.local` / password `1` / OTP `1`.
- Applicable skills: none. Delegated agents: none.

## 2026-06-06 (M06 Search & Storage on AWS)

### Added

- Added Relay activity-event search indexing behind `taskmind.activity-search.enabled`, with task domain events mapped to the `activity-events` OpenSearch-compatible document shape.
- Added Core `GET /v1/activity/search` facade behavior that requires authentication and returns a predictable `503` when search is not configured.
- Added Core task-scoped attachment upload, list, download, and delete flows with DDD layering, Flyway-managed metadata, filesystem test/local storage, and S3-ready object storage.
- Updated the Core OpenAPI contract for activity search and attachment endpoints and added targeted backend/Relay coverage for disabled search, ownership, persistence, storage, indexing, and contract alignment.

### Verification Notes

- Advanced primary milestone: M06 Search & Storage on AWS.
- No frontend UI implementation or browser E2E was completed in this backend-focused pass; frontend hooks/components remain follow-up work for the UI slice.
- No Codex skill or delegated sub-agent was used.

## 2026-06-06 (M04 Scheduler OpenAPI Contract Sync)

### Changed

- Expanded the public Core OpenAPI scheduler contract with examples for scheduling preferences, scheduled blocks, schedule generation, and reschedule proposals.
- Called out optimistic-lock version fields on scheduler update request DTOs and aligned the complete-block response set with the Core controller's ownership guard.

### Verification Notes

- Advanced primary milestone: M04 Scheduler.
- Documentation-only Core contract sync; no frontend changes, UI E2E, Codex skills, or delegated sub-agents were applicable.

## 2026-06-06 (M05 Eventing + Relay)

### Added

- Added the shared `taskmind-events` library with a domain event envelope, event type registry, JSON schema validation, mapper, and transport abstraction.
- Added Core activity and transactional outbox tables after the scheduler migration ceiling, plus the analytics schema bootstrap consumed by Relay.
- Added Core activity/event/outbox packages so task and project writes persist activity and outbox rows in the same transaction as state changes.
- Added Redis Stream outbox publishing to `taskmind.events` with retryable publish failures and backpressure checks.
- Added the Relay service with stream ingest, event-store idempotency, task/project projections, daily metrics, DLQ handling, service-token-protected `/internal/**` routes, and context query endpoints.

### Verification Notes

- Advanced primary milestone: M05 Eventing + Relay.
- No frontend changes or UI E2E were applicable. No Codex skill or delegated sub-agent was used.

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
- Completed remaining M08 Core/Nova AI flows by adding typed facade methods and REST endpoints for goal breakdown, weekly review, project brief, scheduler duration estimates, scheduler rationale phrases, and dashboard insights with deterministic local fallbacks.
- Replaced M08 placeholder Nova capability behavior for capture, goal breakdown, weekly review, project brief, describe/autocomplete/translate, duration estimate, rationale phrase, and dashboard insights with typed deterministic mock outputs.

- **M11 Notifications:** Added Core notification DDD module with in-app records, per-user preferences, SSE delivery hub, reminder and digest jobs, Slack/email provider ports with deterministic local adapters, delivery attempt tracking, reminder state, ShedLock support, and OpenAPI notification contracts.

## 2026-06-17 (M13 Rate Limiting)

### Added

- Added Core rate-limit module with configurable anonymous/IP, authenticated/user, auth-flow, and AI-heavy endpoint buckets.
- Integrated rate limiting after bearer authentication so public Core API traffic is limited by user identity when available and internal service-only paths are skipped.
- Added focused tests for disabled mode, anonymous and authenticated limits, 429 response headers, and Redis fallback behavior.

### Notes

- Rate limiting can be disabled through `taskmind.ratelimit.enabled` for local debugging and test profiles.
