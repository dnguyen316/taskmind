## 2026-07-18 - Attachment S3 Sydney defaults

### Changed

- Changed Core attachment S3 region defaults and local LocalStack fallback from `us-east-1` to Sydney (`ap-southeast-2`) while preserving `TASKMIND_ATTACHMENTS_S3_REGION` overrides.

### Tests

- Added attachment storage property coverage for the POJO default, explicit binding override, and `application.properties` environment placeholder fallback.

### Closeout notes

- Primary milestone: M06 Search/storage AWS.
- Skills used: none.
- Agent delegation: none.

## 2026-07-15 - Task type validation problem metadata

### Changed

- Added task-type validation problem metadata for project, key, name, and unknown-type failures so clients receive stable `field` and `reason` values alongside the existing validation code.
- Documented optional `ProblemDetail.field` and `ProblemDetail.projectId` properties in the Core OpenAPI contract.

### Tests

- Added task type controller coverage asserting `code`, `field`, and `reason` for missing project ID, key, and name validation failures.

### Closeout notes

- Primary milestone: M02 backend foundation/error handling hardening.
- Skills used: none.
- Agent delegation: none.

## 2026-07-12 - Notification optimistic locking hardening

### Changed

- Hardened single-notification repository updates with a compare-and-swap version predicate so stale read-state writes fail with the shared 409 conflict mapping.
- Documented that notification read-all remains an idempotent bulk operation that intentionally bypasses per-row optimistic locking.

### Verification notes

- Added repository integration coverage for stale single-notification updates.
- Advanced primary milestone: M11 Notifications.
- Applicable skills: none. Delegated agents: none.


## 2026-07-15 - Task API unreadable-task 404 policy

- Documented the Task API policy to return `404 Not Found` for both missing and unreadable task ids to avoid existence disclosure, while keeping non-task-scoped authorization failures as `403 Forbidden`.
- Aligned task detail, hierarchy, mutation, status, archive, and task-link operations to use the same safe not-found behavior for unreadable task ids.
- Added controller coverage for missing tasks, personal tasks owned by another user, project-member task reads, and non-member project-task reads.

## 2026-07-15 - Stable task validation reasons

### Changed

- Added stable task validation `reason` values for known hierarchy and task-type rule failures while preserving the top-level `TASK_VALIDATION_FAILED` problem code.
- Documented the optional `ProblemDetail.reason` property in the Core OpenAPI contract.

### Tests

- Added task controller coverage for invalid hierarchy levels and invalid task-type levels asserting both `400 Bad Request` and the stable validation reason.

### Closeout notes

- Primary milestone: M02 backend foundation/error handling hardening.
- Skills used: none.
- Agent delegation: none.

## 2026-07-12 - Notification preference concurrency hardening

### Changed

- Hardened notification preference creation so concurrent first-time writes for a user converge on one row instead of surfacing duplicate-key failures.
- Added compare-and-swap version checks to notification preference updates and documented the optional request version in Core OpenAPI.

### Verification notes

- Added backend coverage for concurrent first-time preference writes and stale update rejection through both service and REST flows.
- Applicable skills: none. Delegated agents: none.

# Backend Feature Changelog

## 2026-07-12 (M05 Relay Redis Stream Consumer Groups)

### Changed

- Updated Relay Redis stream ingestion to create/verify the `taskmind-relay` consumer group at startup, read new records through Redis consumer-group semantics, acknowledge successes with `XACK`, and keep event-store idempotency as the redelivery guard.
- Added bounded pending-entry retry handling that claims stale records for intentional redelivery and moves repeatedly failed pending records to the Redis stream dead-letter key before acknowledgement.
- Added focused Relay consumer job tests covering group creation, non-overlapping new-entry delivery across two consumers, intentional pending redelivery, and retry-ceiling dead-letter acknowledgement.

### Verification Notes

- Advanced primary milestone: M05 Eventing + Relay hardening.
- Backend-only change; no frontend UI E2E, Codex skill, or delegated sub-agent was used.

## 2026-07-12 - Reports rollup availability metadata

### Changed

