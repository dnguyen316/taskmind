variable "environment" {
  type = string
}

variable "account_suffix" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "private_subnet_ids" {
  type = list(string)
}

variable "ecs_security_group_ids" {
  type    = list(string)
  default = []
}

variable "db_username" {
  type    = string
  default = "taskmind"
}

variable "rds_instance_class" {
  type    = string
  default = "db.t4g.medium"
}

variable "rds_multi_az" {
  type    = bool
  default = true
}

variable "backup_retention_days" {
  type    = number
  default = 7
}

variable "deletion_protection" {
  type    = bool
  default = true
}

variable "redis_node_type" {
  type    = string
  default = "cache.t4g.small"
}

variable "redis_replicas" {
  type    = number
  default = 2
}

variable "opensearch_instance_type" {
  type    = string
  default = "t3.small.search"
}

variable "opensearch_instance_count" {
  type    = number
  default = 2
}

variable "opensearch_volume_size" {
  type    = number
  default = 20
}

variable "cloudfront_distribution_arn" {
  description = "CloudFront distribution ARN allowed to read frontend artifacts through OAC. Leave null until an edge module/root wires the distribution."
  type        = string
  default     = null
}

variable "tags" {
  type    = map(string)
  default = {}
}
