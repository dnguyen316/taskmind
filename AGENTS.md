# AGENTS.md - TaskMind rebuild (Codex cloud agent guide)

> This file is the **entry point** for the autonomous agent rebuilding TaskMind from an
> empty repository. Codex reads `AGENTS.md` automatically. Read this fully before writing
> any code, then follow [`docs/build-kit/01-build-order.md`](docs/build-kit/01-build-order.md) milestone by milestone.

## Mission

Rebuild **TaskMind** - an AI-centered task manager - from scratch with **full feature
parity** to the reference implementation, using the **identical stack** and an **AWS
managed data plane** (Amazon S3 for object storage, Amazon OpenSearch Service for search,
RDS Postgres + ElastiCache Redis in production).

You are building a polyglot monorepo: **one Vue 3 SPA + three Spring Boot services + two
shared Java libraries**. It is *not* a single backend - most work touches exactly one
service. Respect [service boundaries](docs/build-kit/00-overview.md#service-boundaries) at all times.

## Tech stack (do not substitute)

| Layer | Technology |
|-------|------------|
| Language (services) | Java 17 |
| Framework | Spring Boot 3.3.5 (Maven reactor) |
| AI orchestration | Spring AI 1.0.0 |
| Frontend | Vue 3.5 + Vite 5 + TypeScript 5.8 |
| UI / state | Ant Design Vue 4 + Pinia 3 + vue-router 4 |
| Rich text | Tiptap 3 |
| DB | PostgreSQL 16 (RDS in prod), schema owned by Flyway |
| Cache / streams | Redis 7 (ElastiCache in prod) |
| Object storage | **Amazon S3** (local: LocalStack / filesystem) |
| Search | **Amazon OpenSearch Service** (local: OpenSearch/ES container) |
| Job locks | ShedLock 5.16 (JDBC) |
| Rate limiting | Bucket4j 8.14 (Redis-backed) |
| Schema validation | networknt json-schema-validator 1.5.6 |

## Monorepo layout (target)

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

**Maven build order (reactor):** `libs/events` -> `libs/ai-contracts` -> `apps/backend`
-> `apps/relay` -> `apps/ai`. The frontend is a **separate npm project**, never in Maven.

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

## Build, run & verify commands

```bash
# Build all Java modules (libs first, then apps)
make build              # mvn clean install -DskipTests

# Run tests
make test               # mvn test (H2 in PostgreSQL mode, Flyway still runs)

# THE quality gate - run before claiming any milestone done
make vibe-verify        # = ./scripts/vibe-verify.sh : mvn test + FE typecheck
                        # per-step timeouts: 900s Java / 180s FE (exit 124 = timeout)

# Frontend (from apps/frontend)
npm install
npm run dev             # Vite :5173
npm run typecheck       # vue-tsc --noEmit (FE "lint")
npm run build           # vite build (also type-checks)

# Local infra + services
make env-example        # infra/env/.env.example -> infra/env/.env
make infra-up           # Postgres + Redis + OpenSearch + (LocalStack S3)
make run-backend        # Core :8080
make run-relay          # Relay :8081
make run-ai             # Nova :8082
make run-frontend       # Vue :5173
```

Targeted Java tests (from the relevant `apps/<service>` dir):

```bash
mvn -q -Dtest=TaskControllerTest test
mvn -q -Dtest='*Task*' test
mvn -q -Dtest=TaskControllerTest#rejectsStaleTaskUpdate test
```

## Implementation polish loop

After each implementation pass and before final verification, do a deliberate polish loop:

1. **Format backend code** from the owning Spring Boot service directory with
   `mvn -q spotless:apply` when Java files changed. Re-run `mvn -q spotless:check` if
   you need a no-diff formatter guard.
2. **Format frontend code** from `apps/frontend` with `npm run format` when Vue,
   TypeScript, CSS, JSON, or Markdown files changed. Re-run `npm run format:check` for
   a no-diff formatter guard.
3. **Code review before tests**: scan your own diff with `git diff --check` and
   `git diff --stat`, then review the changed files by feature boundary to catch missing
   tests, contract drift, dead code, unsafe auth assumptions, and formatting noise quickly.
4. Run the smallest targeted tests/typechecks for the touched area, then the required
   `make vibe-verify` gate before claiming work is done.

## Hard rules (guardrails)

1. **Never commit, push, or create branches** unless the human explicitly asks. Staging
   (`git add`) and read-only inspection (`git status`/`diff`/`log`) are fine.
2. **Schema is owned by Flyway.** `spring.jpa.hibernate.ddl-auto=validate`. Add new
   migrations as `V<n>__description.sql` - **append the next integer; never edit an
   applied migration.** Entities carry optimistic-lock `@Version` columns.
3. **Respect service boundaries.** The frontend calls **only** Core. Core exposes facade
   endpoints to Nova/Relay. All LLM prompts/calls live in **Nova**. Analytics projections
   and read-context live in **Relay**. See [docs/build-kit/00-overview.md](docs/build-kit/00-overview.md).
4. **DDD four layers per feature module** - `interfaces/rest` -> `application` ->
   `domain` (model + repository ports) -> `infrastructure/persistence/jpa`. Keep each
   change in the layer that owns the concern. See [docs/build-kit/conventions.md](docs/build-kit/conventions.md).
5. **Keep `apps/backend/openapi.yaml` in sync** with Core DTOs whenever you add/modify a
   request/response field.
6. **TDD for Java**: RED -> GREEN -> REFACTOR. Write the failing test first.
7. **Verify before done.** A milestone is complete only when `make vibe-verify` passes
   and (for UI work) a browser E2E confirms it. Do not move on with red tests.
8. **No secrets in code or git.** Use env vars; production secrets come from AWS Secrets
   Manager. Local dev uses `infra/env/.env`.

## Security & E2E auth

Core is a **stateless JWT resource server**. Public routes: `/api/health` and the auth
flows (`/v1/auth/login`, `/signup`, `/verify`, `/oauth`, `/password`, `/token/refresh`,
`/logout`); everything else under `/v1/**` requires authentication; all else is denied.

**E2E auth bypass** (`taskmind.auth.e2e-bypass.*`) seeds a super-admin and is enabled in
`local`/`staging`/`test` only - the app **fails to start in `prod`** if it is enabled.

```text
E2E login: superadmin@taskmind.local / password 1 / OTP 1
```

## Definition of Done (every milestone)

- [ ] All "Files to create" for the milestone exist and compile.
- [ ] New behavior covered by tests (Java) / typechecks (FE).
- [ ] `make vibe-verify` passes (Java tests + FE typecheck, within timeouts).
- [ ] For UI milestones: browser E2E on `localhost:5173` with the superadmin bypass.
- [ ] `openapi.yaml` updated if Core contracts changed.
- [ ] Milestone acceptance criteria all checked.

## Where to go next

1. [`docs/build-kit/00-overview.md`](docs/build-kit/00-overview.md) - product + target architecture + service boundaries.
2. [`docs/build-kit/01-build-order.md`](docs/build-kit/01-build-order.md) - the milestone roadmap M00-M13 (execute in order).
3. [`docs/build-kit/conventions.md`](docs/build-kit/conventions.md) - coding patterns you must follow.
4. [`docs/build-kit/reference/`](docs/build-kit/reference/) - distilled contracts (data model, API, events, AI, AWS).
5. [`docs/build-kit/phases/`](docs/build-kit/phases/) - one self-contained spec per milestone.
