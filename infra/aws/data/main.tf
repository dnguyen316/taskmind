terraform {
  required_version = ">= 1.6.0"
  required_providers { aws = { source = "hashicorp/aws" version = "~> 5.0" } }
}

locals {
  name = "taskmind-${var.environment}"
  tags = merge(var.tags, { Project = "taskmind", Environment = var.environment, ManagedBy = "opentofu" })
}


resource "aws_db_subnet_group" "this" { name = "${local.name}-db" subnet_ids = var.private_subnet_ids tags = local.tags }
resource "aws_db_instance" "postgres" {
  identifier = "${local.name}-postgres"
  engine = "postgres"
  engine_version = "16"
  instance_class = var.rds_instance_class
  allocated_storage = 50
  max_allocated_storage = 500
  storage_type = "gp3"
  db_name = "taskmind"
  username = var.db_username
  manage_master_user_password = true
  port = 5432
  multi_az = var.rds_multi_az
  backup_retention_period = var.backup_retention_days
  deletion_protection       = var.deletion_protection
  skip_final_snapshot       = var.skip_final_snapshot
  final_snapshot_identifier = var.skip_final_snapshot ? null : "${var.final_snapshot_identifier_prefix}-${var.environment}"
  db_subnet_group_name = aws_db_subnet_group.this.name
  vpc_security_group_ids = var.rds_security_group_ids
  performance_insights_enabled = true
  auto_minor_version_upgrade = true
  storage_encrypted = true
  tags = local.tags

  lifecycle {
    precondition {
      condition     = !contains(["prod", "production"], lower(var.environment)) || var.deletion_protection || !var.skip_final_snapshot
      error_message = "Production RDS instances must keep deletion protection enabled or require a final snapshot on deletion."
    }

    precondition {
      condition     = var.skip_final_snapshot || length(trimspace(var.final_snapshot_identifier_prefix)) > 0
      error_message = "final_snapshot_identifier_prefix must be non-empty when skip_final_snapshot is false."
    }
  }
}

resource "aws_elasticache_subnet_group" "this" { name = "${local.name}-redis" subnet_ids = var.private_subnet_ids tags = local.tags }
resource "aws_elasticache_replication_group" "redis" {
  replication_group_id = "${local.name}-redis"
  description = "TaskMind Redis 7 for streams, cache, rate limits, and sessions"
  engine = "redis"
  engine_version = "7.1"
  node_type = var.redis_node_type
  num_cache_clusters = var.redis_replicas
  automatic_failover_enabled = var.redis_replicas > 1
  multi_az_enabled = var.redis_replicas > 1
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  subnet_group_name = aws_elasticache_subnet_group.this.name
  security_group_ids = var.redis_security_group_ids
  tags = local.tags
}

resource "aws_opensearch_domain" "activity" {
  domain_name = "${local.name}-activity"
  engine_version = "OpenSearch_2.13"
  cluster_config { instance_type = var.opensearch_instance_type instance_count = var.opensearch_instance_count zone_awareness_enabled = var.opensearch_instance_count > 1 dynamic "zone_awareness_config" { for_each = var.opensearch_instance_count > 1 ? [1] : [] content { availability_zone_count = 2 } } }
  ebs_options { ebs_enabled = true volume_type = "gp3" volume_size = var.opensearch_volume_size }
  vpc_options { subnet_ids = slice(var.private_subnet_ids, 0, min(length(var.private_subnet_ids), var.opensearch_instance_count)) security_group_ids = var.opensearch_security_group_ids }
  encrypt_at_rest { enabled = true }
  node_to_node_encryption { enabled = true }
  domain_endpoint_options { enforce_https = true tls_security_policy = "Policy-Min-TLS-1-2-2019-07" }
  tags = local.tags
}

resource "aws_s3_bucket" "attachments" {
  bucket = "taskmind-attachments-${var.environment}-${var.account_suffix}"
  tags   = local.tags
}

resource "aws_s3_bucket_versioning" "attachments" {
  bucket = aws_s3_bucket.attachments.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "attachments" {
  bucket = aws_s3_bucket.attachments.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "attachments" {
  bucket                  = aws_s3_bucket.attachments.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_lifecycle_configuration" "attachments" {
  bucket = aws_s3_bucket.attachments.id

  rule {
    id     = "expire-old-versions"
    status = "Enabled"

    noncurrent_version_expiration {
      noncurrent_days = 90
    }
  }
}

resource "aws_s3_bucket" "frontend" {
  bucket = "taskmind-frontend-${var.environment}-${var.account_suffix}"
  tags   = local.tags
}

resource "aws_s3_bucket_versioning" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket                  = aws_s3_bucket.frontend.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
