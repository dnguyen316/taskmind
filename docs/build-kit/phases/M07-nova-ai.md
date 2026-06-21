# M07 — Nova AI

## Purpose

Build the **Nova** service foundation for TaskMind's AI workflows. This milestone creates
an independently testable Spring Boot service at `apps/ai` and wires it to Core through
shared DTOs in `libs/ai-contracts`. It does **not** implement the product AI workflows
(capture, goal breakdown, weekly review, spec breakdown, or dashboard insights); those
belong to M08 and M09. M07 provides the safe runtime those workflows will use.

## Scope

| Area | Owner | In scope for M07 |
|------|-------|------------------|
| AI contracts | `libs/ai-contracts` | Request/response DTOs shared by Core and Nova |
| Nova service | `apps/ai` | Provider router, mock provider, capability registry, agent runtime, chat sessions, `ai_runs` audit |
| Core service | `apps/backend` | Authenticated AI facade endpoints and internal service-token clients to Nova |
| Relay service | `apps/relay` | Read-only context client target only; no Relay projection work in this milestone |
| Frontend | `apps/frontend` | Out of scope except for existing routes continuing to compile |

## Dependencies and prerequisites

- Complete M00-M02 first so the Maven reactor, Core security, tasks, projects, and service
  contracts exist.
- M05/M06 may already be complete in the normal roadmap order; Nova should be able to use
  Relay context endpoints when they exist, but M07 must still pass with deterministic mock
  behavior if Relay is unavailable in local tests.
- Follow the service-boundary rule: **all prompts, provider calls, chat sessions, agent
  runtime code, and AI audit persistence live in Nova**. Core exposes BFF/facade endpoints
  only; the frontend never calls Nova directly.

## Target capabilities delivered by this milestone

1. Nova boots as a Spring Boot 3.3.5 application on port `8082`.
2. `libs/ai-contracts` contains stable Core ↔ Nova DTOs for chat and capability execution.
3. Core exposes authenticated `/v1/ai/**` and/or `/v1/nova/**` facades that call Nova with
   `X-Service-Token`.
4. Nova accepts only internal service-token requests on `/internal/**` routes.
5. Nova has a provider abstraction with a deterministic `mock` provider used by tests and
   local verification.
6. Nova has a provider router that can select configured provider IDs such as `mock`,
   `openai`, `anthropic`, or `namc` without changing Core contracts.
7. Nova has a capability registry and agent runtime so later milestones add capabilities
   without adding provider-specific controller code.
8. Nova stores audit rows in the Postgres `ai` schema through Flyway-owned migrations.
9. Nova stores short-lived chat session state in Redis when Redis is available, with test
   coverage that does not require a live Redis container.
10. All Java tests and frontend typechecks pass through `make vibe-verify`.

## Files to create or update

### Shared contracts

- `libs/ai-contracts/pom.xml`
- `libs/ai-contracts/src/main/java/com/taskmind/ai/contracts/**`
- Contract tests or serialization tests under `libs/ai-contracts/src/test/java/**`

Recommended DTO groups:

- `CapabilityRequest`, `CapabilityResponse`, `CapabilityError`
- `ChatRequest`, `ChatResponse`, `ChatMessage`, `ChatSessionSummary`
- `AiRunSummary`, `AiRunStatus`, `AiProviderId`, `AiCapabilityId`
- Strict schema DTOs that represent outputs consumed by Core facades; avoid exposing raw
  provider responses to Core.

### Nova service

- `apps/ai/pom.xml`
- `apps/ai/src/main/java/com/taskmind/ai/NovaAiApplication.java`
- `apps/ai/src/main/resources/application.yml`
- `apps/ai/src/main/resources/application-local.yml`
- `apps/ai/src/main/resources/application-test.yml`
- `apps/ai/src/main/resources/db/migration/V1__create_ai_schema.sql`
- `apps/ai/src/main/java/com/taskmind/ai/config/**`
- `apps/ai/src/main/java/com/taskmind/ai/security/**`
- `apps/ai/src/main/java/com/taskmind/ai/provider/**`
- `apps/ai/src/main/java/com/taskmind/ai/capability/**`
- `apps/ai/src/main/java/com/taskmind/ai/agent/**`
- `apps/ai/src/main/java/com/taskmind/ai/chat/**`
- `apps/ai/src/main/java/com/taskmind/ai/audit/**`
- `apps/ai/src/main/java/com/taskmind/ai/internal/**`
- `apps/ai/src/test/java/com/taskmind/ai/**`

