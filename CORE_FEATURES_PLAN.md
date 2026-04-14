# Backend Core Features Implementation Plan

## 1) Scope and Objective
Implement backend services for an AI task management product MVP that supports:
1. AI inbox capture
2. Goal-to-plan breakdown
3. Smart daily planning
4. Adaptive rescheduling
5. Weekly review copilot

Primary objective: ship a production-ready backend that is secure, observable, and resilient, with predictable AI behavior through strict schemas and validation.

---

## 2) Proposed Backend Architecture

### 2.1 Services
- **API Service**
  - Authenticated REST API (or GraphQL gateway) for client apps.
  - Handles task CRUD, planning endpoints, review endpoints.
- **AI Orchestration Service**
  - Prompt templates, model routing, tool invocation, schema validation.
  - Fallback logic and confidence scoring.
- **Scheduling/Rules Engine**
  - Priority scoring, deadline risk checks, and reschedule proposals.
  - Deterministic logic (no model required) for core ranking constraints.
- **Async Worker Service**
  - Queue-based jobs for heavy workflows (goal decomposition, weekly summaries).
  - Retries, dead-letter queue, and idempotency keys.
- **Notification Service (MVP-light)**
  - EOD missed-task processing and weekly review reminders.

### 2.2 Data Storage
- **PostgreSQL** for transactional data (users, tasks, projects, plans, AI actions).
- **Redis** for caching and short-lived orchestration state (job status, rate limiting).
- **Object storage** (optional) for prompt/response audit exports.

### 2.3 Integrations
- LLM provider via abstraction layer (swap provider without API changes).
- Optional calendar integration (phase 2) for time-aware planning.

---

## 3) Data Model (MVP)

### 3.1 Core Tables
- `users`
- `projects`
- `tasks`
- `task_dependencies`
- `goals`
- `milestones`
- `plans_daily`
- `plans_weekly`
- `ai_runs`
- `ai_suggestions`
- `activity_events`

### 3.2 Task Entity (strict schema)
- `id (uuid)`
- `user_id`
- `project_id (nullable)`
- `title`
- `description (nullable)`
- `status` (todo, in_progress, done, archived)
- `priority` (1..4)
- `due_at (nullable)`
- `duration_minutes (nullable)`
- `energy_level` (low, medium, high; nullable)
- `source` (manual, ai_capture, ai_plan, ai_reschedule)
- `confidence` (0..1, nullable)
- `created_at`, `updated_at`

### 3.3 AI Audit Fields
For trust and debugging:
- request payload hash
- prompt template version
- model name + latency
- validation result
- user decision (accepted, edited, rejected)

---

## 4) API Design (v1)

### 4.1 Task APIs
- `POST /v1/tasks`
- `GET /v1/tasks`
- `PATCH /v1/tasks/{id}`
- `POST /v1/tasks/bulk` (guarded + approval token)

### 4.2 AI Inbox Capture
- `POST /v1/ai/capture`
  - Input: raw text/voice transcript
  - Output: normalized task drafts + confidence + clarification question (optional)

### 4.3 Goal Breakdown
- `POST /v1/ai/goals/{goalId}/breakdown`
  - Input: goal, deadline, weekly availability
  - Output: milestones + sequenced tasks + risk notes

### 4.4 Daily Planner
- `POST /v1/planner/daily/generate`
  - Input: available minutes, constraints
  - Output: ordered tasks + rationale + overflow list

### 4.5 Adaptive Rescheduling
- `POST /v1/planner/reschedule/proposals`
  - Input: selected or overdue tasks
  - Output: options (`move`, `split`, `defer`, `drop`) + conflict warnings

### 4.6 Weekly Review
- `POST /v1/review/weekly/generate`
  - Output: summary, slippage insights, 3 recommendations, next-week draft priorities

---

## 5) Feature-by-Feature Backend Implementation

### 5.1 AI Inbox Capture
**Build**
- NL parse pipeline: extractor -> schema validator -> confidence scorer.
- Deterministic date parser fallback for common phrases.
- Task split logic for multi-item input.

**Done when**
- p95 API latency <= 3s for single input.
- >=85% parser accuracy on seeded phrase test set.
- No invalid task payload persists to DB.

### 5.2 Goal-to-Plan Breakdown
**Build**
- Goal planner async job with milestone/task graph output.
- Dependency generation rules (sequence + blockers).
- Editable draft plan state before final save.

**Done when**
- Generation completes <10s p95 async runtime.
- Every generated plan includes at least one milestone and one immediate next action.

