output "rds_endpoint" {
  value = aws_db_instance.postgres.address
}
output "redis_primary_endpoint" {
  value = aws_elasticache_replication_group.redis.primary_endpoint_address
}
output "opensearch_endpoint" {
  value = aws_opensearch_domain.activity.endpoint
}
output "attachments_bucket_name" {
  value = aws_s3_bucket.attachments.bucket
}
output "frontend_bucket_name" {
  value = aws_s3_bucket.frontend.bucket
}
output "frontend_bucket_regional_domain_name" {
  value = aws_s3_bucket.frontend.bucket_regional_domain_name
}
