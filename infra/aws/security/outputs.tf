output "service_security_group_ids" {
  description = "ECS task security groups keyed by service name."
  value = {
    core  = aws_security_group.core.id
    relay = aws_security_group.relay.id
    nova  = aws_security_group.nova.id
  }
}

output "core_security_group_id" {
  value = aws_security_group.core.id
}

output "relay_security_group_id" {
  value = aws_security_group.relay.id
}

output "nova_security_group_id" {
  value = aws_security_group.nova.id
}

output "rds_security_group_id" {
  value = aws_security_group.rds.id
}

output "redis_security_group_id" {
  value = aws_security_group.redis.id
}

output "opensearch_security_group_id" {
  value = aws_security_group.opensearch.id
}
