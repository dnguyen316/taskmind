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

## API Contract

- Core OpenAPI spec: `apps/backend/openapi.yaml`
- Keep `apps/backend/openapi.yaml` in sync whenever Core request or response DTOs change.

## Next Steps

Follow the build-kit roadmap milestone by milestone: `docs/build-kit/01-build-order.md`.

The current repository is still scaffold-era and does not yet contain the full target reactor (`libs/events`, `libs/ai-contracts`, `apps/relay`, `apps/ai`, root `pom.xml`, and infra wrappers). Start with the next incomplete build-kit milestone, **M00 Bootstrap**, to establish the full monorepo shape and green `make vibe-verify`. After M00 is complete, continue to the next backend/Core milestone, **M01 Core foundations**.
