# AWS CI/CD preparation

TaskMind deploys to AWS through GitHub Actions, AWS OIDC, ECR, ECS Fargate, S3, and
CloudFront. The pipelines are split so application validation, infrastructure validation,
and deployment can be operated independently.

## Pipelines

| Workflow | Trigger | Purpose |
| --- | --- | --- |
| `CI` (`.github/workflows/ci.yml`) | Pull requests and pushes to `main` | Runs the repository quality gate: Java tests plus frontend typecheck. |
| `AWS Infrastructure Plan` (`.github/workflows/infra-plan.yml`) | Pull requests touching `infra/aws/**`; manual environment plan | Validates each OpenTofu module and, when an environment root exists, plans staging or production. |
| `Deploy` (`.github/workflows/deploy.yml`) | Pushes to `main`; manual staging/production dispatch | Verifies the repo, builds service images, pushes ECR images, publishes the SPA, invalidates CloudFront, and rolls ECS services. |

## Required GitHub environment variables

Create `staging` and `production` GitHub Environments with these variables:

| Variable | Used by | Description |
| --- | --- | --- |
| `AWS_ROLE_TO_ASSUME` | Deploy and infra plan | IAM role trusted by GitHub OIDC. |
| `AWS_ACCOUNT_ID` | Deploy | Expected AWS account ID. Deploy preflight fails if the OIDC role authenticates into a different account. |
| `AWS_REGION` | Deploy and infra plan | AWS region for ECR, ECS, S3, CloudFront invalidation calls, and OpenTofu. |
| `CORE_ECR_REPOSITORY` | Deploy | ECR repository name for Core API. |
| `RELAY_ECR_REPOSITORY` | Deploy | ECR repository name for Relay. |
| `NOVA_ECR_REPOSITORY` | Deploy | ECR repository name for Nova AI. |
| `ECS_CLUSTER` | Deploy | ECS cluster name or ARN. |
| `CORE_ECS_SERVICE` | Deploy | Core ECS service name. |
| `RELAY_ECS_SERVICE` | Deploy | Relay ECS service name. |
| `NOVA_ECS_SERVICE` | Deploy | Nova ECS service name. |
| `CORE_TASK_DEFINITION` | Deploy | Current Core task definition family or ARN. |
| `RELAY_TASK_DEFINITION` | Deploy | Current Relay task definition family or ARN. |
| `NOVA_TASK_DEFINITION` | Deploy | Current Nova task definition family or ARN. |
| `CORE_CONTAINER_NAME` | Deploy | Container name inside the Core task definition, normally `core`. |
| `RELAY_CONTAINER_NAME` | Deploy | Container name inside the Relay task definition, normally `relay`. |
| `NOVA_CONTAINER_NAME` | Deploy | Container name inside the Nova task definition, normally `nova`. |
| `FRONTEND_S3_BUCKET` | Deploy | Private S3 bucket serving the SPA through CloudFront. |
| `CLOUDFRONT_DISTRIBUTION_ID` | Deploy | CloudFront distribution to invalidate after SPA upload. |
| `VITE_API_BASE_URL` | Deploy | Build-time API base URL for the frontend. |

## ALB certificate requirement

The AWS edge module requires `alb_certificate_arn` for every non-local environment so
Core API traffic is served through HTTPS. Configure or import an ACM certificate in the
same region as the Core ALB before planning staging or production, then pass its ARN into
the edge module. With a certificate, port 80 only redirects to port 443 and the HTTPS
listener is the only listener that forwards to the Core target group. Without a
certificate, the module keeps port 80 from reaching Core by returning a fixed `426`
response; this fallback is intended for local/non-production composition only.

## AWS bootstrap checklist

1. Create the remote state S3 bucket and DynamoDB lock table per environment before using
   composed environment roots.
2. Create ECR repositories for Core, Relay, and Nova.
3. Create a GitHub OIDC IAM role with least-privilege access to ECR push, ECS task
   definition/service updates, S3 SPA sync, CloudFront invalidation, and read-only plan
   access for OpenTofu.
4. Apply or import AWS infrastructure so the ECS cluster, services, task definition
   families, frontend bucket, and CloudFront distribution exist.
5. Add all required GitHub Environment variables above.
6. Run `CI`, then `AWS Infrastructure Plan`, then `Deploy` against `staging` before
   enabling production approvals.

## Promotion model

- Pull requests run `CI` and OpenTofu module validation.
- Merges to `main` deploy to `staging` by default.
- Production deploys are manual through `workflow_dispatch` and should be protected by
  GitHub Environment reviewers.

## ECS service secret inputs

The AWS compute module grants each ECS task role access only to the secrets listed for
that service. Do not use wildcard secret ARNs. Keep every `service_secrets` entry aligned
with the matching service-specific IAM input so a task definition never injects a secret
that the task role cannot read:

| Service | Compute module input | Expected secret names |
| --- | --- | --- |
| Core API | `core_secret_arns` | `taskmind/<environment>/core/jwt`, `taskmind/<environment>/core/database`, `taskmind/<environment>/core/redis`, `taskmind/<environment>/core/s3`, and any Core-only OAuth, SMTP, or webhook secrets. |
| Relay | `relay_secret_arns` | `taskmind/<environment>/relay/database`, `taskmind/<environment>/relay/redis`, `taskmind/<environment>/relay/opensearch`, and Relay-only analytics integration secrets. |
| Nova AI | `nova_secret_arns` | `taskmind/<environment>/nova/database`, `taskmind/<environment>/nova/redis`, `taskmind/<environment>/nova/opensearch`, and LLM provider credentials such as `taskmind/<environment>/nova/openai`. |

Use the full Secrets Manager or SSM Parameter ARN in both the appropriate
`<service>_secret_arns` list and the corresponding `service_secrets["<service>"].valueFrom`
entry. If a secret is shared intentionally, list its ARN in each service-specific input
for every task that injects it.
