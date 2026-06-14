# AI Capability Catalog

This catalog defines the TaskMind AI feature set for the rebuild. It is a planning and
implementation reference for agents following `AGENTS.md` and the build kit milestones;
it is not a claim that every capability is already implemented in the current tree.

## Sources of truth

- Start with `AGENTS.md`, then `docs/build-kit/00-overview.md`, then the milestone order
  in `docs/build-kit/01-build-order.md`.
- Keep service boundaries from the overview authoritative: the frontend calls only Core,
  Core owns business state and AI facades, Relay owns analytics/context projections, and
  Nova owns prompts, provider calls, agent runtime, chat sessions, and AI run audit.
- Keep Core API contracts in `apps/backend/openapi.yaml` synchronized whenever a Core
  request or response shape changes.
- Keep shared Core-to-Nova request/response DTOs in `libs/ai-contracts` once that library
  exists in the Maven reactor.

## Runtime ownership model

| Layer | AI responsibility | Must not own |
|-------|-------------------|--------------|
| `apps/frontend` | User review surfaces, confidence/rationale display, accept/edit/reject flows, async job status, and calls to Core facade endpoints only. | Direct calls to Nova or Relay. |
| `apps/backend` (Core) | Authenticated `/v1/ai/**` and `/v1/nova/**` facades, authorization, idempotency, persistence of accepted mutations, attachments, scheduling state, task/project hierarchy, outbox events, and OpenAPI. | LLM prompts or provider-specific orchestration. |
| `apps/relay` | Projection-backed context for weekly review, dashboard insights, project health, and activity search. | Business-state writes or LLM provider calls. |
| `apps/ai` (Nova) | Spring AI orchestration, provider routing, capability implementations, prompt templates, structured output validation, tool calls to Core/Relay internal APIs, Redis chat/session state, and `ai_runs` audit. | Task/project/scheduler ownership. |
| `libs/ai-contracts` | DTOs shared by Core and Nova for AI requests, responses, warnings, rationales, citations, and async job envelopes. | Service logic. |

## Cross-cutting AI guardrails

- Use the Nova capability pattern from M07: each capability has an explicit request model,
  structured response model, prompt/template version, validation step, and deterministic
  tests with the mock provider.
- Validate model output before Core persists any AI-created or AI-edited business state.
- Treat AI output as drafts or proposals until the user or an explicit workflow accepts it.
- Preserve human review for destructive or high-impact changes; archive instead of hard
  deleting where an AI workflow suggests removal.
- Record audit metadata for AI work: correlation/trace id, capability name, prompt version,
  model/provider, latency, validation outcome, and user decision when applicable.
- Use idempotency keys for AI mutation workflows so retries do not duplicate tasks,
  hierarchy nodes, schedule blocks, attachments, or integration publishes.
- Do not log secrets or sensitive prompt payloads. Redact PII in operational logs and keep
  provider credentials in environment variables or AWS Secrets Manager.
- Prefer deterministic Core rules for scheduling constraints and validation; use Nova for
  language understanding, explanation, summarization, and proposal generation.

## Capability map

| Capability | Milestone | Core facade / surface | Nova role | Core / Relay role | Output contract notes |
|------------|-----------|------------------------|-----------|-------------------|-----------------------|
| AI capture | M08 | `/v1/ai/capture` | Convert free-form text into structured task drafts, confidence, warnings, and optional clarification. | Core authorizes the user, validates drafts, and persists only accepted task data. | Drafts must include enough task fields for review; low-confidence fields must be visible to the user. |
| Goal breakdown | M08 | `/v1/ai/goals/{goalId}/breakdown` or successor Core facade | Break a goal into milestones and sibling tasks with sequencing, dependencies, risks, and rationale. | Core owns goal/task/project persistence and applies accepted drafts. | Response is a draft plan; it must not silently mutate existing task hierarchy. |
| Auto-scheduler / daily planner | M04, with AI explanations in M08 where used | `/v1/planner/daily/generate` and scheduler endpoints | Optional natural-language rationale or proposal explanation. | Core owns scheduling preferences, calendar blocks, dependency checks, available-time constraints, and persisted schedule changes. | Plans must respect Core constraints and identify overflow or blocked tasks. |
| Adaptive rescheduling | M04, with AI explanations in M08 where used | `/v1/planner/reschedule/proposals` and scheduler apply endpoints | Optional explanation of proposed moves, splits, deferrals, or drops. | Core detects overdue/conflicting work, validates proposals, requires confirmation for bulk/high-impact apply, and persists accepted changes. | Proposals must include conflict warnings and an explicit user-confirmed apply path. |
| Weekly review | M08 | `/v1/review/weekly/generate` or successor Core facade | Generate a narrative summary and recommendations from approved context. | Relay provides projected activity/context; Core returns the facade response and records user decisions/adoption telemetry. | Output must include a summary, slippage insights, and exactly three recommendations when this workflow is implemented. |
| Dashboard insights | M08 and M12 | Core dashboard/AI facade endpoints | Summarize trends and explain notable changes. | Relay computes projections and context; Core exposes user-facing dashboard responses. | Distinguish facts from generated narrative and include traceable context ids when available. |
| Project brief / project health | M08 and M12 | Core project/AI facade endpoints | Produce concise status briefs, risks, blockers, and next actions. | Relay provides project-health context; Core enforces project membership and returns the facade response. | Must not reveal projects the user cannot access. |
| Describe / translate | M08 | Core AI utility facade | Rewrite, describe, or translate user-provided text without changing business state. | Core authorizes the request and can rate-limit by user. | Treat output as generated text; no persistence unless a user saves it through an owning Core endpoint. |
| Nova chat | M07, expanded by M08+ | `/v1/nova/**` Core facades | Own chat sessions in Redis, agent runtime, tool selection, and provider calls. | Core authenticates the user and exposes allowed facades/tools; Relay supplies context when requested through internal APIs. | Tools must be scoped to the authenticated user and service-token protected internal calls. |
| Spec breakdown | M09 | Core spec-breakdown facades | Convert a product spec and related context into an Epic → Story → Subtask draft hierarchy. | Core owns async job state, attachments, accepted task hierarchy, and Jira publish flow. | Long-running jobs should use the standard async job envelope and require user review before persistence/publish. |