- Added real Core reports values for projects-created and assignee throughput from existing analytics rollups.
- Added reports availability/freshness metadata so clients can distinguish live rollups from metrics that are not projected yet, including priority segments.
- Updated the Core OpenAPI reports response contract.

### Verification notes

- Added backend coverage for populated reports rollups and empty/unavailable metric metadata.
- Applicable skills: none. Delegated agents: none.

## 2026-07-11 - OpenSearch domain access policy

- Added AWS data-plane IaC for an explicit OpenSearch domain access policy that scopes activity-search access to Core and Relay ECS task roles without wildcard principals.
- Exposed compute task role ARNs for composed Terraform/OpenTofu roots and documented the OpenSearch role-scoped policy expectations.

## 2026-07-11 - AI facade provenance metadata

### Changed

- Added `source` and `degraded` provenance metadata to Core AI facade responses so clients can distinguish Nova-generated results from local fallback output.
- Updated the Core OpenAPI AI response schemas for capture, goal breakdown, weekly review, task describe/autocomplete/translate, project brief, scheduler duration estimates, and rationale phrases.

### Tests

- Added AI facade service coverage for Nova success, Nova unavailability, invalid Nova output shape, and degraded local fallback metadata.

## 2026-07-05 - Nova AI run timestamp binding hardening

### Changed

- Converted Nova AI run audit `Instant` values to UTC `OffsetDateTime` values before binding `TIMESTAMP WITH TIME ZONE` JDBC parameters so PostgreSQL can persist start, success, and failure timestamps without driver cast errors.

### Verification notes

- Exact verification commands for this slice: `mvn -q -pl apps/ai -am -Dtest=AiRunAuditRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false test`.
- Applicable skills: none. Delegated agents: none.

## 2026-07-02 - Rich task filters and saved views

### Changed

- Extended Core task listing with richer filters for due-today, overdue, blocked, unassigned, no-due-date, stale, archived, priority, project, assignee, pagination, and sorting.
- Added per-user saved task view persistence and REST endpoints backed by Flyway-owned `task_saved_views`.
- Updated the OpenAPI task query and saved-view contracts.

### Tests

- Added saved-view ownership coverage and ran task controller/OpenAPI contract coverage for the touched API surface.

## 2026-07-01 - Spec breakdown hierarchy materialization hardening

### Changed

- Made spec-breakdown materialization idempotent after a draft has already been materialized so repeated Core calls do not duplicate generated tasks.
- Returned a client validation error for invalid candidate hierarchy payloads during materialization.
- Added controller coverage for nested hierarchy creation, validation, idempotency, and user ownership filtering.

### Verification notes

- Targeted backend verification: `mvn -q -pl apps/backend -am -Dtest=SpecBreakdownControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`.
- Applicable skills: none. Delegated agents: none.

## 2026-06-30 (AI approval gates)

### Changed

- Added workflow-template approval gate fields for read-only auto approval and comments, branch, pull-request, and task-mutation approval requirements.
- Added Core AI task-resolution action proposal persistence and review endpoints for listing, approving, and rejecting proposed Nova actions.
- Updated Nova mock task-resolution output to return reviewable action proposals rather than treating blocked tool calls as direct execution.

### Tests

- Ran targeted backend compile checks for Core and Nova while this focused slice was implemented.

## 2026-06-28 - Dashboard Relay JSON scalar coercion

- Fixed Core dashboard aggregation to accept Relay REST JSON scalar values for task/project IDs and timestamps instead of assuming in-process UUID and OffsetDateTime instances.
- Added dashboard controller coverage for Relay-style string UUID/date-time values so `/v1/dashboard` no longer returns a 500 when Core consumes Relay over HTTP.

### Verification notes

- Exact verification commands for this slice: `mvn -q -pl apps/backend -am -Dtest=DashboardControllerTest#acceptsRelayJsonScalarValues -Dsurefire.failIfNoSpecifiedTests=false test`; `mvn -q -pl apps/backend -am -Dtest=DashboardControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`; `make vibe-verify`.
- Applicable skills: none. Delegated agents: none.

## 2026-06-28

