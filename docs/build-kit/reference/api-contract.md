# Reference — API Contract

Core exposes the only **public** API; the frontend talks only to Core. Relay and Nova expose **internal** APIs guarded by `X-Service-Token`. Keep the public surface documented in `apps/backend/openapi.yaml` (OpenAPI 3.0.3, server `http://localhost:8080`).

## Public Core endpoints (by module / tag)

| Tag / module | Paths | Notes |
|---|---|---|
| Health | `GET /api/health` | Public, unauthenticated. |
| Auth | `/v1/auth/signup/email`, `/verify/**`, `/login`, `/oauth/google`, `/password/**`, `/token/refresh`, `/logout`, `GET\|PATCH /v1/auth/me` | Public flows + current user. |
| Tasks | `/v1/tasks`, `/v1/tasks/{id}`, `/status`, `/completion`, `/archive`, `/children`, `/ancestors`, links | Full lifecycle + hierarchy. |
| Task links | `/v1/tasks/{id}/links`, `/v1/task-links/{linkId}` | Cross-task links. |
| Releases | `/v1/projects/{id}/releases` | Release stats + task module. |
| Attachments | `/v1/tasks/{taskId}/attachments/**` | Upload/list/download/delete. |
| Comments | `/v1/tasks/{taskId}/comments`, `/v1/comments/{commentId}`, reactions | CRUD + reactions. |
| Projects | `/v1/projects`, `/v1/projects/{id}`, members, archive | CRUD + membership. |
| Project brief (AI) | `/v1/projects/{projectId}/ai-brief` | BFF → Nova. |
| Scheduler | `/v1/scheduler/generate`, `/blocks`, `/preferences` | Auto-schedule + blocks. |
| Dashboard | `/v1/dashboard` | Aggregated home. |
| Analytics | `/v1/reports` (`week`/`month`/`quarter`) | From analytics rollups. |
| Activity | `/v1/activity/search` | OpenSearch-backed. |
| Team | `/v1/team/directory` | Team aggregation. |
| Integrations | `/v1/integrations/**` | Jira/GitHub OAuth callbacks, import, publish, wiki. |
| Notifications | `/v1/notifications/**`, preferences, `GET /v1/notifications/stream` (SSE) | In-app + SSE. |
| Spec breakdown | `/v1/spec-breakdown/**`, `/v1/projects/{projectId}/spec-templates`, `/v1/spec-templates/{id}` | Async jobs + templates. |
| AI BFF | `/v1/ai/capture`, `/v1/ai/goals/{id}/breakdown`, `/v1/ai/tasks/describe`, `/v1/ai/tasks/describe/autocomplete`, `/v1/ai/tasks/translate`, `/v1/planner/**`, `/v1/review/weekly/generate` | Facades to Nova. |
| Nova chat | `POST /v1/nova/chat`, `POST /v1/nova/chat/stream` (SSE) | Chat facade. |

Rate limiting: `/v1/**` may return `429 Too Many Requests`.

## Internal endpoints (service-token only, not in public OpenAPI)

| Service | Path | Used by |
|---|---|---|
| Core | `GET /internal/tasks`, `/internal/tasks/{id}`, comments, project members | Nova (`CoreReadClient`). |
| Core | `GET /internal/projects/**` | Nova. |
| Relay | `POST /internal/events` | Core HTTP fallback ingest. |
| Relay | `GET /internal/context/users/{id}/weekly-review` | Nova. |
| Relay | `GET /internal/context/users/{id}/dashboard-insights` | Nova. |
| Relay | `GET /internal/context/projects/{id}/health` | Nova. |
| Nova | `POST /internal/v1/agent/run` | Core (`NovaAiClient`). |
| Nova | `POST /internal/v1/chat`, `POST /internal/v1/chat/stream` | Core. |
| Nova | `GET /internal/v1/runs/{runId}` | Core audit lookup. |

All `/internal/**` routes require the `X-Service-Token` header matching the target service's `taskmind.<svc>.service-token` (or the shared `TASKMIND_*_SERVICE_TOKEN`).

## Security rules (Core)

- **Public**: `/api/health`, `/v1/auth/{login,signup/**,verify/**,oauth/**,password/**,token/refresh,logout}`, integration OAuth callbacks.
- **Authenticated**: everything else under `/v1/**`.
- **Denied**: all else.
- `/internal/**`: separate `@Order(1)` chain, service-token auth.
- Errors: RFC-7807 `ProblemDetail` JSON.

## Conventions

- DTOs are Java records under `interfaces/rest/dto`. Requests: `Create*Request`, `Update*Request`. Responses: `*Response`.
- Optimistic concurrency: update endpoints accept/return a version and reject stale writes (`409 Conflict`).
- **Update `openapi.yaml` whenever a public request/response field changes.**