### 5.3 Smart Daily Planner
**Build**
- Priority score formula: urgency + impact + effort + due-risk + user preference signals.
- Hard constraints: due dates, available minutes, dependency completion.
- Explanation generator from deterministic score contributions.

**Done when**
- Sync response <=2s p95 from warm cache.
- Plans never include blocked tasks unless explicitly allowed.

### 5.4 Adaptive Rescheduling
**Build**
- Overdue scanner cron job.
- Proposal engine with conflict checks against due dates/dependencies.
- Bulk apply endpoint with mandatory user confirmation token.

**Done when**
- 100% overdue tasks receive >=1 proposal.
- Deadline conflict precision >90% on simulation suite.

### 5.5 Weekly Review Copilot
**Build**
- Event aggregation pipeline (done tasks, slipped tasks, blockers).
- Summary generation job with recommendation template constraints.
- Persist “review accepted” + recommendation adoption telemetry.

**Done when**
- Job runtime <=5s p95 for standard active user dataset.
- Output always includes exactly 3 recommendations.

---

## 6) Reliability, Security, and Guardrails

### 6.1 Reliability
- Idempotency keys for all AI mutation endpoints.
- Circuit breaker + fallback messages when LLM unavailable.
- Queue retry policy: exponential backoff + DLQ alerts.

### 6.2 Security & Privacy
- JWT-based auth and per-user row-level authorization.
- Encrypt sensitive fields at rest where applicable.
- Redact PII in logs and AI traces.
- Configurable data-retention window for AI run artifacts.

### 6.3 Guardrails
- JSON schema validation required before writes.
- “No silent destructive edits” rule: archive, never hard delete by AI.
- Human confirmation required for bulk edits/rescheduling.

---

## 7) Observability and Analytics

### 7.1 Operational Metrics
- API p50/p95 latency per endpoint
- LLM error rate + timeout rate
- Queue depth and job success rate
- DB query latency and lock contention

### 7.2 Product Metrics
- capture->task conversion
- AI suggestion acceptance/edit/reject
- daily plan completion
- overdue recovery rate
- weekly review completion

### 7.3 Logging/Tracing
- Correlation IDs from edge to worker.
- Structured logs with event type and user-safe identifiers.
- Distributed tracing on AI orchestration calls.

---

## 8) Delivery Plan (12 Weeks)

### Phase 0 (Week 1): Foundation
- Finalize schema, migrations, API contracts, auth middleware.
- Stand up queue, cache, observability baseline.

### Phase 1 (Weeks 2-4): AI Inbox Capture
- Implement capture endpoint, parsers, validation, and correction flow.
- Ship telemetry + parser evaluation harness.

### Phase 2 (Weeks 5-7): Goal Breakdown + Daily Planner
- Launch async goal decomposition.
- Implement deterministic ranking + rationale payload.

### Phase 3 (Weeks 8-10): Rescheduling Engine
- Build overdue scanner + proposal generator + confirmation flow.
- Add risk/conflict detector test suite.

### Phase 4 (Weeks 11-12): Weekly Review + Hardening
- Add weekly review job and recommendation templates.
- Performance tuning, chaos testing, and rollback drills.

---

## 9) Testing Strategy

### 9.1 Automated Tests
- Unit tests for scoring, dependency rules, date parsing.
- Contract tests for all public API endpoints.
- Integration tests for DB + queue workflows.
- Golden tests for AI output schema compliance.

### 9.2 Non-functional
- Load tests for top endpoints (`/ai/capture`, `/planner/daily/generate`).
- Failure-injection tests for LLM outage and queue delays.
- Security tests: authz checks, rate-limit abuse, payload fuzzing.

### 9.3 Release Gates
- No P0/P1 open defects.
- p95 latency SLOs met for critical endpoints.
- AI schema validation pass rate >=99.5% in staging.

---

## 10) Risks and Mitigations
- **Inconsistent AI output** -> strict schema enforcement + deterministic fallback templates.
- **Latency spikes** -> async offload + caching + timeout budgets.
- **Low user trust** -> rationale fields + confirmation steps + full activity history.
- **Scope creep** -> feature flags and strict MVP endpoint freeze after Week 7.

---

## 11) Definition of Done (Backend MVP)
Backend MVP is complete when:
1. All five core features are available via stable v1 endpoints.
2. AI-driven writes are schema-validated, auditable, and reversible.
3. Critical SLOs are met in staging and production canary.
4. Analytics events are emitted for all core feature funnels.
5. Runbooks exist for incident response and model/provider degradation.

