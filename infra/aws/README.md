# TaskMind AWS Infrastructure

TaskMind's M13 AWS layout is defined with **OpenTofu/Terraform-compatible HCL**. The
configuration is intentionally split by deployment concern so environments can wire the
same building blocks from a root stack or Terragrunt later without changing module
interfaces.

## Tooling choice

- **Chosen IaC tool:** OpenTofu 1.8+ using the HashiCorp AWS provider 5.x syntax.
- **Terraform compatibility:** the modules avoid OpenTofu-only language features and can be
  run by Terraform 1.6+ if an environment standardizes on Terraform instead.
- **State:** store remote state in an encrypted S3 backend with DynamoDB locking per
  environment when these modules are promoted into an environment root.
- **Backend-state region:** the TaskMind Sydney AWS account stores the staging and
  production Terraform/OpenTofu state buckets and DynamoDB lock tables in
  `ap-southeast-2`, matching the deployment region. Do not treat the backend region as
  independent from the workload region unless the bootstrap resources are deliberately
  migrated and the committed backend blocks are updated in the same change.

## Layout

```text
infra/aws/
  network/        # VPC, public/private subnets, routing, NAT, VPC endpoints
  security/       # Shared ECS/data-plane security groups used across modules
  data/           # RDS PostgreSQL 16, ElastiCache Redis 7, OpenSearch, S3 buckets
  compute/        # ECS cluster, task definitions/services, task roles/policies
  edge/           # ALB/WAF for Core, CloudFront for the SPA, ACM/Route53 hooks
  observability/  # CloudWatch logs/alarms/dashboards and X-Ray sampling
  envs/
    staging/      # Composed staging root with S3 state + DynamoDB locking
    production/   # Composed protected production root with stricter defaults
```


## Remote state migration

Decision: staging and production state storage are migrated/recreated in
`ap-southeast-2` for the Sydney AWS account. The previous `us-east-1` backend setting was a
bootstrap default and should not be used for this account unless an explicit rollback plan
recreates the old backend resources.

Preferred migration flow for each environment (`staging`, then `production`):

1. Confirm the destination `ap-southeast-2` S3 state bucket and DynamoDB lock table exist
   with encryption, versioning, and least-privilege IAM equivalent to the previous backend.
2. From `infra/aws/envs/<environment>`, run `tofu init -migrate-state` and approve the
   backend migration only after OpenTofu shows the source and destination bucket, key,
   lock table, and region values expected for that environment. Terraform users can run
   the equivalent `terraform init -migrate-state`.
3. Run `tofu state list` to verify state is readable from the Sydney backend, then run a
   read-only validation such as `tofu plan -refresh-only` before applying any
   infrastructure changes.
4. Keep the old `us-east-1` bucket and lock table retained until the migrated state has
   been backed up and at least one successful plan has completed from the
   `ap-southeast-2` backend.

## Deployment model

Core is the only public API and is exposed through ALB + WAF. Relay and Nova run as
private ECS Fargate services and are reachable only from inside the VPC via Cloud Map.
Non-local AWS environments must provide `alb_certificate_arn` for an ACM certificate in
the primary `ap-southeast-2` region on the Core ALB: only the HTTPS listener forwards to
the Core target group, while the HTTP listener redirects to HTTPS when a certificate
exists and otherwise returns a fixed `426` response instead of forwarding plaintext
traffic. The Vue SPA is built separately, published to the frontend artifact bucket, and
served by CloudFront using origin access control. The primary TaskMind deployment region
is `ap-southeast-2`; CloudFront ACM viewer certificates for SPA aliases may still be
issued in `us-east-1` because CloudFront requires viewer certificates there. The
CloudFront distribution maps SPA `403` and `404` responses to `/index.html` so
client-side routing works on deep links.

## Example composition

Environment roots live under `infra/aws/envs/<environment>` and instantiate the modules in
this dependency order:

1. `network` creates the VPC and subnets.
2. `security` creates the shared ECS, RDS, Redis, and OpenSearch security groups. These
   security groups are owned outside `compute` so `data` can authorize ECS traffic without
   depending on any compute outputs.
