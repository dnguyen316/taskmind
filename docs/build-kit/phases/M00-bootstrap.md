# M00 - Bootstrap

## Objective

Stand up an empty-but-running TaskMind monorepo that matches the repository shape and
stack declared in [`AGENTS.md`](../../../AGENTS.md) and the architecture overview in
[`00-overview.md`](../00-overview.md). By the end of this milestone, the Maven reactor
builds two shared Java libraries and three Spring Boot services, the Vue 3 frontend serves
a placeholder route, local infrastructure can be started through Docker Compose, and the
CI quality gate runs `scripts/vibe-verify.sh`.

## Depends on

Nothing. This is the first milestone.

## Scope

**In:** repository skeleton, Maven parent reactor, two shared library shells, three minimal
Spring Boot service shells, service health endpoints, Vue application shell, local Compose
infrastructure, Makefile shortcuts, CI workflow, README, and verification script.

**Out:** feature logic, database schema migrations, authentication, authorization,
service-to-service security, task/project data models, OpenAPI business contracts, event
publishing, AI orchestration, analytics projections, storage/search integrations beyond
local infrastructure declarations. Those begin in later milestones.

## Files to create

```text
pom.xml
Makefile
docker-compose.yml
scripts/vibe-verify.sh
.github/workflows/ci.yml
README.md
CLAUDE.md

libs/events/pom.xml
libs/events/src/main/java/com/taskmind/events/package-info.java
libs/ai-contracts/pom.xml
libs/ai-contracts/src/main/java/com/taskmind/aicontracts/package-info.java

apps/backend/pom.xml
apps/backend/Dockerfile
apps/backend/src/main/java/com/taskmind/backend/TaskmindBackendApplication.java
apps/backend/src/main/java/com/taskmind/backend/HealthController.java
apps/backend/src/main/resources/application.properties
apps/backend/src/test/java/com/taskmind/backend/HealthControllerTest.java

apps/relay/pom.xml
apps/relay/Dockerfile
apps/relay/src/main/java/com/taskmind/relay/RelayApplication.java
apps/relay/src/main/resources/application.properties

apps/ai/pom.xml
apps/ai/Dockerfile
apps/ai/src/main/java/com/taskmind/ai/AiApplication.java
apps/ai/src/main/resources/application.properties

apps/frontend/package.json
apps/frontend/vite.config.ts
apps/frontend/tsconfig.json
apps/frontend/tsconfig.node.json
apps/frontend/index.html
apps/frontend/src/main.ts
apps/frontend/src/App.vue
apps/frontend/src/router/index.ts

infra/compose/docker-compose.infra.yml
infra/compose/docker-compose.apps.yml
infra/compose/nginx/nginx.conf
infra/env/.env.example
```

## Required contents

### Root Maven reactor

- Use Java 17 and Spring Boot 3.3.5, matching `AGENTS.md`.
- Build exactly these Java reactor modules, in this order:
  1. `libs/events`
  2. `libs/ai-contracts`
  3. `apps/backend`
  4. `apps/relay`
  5. `apps/ai`
- Keep the frontend outside the Maven reactor. It is an npm/Vite project only.
- Centralize versions needed by later milestones without implementing their features yet:
  Spring AI 1.0.0, networknt JSON Schema Validator 1.5.6, Testcontainers 1.21.4,
  ShedLock 5.16, and Bucket4j 8.14.

### Java modules

- `libs/events` and `libs/ai-contracts` are placeholder Java libraries that compile.
  A `package-info.java` is sufficient for M00.
- `apps/backend` is the Core API shell on port `8080`.
  - It exposes `GET /api/health` and returns HTTP 200.
  - Add a controller test for the health endpoint.
- `apps/relay` is the Relay shell on port `8081`.
  - It only needs to boot and expose actuator health for M00.
- `apps/ai` is the Nova AI shell on port `8082`.
  - It only needs to boot and expose actuator health for M00.
- Do not add persistence entities, Flyway migrations, auth rules, service tokens, or DDD
  feature modules in M00.

### Frontend shell

