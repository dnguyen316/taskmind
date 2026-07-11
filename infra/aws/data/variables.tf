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

variable "skip_final_snapshot" {
  description = "Whether to skip creating a final RDS snapshot when the database instance is deleted. Defaults to false so destructive changes preserve a final recovery point."
  type        = bool
  default     = false
}

variable "final_snapshot_identifier_prefix" {
  description = "Prefix used when naming the final RDS snapshot. The environment is appended automatically."
  type        = string
  default     = "taskmind-final-snapshot"

  validation {
    condition     = can(regex("^[a-zA-Z][a-zA-Z0-9-]{0,190}$", var.final_snapshot_identifier_prefix)) && !endswith(var.final_snapshot_identifier_prefix, "-")
    error_message = "final_snapshot_identifier_prefix must start with a letter, contain only letters, numbers, or hyphens, must not end with a hyphen, and must leave room for the environment suffix."
  }
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

variable "core_task_role_arns" {
  description = "ECS task role ARNs allowed to search the activity OpenSearch domain."
  type        = list(string)
}

variable "relay_task_role_arns" {
  description = "ECS task role ARNs allowed to ingest, update, and delete activity OpenSearch documents."
  type        = list(string)
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