- M02 backend: task type definitions now carry validation metadata for default hierarchy level, allowed levels, child/container behavior, and nullable system kind; Core task create/update validates loaded type definitions so custom task types can opt into or out of hierarchy levels while inactive types remain unassignable.

## 2026-06-25 - Auth OTP attempt limit hardening

- Added configurable Core OTP verification attempt limits with `taskmind.auth.otp.max-attempts` / `TASKMIND_AUTH_OTP_MAX_ATTEMPTS` defaulting to `5`.
- Invalid OTP verification now increments challenge attempts and consumes the active challenge when failures reach the configured maximum, requiring a freshly issued OTP before verification can succeed.
- Added auth integration coverage for successful verification before the limit, failed-attempt counting, lockout of the consumed challenge, and successful verification with a newly issued OTP.

## 2026-06-22 (OpenSearch Recommendation Query Design)

### Changed

- Documented the Relay activity-event recommendation index mapping with `title.autocomplete`, normalized keyword fields, and searchable `payloadText`.
- Populated normalized Relay recommendation document fields for entity type, event type, and status.
- Updated Core recommendation queries to boost exact title/entity matches, autocomplete title prefixes, payload text, and recent events while returning typed hits with OpenSearch scores.

### Verification notes

- Exact verification commands for this slice: `mvn -q -pl apps/backend,apps/relay -am -Dtest=ActivitySearchElasticsearchRepositoryTest,ElasticsearchIndexerTest,ActivityEventIndexMappingTest -Dsurefire.failIfNoSpecifiedTests=false test`; `cd apps/frontend && npm run typecheck`; `make vibe-verify`.
- Applicable skills: none. Delegated agents: none.

## 2026-06-21 (Relay Recommendation Index Entity Types)

### Changed

- Replaced Relay's task-only OpenSearch indexing guard with the configurable `taskmind.activity-search.recommendation-entity-types` allowlist, defaulting to task, project, attachment, document, spec, and spec-document entities.
- Made Relay activity search documents derive titles/status text safely from non-task payload shapes such as projects and attachments.
- Aligned the frontend activity entity filter options with the Relay recommendation indexing allowlist.

### Verification notes

- Exact verification commands for this slice: `mvn -q -pl apps/relay -am -Dtest=ElasticsearchIndexerTest,ActivityEventDocumentTest -Dsurefire.failIfNoSpecifiedTests=false test`; `cd apps/frontend && npm run typecheck`; `make vibe-verify`.
- Applicable skills: none. Delegated agents: none.

## 2026-06-21 (Typed Activity Search Recommendations)

### Changed

- Added Core `GET /v1/activity/search/recommendations` for typed recommendation items while keeping `/suggest` as the legacy string endpoint.
- Mapped OpenSearch activity hits to recommendation metadata including label, entity type/id, event type, status, title, occurred time, and route name.
- Updated Core OpenAPI with the typed recommendation schema.

## 2026-06-21 (Nova AI Run Timestamp Binding)

### Changed

- Bound Nova AI run audit `Instant` values with explicit `TIMESTAMP_WITH_TIMEZONE` JDBC types so PostgreSQL can persist run start, success, and failure timestamps without driver type inference errors.

### Verification notes

- Exact verification commands for this slice: `mvn -q -pl apps/ai -am -Dtest=AiRunAuditRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false test`; `mvn -q -pl apps/ai -am test`; `make vibe-verify`.
- Applicable skills: none. Delegated agents: none.

## 2026-06-21 (Relay JDBC Timestamp Binding)

### Changed

- Converted Relay `Instant` JDBC parameters to `java.sql.Timestamp` before binding so PostgreSQL can persist DLQ, event store, and projection timestamps without driver type inference errors.

### Verification notes

- Exact verification commands for this slice: `mvn -q -pl apps/relay -am -Dtest=RelayJdbcParametersTest,RelayIndexingFailureIngestTest test`; `make vibe-verify`.
- Applicable skills: none. Delegated agents: none.

## 2026-06-20 (Activity Search Structured Filters)

### Changed

- Normalized activity search filters for entity type, status, project id, event type, and occurred-at date range before repository dispatch.
- Extended OpenSearch activity search queries and suggestions to keep the user isolation filter while adding structured filter clauses.