- Create a Vue 3.5 + Vite 5 + TypeScript 5.8 app under `apps/frontend`.
- Include Ant Design Vue 4, Pinia 3, and vue-router 4.
- `src/main.ts` should create the app, install Pinia, Ant Design Vue, and the router, then
  mount the application.
- `src/App.vue` should render an Ant Design Vue app/config provider with a router view or
  a simple placeholder layout.
- `src/router/index.ts` should define one placeholder route.
- `npm run typecheck` must run `vue-tsc --noEmit`.
- `npm run build` should run type checking before the Vite build.

### Local infrastructure and Compose

- Root `docker-compose.yml` is a thin wrapper that includes or delegates to
  `infra/compose/docker-compose.infra.yml` and `infra/compose/docker-compose.apps.yml`.
- `infra/compose/docker-compose.infra.yml` declares local Postgres 16, Redis 7,
  OpenSearch or Elasticsearch-compatible search on port `9200`, and LocalStack for S3.
- `infra/compose/docker-compose.apps.yml` declares app-service containers for Core,
  Relay, Nova, and an nginx gateway.
- `infra/compose/nginx/nginx.conf` should route local gateway traffic to the frontend,
  Core, Relay, and Nova placeholders without changing the service boundary rule that the
  frontend talks only to Core.
- `infra/env/.env.example` documents the local environment variables needed by the Compose
  files. Do not include real secrets.

### Makefile and verification

Implement the shortcuts documented in `AGENTS.md`:

- `make build` runs `mvn clean install -DskipTests`.
- `make test` runs `mvn test`.
- `make vibe-verify` runs `scripts/vibe-verify.sh`.
- `make env-example` copies `infra/env/.env.example` to `infra/env/.env` when needed.
- `make infra-up` starts the local infrastructure Compose file.
- `make run-backend`, `make run-relay`, and `make run-ai` run the corresponding Spring
  Boot services.
- `make run-frontend` runs the Vite dev server from `apps/frontend`.

`scripts/vibe-verify.sh` must:

- fail fast and print the command being run;
- run Java tests with a 900-second timeout;
- install frontend dependencies when `apps/frontend/node_modules` is missing;
- run frontend type checking with a 180-second timeout;
- return exit code `124` when a timed step exceeds its timeout.

### CI workflow

`.github/workflows/ci.yml` should run the same quality gate as local development:

- check out the repository;
- set up JDK 17;
- set up Node 20;
- run `./scripts/vibe-verify.sh`.

## Key design notes

- M00 establishes shape and buildability only. It must not pre-implement later milestone
  behavior.
- Maven build order is **libs first, then apps**. The frontend is **not** in the reactor.
- The Core API owns the M00 public health endpoint at `/api/health` on port `8080`.
- Relay and Nova can rely on Spring Boot actuator health during M00.
- Keep local infrastructure aligned with the target AWS data plane: Postgres maps to RDS,
  Redis maps to ElastiCache, OpenSearch maps to Amazon OpenSearch Service, and LocalStack
  S3 maps to Amazon S3.
- Compose is split between `infra` for local data stores and `apps` for service/gateway
  containers.

## Acceptance criteria

- [ ] `make build` succeeds and builds all five Java reactor modules.
- [ ] `GET /api/health` on Core returns HTTP 200.
- [ ] Relay boots on port `8081` and exposes actuator health.
- [ ] Nova boots on port `8082` and exposes actuator health.
- [ ] `npm run dev` serves the Vue placeholder.
- [ ] `npm run typecheck` passes from `apps/frontend`.
- [ ] `make infra-up` starts Postgres, Redis, OpenSearch or Elasticsearch-compatible
      search, and LocalStack.
- [ ] `make vibe-verify` passes end to end.
- [ ] `.github/workflows/ci.yml` runs the quality gate on push or pull request.

## Verification

```bash
make build
make vibe-verify
make infra-up
make run-backend  # in another shell, then curl http://localhost:8080/api/health
```

## Definition of Done

The reactor builds, all three services boot, the frontend type-checks and serves its
placeholder route, local infrastructure starts through Compose, and the CI gate is green.
This matches the milestone Definition of Done in [`AGENTS.md`](../../../AGENTS.md#definition-of-done-every-milestone).
