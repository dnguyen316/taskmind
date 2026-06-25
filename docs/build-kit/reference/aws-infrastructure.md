# Reference - AWS Infrastructure & Data Plane

The rebuild keeps the identical application stack but targets **AWS managed services** for
the data plane: **Amazon S3** (object storage, replacing MinIO) and **Amazon OpenSearch
Service** (search, replacing self-hosted Elasticsearch), with **RDS PostgreSQL** and
**ElastiCache Redis** in production. Local development still uses containers and
LocalStack so the inner loop is offline and fast.

## Local -> AWS mapping

| Concern | Local dev | AWS production |
|---------|-----------|----------------|
| Postgres | container `postgres:16` | **RDS PostgreSQL 16.x** (Multi-AZ in prod) |
| Redis | container `redis:7` | **ElastiCache Redis 7.x** (replication group) |
| Search | container OpenSearch/ES (`:9200`) | **Amazon OpenSearch Service** (VPC endpoint) |
| Object storage | **LocalStack S3** (`:4566`) or filesystem fallback | **Amazon S3** + IAM task role |
| Edge / nginx gateway | nginx gateway (compose) | **ALB + WAF** (+ optional nginx sidecar for SSE) |
| Compute | `spring-boot:run` / compose | **ECS Fargate** (Core, Relay, Nova) |
| Frontend | Vite dev server | **S3 + CloudFront** (OAC, `403/404 -> /index.html`) |
| Secrets | `infra/env/.env` | **AWS Secrets Manager** |
| Internal DNS | localhost ports | **AWS Cloud Map** (`*.taskmind.local`) |

One database `taskmind` contains three schemas: **public** (Core), **ai** (Nova), and
**analytics** (Relay). Only **Core** and **Nova** run Flyway.

## Object storage: Amazon S3 (replaces MinIO)

Implement the `ObjectStoragePort` in the `attachment` module with an **S3 adapter** using
**AWS SDK for Java v2**.

- Default profile (`@Profile("!test")`): `S3ObjectStorageAdapter`.
- **Prod**: use the **default credential chain + IAM task role**; do **not** set
  `endpointOverride`. Bucket: `taskmind-attachments-{env}`. Block public access, enable
  versioning, and configure lifecycle rules.
- **Local**: point at LocalStack (`TASKMIND_STORAGE_ENDPOINT=http://localhost:4566`,
  path-style access, static credentials) or use a filesystem fallback adapter for tests.

Environment variables, carried from the reference and S3-flavored:

```env
TASKMIND_STORAGE_ENDPOINT=        # empty in prod (real S3); set for LocalStack
TASKMIND_STORAGE_BUCKET=taskmind-attachments
TASKMIND_STORAGE_REGION=us-east-1
TASKMIND_STORAGE_ACCESS_KEY=      # local only; prod uses IAM role
TASKMIND_STORAGE_SECRET_KEY=      # local only
```

## Search: Amazon OpenSearch Service (replaces self-hosted ES)

Relay, plus Core's activity-search read API, target OpenSearch:

- `spring.elasticsearch.uris` points to the OpenSearch domain HTTPS endpoint.
- **Auth**: use AWS SigV4 request signing, or fine-grained access control with a master
  user inside a VPC. Add an OpenSearch/SigV4 request interceptor to the Elasticsearch REST
  client.
- Index: `activity-events`; documents: `ActivityEventDocument`.
- Relay ingest keeps event-store/projection writes atomic with activity indexing. An
  OpenSearch indexing exception is DLQ'd and counted, but the event-store insert is
  rolled back so a later retry can index the same event instead of being blocked by
  `analytics.event_store` dedupe.
- **Local**: use an OpenSearch or Elasticsearch 8.x container. The Spring Data ES client
  works against both for development and tests. The `test` profile excludes
  ES/OpenSearch autoconfiguration.
- Feature-flag search with `TASKMIND_ACTIVITY_SEARCH_ENABLED`; the indexer bean is
  conditional on a configured URI so the stack runs without search locally.

## Postgres / Redis

- **RDS PostgreSQL 16**: use `db.t4g.medium` in staging and `db.r6g.large` or larger in
  production, with Multi-AZ, gp3 storage autoscaling, and PITR. Use a single database with
  three schemas.
