# TaskMind

TaskMind is an AI-centered task manager rebuilt as a production-style polyglot monorepo. It is designed to show how a modern task product can combine a Vue SPA, Spring Boot microservices, AWS-managed infrastructure patterns, and AI orchestration while keeping service boundaries explicit.

Use this README as the public project brief for interviewers, reviewers, and future contributors. Deeper implementation notes live in the `docs/` tree.

## What This Project Demonstrates

- **Product thinking:** task, project, planning, AI assistant, search, analytics, notifications, and integration workflows are organized as milestone-driven slices.
- **Backend architecture:** three Spring Boot services with clear ownership: Core API, Relay analytics/context service, and Nova AI orchestration service.
- **Frontend architecture:** Vue 3 SPA with typed API access, route guards, state management, and Ant Design Vue UI patterns.
- **Cloud-ready data plane:** PostgreSQL, Redis, OpenSearch, and S3-compatible storage locally, with production intent mapped to RDS, ElastiCache, Amazon OpenSearch Service, and Amazon S3.
- **Engineering discipline:** Maven reactor builds, frontend type checks, OpenAPI contract sync, Flyway-owned schema, CI-quality verification, and milestone changelogs.

## System at a Glance

```text
Browser (Vue 3 SPA :5173)
        |
        v
Core API (Spring Boot :8080)  <--- public API surface
   |            |
   |            +--> Nova AI (Spring Boot :8082, internal)
   |
   +--> Relay (Spring Boot :8081, internal)

Data plane: PostgreSQL + Redis + OpenSearch + S3-compatible object storage
```

The frontend calls **only Core**. Relay and Nova stay internal and are reached through Core facade endpoints. This keeps authentication, contracts, and public API behavior centralized.

## Repository Layout

```text
taskmind/
  pom.xml                  # Maven parent reactor for Java libraries and services
  Makefile                 # build, test, run, and verification shortcuts
  docker-compose.yml       # wrapper for local infrastructure compose files
  README.md                # project entry point for humans
  AGENTS.md                # agent/contributor implementation guardrails
  scripts/vibe-verify.sh   # repository quality gate
  libs/
    events/                # shared domain event envelope and event contracts
    ai-contracts/          # shared Core <-> Nova DTO contracts
  apps/
    backend/               # Core API, public backend surface, port 8080
    relay/                 # analytics/context relay, internal, port 8081
    ai/                    # Nova LLM orchestration, internal, port 8082
    frontend/              # Vue 3 SPA, dev port 5173
  infra/
    compose/               # local Postgres, Redis, OpenSearch, S3/LocalStack setup
    env/.env.example       # local environment template
  docs/                    # architecture, build milestones, references, changelogs
```

## Technology Stack

| Area | Technology |
|------|------------|
| Backend language | Java 17 |
| Backend framework | Spring Boot 3.3.5 |
| AI orchestration | Spring AI 1.0.0 |
| Java build | Maven reactor |
| Frontend | Vue 3.5, Vite 5, TypeScript 5.8 |
| UI and state | Ant Design Vue 4, Pinia 3, vue-router 4 |
| Rich text | Tiptap 3 |
| Database | PostgreSQL 16 with Flyway migrations |
| Cache and streams | Redis 7 |
| Search | OpenSearch-compatible local service / Amazon OpenSearch Service target |
| Object storage | S3-compatible local storage / Amazon S3 target |
| Job locks and rate limits | ShedLock and Bucket4j |

## Service Boundaries

| Component | Path | Responsibility | Port | Public? |
|-----------|------|----------------|------|---------|
| Frontend | `apps/frontend` | Browser UI, typed Core API usage, route guards, user workflows | 5173 | Dev only |
| Core API | `apps/backend` | Auth, tasks, projects, public REST API, OpenAPI contract, facades to internal services | 8080 | Yes |
| Relay | `apps/relay` | Event consumption, analytics projections, searchable context | 8081 | No |
| Nova AI | `apps/ai` | LLM provider routing, AI capabilities, prompt/orchestration runtime | 8082 | No |
| Events library | `libs/events` | Shared event envelope and event DTOs | n/a | Internal |
| AI contracts library | `libs/ai-contracts` | Shared Core/Nova request and response DTOs | n/a | Internal |

## Current Implemented API Areas

Core is the only public backend API surface. The current contract areas include:

- **Health:** readiness and liveness checks.
- **Authentication:** login, signup, verification, OAuth/password/token flows, logout, and local/test/staging E2E bypass.
- **Tasks:** creation, listing, updates, status changes, completion details, and archive behavior.
- **Task links:** task-to-task relationships.
- **Task releases:** release grouping and task release assignment.
- **Projects:** project creation, listing, details, and updates.
- **Project memberships:** member listing and membership management.
- **Planner and AI scaffolds:** deterministic Core facade endpoints for capture, goal breakdown, daily planning, rescheduling, and weekly review surfaces while deeper Nova orchestration is built out.

The Core OpenAPI contract lives at `apps/backend/openapi.yaml` and should change in the same pull request as any Core DTO or endpoint contract change.

## Prerequisites

Install the following before running the project locally:

- Java 17
- Maven 3.9+
- Node.js 20+
- npm 10+
- Docker or a compatible container runtime for local infrastructure

## Quick Start

```bash
# 1. Create local environment file from the template
make env-example

# 2. Start local data-plane dependencies
make infra-up

# 3. Build Java libraries and services
make build

# 4. In separate terminals, start the app services
make run-backend
make run-relay
make run-ai
make run-frontend
```

Then open the frontend at `http://localhost:5173`.

For local E2E/dev login when the bypass profile is enabled:

```text
Email: superadmin@taskmind.local
Password: password 1
OTP: 1
```

## Build, Test, and Verification

Run these commands from the repository root unless noted otherwise:

```bash
make build        # mvn clean install -DskipTests
make test         # mvn test
make vibe-verify  # required quality gate: Java tests + frontend typecheck
```

Frontend-only commands from `apps/frontend`:

```bash
npm install
npm run dev
npm run typecheck
npm run build
```

Targeted Java test examples from the owning service directory:

```bash
mvn -q -Dtest=TaskControllerTest test
mvn -q -Dtest='*Task*' test
mvn -q -Dtest=TaskControllerTest#rejectsStaleTaskUpdate test
```

## Maven Reactor Order

Java modules build in this order:

```text
libs/events -> libs/ai-contracts -> apps/backend -> apps/relay -> apps/ai
```

`apps/frontend` is intentionally separate from Maven and uses npm scripts.

## Roadmap and Documentation Map

| Need | Start here |
|------|------------|
| Product and architecture overview | `docs/build-kit/00-overview.md` |
| Milestone build order | `docs/build-kit/01-build-order.md` |
| Coding conventions and DDD layering | `docs/build-kit/conventions.md` |
| API, data, events, AI, and frontend references | `docs/build-kit/reference/` |
| Per-milestone implementation specs | `docs/build-kit/phases/` |
| Backend implementation history | `docs/backend-feature-changelog.md` |
| Frontend implementation history | `docs/frontend-feature-changelog.md` |
| Agent/session workflow | `docs/agent-session-workflow.md` |
| AWS deployment notes | `infra/aws/README.md` |

## Milestone Snapshot

| Milestone | Focus |
|-----------|-------|
| M00 | Bootstrap monorepo, Maven reactor, service shells, frontend shell, CI gate |
| M01 | Core foundations: persistence, JWT security, E2E bypass, health, error handling |
| M02 | Core task and project modules plus OpenAPI contract |
| M03 | Frontend shell, auth, task pages, and project pages |
| M04 | Scheduler preferences, blocks, proposals, and calendar UI |
| M05 | Outbox eventing, Redis Streams, Relay projections, analytics schema |
| M06 | Search and object storage on the AWS-aligned data plane |
| M07 | Nova AI provider router, capabilities, chat, and audit foundations |
| M08-M13 | AI features, spec breakdown, integrations, notifications, analytics, hardening, and AWS deployment |

See `docs/build-kit/01-build-order.md` for the source-of-truth milestone tracker.

## Contributor Notes

- Keep frontend calls routed through Core only.
- Keep service-specific behavior inside the service that owns it.
- Use Flyway for schema changes; do not rely on Hibernate DDL generation.
- Keep `apps/backend/openapi.yaml` synchronized with Core request and response contracts.
- Run the smallest targeted checks first, then `make vibe-verify` before calling a feature complete.
- For UI behavior changes, also perform a browser E2E check on `localhost:5173` when the environment supports it.