### Verification notes

- Exact verification commands for this slice: `mvn -q -pl apps/backend -am -Dtest=ActivitySearchControllerTest,ActivitySearchElasticsearchRepositoryTest test`; `cd apps/frontend && npm run typecheck`; `make vibe-verify`.
- Applicable skills: none. Delegated agents: none.

## 2026-06-19 (Activity Search Suggestions)

### Added

- Added authenticated Core `/v1/activity/search/suggest` suggestions backed by the activity search repository and documented the response contract in OpenAPI.
- Implemented OpenSearch suggestion lookup with the same userId filter as activity search across title, event type, status, and payload text fields.

### Tests

- Added controller coverage for disabled/authenticated suggestion behavior and repository coverage for the generated OpenSearch query and de-duplicated suggestion results.

## 2026-06-19 (M10 Integration Import Idempotency)

### Changed

- Made repeated Jira/GitHub issue imports idempotent by detecting existing external issue links before task creation and reporting duplicates through `skippedCount`.
- Added external link identity uniqueness constraints for provider/resource external id and key to guard against duplicate imports at the database layer.
- Expanded integration controller coverage for first import, repeated import, partial duplicate import, and accurate import/skip counts.

### Verification notes

- Targeted backend verification: `mvn -q -pl apps/backend -am -Dtest=IntegrationControllerTest test`.
- Applicable skills: none. Delegated agents: none.

## 2026-06-19 (M11 Notification Delivery Completion)

### Changed

- Replaced fake-by-default notification email/Slack delivery with configurable SMTP/SES-compatible email delivery and Slack webhook/API delivery while keeping fake delivery defaults only for local, staging, and test profiles.
- Added notification delivery attempt coordination so successful and failed external channel attempts are persisted and failed attempts observe a configurable retry backoff before jobs retry them.
- Updated notification digest and Slack notification paths to record provider failures without losing in-app notifications or stopping scheduled processing.

### Verification notes

- Targeted backend verification: `mvn -q -pl apps/backend -am -Dtest='Notification*Test' -Dsurefire.failIfNoSpecifiedTests=false test`.
- Applicable skills: none. Delegated agents: none.

## 2026-06-18 (M09 Spec Breakdown Async Worker)

### Changed

- Refactored Core spec breakdown job creation so `POST /v1/spec-breakdown/drafts/{id}/jobs` persists a `QUEUED` job and returns `202 Accepted` without invoking Nova in the request path.
- Added a scheduled spec breakdown worker that claims queued jobs, marks them `RUNNING`, invokes Nova outside the queueing transaction, records output checkpoints, and completes jobs as `SUCCEEDED`, `FAILED`, `PAUSED`, or `CANCELED`.
- Updated the OpenAPI job response description and schema examples to document the queued asynchronous workflow and command flags.

### Verification notes

- Targeted backend verification: `mvn -q -pl apps/backend -am -Dtest=SpecBreakdownWorkerTest,SpecBreakdownControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`.
- Applicable skills: none. Delegated agents: none.

## 2026-06-18 (M09 Spec Breakdown Attachment Persistence)

### Changed

- Replaced the in-memory spec breakdown draft attachment controller state with a DDD application service, repository port, Flyway-backed JPA metadata persistence, and shared object-storage port usage.
- Spec breakdown draft attachments now enforce authenticated draft access before upload, list, and delete, hide storage keys from API responses, scope lists to the requested draft, and soft-delete metadata after best-effort object cleanup.
- Added OpenAPI coverage for the public Core `/v1/spec-breakdown/drafts/{draftId}/attachments` upload/list/delete endpoints.

### Verification notes

- Targeted backend verification: `mvn -q -pl apps/backend -am -Dtest=SpecBreakdownAttachmentControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`.
- Applicable skills: none. Delegated agents: none.

## 2026-06-18 (M06 Attachment Download Streaming)

### Changed

- Refactored Core task attachment downloads to stream object storage reads through Spring `Resource` responses instead of materializing full objects as byte arrays, while preserving metadata-backed `Content-Length` headers.

