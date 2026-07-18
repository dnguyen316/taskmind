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

variable "rds_security_group_ids" {
  description = "Security group IDs to attach to the RDS instance."
  type        = list(string)
}

variable "redis_security_group_ids" {
  description = "Security group IDs to attach to the ElastiCache replication group."
  type        = list(string)
}

variable "opensearch_security_group_ids" {
  description = "Security group IDs to attach to the OpenSearch domain."
  type        = list(string)
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
  description = "Prefix used when naming the final RDS snapshot. The environment is appended automatically for a deterministic per-environment snapshot identifier."
  type        = string
  default     = "taskmind-final-snapshot"

  validation {
    condition     = can(regex("^[a-zA-Z][a-zA-Z0-9-]{0,190}$", var.final_snapshot_identifier_prefix)) && !endswith(var.final_snapshot_identifier_prefix, "-")
    error_message = "final_snapshot_identifier_prefix must start with a letter, contain only letters, numbers, or hyphens, must not end with a hyphen, and must leave room for the environment suffix."
  }
}


variable "redis_auth_token" {
  description = "Authentication token used by ElastiCache Redis when in-transit encryption is enabled. Store this value in Secrets Manager/SSM and inject the same value into Relay and Nova as TASKMIND_REDIS_PASSWORD."
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.redis_auth_token) >= 16 && length(var.redis_auth_token) <= 128
    error_message = "redis_auth_token must be 16 to 128 characters to satisfy ElastiCache AUTH token requirements."
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


variable "tags" {
  type    = map(string)
  default = {}
}
