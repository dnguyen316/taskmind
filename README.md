# TaskMind Monorepo

TaskMind is an AI-centered task manager rebuilt as a polyglot monorepo with one Vue 3 SPA, three Spring Boot services, and two shared Java libraries. The frontend talks only to Core; Relay and Nova are internal services behind Core facade endpoints.

## Target Monorepo Layout

```text
taskmind/
  pom.xml                  # Maven parent reactor (Spring Boot 3.3.5, Java 17)
  Makefile                 # build/run/verify shortcuts
  docker-compose.yml       # thin wrapper -> infra/compose/*
  AGENTS.md  CLAUDE.md  README.md
  .github/workflows/ci.yml # runs scripts/vibe-verify.sh
  scripts/vibe-verify.sh   # CI/agent quality gate
  libs/
    events/                # taskmind-events: domain event envelope (Core -> Relay)
    ai-contracts/          # taskmind-ai-contracts: Core <-> Nova DTOs
  apps/
    backend/               # Core API (Spring Boot)                  :8080
    relay/                 # Analytics/context relay                 :8081
    ai/                    # Nova LLM orchestration                  :8082
    frontend/              # Vue 3 SPA (npm, not in reactor)         :5173
  infra/
    compose/               # docker-compose.infra.yml + .apps.yml + nginx/
    env/.env.example       # env template
  docs/                    # architecture + product docs
```

## Prerequisites

- Node.js 20+
- npm 10+
- Java 17
- Maven 3.9+

## Maven Reactor

The Java workspace is a Maven reactor. Build modules in this order:

```text
libs/events -> libs/ai-contracts -> apps/backend -> apps/relay -> apps/ai
```

`apps/frontend` is a separate npm project and is never part of the Maven reactor.

## Build, Test, Run, and Verify

Use the repository Makefile from the repo root for the standard build-kit workflow:

```bash
# Build all Java modules (libs first, then services)
make build

# Run Java tests
make test

# Required quality gate: Java tests + frontend typecheck
make vibe-verify

# Start local infrastructure: Postgres, Redis, OpenSearch, and S3-compatible local storage
make infra-up

# Run application services
make run-backend   # Core API on :8080
make run-relay     # Relay on :8081
make run-ai        # Nova AI on :8082
make run-frontend  # Vue dev server on :5173
```

For frontend-only work from `apps/frontend`:

```bash
npm install
npm run dev
npm run typecheck
npm run build
```

## Ports

| Service | Port | Public? |
|---------|------|---------|
| Frontend (Vite) | 5173 | dev only |
| Core API | 8080 | yes (FE talks only to Core) |
| Relay | 8081 | no (internal) |
| Nova AI | 8082 | no (internal) |
| Postgres | 5432 | no |
| Redis | 6379 | no |
| OpenSearch / ES | 9200 | no |
| S3 / LocalStack | 4566 / 9000 | no |

## Current Core API Areas

Core (`apps/backend`) is the only public API surface the frontend calls. Its current
contract areas are:

- **Health** - service readiness and liveness checks.
- **Auth** - login, signup, verification, OAuth, password, token refresh, logout, and the
  local/test/staging E2E bypass flow.
- **Tasks** - task creation, listing, updates, status transitions, completion details, and
  archive behavior.
- **Task links** - task-to-task relationship endpoints.
- **Task releases** - release grouping and task release assignment endpoints.
- **Projects** - project creation, listing, detail, and update endpoints.
- **Project memberships** - project member listing and membership management endpoints.
- **Planner/AI scaffold endpoints** - deterministic Core facade endpoints for capture,
  goal breakdown, daily planning, reschedule proposals, and weekly review surfaces while
  deeper Nova orchestration is built out.

## Core API Contract

Core contracts live in `apps/backend/openapi.yaml`. Update that OpenAPI file in the same
change set whenever Core request or response DTOs change so frontend integration, tests,
and build-kit references stay aligned.

## Development Docs

- Build-kit roadmap and milestone order: `docs/build-kit/01-build-order.md`
- Backend implementation history: `docs/backend-feature-changelog.md`
- Agent session hygiene, session-update rules, closeout order, and token-saving inspection
  workflow: `docs/agent-session-workflow.md`

## Build-Kit Milestone Tracker

Use `docs/build-kit/01-build-order.md` as the source-of-truth tracker for rebuild status.
Work the milestones in order, verify each one with `make vibe-verify` before moving on,
and record backend-visible implementation progress in `docs/backend-feature-changelog.md`.

| Milestone | Focus | Tracker |
|-----------|-------|---------|
| M00 | Bootstrap monorepo, reactor, services, frontend shell, CI gate | `docs/build-kit/01-build-order.md` |
| M01 | Core foundations: persistence, JWT security, E2E bypass, health, error handling | `docs/build-kit/01-build-order.md` |
| M02 | Tasks and projects Core modules plus OpenAPI contract | `docs/build-kit/01-build-order.md` |
| M03 | Frontend shell, auth, tasks, and projects pages | `docs/build-kit/01-build-order.md` |
| M04-M13 | Scheduler, Relay/eventing, search/storage, Nova AI, integrations, notifications, analytics, and AWS hardening | `docs/build-kit/01-build-order.md` |
