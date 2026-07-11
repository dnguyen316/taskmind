output "rds_endpoint" {
  value = aws_db_instance.postgres.address
}

output "rds_identifier" {
  value = aws_db_instance.postgres.identifier
}

output "redis_primary_endpoint" {
  value = aws_elasticache_replication_group.redis.primary_endpoint_address
}

output "redis_replication_group_id" {
  value = aws_elasticache_replication_group.redis.replication_group_id
}

output "opensearch_endpoint" {
  value = aws_opensearch_domain.activity.endpoint
}

output "opensearch_domain_arn" {
  value = aws_opensearch_domain.activity.arn
}

output "opensearch_domain_name" {
  value = aws_opensearch_domain.activity.domain_name
}

output "attachments_bucket_name" {
  value = aws_s3_bucket.attachments.bucket
}

output "attachments_bucket_arn" {
  value = aws_s3_bucket.attachments.arn
}

output "frontend_bucket_name" {
  value = aws_s3_bucket.frontend.bucket
}

output "frontend_bucket_arn" {
  value = aws_s3_bucket.frontend.arn
}

output "frontend_bucket_regional_domain_name" {
  value = aws_s3_bucket.frontend.bucket_regional_domain_name
}
