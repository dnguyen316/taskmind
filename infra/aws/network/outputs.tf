output "vpc_id" {
  value = aws_vpc.this.id
}
output "vpc_cidr" {
  value = aws_vpc.this.cidr_block
}
output "public_subnet_ids" {
  value = aws_subnet.public[*].id
}
output "private_subnet_ids" {
  value = aws_subnet.private[*].id
}

output "public_subnet_cidrs" {
  description = "CIDR ranges allocated to public subnets, ordered by availability zone."
  value       = local.public_subnets
}

output "private_subnet_cidrs" {
  description = "CIDR ranges allocated to private subnets, ordered by availability zone."
  value       = local.private_subnets
}
output "vpc_endpoint_security_group_id" {
  value = aws_security_group.vpc_endpoints.id
}

output "s3_prefix_list_id" {
  description = "AWS-managed S3 prefix list associated with the gateway endpoint."
  value       = aws_vpc_endpoint.s3.prefix_list_id
}