### Verification Notes

- Advanced primary milestone: M06 Search & Storage on AWS.
- No Codex skill or delegated sub-agent was used.

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

## 2026-06-18 (Spec Breakdown Templates)

### Changed

- Replaced the Core spec-breakdown template controller's in-memory map with a DDD-aligned domain model, application service, repository port, and JPA adapter backed by `spec_breakdown_templates`.
- Added WebMvc coverage for template create, project list, update, delete, and not-found responses.
- Added repository persistence coverage for saving, reading, updating, listing, and deleting spec-breakdown templates.
- Documented update and delete template endpoints in the Core OpenAPI spec.

## 2026-06-18 (M13 Observability)

### Added

- Added local Grafana provisioning for the Prometheus datasource and the TaskMind Observability dashboard covering Core API latency/outbox lag, Relay stream processing/dead letters, and Nova token/LLM latency metrics.
- Added request correlation filters, MDC helpers, request logging properties, and problem-detail correlation enrichment helpers across Core, Relay, and Nova.
- Configured Core outbound Relay and Nova RestClient beans to propagate the active `X-Correlation-Id` header.
- Added per-service Logback configuration with readable local/test console output and JSON console output under the `prod` profile.
- Added focused servlet filter tests covering generated and inbound correlation IDs, response header emission, and MDC cleanup.

## 2026-06-19 (Integration Credential Storage Hardening)

- Replaced the integrations token XOR obfuscation with AES-GCM authenticated encryption, storing ciphertext as `enc:v2:gcm:<key-version>:<nonce>:<ciphertext>` so credentials carry key-version and nonce metadata.
- Added startup validation that requires `taskmind.integrations.token-key` outside local/test profiles and fails prod when the secure key is absent, while keeping legacy `enc:` XOR values decryptable with the configured key for migration compatibility.
- Added TokenCipher coverage for wrong-key failures, tamper rejection, blank/null handling, prod missing-key guardrails, and legacy decrypt compatibility.

## 2026-06-19 (M10 Real Integration Provider Clients)

### Changed

- Replaced placeholder Jira, GitHub, and wiki integration clients with real HTTP calls using each connection's configured provider base URL and decrypted bearer access token.
- Integration import and publish services now resolve credentials from the linked `IntegrationConnection` before invoking provider APIs, so project keys/repository names are no longer treated as sufficient credentials.
- Added stable provider failure mapping for auth failures, provider 4xx/5xx responses, and rate limits, including retry-safe metadata on Core problem responses.

### Tests

- Added focused provider client tests for successful Jira import/publish, GitHub import, wiki publish, provider auth failure, rate limiting, and 5xx retry-safe mapping.

## 2026-06-19 (Task REST Optimistic Lock Contract)

### Changed

- Task REST reads and mutation responses now use an explicit `TaskResponse` DTO instead of serializing the domain `Task` record directly.
- Core task response payloads now expose the optimistic-lock `version` so clients can submit stale-update guards on subsequent detail edits.
- Updated the Core OpenAPI `Task` schema to document the `version` field.

### Tests

- Expanded `TaskControllerTest#rejectsStaleTaskUpdateWithConflict` to assert create, detail read, and list task responses include numeric versions before verifying stale updates return `409 Conflict`.

## 2026-06-19 (Integration Publish Identity)

### Changed

- Integration publish idempotency now scopes lookup to the exact task/project-link pair instead of task/provider, so one TaskMind task can be published to multiple Jira project links or wiki spaces for the same provider.
- Added a publish-record uniqueness constraint on `(task_id, project_link_id)` to prevent duplicate publish records for the same linked destination while allowing separate links under the same provider.

### Tests

- Expanded integration controller coverage for publishing one task to two Jira links, publishing one task to two wiki spaces, and idempotent re-publish behavior for the original link.

## 2026-06-20 (Activity Search AI Assist)

- Added authenticated Core `POST /v1/activity/search/assist` facade that sends natural-language search intent to Nova and returns a structured query proposal.
- Added Nova activity-search assist validation that strips unsafe query syntax and rejects unsupported filters until the activity search model formally supports them.
- Updated Core OpenAPI for the activity search assist request/response contract.