## M07 Nova internal foundation endpoints

Nova exposes the following service-token-protected internal endpoints for Core-to-Nova integration. Core remains the only frontend-facing API owner; these endpoints are not browser-facing.

| Endpoint | Purpose | Notes |
|----------|---------|-------|
| `POST /internal/ai/chat` | Create or continue a Nova chat turn. | Uses `ChatRequest`/`ChatResponse`, writes an `ai_runs` row with capability `chat`, and stores short-lived session state through the chat session store. |
| `POST /internal/ai/capabilities/{capabilityId}:run` | Execute a registered capability through the provider router and agent runtime. | M08 registers typed capabilities for capture, goal breakdown, weekly review, project brief, describe task, autocomplete task, translate task, duration estimate, rationale phrase, and dashboard insights; the mock provider returns deterministic structured output for local verification. |
| `GET /internal/ai/runs/{runId}` | Fetch provider-neutral audit metadata for a Nova run. | Returns `AiRunSummary`; prompt and response payloads stay internal to the audit table. |
| `GET /internal/ai/capabilities` | List registered capabilities with input/output schema metadata. | Used by Core integration and verification; unknown capability execution returns `UNKNOWN_CAPABILITY`. |

All `/internal/**` routes require `X-Service-Token` (or the matching service bearer token for internal clients); `/api/health` remains public for service health checks.

## Provider and model routing

- Build Nova on Spring AI 1.0.0 with a provider router abstraction so capability code is
  not coupled to a specific vendor.
- Implement the mock provider first for deterministic CI and agent verification.
- Wire real providers through configuration after schema validation, audit logging,
  rate limiting, and failure handling exist.
- Provider failures should return actionable fallback errors through Core facades rather
  than partial invalid business mutations.

## Context and tool access

Nova may use tools, but tool access must preserve the service boundaries:

1. Core calls Nova over service-token HTTP for AI work.
2. Nova reads real-time business facts from Core `/internal/**` endpoints using a service
   token and the authenticated user's scoped context.
3. Nova reads aggregated analytics, activity search, weekly-review context, dashboard
   context, and project-health context from Relay `/internal/context/**` endpoints.
4. Relay builds those contexts from Core domain events, analytics projections, and
   OpenSearch activity indexes; it does not write business state.
5. Any accepted mutation returns to Core, where authorization, validation, optimistic
   locking, Flyway-owned schema constraints, and outbox event publication apply.

## Contract patterns

Use these patterns consistently across AI endpoints and DTOs:

- `capability`: stable capability id such as `capture`, `goal-breakdown`, `weekly-review`,
  `project-brief`, `nova-chat`, or `spec-breakdown`.
- `traceId` / `correlationId`: safe id the UI can show for support and diagnostics.
- `promptVersion`: prompt/template version for audit and reproducibility.
- `model` / `provider`: model metadata when a real provider is used; mock provider values
  are acceptable in deterministic tests.
- `confidence`: numeric confidence where the capability makes extracted or inferred claims.
- `warnings`: user-visible caveats, conflicts, missing context, or validation concerns.
- `rationale`: concise explanation of why a draft, recommendation, or proposal was made.
- `decisionRequired`: whether the user must accept/edit/reject before persistence.
- `jobId`, `status`, `startedAt`, `completedAt`, `errorCode`: standard async envelope
  fields for long-running workflows such as spec breakdown.

## Milestone alignment

- M07 establishes Nova foundations: provider router, mock provider, capability pattern,
  agent runtime, chat/session state, `ai_runs` audit, and Core AI facades.
- M08 implements user-facing AI features: capture, goal breakdown, weekly review, project
  brief, describe/translate, and insights.
- M09 implements the asynchronous spec-breakdown pipeline and Jira publish workflow.
- M12 consumes Relay projections for analytics/dashboard context used by insights and
  project-health narratives.
- M13 hardens the full AI surface with rate limiting, observability, production profile
  controls, and AWS deployment configuration.

## Verification expectations

- Unit and slice tests must cover happy paths, invalid structured output, provider failure,
  authorization boundaries, and idempotent retry behavior.
- Mock-provider tests should make AI behavior deterministic in `make vibe-verify`.
- Frontend AI surfaces must typecheck and display loading, partial-success, success,
  recoverable failure, and non-recoverable failure states.
- UI workflows that persist AI output need browser E2E proof using the local/staging/test
  e2e bypass user, never production bypass configuration.
