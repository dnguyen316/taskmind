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
  description = "Shared prefix used when naming final RDS snapshots. The environment and operator-supplied replacement suffix are appended automatically. Do not include the environment in this value."
  type        = string
  default     = "taskmind-final-snapshot"

  validation {
    condition     = can(regex("^[a-zA-Z][a-zA-Z0-9-]{0,159}$", var.final_snapshot_identifier_prefix)) && !strcontains(var.final_snapshot_identifier_prefix, "--") && !endswith(var.final_snapshot_identifier_prefix, "-")
    error_message = "final_snapshot_identifier_prefix must be 1-160 characters, start with a letter, contain only letters, numbers, or single hyphens, and must not end with a hyphen. The environment is appended automatically."
  }
}

variable "final_snapshot_identifier_suffix" {
  description = "Operator-supplied unique suffix for this RDS replacement generation (for example change-20260801-01). Change it before applying any intentional database replacement so a previously created final snapshot identifier is never reused."
  type        = string

  validation {
    condition     = can(regex("^[a-zA-Z0-9][a-zA-Z0-9-]{0,62}$", var.final_snapshot_identifier_suffix)) && !strcontains(var.final_snapshot_identifier_suffix, "--") && !endswith(var.final_snapshot_identifier_suffix, "-")
    error_message = "final_snapshot_identifier_suffix must be 1-63 characters, contain only letters, numbers, or single hyphens, and must not end with a hyphen."
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
  description = "Number of data nodes in the OpenSearch domain."
  type        = number
  default     = 2
}

variable "opensearch_availability_zone_count" {
  description = "Number of Availability Zones used by the OpenSearch domain. AWS supports one, two, or three zones."
  type        = number
  default     = 2

  validation {
    condition     = contains([1, 2, 3], var.opensearch_availability_zone_count)
    error_message = "opensearch_availability_zone_count must be one of the AWS-supported values: 1, 2, or 3."
  }
}

variable "opensearch_volume_size" {
  type    = number
  default = 20
}


variable "tags" {
  type    = map(string)
  default = {}
}