---

## 12) Development Task Breakdown (Aligned to Timeline)

Use this as the execution backlog for engineering sprints. Each task includes owner suggestions and clear exit criteria.

### Phase 0 (Week 1): Foundation

#### Phase 0 Progress Update (2026-04-14)
- [x] Backend started: baseline Task API scaffold implemented (`POST /v1/tasks`, `GET /v1/tasks`, `PATCH /v1/tasks/{id}`).
- [x] Task status workflow started: dedicated status update and completion check endpoints added (`PATCH /v1/tasks/{id}/status`, `GET /v1/tasks/{id}/completion`).
- [x] Request validation guardrails added at API boundary (priority, confidence, required fields, duration constraints).
- [x] DDD-oriented backend folder refactor applied for scalable teamwork (domain/application/infrastructure/interfaces).
- [ ] Next: replace in-memory task store with PostgreSQL migrations/repositories.
- [ ] Next: add JWT auth middleware and per-user authorization checks.

#### 0.1 Architecture & Contracts
- [x] **BE Lead**: Finalize API style (REST for v1 scaffold) and publish v1 contract draft (initial task APIs).
- [ ] **BE + FE**: Define request/response schemas for all core endpoints in OpenAPI. *(Task endpoint DTOs drafted in code; OpenAPI file pending).*
- [ ] **BE**: Add idempotency strategy for AI mutation endpoints.
- [ ] **Done when**: OpenAPI spec merged, reviewed by FE, and mock server runs locally.

#### 0.2 Data Layer
- [ ] **BE**: Create initial PostgreSQL migrations for all MVP tables.
- [ ] **BE**: Add constraints/indexes (task status enums, due date indexes, foreign keys).
- [ ] **BE**: Seed minimal dev fixtures (1 user, 2 projects, mixed task set).
- [ ] **Done when**: `up/down` migration cycle succeeds and fixtures load in CI.

#### 0.3 Platform Baseline
- [ ] **Infra/BE**: Provision Redis, queue workers, and DLQ.
- [ ] **BE**: Add auth middleware (JWT validation + per-user scoping).
- [ ] **BE**: Add structured logging, correlation IDs, base tracing.
- [ ] **Done when**: Healthcheck includes DB/Redis/queue dependencies and tracing is visible end-to-end.

#### 0.4 Quality & Guardrails
- [x] **BE**: Set up test scaffolding (initial web/controller test coverage for task APIs).
- [ ] **BE**: Add centralized JSON schema validation layer.
- [ ] **BE**: Add linting + CI checks required for merge.
- [ ] **Done when**: CI fails on schema violations and unauthorized data access attempts.

---

### Phase 1 (Weeks 2–4): AI Inbox Capture

#### 1.1 Endpoint & Pipeline
- [ ] **BE**: Implement `POST /v1/ai/capture` endpoint.
- [ ] **AI/BE**: Build extractor -> validator -> confidence scorer pipeline.
- [ ] **BE**: Add deterministic date parser fallback for common phrases.
- [ ] **Done when**: Endpoint returns normalized drafts, confidence, and optional clarification.

#### 1.2 Parsing Quality
- [ ] **AI/BE**: Build seeded phrase evaluation set (single task, multi-task, ambiguous date, vague input).
- [ ] **BE**: Add parser accuracy job in CI or nightly run.
- [ ] **Done when**: Accuracy benchmark reaches >=85% on seeded set.

#### 1.3 Persistence & Safety
- [ ] **BE**: Persist only schema-valid task drafts.
- [ ] **BE**: Record `ai_runs` + `ai_suggestions` metadata for each invocation.
- [ ] **BE**: Add rate limiting and abuse controls for capture endpoint.
- [ ] **Done when**: No invalid payload reaches `tasks`, AI audit row exists for every capture call.

#### 1.4 Observability
- [ ] **BE**: Add endpoint latency dashboards (p50/p95), error rate, timeout rate.
- [ ] **BE**: Add alert for elevated parse failures.
- [ ] **Done when**: On-call can trace request -> AI run -> DB write with one correlation ID.

---

### Phase 2 (Weeks 5–7): Goal Breakdown + Daily Planner

#### 2.1 Goal Breakdown Engine
- [ ] **BE**: Implement `POST /v1/ai/goals/{goalId}/breakdown`.
- [ ] **BE**: Add async job orchestration and status polling endpoint/webhook.
- [ ] **AI/BE**: Generate milestones + task graph + risk notes.
- [ ] **Done when**: Every generated plan includes >=1 milestone and >=1 immediate next task.

