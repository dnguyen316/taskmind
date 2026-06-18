output "cluster_arn" {
  value = aws_ecs_cluster.this.arn
}
output "ecs_security_group_id" {
  value = aws_security_group.ecs.id
}
output "service_names" {
  value = { for k, v in aws_ecs_service.service : k => v.name
} }
output "cloud_map_namespace" {
  value = aws_service_discovery_private_dns_namespace.this.name
}
