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

module "data" {
  source = "../../data"

  environment                      = var.environment
  account_suffix                   = var.account_suffix
  vpc_id                           = module.network.vpc_id
  private_subnet_ids               = module.network.private_subnet_ids
  ecs_security_group_ids           = [module.compute.ecs_security_group_id]
  core_task_role_arns              = [module.compute.core_task_role_arn]
  relay_task_role_arns             = [module.compute.relay_task_role_arn]
  cloudfront_distribution_arn      = module.edge.cloudfront_distribution_arn
  deletion_protection              = true
  skip_final_snapshot              = false
  final_snapshot_identifier_prefix = "taskmind-production-final-snapshot"
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

module "compute" {
  source = "../../compute"

  environment            = var.environment
  aws_region             = var.aws_region
  vpc_id                 = module.network.vpc_id
  private_subnet_ids     = module.network.private_subnet_ids
  alb_security_group_id  = module.edge.alb_security_group_id
  core_target_group_arn  = module.edge.core_target_group_arn
  attachments_bucket_arn = module.data.attachments_bucket_arn
  opensearch_domain_arn  = module.data.opensearch_domain_arn
  core_secret_arns       = var.core_secret_arns
  relay_secret_arns      = var.relay_secret_arns
  nova_secret_arns       = var.nova_secret_arns
  service_images         = var.service_images
  service_environment    = var.service_environment
  service_secrets        = var.service_secrets
  core_desired_count     = 2
  relay_desired_count    = 1
  nova_desired_count     = 1
  log_retention_days     = 90
  enable_execute_command = false
  tags                   = local.tags
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