- **ElastiCache Redis 7**: begin with cluster mode disabled because streams work on one
  shard. Use Multi-AZ replication in production. Redis is used for the outbox stream
  `taskmind.events`, rate limiting, dashboard cache, and Nova chat sessions.

## Compute: ECS Fargate

| Service | CPU / Mem | Min tasks | Public? |
|---------|-----------|-----------|---------|
| Core | 1 vCPU / 2 GB | 2 | yes (via ALB) |
| Relay | 0.5 vCPU / 1 GB | 1-2 | no |
| Nova | 1 vCPU / 2 GB | 1-2 | no |

- A per-service `Dockerfile` exists under `apps/*/Dockerfile`; build from the repository
  root so the Spring Boot images can resolve the Maven reactor and the frontend can inject
  `VITE_API_BASE_URL` at build time. Use `make image-build` for all deployable images or
  target a single app with `make image-build-backend`, `make image-build-relay`,
  `make image-build-ai`, or `make image-build-frontend`. Override `IMAGE_REGISTRY`,
  `IMAGE_TAG`, and `VITE_API_BASE_URL` as needed before pushing to ECR or a web edge
  runtime registry.
- Startup order: RDS, Redis, and OpenSearch healthy -> Core (Flyway and
  `/actuator/health`) -> Relay -> Nova.
- **SSE** (`/v1/nova/chat/stream`, `/v1/notifications/stream`): set the ALB idle timeout
  to 3600 seconds, or run the nginx sidecar to mirror local configuration.
- Internal service URLs resolve through Cloud Map, for example
  `TASKMIND_CORE_BASE_URL=http://core.taskmind.local:8080` and equivalent Relay and Nova
  URLs.

## Network & security

- Use one VPC across two Availability Zones.
- Use public subnets for the ALB and NAT, and private subnets for ECS, RDS, Redis, and
  OpenSearch.
- Exposure: **Core only** is public; Relay, Nova, and the data plane stay private.
- Put **WAF** on the ALB for coarse IP limits. Keep **Redis-backed application rate
  limits** (`taskmind.ratelimit.*`) for per-user and AI quotas. Configure Core OTP abuse
  protection with `TASKMIND_AUTH_OTP_MAX_ATTEMPTS` / `taskmind.auth.otp.max-attempts`
  (default `5`) so failed verification challenges are invalidated after the limit.
- Store secrets in **AWS Secrets Manager**: `TASKMIND_JWT_SECRET`,
  `TASKMIND_*_SERVICE_TOKEN`, LLM API keys, database password, and OAuth client secrets.
- Set `SPRING_PROFILES_ACTIVE=prod` in production. The production profile disables the E2E
  bypass and the application enforces that guard in code.

## CI/CD

```text
1. vibe-verify (scripts/vibe-verify.sh)
2. `make image-build IMAGE_REGISTRY=<registry> IMAGE_TAG=<sha> VITE_API_BASE_URL=<core-url>`
3. push backend, relay, ai, and frontend images to the target registry
4. ecs deploy per service (staging -> manual approval -> prod)
5. frontend image smoke test: curl `/healthz` and `/` before promotion; S3/CloudFront remains
   an optional static-hosting deployment variant
```

IaC suggestion using Terraform/OpenTofu modules: `network/`, `data/`, `compute/`,
`edge/`, and `observability/`.

## Phased rollout

1. **Staging** - single region, one NAT, RDS single-AZ, small OpenSearch, ECS min=1.
   Validate Flyway, outbox-Relay, AI facades, SSE, S3 upload, and OpenSearch indexing.
2. **Production baseline** - Multi-AZ RDS + Redis replication, two or more Core tasks,
   WAF, Route 53 + ACM TLS, Secrets Manager + IAM role for S3 (no static keys), and tested
   backup/restore.
3. **Scale** - Aurora/read replica for reporting, OpenSearch ILM, optional Bedrock as a
   Nova provider. Keep the Nova service boundary.

This mirrors the reference infrastructure direction. The rebuild's only deltas are making
**S3 the default object-storage adapter** and **OpenSearch the default search target**
from the start, rather than MinIO or self-hosted Elasticsearch.