## 2026-06-20 (Spec Breakdown Rate Limit Path)

### Changed

- Corrected Core's default AI-heavy rate-limit path prefix from the stale plural spec-breakdown prefix to `/v1/spec-breakdown/` so spec-breakdown draft and job endpoints use the intended AI-heavy bucket.

### Tests

- Added rate-limit filter coverage proving `/v1/spec-breakdown/drafts` consumes the AI-heavy bucket for authenticated users.

## 2026-06-21 (Core/Nova Service Token Alignment)

### Changed

- Standardized Core-to-Nova service-token configuration on `TASKMIND_NOVA_SERVICE_TOKEN`, with `TASKMIND_AI_SERVICE_TOKEN` retained as a deprecated alias for older Nova deployments.
- Matched Core and Nova local fallback tokens and required explicit non-local service-token secrets in production profiles.
- Added a local `infra/env/.env.example` template and Makefile env-file sourcing so local Core and Nova app runs receive the same shared token intentionally.

### Tests

- Added configuration tests for Core/Nova local defaults and production token placeholders, and expanded Nova security coverage for both `X-Service-Token` and bearer-token internal auth.

## 2026-06-25 (Staging Auth Bypass Hardening)

### Changed

- Disabled the E2E auth bypass and fixed OTP code in generic staging configuration by default.
- Added a dedicated `e2e` profile for isolated browser E2E runs that intentionally need the super-admin bypass.
- Hardened bypass startup validation so enabled bypasses require the `local`, `test`, or `e2e` profile.

### Tests

- Expanded `AuthE2eBypassGuardTest` to cover disabled staging defaults, rejected staging-only forced bypasses, and staging plus `e2e` opt-in behavior.

## 2026-06-26 (Nova Local Datasource Defaults)

### Changed

- Added default Nova datasource and Flyway schema settings to the shared AI service configuration so `make run-ai` with the `local` profile can create the `ai` schema against the local Postgres database instead of failing before startup when no datasource URL is provided.

### Tests

- Expanded Nova local-default configuration coverage to assert the local datasource, driver, and Flyway schema defaults used by the AI service.

## 2026-06-27 (Core AI Workflow Templates)

### Changed

- Added Core-owned, project-scoped AI workflow template storage for task resolution, bug triage, and PR review templates, including tool allow-list, approval policy, default model policy, audit timestamps, archive state, and optimistic-lock versioning.
- Added authenticated REST endpoints to create, list, get, update, and archive active templates, with read access gated by project membership and management access gated by project owner/admin or privileged roles.
- Documented the public AI workflow template contract in `apps/backend/openapi.yaml` and the API reference.

### Tests

- Added controller coverage for CRUD, member/admin authorization, optimistic-lock conflict handling, and hiding archived templates from list/get responses.

## Configurable task types

- Added `V27__create_configurable_task_types.sql` to introduce `task_types`, seed system type definitions, and backfill `tasks.task_type_id` while preserving the legacy `tasks.task_type` key during the deprecation window.
- Added Core task type DDD module and `/v1/task-types` read/create/update endpoints; task create/update now resolve stable string task type keys through active configured definitions.

## 2026-06-28 - Nova task resolution capability

- Added the `task-resolution-agent` Nova capability with structured validation for task, project, GitHub repository, workflow template, allowed tools, and approval policy inputs.
- Added typed AI contract DTOs for task-resolution plans, action proposals, and Core-routed tool calls.
- Extended Nova AI run audit metadata with prompt version and validation outcome fields.

## 2026-06-28 - Core GitHub internal tools

### Changed

- Added service-token-protected Core internal GitHub tool endpoints for Nova to read issues and pull requests and to create comments, branches, and pull requests against linked repositories.
- Enforced Nova job context scope headers, linked repository operation flags, required idempotency keys for mutations, and provider error mapping without returning integration secrets.

### Tests

- Added integration-controller coverage for missing service tokens, permission denial, unknown/out-of-scope repository links, provider failure problem details, and idempotent mutation retry behavior.

## 2026-07-01 - Task type project access

### Changed