Use the same four-layer style as the rest of the build kit where a feature owns state:
`interfaces/rest` → `application` → `domain` → `infrastructure`. Provider adapters are
infrastructure; capability orchestration is application/domain logic; controllers should be
thin.

### Core facade updates

- `apps/backend/src/main/java/com/taskmind/backend/ai/**`
- `apps/backend/src/main/java/com/taskmind/backend/internal/**` if Nova needs read-only
  facts from Core.
- `apps/backend/src/main/resources/application*.yml` for Nova base URL and service token
  configuration.
- `apps/backend/openapi.yaml` for every Core request/response shape added or changed.
- Core tests under `apps/backend/src/test/java/**` for facade auth, service-token
  forwarding, error mapping, and deterministic mock responses.

### Build and local runtime

- Root `pom.xml` reactor module list: include `libs/ai-contracts` before `apps/backend`
  and `apps/ai` after `apps/relay`.
- `Makefile`: ensure `make run-ai` starts Nova on `8082`.
- `infra/env/.env.example`: include non-secret placeholders for Nova provider selection,
  service tokens, and model names.
- `infra/compose/**`: include Nova only if app-compose files already exist in the current
  milestone sequence; do not make frontend call Nova directly.

## Contract shape guidelines

Core-facing contracts must be stable, deterministic, and provider-neutral.

Minimum chat request fields:

- `sessionId` optional for new conversations.
- `message` user text.
- `timezone` and `locale` optional user context.
- `correlationId` propagated from Core request context.

Minimum chat response fields:

- `sessionId`.
- `message` assistant text.
- `runId` pointing to the `ai_runs` audit row.
- `toolCalls` or `actions` as structured metadata only; do not let Nova mutate Core state
  directly in M07.

Minimum capability request fields:

- `capabilityId`.
- `userId` and tenant/workspace identifier when available.
- `input` as a typed JSON payload validated before provider execution.
- `correlationId` and idempotency key when the Core endpoint accepts one.

Minimum capability response fields:

- `runId`.
- `status`.
- `output` as a typed, schema-validated payload.
- `warnings` for degraded context or fallback behavior.

## Nova internal API

Expose internal Nova endpoints only for Core-to-Nova traffic. Protect them with
`X-Service-Token` and deny unauthenticated requests.

Recommended endpoints:

- `GET /api/health` — public health endpoint.
- `POST /internal/ai/chat` — create or continue a Nova chat turn.
- `POST /internal/ai/capabilities/{capabilityId}:run` — execute one registered capability.
- `GET /internal/ai/runs/{runId}` — return an audit summary for troubleshooting.
- `GET /internal/ai/capabilities` — list enabled capabilities and provider routing metadata
  safe for Core to expose.

## Core facade API

Expose user-authenticated facade endpoints under Core only. Keep Core responsible for JWT,
RBAC, request validation, rate-limit hooks, and OpenAPI. Core should translate Nova
failures into stable client errors without leaking provider internals.

Recommended endpoints:

- `POST /v1/nova/chat` — calls Nova `/internal/ai/chat`.
- `GET /v1/nova/capabilities` — lists enabled capabilities through Core.
- `GET /v1/nova/runs/{runId}` — returns run status for the authenticated user if allowed.

If the existing Core contract uses `/v1/ai/**`, keep it consistent and document the exact
paths in `apps/backend/openapi.yaml`.

## Provider router requirements

- Implement `AiProvider` as an interface with a provider ID, supported capabilities, and a
  method that accepts provider-neutral prompt/context input.
- Implement `MockAiProvider` first. It must be deterministic and must not require network
  access or secrets.
- Add real-provider adapters behind configuration only. Missing real-provider credentials
  must not break tests.
- Keep provider prompts and model-specific options inside Nova. Core and the frontend must
  never depend on OpenAI-, Anthropic-, or NAMC-specific response shapes.
- Record the selected provider ID, model ID, latency, token counts when available, and
  status in `ai_runs`.

## Capability and agent runtime requirements

- Define a `Capability` interface with an ID, input schema, output schema, and execution
  method.
- Validate capability input before provider execution and validate capability output before
  returning to Core.
- Include a small set of placeholder capabilities that return mock, schema-valid outputs to
  prove the pattern. Product workflows are completed in later milestones.
- Agent tools may read from Core internal APIs or Relay context APIs through service tokens.
  They must not write Core task/project state directly in M07.
- Any suggested mutation must be returned as a structured proposal for Core or the user to
  accept in later milestones.