3. `data` creates RDS, Redis, OpenSearch, and the S3 buckets using security group IDs
   passed in by the root. The module does not need ECS task role ARNs or other compute
   outputs.
4. `edge` creates the ALB, Core target group, WAF, and CloudFront distribution after the
   frontend bucket exists. It exposes the ALB security group ID and Core target group ARN
   so the root can add the ALB-to-ECS ingress rule and `compute` can register the Core
   service without `edge` depending on `compute`.
5. `compute` creates the ECS cluster, services, task definitions, and task roles after
   `data` and `edge` exist. It consumes the shared ECS security group ID, attachment
   bucket ARN, OpenSearch domain ARN, and Core target group ARN.
6. Root-owned cross-module policies are applied after both sides exist: the frontend S3
   bucket policy is composed after `data` and `edge`, and the OpenSearch domain policy is
   composed after `data` and `compute`.
7. `observability` wires dashboards and alarms after `data`, `edge`, and `compute` expose
   their identifiers.

The roots pass module outputs explicitly instead of reconstructing ARNs. They use S3 remote
state and DynamoDB locking in each `backend.tf`. For the Sydney AWS account, the committed
backend blocks point at `ap-southeast-2` state buckets and lock tables for both staging and
production, so `var.aws_region` and the backend `region` remain aligned by default. If a
different account uses nonstandard bootstrap bucket or lock table names, pass OpenTofu
`-backend-config` overrides during `tofu init`; only override the backend region when the
state bucket and DynamoDB table actually exist in that alternate region. Production requires
the protected `production` GitHub Environment, an ALB HTTPS certificate ARN, RDS deletion
protection, and final snapshots. Keep all secrets in AWS Secrets Manager or SSM Parameter
Store, and never commit generated state files or plaintext secrets.

### RDS final snapshots and disposable previews

The shared data module defaults `skip_final_snapshot` to `false`, and the staging and
production roots pass `deletion_protection = true` with `skip_final_snapshot = false` so
managed RDS databases retain a final recovery point if deletion is ever approved.
Disposable preview stacks may intentionally set `deletion_protection = false` and
`skip_final_snapshot = true` only when their data is ephemeral, reproducible, and not
needed for incident response or rollback. Keep that preview-specific behavior in the
preview root or its variable files rather than weakening staging or production defaults.

## Observability and metrics

TaskMind standardizes on **Micrometer** metrics in every Spring Boot service. Local
development exposes each service's Prometheus scrape endpoint at `/actuator/prometheus`
and uses the local observability compose stack to run **Prometheus** and **Grafana**. The
Prometheus config scrapes Core on `backend:8080`, Relay on `relay:8081`, and Nova on
`ai:8082` with `metrics_path: /actuator/prometheus`; Grafana is provisioned with that
Prometheus datasource.

Production metrics should use the same Micrometer/Prometheus metric contracts. The
preferred AWS path is **Amazon Managed Service for Prometheus** scraping the ECS service
endpoints or sidecar/collector targets for Core, Relay, and Nova, with dashboards in
Amazon Managed Grafana or the committed CloudWatch dashboard module. If a deployment does
not run Prometheus-compatible scraping, enable **CloudWatch Container Insights** for ECS
platform/task health and forward the application Prometheus metrics through an
OpenTelemetry or CloudWatch agent pipeline so the service-level names below remain the
canonical alert inputs.

Canonical application metrics:

| Concern | Micrometer meter name | Prometheus export / primary query input | Notes |
| --- | --- | --- | --- |
| API latency | `http.server.requests` | `http_server_requests_seconds` (`_bucket`, `_count`, `_sum`, `_max`) | Built-in Spring Boot HTTP server timer; tag by bounded `uri`, `method`, `status`, and service/job labels. |
| Outbox lag | `taskmind.outbox.lag.events` | `taskmind_outbox_lag_events` | Core gauge of unpublished `outbox_events` rows. |
| Redis stream pending | `taskmind.relay.stream.pending` | `taskmind_relay_stream_pending` | Relay gauge for pending entries in the configured Redis consumer group. |
| Redis stream processing time | `taskmind.relay.stream.processing.duration` | `taskmind_relay_stream_processing_duration_seconds` (`_bucket`, `_count`, `_sum`, `_max`) | Relay timer tagged with `result` (`ingested`, `duplicate`, `dead_letter`, `failed`). |
| AI prompt tokens | `taskmind.ai.tokens.prompt` | `taskmind_ai_tokens_prompt_total` | Nova counter tagged only by bounded `provider`, `model`, `capability`, and `status`. |
| AI completion tokens | `taskmind.ai.tokens.completion` | `taskmind_ai_tokens_completion_total` | Same bounded tags as prompt tokens. |
| AI total tokens | `taskmind.ai.tokens.total` | `taskmind_ai_tokens_total_total` | Primary token-spend alert input. |
| LLM response duration | `taskmind.ai.llm.response.duration` | `taskmind_ai_llm_response_duration_seconds` (`_bucket`, `_count`, `_sum`, `_max`) | Nova provider latency timer with the same bounded AI tags. |

Suggested alerts start as environment-specific defaults and should be tuned from baseline
traffic before paging:

- **p99 API latency:** page when `histogram_quantile(0.99, sum by (le, job) (rate(http_server_requests_seconds_bucket{uri!~"/actuator/.*|/api/health"}[5m])))` stays above the service SLO (for example 1s for Core user APIs or 2s for AI facades) for 10 minutes.
- **Outbox lag:** warn when `taskmind_outbox_lag_events` is above 1,000 for 10 minutes; page when it is above 10,000 or monotonically increasing for 15 minutes because Relay freshness is at risk.
- **Relay dead-letter rate:** page when `sum(rate(taskmind_relay_events_dead_letters_total[5m]))` is greater than zero for 10 minutes, and warn on any single dead-letter event in staging so schema/projection drift is caught early.
- **AI token spikes:** warn when `sum(rate(taskmind_ai_tokens_total_total[5m]))` is more than 3x the same service's recent baseline for 15 minutes; page when the spike coincides with elevated `taskmind_ai_llm_response_duration_seconds` p95/p99 or provider failure status because runaway prompts or retries can drive cost and user-visible latency.

## CloudWatch log encryption

The compute module keeps ECS service log retention configurable with `log_retention_days`
and accepts an optional `cloudwatch_logs_kms_key_id` for encrypting the Core, Relay, and
Nova CloudWatch log groups with a customer managed KMS key. Production environments should
provide a regional customer managed key instead of relying on the default CloudWatch Logs
encryption. The key policy must permit the CloudWatch Logs service principal for the target
region, `logs.<region>.amazonaws.com`, to use the key for the log group ARNs, including
`kms:Encrypt`, `kms:Decrypt`, `kms:ReEncrypt*`, `kms:GenerateDataKey*`, and
`kms:Describe*`. Scope the policy with an encryption-context condition such as
`kms:EncryptionContext:aws:logs:arn = arn:aws:logs:<region>:<account-id>:log-group:/taskmind/<environment>/*`
so only the TaskMind log groups can use the key.

## CI/CD pipelines

The repository includes three GitHub Actions workflows for AWS deployment preparation:

- `CI` (`.github/workflows/ci.yml`) runs `scripts/vibe-verify.sh` for pull requests and
  pushes to `main`.
- `AWS Infrastructure Plan` (`.github/workflows/infra-plan.yml`) validates the OpenTofu
  modules in this directory and can manually plan a composed `infra/aws/envs/<environment>`
  root once one is added.
- `Deploy` (`.github/workflows/deploy.yml`) uses GitHub OIDC to build/push Core, Relay,
  and Nova images to ECR, sync the Vue SPA to S3, invalidate CloudFront, and roll ECS
  services with newly rendered task definitions.

See [`docs/deployment/aws-cicd.md`](../../docs/deployment/aws-cicd.md) for the GitHub
Environment variables, AWS bootstrap checklist, and staging-to-production promotion model.