- Gated project-scoped task type listing and management through project ownership, membership, admin membership, or privileged roles while preserving global/system type visibility for non-members.
- Updated the Core task type OpenAPI responses to document authentication and authorization failures.

### Tests

- Added task type controller coverage for non-member list/create/patch denial and owner/member/admin permitted paths.

## 2026-07-01 - Global task type key uniqueness

### Changed

- Added a Core Flyway migration to enforce global task type key uniqueness with a partial unique index on `task_types(type_key)` for rows where `project_id IS NULL`, while retaining the existing `(project_id, type_key)` constraint for project-scoped uniqueness. This advances configurable task type hardening.

### Tests

- Expanded Flyway startup integration coverage to prove duplicate global task type keys are rejected after applying the migration chain.

## 2026-07-01 - Task type optimistic update contract

### Changed

- Added a `version` field to the Core task type update request contract and documented stale update conflicts for `PATCH /v1/task-types/{id}`.
- Enforced optimistic version checks in the task type application service before saving task type edits.

### Tests

- Added controller coverage for matching-version task type updates and stale-version `409 Conflict` problem responses.

## Onboarding workspace setup

- Added Core onboarding state on users, onboarding completion APIs, seeded workspace templates, and local/demo-only demo workspace reset with sample projects, tasks, schedule blocks, and activity events.
- Added frontend onboarding routes and setup flow that redirects authenticated users until onboarding is complete.

## 2026-07-05 - Project health facade

### Changed

- Added a Core project health facade endpoint that calculates deterministic task metrics for visible projects, including completion, overdue, blocked, unassigned, stale, deadline-risk, workload, and a generated narrative once metrics exist.
- Updated the Core OpenAPI contract with the project health response schema.

### Tests

- Added project health metric accuracy, empty-project behavior, and project health access-control coverage.

## 2026-07-08 - Team membership management

### Changed

- Added Team-owned project membership management commands, REST DTOs, and Core endpoints for assigning project members, changing project roles, removing project members, and delegating global role changes to an auth-owned service.
- Enforced `team.manage` / `project.members.manage` for Team project membership writes and `rbac.roles.manage` for global role changes, while reusing the project membership application service for project-scoped persistence.
- Updated the Core OpenAPI contract for the new Team management endpoints.

### Tests

- Added Team controller coverage for permitted project assignment/change/removal and forbidden project/global role management attempts.

## 2026-07-08 - Project role capability matrix

### Changed

- Documented the project membership role capability matrix in Core and centralized project read, mutation, member listing, and member management checks through an application-level authorization service.
- Allowed project `ADMIN` memberships to update and archive project settings alongside project owners, while keeping `MEMBER` and `VIEWER` access read/list-only.
- Updated the Core OpenAPI contract for project mutation and membership endpoints to describe role-based authorization outcomes.

### Tests

- Added project and project-membership controller coverage for `OWNER`, `ADMIN`, `MEMBER`, and `VIEWER` read, mutation, list, and member-management capabilities.

- Auth refresh sessions now rotate through an HttpOnly SameSite cookie (`taskmind_refresh`) on login, signup verification, token refresh, and logout; legacy refresh-token request bodies remain accepted as a fallback.

## 2026-07-12 - Scoped Nova chat context contract

### Changed

- Extended the shared Nova chat request contract and Core OpenAPI schema with optional `projectId`, `taskId`, and `scope` fields so clients can send user-selected workspace context boundaries.
- Included scoped chat context in Nova provider input and chat request hashing for auditability.

### Tests

- Updated AI contract serialization coverage for scoped chat requests.

## 2026-07-12 - Outbox row-level publisher claiming

### Changed

- Added Core outbox claim metadata and row-level claiming before publication so concurrent pollers only publish rows claimed by their own poller instance while preserving Relay idempotency as a downstream safety net. This advances the backend eventing/outbox reliability path.

### Tests

- Added OutboxPollerJob concurrency coverage proving simultaneous pollers do not publish the same outbox event.

### Closeout notes

- Primary milestone: M02 backend foundation/eventing reliability.
- Skills used: none.
- Agent delegation: none.

