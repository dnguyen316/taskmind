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
  data/           # RDS PostgreSQL 16, ElastiCache Redis 7, OpenSearch, S3 buckets
  compute/        # ECS cluster, task definitions/services, task roles/policies
  edge/           # ALB/WAF for Core, CloudFront for the SPA, ACM/Route53 hooks
  observability/  # CloudWatch logs/alarms/dashboards and X-Ray sampling
```

## Deployment model

Core is the only public API and is exposed through ALB + WAF. Relay and Nova run as
private ECS Fargate services and are reachable only from inside the VPC via Cloud Map.
The Vue SPA is built separately, published to the frontend artifact bucket, and served by
CloudFront using origin access control. The CloudFront distribution maps SPA `403` and
`404` responses to `/index.html` so client-side routing works on deep links.

## Example composition

A future environment root should instantiate the modules in this order:

1. `network`
2. `data`
3. `compute`
4. `edge`
5. `observability`

Pass outputs between modules explicitly, keep all secrets in AWS Secrets Manager or SSM
Parameter Store, and never commit generated state files or plaintext secrets.
