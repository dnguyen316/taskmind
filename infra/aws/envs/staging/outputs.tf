output "alb_dns_name" {
  value = module.edge.alb_dns_name
}

output "cloudfront_distribution_id" {
  value = module.edge.cloudfront_distribution_id
}

output "cloudfront_domain_name" {
  value = module.edge.cloudfront_domain_name
}

output "ecs_service_names" {
  value = module.compute.service_names
}

output "attachments_bucket_name" {
  value = module.data.attachments_bucket_name
}

output "frontend_bucket_name" {
  value = module.data.frontend_bucket_name
}

output "dashboard_name" {
  value = module.observability.dashboard_name
}

output "rds_master_user_secret_arn" {
  value     = module.data.rds_master_user_secret_arn
  sensitive = true
}