## 2026-07-15 - Task error metadata responses

### Changed

- Added structured task error metadata for task and task-type application exceptions and surfaced safe metadata fields on Core API `ProblemDetail` responses.
- Changed task access-denied responses to use generic detail text while allowing safe operation metadata and omitting resource identifiers.

### Tests

- Added API exception handler coverage for safe metadata propagation and access-denied sanitization.

### Closeout notes

- Primary milestone: M02 backend foundation/error handling hardening.
- Skills used: none.
- Agent delegation: none.

## 2026-07-12 - Task status optimistic locking

### Changed

- Added status-only task update version checks so stale status mutations return the existing optimistic-lock conflict response before saving.
- Updated the Core OpenAPI status update request contract to include the task version sent by clients.

### Tests

- Added task controller coverage for stale status-only updates returning `409 Conflict`.

### Closeout notes

- Primary milestone: task concurrency hardening.

## 2026-07-12 - Asynchronous notification delivery queue

### Changed

- Changed Core notification creation so `NotificationService.notify()` only persists in-app notifications and enqueues durable `PENDING` delivery attempts for asynchronous Slack delivery.
- Added a scheduled notification delivery worker protected by ShedLock and row-level pending delivery claims; the worker sends Slack/email deliveries, records success or failure, and stores retry backoff timestamps without rolling back the in-app notification.

### Tests

- Added notification service and worker coverage proving notify does not call Slack directly, successful sends are recorded, failures are backed off for retry, pending attempts wait for backoff expiry, and duplicate workers do not process the same attempt twice.

### Closeout notes

- Primary milestone: M09 notifications reliability.
- Skills used: none.
- Agent delegation: none.

## 2026-07-12 - Task global exception handling

### Changed

- Added typed task and task-type application exceptions for validation, access-denied, and not-found cases, then centralized their HTTP `ProblemDetail` mappings in the global Core API exception handler.
- Removed task/task-link/task-type controller-local exception translation so REST endpoints now let typed application exceptions flow to the shared handler.
- Updated task type validation outcomes to return bad-request semantics while project membership and mutation authorization failures remain forbidden.

### Tests

- Updated task controller coverage for invalid task type level and inactive custom task type validation responses.

### Closeout notes

- Primary milestone: M02 backend foundation/error handling hardening.

## 2026-07-12 - Nova runtime provider metrics

### Changed

- Added Nova runtime metrics for provider prompt/completion/total tokens, response latency, and run totals using only bounded provider, model, capability, and status tags.
- Recorded success and failure metrics from the AI agent runtime without high-cardinality or sensitive user, workspace, run, prompt, or correlation identifiers.

### Tests

- Added AI agent runtime coverage with `SimpleMeterRegistry` for successful provider token/latency counters and failed provider latency/run metrics.

### Closeout notes

- Primary milestone: M13 observability hardening.

## 2026-07-12 - Relay ingest Micrometer metrics

### Changed

- Replaced Relay's in-memory ingest counters with Micrometer counters and added Redis stream processing timers tagged by result.
- Added Relay Redis stream pending and length gauges backed by `StringRedisTemplate` stream operations.

### Tests

- Updated Relay stream consumer coverage to use `SimpleMeterRegistry` and assert success/dead-letter counter and timer metrics.

### Closeout notes

- Primary milestone: Relay observability hardening.
- Skills used: none.
- Agent delegation: none.

## 2026-07-15 - Release summary problem details

### Changed

- Replaced generic release-summary project lookup and authorization exceptions with typed task not-found and access-denied exceptions carrying safe project metadata.
- Kept release-summary error responses on the global Core API problem-detail path so clients receive stable error codes and correlation metadata.

### Tests

- Added release-summary controller coverage for missing projects returning `404 Not Found` and non-member access returning `403 Forbidden` with stable problem metadata.

### Closeout notes

- Primary milestone: M02 backend foundation/error handling hardening.
- Skills used: none.
- Agent delegation: none.

- Hardened cookie-backed auth refresh/logout with strict trusted Origin/Referer validation for the `taskmind_refresh` cookie while keeping legacy request-body refresh tokens for non-browser clients.
