# Reference — AWS Infrastructure & Data Plane

The rebuild keeps the identical app stack but targets **AWS managed services** for the data
plane: **Amazon S3** (object storage, replacing MinIO) and **Amazon OpenSearch Service**
(search, replacing self-hosted Elasticsearch), with **RDS Postgres** + **ElastiCache Redis**
in production. Local dev still uses containers / LocalStack so the inner loop is offline and
fast.

## Local → AWS mapping

| Concern | Local dev | AWS production |
|---------|-----------|----------------|
| Postgres | container `postgres:16` | **RDS PostgreSQL 16** (Multi-AZ in prod) |
| Redis | container `redis:7` | **ElastiCache Redis 7** (replication group) |
| Search | container OpenSearch/ES (`:9200`) | **Amazon OpenSearch Service** (VPC endpoint) |
| Object storage | **LocalStack S3** (`:4566`) or filesystem fallback | **Amazon S3** + IAM task role |
| Edge / nginx gateway | nginx gateway (compose) | **ALB + WAF** (+ optional nginx sidecar for SSE) |
| Compute | `spring-boot:run` / compose | **ECS Fargate** (Core, Relay, Nova) |
| Frontend | Vite dev server | **S3 + CloudFront** (OAC, `403/404 → /index.html`) |
| Secrets | `infra/env/.env` | **AWS Secrets Manager** |
| Internal DNS | localhost ports | **AWS Cloud Map** (`*.taskmind.local`) |

One database `taskmind` with three schemas: **public** (Core), **ai** (Nova),
**analytics** (Relay). Only **Core** and **Nova** run Flyway.

## Object storage: Amazon S3 (replaces MinIO)

Implement the `ObjectStoragePort` (in the `attachment` module) with an **S3 adapter** using
**AWS SDK for Java v2**:

- Default profile (`@Profile("!test")`): `S3ObjectStorageAdapter`.
- **Prod**: use the **default credential chain + IAM task role**; do **not** set
  `endpointOverride`. Bucket `taskmind-attachments-{env}`, block public access,
  versioning + lifecycle rules.
- **Local**: point at LocalStack (`TASKMIND_STORAGE_ENDPOINT=http://localhost:4566`,
  path-style access, static creds) or use a filesystem fallback adapter for tests.

Env vars (carried from the reference, S3-flavored):

```text
TASKMIND_STORAGE_ENDPOINT=        # empty in prod (real S3); set for LocalStack
TASKMIND_STORAGE_BUCKET=taskmind-attachments
TASKMIND_STORAGE_REGION=us-east-1
TASKMIND_STORAGE_ACCESS_KEY=      # local only; prod uses IAM role
TASKMIND_STORAGE_SECRET_KEY=      # local only
```

## Search: Amazon OpenSearch Service (replaces self-hosted ES)

Relay (and Core's activity-search read API) target OpenSearch:

- `spring.elasticsearch.uris` → the OpenSearch domain HTTPS endpoint.
- **Auth**: AWS SigV4 request signing (or fine-grained access control with a master user in
  a VPC). Add an OpenSearch/SigV4 request interceptor to the Elasticsearch/REST client.
- Index `activity-events`; documents `ActivityEventDocument`.
- **Local**: an OpenSearch (or ES 8.x) container; the Spring Data ES client works against
  both for dev/test. The `test` profile **excludes** ES/OpenSearch autoconfig.
- Feature-flag with `TASKMIND_ACTIVITY_SEARCH_ENABLED`; the indexer bean is conditional on
  a configured URI so the stack runs without search locally.

## Postgres / Redis

- **RDS PostgreSQL 16**: `db.t4g.medium` (staging) → `db.r6g.large`+ (prod), Multi-AZ,
  gp3 autoscaling, PITR. Single DB, three schemas.
- **ElastiCache Redis 7**: cluster mode disabled initially (streams work on one shard).
  Multi-AZ replication in prod. Used for outbox stream `taskmind.events`, rate limiting,
  dashboard cache, Nova chat sessions.

## Compute: ECS Fargate

| Service | CPU / Mem | Min tasks | Public? |
|---------|-----------|-----------|---------|
| Core | 1 vCPU / 2 GB | 2 | yes (via ALB) |
| Relay | 0.5 vCPU / 1 GB | 1–2 | no |
| Nova | 1 vCPU / 2 GB | 1–2 | no |

- Per-service `Dockerfile` already exists (`apps/*/Dockerfile`); push to 3 ECR repos.
- Startup order: RDS/Redis/OpenSearch healthy → Core (Flyway, `/actuator/health`) → Relay →
  Nova.
- **SSE** (`/v1/nova/chat/stream`, `/v1/notifications/stream`): set ALB idle timeout to
  3600s, or run the nginx sidecar to mirror local config.
- Internal service URLs via Cloud Map:
  `TASKMIND_CORE_BASE_URL=http://core.taskmind.local:8080` (and relay/ai equivalents).

## Network & security

- VPC, 2 AZ min: public subnets (ALB, NAT), private subnets (ECS, RDS, Redis, OpenSearch).
- Exposure: **Core only** is public; Relay/Nova/data-plane private.
- **WAF** on ALB for coarse IP limits; keep **Redis-backed app rate limits**
  (`taskmind.ratelimit.*`) for per-user/AI quotas.
- Secrets in **Secrets Manager**: `TASKMIND_JWT_SECRET`, `TASKMIND_*_SERVICE_TOKEN`, LLM API
  keys, DB password, OAuth client secrets. Set `SPRING_PROFILES_ACTIVE=prod` (disables E2E
  bypass — enforced in code).

## CI/CD

```text
1. vibe-verify (scripts/vibe-verify.sh)
2. mvn package + docker build (backend, relay, ai)
3. push to ECR (3 repos)
4. ecs deploy per service (staging → manual approval → prod)
5. FE: npm build → S3 sync → CloudFront invalidation
```

IaC suggestion (Terraform/OpenTofu modules): `network/`, `data/`, `compute/`, `edge/`,
`observability/`.

## Phased rollout

1. **Staging** — single region, 1 NAT, RDS single-AZ, small OpenSearch, ECS min=1. Validate
   Flyway, outbox-Relay, AI facades, SSE, S3 upload, OpenSearch indexing.
2. **Production baseline** — Multi-AZ RDS + Redis replication, 2+ Core tasks, WAF, Route 53 +
   ACM TLS, Secrets Manager + IAM role for S3 (no static keys), tested backup/restore.
3. **Scale** — Aurora/read replica for reporting, OpenSearch ILM, optional Bedrock as a Nova
   provider (keep the Nova service boundary).

This mirrors the reference `docs/architecture/aws-infrastructure.md`; the rebuild's only
deltas are making **S3 the default object-storage adapter** and **OpenSearch the default
search target** (rather than MinIO/self-hosted ES) from the start.