#### 2.2 Dependency & Draft Workflow
- [ ] **BE**: Add dependency generation rules (sequence + blocker logic).
- [ ] **BE**: Add editable draft state before user confirms final save.
- [ ] **BE**: Prevent circular dependencies via validation.
- [ ] **Done when**: Draft plans are editable and invalid dependency graphs are rejected.

#### 2.3 Daily Planner Core
- [ ] **BE**: Implement `POST /v1/planner/daily/generate`.
- [ ] **BE**: Implement deterministic priority formula (urgency, impact, effort, due risk, preference).
- [ ] **BE**: Enforce hard constraints (available time, dependencies, due windows).
- [ ] **Done when**: Planner excludes blocked tasks unless explicit override is set.

#### 2.4 Planner Explainability
- [ ] **BE**: Add rationale payload from score contribution breakdown.
- [ ] **BE**: Add overflow list behavior and deterministic tie-breakers.
- [ ] **Done when**: Response includes task ordering rationale and overflow justification.

---

### Phase 3 (Weeks 8–10): Adaptive Rescheduling

#### 3.1 Overdue Detection
- [ ] **BE**: Implement overdue scanner cron/scheduler.
- [ ] **BE**: Mark overdue candidates and enqueue proposal jobs.
- [ ] **Done when**: 100% overdue tasks are picked up within scheduled scan window.

#### 3.2 Proposal Engine
- [ ] **BE**: Implement proposal generator (`move`, `split`, `defer`, `drop`).
- [ ] **BE**: Add due-date and dependency conflict checks.
- [ ] **BE**: Implement `POST /v1/planner/reschedule/proposals`.
- [ ] **Done when**: Each overdue task receives >=1 valid proposal option.

#### 3.3 Safe Apply Flow
- [ ] **BE**: Build bulk apply endpoint with confirmation token.
- [ ] **BE**: Enforce “no silent destructive edits” (archive/soft-change only).
- [ ] **BE**: Log user decision (accepted/edited/rejected) for each proposal.
- [ ] **Done when**: Bulk apply rejected without valid user confirmation token.

#### 3.4 Validation & Performance
- [ ] **BE/QA**: Create simulation suite for deadline conflict precision.
- [ ] **BE**: Tune DB queries for high-overdue datasets.
- [ ] **Done when**: Conflict precision >90% and response SLAs hold under load.

---

### Phase 4 (Weeks 11–12): Weekly Review + Hardening

#### 4.1 Weekly Review Pipeline
- [ ] **BE**: Implement `POST /v1/review/weekly/generate`.
- [ ] **BE**: Build event aggregation (done/slipped/blockers) and summary generator job.
- [ ] **AI/BE**: Enforce recommendation template with exactly 3 recommendations.
- [ ] **Done when**: Weekly output always includes summary, slippage insights, and exactly 3 recommendations.

#### 4.2 Telemetry & Adoption
- [ ] **BE**: Persist review acceptance and recommendation adoption events.
- [ ] **BE/Product**: Expose review completion and adoption metrics dashboard.
- [ ] **Done when**: Product team can segment acceptance/edit/reject behavior.

#### 4.3 Hardening & Production Readiness
- [ ] **BE/Infra**: Run load tests on `/ai/capture` and `/planner/daily/generate`.
- [ ] **BE/Infra**: Run failure-injection tests (LLM outage, queue delays, Redis unavailability).
- [ ] **BE/Security**: Complete authz, fuzzing, and rate-limit abuse tests.
- [ ] **BE**: Finalize incident runbooks and rollback playbooks.
- [ ] **Done when**: Release gates pass (no P0/P1, SLOs met, schema pass rate >=99.5%).

---

## 13) Suggested Ticketing Structure

To keep execution tight, create tickets with this label pattern:
- `phase:0..4`
- `feature:capture|goal-breakdown|daily-planner|reschedule|weekly-review`
- `type:api|worker|schema|observability|security|test|infra`
- `risk:high|medium|low`

Suggested workflow:
1. Create **epics per phase** (0 through 4).
2. Create **stories per feature block** (e.g., “Daily planner scoring engine”).
3. Split into **1–2 day engineering tasks** with explicit acceptance criteria from this plan.
4. Reserve 20–25% sprint capacity for bugfix/perf/security hardening starting Week 5.
