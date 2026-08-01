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
  single_nat_gateway = false
  tags               = local.tags
}

module "security" {
  source = "../../security"

  environment                    = var.environment
  vpc_id                         = module.network.vpc_id
  vpc_cidr                       = module.network.vpc_cidr
  vpc_endpoint_security_group_id = module.network.vpc_endpoint_security_group_id
  s3_prefix_list_id              = module.network.s3_prefix_list_id
  tags                           = local.tags
}

module "data" {
  source = "../../data"

  environment                        = var.environment
  account_suffix                     = var.account_suffix
  vpc_id                             = module.network.vpc_id
  private_subnet_ids                 = module.network.private_subnet_ids
  rds_security_group_ids             = [module.security.rds_security_group_id]
  redis_security_group_ids           = [module.security.redis_security_group_id]
  opensearch_security_group_ids      = [module.security.opensearch_security_group_id]
  opensearch_availability_zone_count = 2
  redis_auth_token                   = var.redis_auth_token
  deletion_protection                = true
  skip_final_snapshot                = false
  final_snapshot_identifier_prefix   = "taskmind-final-snapshot"
  final_snapshot_identifier_suffix   = var.final_snapshot_identifier_suffix
  tags                               = local.tags
}

data "aws_caller_identity" "current" {}

resource "aws_s3_bucket" "alb_access_logs" {
  bucket = "taskmind-${var.environment}-alb-logs-${var.account_suffix}"

  lifecycle {
    prevent_destroy = true
  }

  tags = merge(local.tags, { Purpose = "alb-access-logs" })
}

resource "aws_s3_bucket_server_side_encryption_configuration" "alb_access_logs" {
  bucket = aws_s3_bucket.alb_access_logs.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "alb_access_logs" {
  bucket = aws_s3_bucket.alb_access_logs.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_lifecycle_configuration" "alb_access_logs" {
  bucket = aws_s3_bucket.alb_access_logs.id

  rule {
    id     = "expire-alb-access-logs"
    status = "Enabled"

    filter { prefix = "core/" }

    expiration { days = 365 }

    abort_incomplete_multipart_upload { days_after_initiation = 7 }
  }
}

data "aws_iam_policy_document" "alb_access_logs" {
  statement {
    sid     = "AllowAlbLogDelivery"
    effect  = "Allow"
    actions = ["s3:PutObject"]

    principals {
      type        = "Service"
      identifiers = ["logdelivery.elasticloadbalancing.amazonaws.com"]
    }

    resources = ["${aws_s3_bucket.alb_access_logs.arn}/core/AWSLogs/${data.aws_caller_identity.current.account_id}/*"]

    condition {
      test     = "StringEquals"
      variable = "aws:SourceAccount"
      values   = [data.aws_caller_identity.current.account_id]
    }

    condition {
      test     = "ArnLike"
      variable = "aws:SourceArn"
      values   = ["arn:aws:elasticloadbalancing:${var.aws_region}:${data.aws_caller_identity.current.account_id}:loadbalancer/*"]
    }
  }
}

resource "aws_s3_bucket_policy" "alb_access_logs" {
  bucket = aws_s3_bucket.alb_access_logs.id
  policy = data.aws_iam_policy_document.alb_access_logs.json
}

module "edge" {
  source = "../../edge"

  depends_on = [aws_s3_bucket_policy.alb_access_logs]

  environment                          = var.environment
  vpc_id                               = module.network.vpc_id
  public_subnet_ids                    = module.network.public_subnet_ids
  frontend_bucket_regional_domain_name = module.data.frontend_bucket_regional_domain_name
  alb_certificate_arn                  = var.alb_certificate_arn
  cloudfront_certificate_arn           = var.cloudfront_certificate_arn
  frontend_aliases                     = var.frontend_aliases
  enable_deletion_protection           = true
  access_logs_bucket                   = aws_s3_bucket.alb_access_logs.id
  access_logs_prefix                   = "core"
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



resource "aws_vpc_security_group_ingress_rule" "alb_to_core" {
  description                  = "ALB to Core"
  security_group_id            = module.security.core_security_group_id
  referenced_security_group_id = module.edge.alb_security_group_id
  from_port                    = 8080
  to_port                      = 8080
  ip_protocol                  = "tcp"
}

module "compute" {
  source = "../../compute"

  environment                = var.environment
  aws_region                 = var.aws_region
  vpc_id                     = module.network.vpc_id
  private_subnet_ids         = module.network.private_subnet_ids
  service_security_group_ids = module.security.service_security_group_ids
  core_target_group_arn      = module.edge.core_target_group_arn
  alb_listener_ready_arn     = module.edge.alb_listener_ready_arn
  attachments_bucket_arn     = module.data.attachments_bucket_arn
  opensearch_domain_arn      = module.data.opensearch_domain_arn
  core_secret_arns           = var.core_secret_arns
  relay_secret_arns          = var.relay_secret_arns
  nova_secret_arns           = var.nova_secret_arns
  service_images             = var.service_images
  service_environment        = var.service_environment
  service_secrets            = var.service_secrets
  core_desired_count         = 2
  relay_desired_count        = 1
  nova_desired_count         = 1
  log_retention_days         = 90
  cloudwatch_logs_kms_key_id = var.cloudwatch_logs_kms_key_id
  enable_execute_command     = false
  tags                       = local.tags
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
  trace_sample_rate          = 0.05
  tags                       = local.tags
}
