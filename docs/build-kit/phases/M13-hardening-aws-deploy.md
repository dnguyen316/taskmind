# M13 — Hardening & AWS Deployment

## Objective

Make TaskMind production-ready after feature parity is complete. This milestone adds the
cross-cutting hardening required by [`AGENTS.md`](../../../AGENTS.md) and the build-kit
architecture: Redis-backed rate limiting, request correlation, structured logs,
metrics/tracing, `prod` profile safety checks, and the AWS deployment path for the Vue SPA,
three Spring Boot services, and managed data plane.

The production target remains the stack defined in the root guide and overview: ECS
Fargate for `apps/backend`, `apps/relay`, and `apps/ai`; RDS PostgreSQL 16; ElastiCache
Redis 7; Amazon OpenSearch Service; Amazon S3; ALB/WAF; CloudFront; and CI/CD.

## Depends on

All prior milestones (`M00` through `M12`) must be implemented and green before this
closeout milestone starts. M13 verifies the complete parity checklist from
[`01-build-order.md`](../01-build-order.md#parity-checklist-track-as-you-go) rather than
introducing new product features.

## Scope

**In:**

- Core `ratelimit` module using Bucket4j and Redis.
- Shared observability conventions across Core, Relay, and Nova.
- `prod` profile hardening for all runnable services.
- AWS infrastructure-as-code and deployment documentation/scripts.
- Dockerfiles for all deployable applications.
- CI/CD path from verification to container images, ECS deployment, and frontend
  publication to S3 + CloudFront.
- Final feature-parity audit.

**Out:**

- New product capabilities beyond the parity checklist.
- Frontend calls to Relay or Nova. The frontend still talks only to Core.
- Service-boundary changes that move business state out of Core, analytics projections out
  of Relay, or LLM prompt/provider ownership out of Nova.

## Files to create or update

```text
# Rate limiting (Core)
apps/backend/src/main/java/.../ratelimit/RateLimitFilter.java
apps/backend/src/main/java/.../ratelimit/RateLimitService.java
apps/backend/src/main/java/.../ratelimit/RedisRateLimitService.java
apps/backend/src/main/java/.../ratelimit/RateLimitProperties.java
apps/backend/src/main/java/.../ratelimit/RateLimitConfig.java

# Observability (all three Spring Boot services)
apps/backend/src/main/java/.../config/logging/RequestContextFilter.java
apps/backend/src/main/java/.../config/logging/RequestContextConfig.java
apps/backend/src/main/java/.../config/logging/RequestCorrelation.java
apps/backend/src/main/java/.../config/logging/RequestLoggingProperties.java
apps/backend/src/main/java/.../config/logging/ProblemDetailLogging.java
apps/backend/src/main/java/.../config/logging/NovaLogContext.java
apps/backend/src/main/resources/logback-spring.xml
apps/relay/src/main/java/.../config/logging/RequestContextFilter.java
apps/relay/src/main/java/.../config/logging/RequestContextConfig.java
apps/relay/src/main/java/.../config/logging/RequestCorrelation.java
apps/relay/src/main/java/.../config/logging/RequestLoggingProperties.java
apps/relay/src/main/java/.../config/logging/ProblemDetailLogging.java
apps/relay/src/main/resources/logback-spring.xml
apps/ai/src/main/java/.../config/logging/RequestContextFilter.java
apps/ai/src/main/java/.../config/logging/RequestContextConfig.java
apps/ai/src/main/java/.../config/logging/RequestCorrelation.java
apps/ai/src/main/java/.../config/logging/RequestLoggingProperties.java
apps/ai/src/main/java/.../config/logging/ProblemDetailLogging.java
apps/ai/src/main/java/.../config/logging/NovaLogContext.java
apps/ai/src/main/resources/logback-spring.xml

# Production profiles
apps/backend/src/main/resources/application-prod.properties
apps/relay/src/main/resources/application-prod.properties
apps/ai/src/main/resources/application-prod.properties

# AWS infrastructure-as-code
infra/aws/network/      # VPC, subnets, NAT, service endpoints
infra/aws/data/         # RDS, ElastiCache, OpenSearch, S3
infra/aws/compute/      # ECS cluster, task definitions/services, service discovery
infra/aws/edge/         # ALB, WAF, CloudFront, Route 53, ACM
infra/aws/observability/# logs, metrics, alarms, traces, dashboards

# CI/CD and deployment packaging
.github/workflows/deploy.yml
apps/backend/Dockerfile
apps/relay/Dockerfile
apps/ai/Dockerfile
apps/frontend/Dockerfile
```

Use the real package names created in prior milestones in place of `...`. If shared
logging code is extracted into a library, keep service ownership and Spring Boot auto-wiring
clear and update this file list with the final location.

## Implementation notes

### Rate limiting

- Implement rate limiting in Core only, because all public API traffic enters through
  Core.
- Use Bucket4j 8.14 with Redis-backed buckets, matching the stack in
  [`AGENTS.md`](../../../AGENTS.md#tech-stack-do-not-substitute).
- Apply the rate-limit filter after bearer authentication so authenticated-user tiers can
  be distinguished from anonymous/IP tiers.
- Return HTTP `429` when a configured bucket is exhausted.
- Make tiers configurable under `taskmind.ratelimit.*`. At minimum support anonymous/IP,
  authenticated user, auth-flow, and AI-heavy endpoint limits.
- Keep the feature flag configurable so rate limiting can be disabled for local debugging
  without changing code.
- Treat WAF rules on the ALB as complementary edge protection, not a substitute for
  application-level limits.

### Observability

- Add request correlation to Core, Relay, and Nova.
- Accept an inbound correlation header when present, create one when absent, place it in
  MDC, and propagate it on Core↔Nova↔Relay service-token calls.
- Emit console-friendly logs for local/test and JSON logs for `prod`.
- Include error/problem-detail logging without leaking secrets, tokens, OTPs, attachment
  contents, or LLM prompt payloads.
- Expose Spring Boot Actuator `health`, `info`, `metrics`, and `prometheus` endpoints as
  appropriate for each environment; `/actuator/prometheus` must be reachable only from
  internal scraper infrastructure and must require service-token authentication rather
  than accepting anonymous public traffic.
- Record Nova provider tokens, run counts, and LLM response latency with bounded tags only
  (`provider`, `model`, `capability`, and `status`); never tag user/workspace/run IDs,
  prompt text, or correlation IDs.
- Add an OTLP tracing toggle. Tracing must be safe to disable locally and enable in
  staging/production.

### Production profile guarantees

- Keep the E2E auth bypass disabled in `prod`. The application must fail fast if
  `taskmind.auth.e2e-bypass.*` is enabled with the `prod` profile.
- Require strong JWT and service-token secrets from the environment or AWS Secrets Manager.
- Set `TASKMIND_AUTH_OTP_MAX_ATTEMPTS` deliberately per environment; Core defaults to `5`
  and invalidates an OTP challenge when failed verification reaches the limit.
- Use the managed AWS endpoints in production: RDS JDBC URL, ElastiCache Redis URL,
  OpenSearch endpoint, and S3 bucket/region.
- Do not allow the production S3 adapter to silently fall back to local filesystem storage
  or LocalStack.
- Use AWS SigV4/IAM task-role access for OpenSearch/S3 where the service integration
  requires it.
- Preserve Flyway ownership from the overview: only Core and Nova run Flyway in the
  shared `taskmind` database; Relay consumes events and writes projections, but does not
  own business-state schema changes.

### AWS deployment path

- Provision private networking for the three Spring Boot services and managed data-plane
  resources.
- Deploy Core publicly through ALB/WAF. Relay and Nova stay private/internal and are called
  through service-token protected internal routes.
- Run Core, Relay, and Nova as separate ECS Fargate services with per-service IAM task
  roles and environment-specific configuration.
- Use RDS PostgreSQL 16 with schemas `public`, `analytics`, and `ai` in the `taskmind`
  database, matching the overview.
- Use ElastiCache Redis 7 for cache, streams, rate-limit state, and chat/session data as
  defined by prior milestones.
- Use Amazon OpenSearch Service for Relay activity search.
- Use Amazon S3 for attachments and for the built frontend artifact.
- Serve the frontend through CloudFront with `403`/`404` fallback to `/index.html` for SPA
  routing.
- Set `VITE_API_BASE_URL` per environment so the frontend points at the Core API only.
- Keep long-running SSE practical at the edge. If the chosen ALB/nginx pattern requires an
  idle timeout, set it explicitly and document the value.

### CI/CD

- Add a deployment workflow that runs the normal verification gate before packaging.
- Build per-service container images for Core, Relay, Nova, and the frontend.
- Push backend images to ECR and deploy the ECS services for the target environment.
- Publish the built frontend to S3 and invalidate CloudFront.
- Keep secrets out of workflow files; use GitHub/AWS identity federation and AWS Secrets
  Manager/SSM where appropriate.

## Acceptance criteria

- [ ] Rate limits are enforced with HTTP `429` per configured tier and can be configured
      or disabled by environment.
- [ ] Correlation IDs appear in logs across Core, Relay, and Nova for a request path that
      crosses service boundaries.
- [ ] `prod` emits structured JSON logs and exposes the expected health/metrics endpoints.
- [ ] `prod` fails fast when E2E bypass is enabled or required secrets/endpoints are
      missing.
- [ ] AWS IaC provisions the network, managed data plane, ECS services, and edge layer.
- [ ] CI/CD verifies, builds, and deploys all three services plus the frontend.
- [ ] Staging smoke test passes: login, create task, AI chat stream, attachment upload,
      dashboard/analytics, notification SSE.
- [ ] The parity checklist in
      [`01-build-order.md`](../01-build-order.md#parity-checklist-track-as-you-go) is
      complete.

## Verification

```bash
make vibe-verify
make infra-up     # managed data-plane containers as a pre-AWS rehearsal
make run-backend
make run-relay
make run-ai
make run-frontend
# Staging: deploy via .github/workflows/deploy.yml, then run the staging smoke checklist.
```

For the smoke test, use the same user-visible path as the product: the browser calls the
frontend, the frontend calls Core, Core delegates to Relay/Nova internally, and attachments
use the configured object-storage adapter.

## Definition of Done

Rate limiting, observability, and production hardening are implemented; the AWS deployment
path works to staging; the full parity checklist is green; `make vibe-verify` passes; and
the staging smoke test passes.