## Audit persistence requirements

Create the `ai` schema and an `ai_runs` table with Flyway. Suggested columns:

- `id` UUID primary key.
- `user_id`, workspace/tenant identifier if present, `capability_id`, `provider_id`,
  `model_id`.
- `status` such as `PENDING`, `SUCCEEDED`, `FAILED`, or `CANCELLED`.
- `request_hash`, `input_json`, `output_json`, `error_code`, `error_message`.
- `prompt_tokens`, `completion_tokens`, `total_tokens`, `latency_ms`.
- `correlation_id`, `created_at`, `started_at`, `completed_at`.
- Optimistic-lock `version` column where a JPA entity owns the row lifecycle.

Do not store secrets. If prompt or response artifacts may contain sensitive user text, make
retention configurable and redact logs.

## Configuration requirements

Use environment variables for all secrets and provider choices. Suggested properties:

- `taskmind.ai.provider.default=mock`
- `taskmind.ai.provider.openai.api-key=${OPENAI_API_KEY:}`
- `taskmind.ai.provider.anthropic.api-key=${ANTHROPIC_API_KEY:}`
- `taskmind.ai.service-token=${TASKMIND_NOVA_SERVICE_TOKEN:${TASKMIND_AI_SERVICE_TOKEN:development-only-nova-service-token}}` (`TASKMIND_AI_SERVICE_TOKEN` is a deprecated alias)
- `taskmind.core.base-url=${TASKMIND_CORE_BASE_URL:http://localhost:8080}`
- `taskmind.core.service-token=${TASKMIND_CORE_SERVICE_TOKEN:local-core-token}`
- `taskmind.relay.base-url=${TASKMIND_RELAY_BASE_URL:http://localhost:8081}`
- `taskmind.relay.service-token=${TASKMIND_RELAY_SERVICE_TOKEN:local-relay-token}`

Production profiles must not rely on default local tokens.

## Test plan

Write tests before implementation for Java behavior.

Required coverage:

1. `libs/ai-contracts` DTO serialization/deserialization remains stable.
2. Nova rejects missing or invalid `X-Service-Token` on `/internal/**`.
3. Nova health endpoint works without service token.
4. Mock provider returns deterministic outputs.
5. Provider router selects the configured provider and falls back only when explicitly
   configured to do so.
6. Capability registry rejects unknown capabilities.
7. Capability execution writes an `ai_runs` audit row for success and failure.
8. Chat endpoint creates a session, appends a turn, and returns the same `sessionId` on
   continuation.
9. Core facade requires user authentication.
10. Core facade forwards `X-Service-Token` to Nova and maps Nova errors to documented Core
    errors.
11. `apps/backend/openapi.yaml` matches every new Core facade shape.
12. `make vibe-verify` passes.

## Implementation order

1. Add `libs/ai-contracts` DTOs and serialization tests.
2. Add the `apps/ai` Maven module and empty boot app with health endpoint.
3. Add Nova service-token security for `/internal/**`.
4. Add the `ai` Flyway schema and `ai_runs` audit persistence.
5. Add provider abstraction and deterministic mock provider.
6. Add provider router and configuration properties.
7. Add capability registry, schemas, and placeholder capability execution.
8. Add Redis-backed chat session port plus an in-memory/test adapter.
9. Add Nova internal REST endpoints.
10. Add Core Nova client, authenticated facade endpoints, and OpenAPI updates.
11. Run targeted tests, then `make vibe-verify`.

## Acceptance criteria

- `make build` succeeds with `libs/ai-contracts` and `apps/ai` in the Maven reactor.
- `make test` succeeds without real provider credentials.
- `make vibe-verify` succeeds.
- `make run-ai` starts Nova on `8082` and `GET /api/health` returns healthy status.
- Core facade endpoints are authenticated and documented in `apps/backend/openapi.yaml`.
- Nova internal endpoints reject unauthenticated requests and accept valid service-token
  requests from Core.
- Every capability/chat run creates an audit row in the `ai.ai_runs` table.
- No frontend code calls Nova directly.
- No secrets are committed.

## Out of scope

- Production-quality prompts for capture, goal breakdown, weekly review, project brief,
  translate/describe, insights, or spec breakdown.
- Direct AI mutations of tasks, projects, releases, comments, attachments, or integrations.
- Jira/GitHub/wiki tool execution.
- Dashboard analytics generation.
- AWS deployment hardening, WAF, ECS task definitions, or production observability beyond
  basic audit rows and logs.
