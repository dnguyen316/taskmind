# TaskMind Agent Memory

This is the compact memory index for Codex Cloud and other coding agents. Keep entries
short and link to canonical docs instead of copying large sections.

## Architecture decisions

- TaskMind is a polyglot monorepo with one Vue SPA, three Spring Boot services, and two
  shared Java libraries; see [`AGENTS.md`](../../AGENTS.md) and
  [`docs/build-kit/00-overview.md`](../build-kit/00-overview.md).
- Core is the only browser-facing API. Relay and Nova are internal services reached
  through service-token-protected routes; see
  [`docs/build-kit/reference/api-contract.md`](../build-kit/reference/api-contract.md).
- PostgreSQL schema is Flyway-owned. Append new migrations and keep Hibernate validation
  enabled; see [`AGENTS.md`](../../AGENTS.md).

## Service-boundary reminders

- `apps/backend`: Core API, authentication, task/project ownership, frontend-facing
  facades, and Core OpenAPI.
- `apps/relay`: analytics projections, event ingest, read-context APIs, and DLQ behavior.
- `apps/ai`: Nova LLM orchestration, prompts, provider routing, agent tools, chat state,
  and AI audit records.
- `apps/frontend`: Vue SPA that calls Core only through typed feature APIs.
- `libs/events` and `libs/ai-contracts`: shared contracts only; avoid service behavior.

## Common verification commands

- Full gate: `make vibe-verify`.
- Java all tests: `make test`.
- Frontend typecheck: `cd apps/frontend && npm run typecheck`.
- Frontend build for runnable UI changes: `cd apps/frontend && npm run build`.
- Token report: `make vibe-token-report -- --group-by workflow`.

## Recurring fixes

- When Core request or response DTOs change, update `apps/backend/openapi.yaml` in the
  same change set.
- For backend behavior changes, update `docs/backend-feature-changelog.md` only for the
  owning visible behavior or workflow change.
- For frontend-visible behavior changes, update `docs/frontend-feature-changelog.md` and
  record UI E2E evidence or the skipped E2E reason.
- Read adjacent tests before broad package scans.

## Token-saving tips

- Start with `rg --files` and targeted `rg -n` searches; do not recursively dump trees.
- Open only `AGENTS.md`, this memory file, the active phase doc, and the owning reference
  doc before implementation.
- Prefer task stubs for deferred work instead of loading unrelated service files.
- After high-token sessions, update this file or a focused skill with the narrower search
  path that would have saved context.
