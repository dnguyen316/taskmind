locals {
  tags = merge(var.tags, {
    Project     = "taskmind"
    Environment = var.environment
    ManagedBy   = "opentofu"
  })
}

module "network" {
  source = "../../network"

  environment        = var.environment
  aws_region         = var.aws_region
  vpc_cidr           = var.vpc_cidr
  az_count           = var.az_count
  single_nat_gateway = var.single_nat_gateway
  tags               = local.tags
}

module "security" {
  source = "../../security"

  environment = var.environment
  vpc_id      = module.network.vpc_id
  tags        = local.tags
}

module "data" {
  source = "../../data"

  environment                      = var.environment
  account_suffix                   = var.account_suffix
  vpc_id                           = module.network.vpc_id
  private_subnet_ids               = module.network.private_subnet_ids
  rds_security_group_ids           = [module.security.rds_security_group_id]
  redis_security_group_ids         = [module.security.redis_security_group_id]
  opensearch_security_group_ids    = [module.security.opensearch_security_group_id]
  deletion_protection              = true
  skip_final_snapshot              = false
  final_snapshot_identifier_prefix = "taskmind-staging-final-snapshot"
  tags                             = local.tags
}

module "edge" {
  source = "../../edge"

  environment                          = var.environment
  vpc_id                               = module.network.vpc_id
  public_subnet_ids                    = module.network.public_subnet_ids
  frontend_bucket_regional_domain_name = module.data.frontend_bucket_regional_domain_name
  alb_certificate_arn                  = var.alb_certificate_arn
  cloudfront_certificate_arn           = var.cloudfront_certificate_arn
  frontend_aliases                     = var.frontend_aliases
  tags                                 = local.tags
}

resource "aws_s3_bucket_policy" "frontend" {
  bucket = module.data.frontend_bucket_name

  depends_on = [
    module.data,
    module.edge,
  ]

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowCloudFrontReadFrontendArtifacts"
        Effect = "Allow"
        Principal = {
          Service = "cloudfront.amazonaws.com"
        }
        Action   = "s3:GetObject"
        Resource = "${module.data.frontend_bucket_arn}/*"
        Condition = {
          StringEquals = {
            "AWS:SourceArn" = module.edge.cloudfront_distribution_arn
          }
        }
      }
    ]
  })
}


resource "aws_security_group_rule" "alb_to_ecs_core" {
  type                     = "ingress"
  description              = "Core from ALB"
  from_port                = 8080
  to_port                  = 8080
  protocol                 = "tcp"
  security_group_id        = module.security.ecs_security_group_id
  source_security_group_id = module.edge.alb_security_group_id
}

module "compute" {
  source = "../../compute"

  environment            = var.environment
  aws_region             = var.aws_region
  vpc_id                 = module.network.vpc_id
  private_subnet_ids     = module.network.private_subnet_ids
  ecs_security_group_id  = module.security.ecs_security_group_id
  core_target_group_arn  = module.edge.core_target_group_arn
  attachments_bucket_arn = module.data.attachments_bucket_arn
  opensearch_domain_arn  = module.data.opensearch_domain_arn
  core_secret_arns       = var.core_secret_arns
  relay_secret_arns      = var.relay_secret_arns
  nova_secret_arns       = var.nova_secret_arns
  service_images         = var.service_images
  service_environment    = var.service_environment
  service_secrets        = var.service_secrets
  core_desired_count     = 1
  relay_desired_count    = 1
  nova_desired_count     = 1
  log_retention_days     = 30
  enable_execute_command = true
  tags                   = local.tags
}

data "aws_iam_policy_document" "opensearch_activity" {
  statement {
    sid     = "CoreActivitySearchRead"
    effect  = "Allow"
    actions = ["es:ESHttpGet", "es:ESHttpPost"]

    principals {
      type        = "AWS"
      identifiers = [module.compute.core_task_role_arn]
    }

    resources = ["${module.data.opensearch_domain_arn}/*"]
  }

  statement {
    sid    = "RelayActivityIndexWrite"
    effect = "Allow"
    actions = [
      "es:ESHttpDelete",
      "es:ESHttpGet",
      "es:ESHttpPost",
      "es:ESHttpPut",
    ]

    principals {
      type        = "AWS"
      identifiers = [module.compute.relay_task_role_arn]
    }

    resources = ["${module.data.opensearch_domain_arn}/*"]
  }
}

resource "aws_opensearch_domain_policy" "activity" {
  domain_name     = module.data.opensearch_domain_name
  access_policies = data.aws_iam_policy_document.opensearch_activity.json
}

module "observability" {
  source = "../../observability"

  environment                = var.environment
  aws_region                 = var.aws_region
  ecs_cluster_name           = module.compute.cluster_name
  alb_arn_suffix             = module.edge.alb_arn_suffix
  rds_identifier             = module.data.rds_identifier
  redis_replication_group_id = module.data.redis_replication_group_id
  opensearch_domain_name     = module.data.opensearch_domain_name
  alarm_topic_arns           = var.alarm_topic_arns
  trace_sample_rate          = 0.10
  tags                       = local.tags
}
