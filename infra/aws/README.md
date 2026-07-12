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

## Deployment model

Core is the only public API and is exposed through ALB + WAF. Relay and Nova run as
private ECS Fargate services and are reachable only from inside the VPC via Cloud Map.
Non-local AWS environments must provide `alb_certificate_arn` for an ACM certificate on
the Core ALB: only the HTTPS listener forwards to the Core target group, while the HTTP
listener redirects to HTTPS when a certificate exists and otherwise returns a fixed `426`
response instead of forwarding plaintext traffic. The Vue SPA is built separately,
published to the frontend artifact bucket, and served by CloudFront using origin access
control. The CloudFront distribution maps SPA `403` and `404` responses to `/index.html`
so client-side routing works on deep links.

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
state and DynamoDB locking in each `backend.tf`; if an AWS account uses different bootstrap
bucket or lock table names, pass OpenTofu `-backend-config` overrides during `tofu init` or
update the committed backend block before planning. Production requires the protected
`production` GitHub Environment, an ALB HTTPS certificate ARN, RDS deletion protection, and
final snapshots. Keep all secrets in AWS Secrets Manager or SSM Parameter Store, and never
commit generated state files or plaintext secrets.

### RDS final snapshots and disposable previews

The shared data module defaults `skip_final_snapshot` to `false`, and the staging and
production roots pass `deletion_protection = true` with `skip_final_snapshot = false` so
managed RDS databases retain a final recovery point if deletion is ever approved.
Disposable preview stacks may intentionally set `deletion_protection = false` and
`skip_final_snapshot = true` only when their data is ephemeral, reproducible, and not
needed for incident response or rollback. Keep that preview-specific behavior in the
preview root or its variable files rather than weakening staging or production defaults.

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
