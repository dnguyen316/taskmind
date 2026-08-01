variable "environment" {
  type = string
}
variable "aws_region" {
  type = string
}
variable "vpc_id" {
  type = string
}
variable "private_subnet_ids" {
  type = list(string)
}
variable "service_security_group_ids" {
  description = "ECS task security groups keyed by core, relay, and nova."
  type        = map(string)

  validation {
    condition     = toset(keys(var.service_security_group_ids)) == toset(["core", "relay", "nova"])
    error_message = "service_security_group_ids must contain exactly core, relay, and nova."
  }
}
variable "core_target_group_arn" {
  type = string
}
variable "alb_listener_ready_arn" {
  description = "ALB listener readiness token consumed by the Core ECS service to enforce listener-before-service ordering."
  type        = string
}
variable "attachments_bucket_arn" {
  type = string
}
variable "opensearch_domain_arn" {
  type = string
}
variable "core_secret_arns" {
  type        = list(string)
  description = "Secrets Manager secret ARNs and SSM parameter ARNs readable by the Core task role."

  validation {
    condition = alltrue([for arn in var.core_secret_arns :
      trimspace(arn) == arn
      && arn != ""
      && !strcontains(arn, "*")
      && can(regex("^arn:(aws|aws-us-gov|aws-cn):secretsmanager:[a-z0-9-]+:[0-9]{12}:secret:[^\\s*]+$|^arn:(aws|aws-us-gov|aws-cn):ssm:[a-z0-9-]+:[0-9]{12}:parameter/[^\\s*]+$", arn))
    ])
    error_message = "core_secret_arns must contain only full Secrets Manager secret ARNs or SSM parameter ARNs; empty strings, '*', wildcard-only values, and malformed ARNs are not allowed."
  }
}
variable "relay_secret_arns" {
  type        = list(string)
  description = "Secrets Manager secret ARNs and SSM parameter ARNs readable by the Relay task role."

  validation {
    condition = alltrue([for arn in var.relay_secret_arns :
      trimspace(arn) == arn
      && arn != ""
      && !strcontains(arn, "*")
      && can(regex("^arn:(aws|aws-us-gov|aws-cn):secretsmanager:[a-z0-9-]+:[0-9]{12}:secret:[^\\s*]+$|^arn:(aws|aws-us-gov|aws-cn):ssm:[a-z0-9-]+:[0-9]{12}:parameter/[^\\s*]+$", arn))
    ])
    error_message = "relay_secret_arns must contain only full Secrets Manager secret ARNs or SSM parameter ARNs; empty strings, '*', wildcard-only values, and malformed ARNs are not allowed."
  }
}
variable "nova_secret_arns" {
  type        = list(string)
  description = "Secrets Manager secret ARNs and SSM parameter ARNs readable by the Nova task role."

  validation {
    condition = alltrue([for arn in var.nova_secret_arns :
      trimspace(arn) == arn
      && arn != ""
      && !strcontains(arn, "*")
      && can(regex("^arn:(aws|aws-us-gov|aws-cn):secretsmanager:[a-z0-9-]+:[0-9]{12}:secret:[^\\s*]+$|^arn:(aws|aws-us-gov|aws-cn):ssm:[a-z0-9-]+:[0-9]{12}:parameter/[^\\s*]+$", arn))
    ])
    error_message = "nova_secret_arns must contain only full Secrets Manager secret ARNs or SSM parameter ARNs; empty strings, '*', wildcard-only values, and malformed ARNs are not allowed."
  }
}
variable "service_images" {
  type = map(string)
}
variable "service_environment" {
  type    = map(list(object({ name = string, value = string })))
  default = {}
}
variable "service_secrets" {
  type    = map(list(object({ name = string, valueFrom = string })))
  default = {}
}
variable "core_desired_count" {
  type    = number
  default = 2
}
variable "relay_desired_count" {
  type    = number
  default = 1
}
variable "nova_desired_count" {
  type    = number
  default = 1
}
variable "log_retention_days" {
  type    = number
  default = 30
}
variable "cloudwatch_logs_kms_key_id" {
  description = "Optional KMS key ARN or ID used to encrypt ECS service CloudWatch log groups. When set, the key policy must allow CloudWatch Logs in the target AWS region (logs.<region>.amazonaws.com) to use the key for the TaskMind log group ARNs."
  type        = string
  default     = null
}
variable "enable_execute_command" {
  type    = bool
  default = false
}
variable "container_health_check_interval_seconds" {
  description = "Seconds between ECS container health checks."
  type        = number
  default     = 30

  validation {
    condition     = var.container_health_check_interval_seconds >= 5 && var.container_health_check_interval_seconds <= 300
    error_message = "container_health_check_interval_seconds must be between 5 and 300 seconds, inclusive."
  }
}
variable "container_health_check_timeout_seconds" {
  description = "Seconds an ECS container health check may run before it fails."
  type        = number
  default     = 5

  validation {
    condition     = var.container_health_check_timeout_seconds >= 2 && var.container_health_check_timeout_seconds <= 60
    error_message = "container_health_check_timeout_seconds must be between 2 and 60 seconds, inclusive."
  }
}
variable "container_health_check_retries" {
  description = "Consecutive failed container health checks required before ECS marks a task unhealthy."
  type        = number
  default     = 3

  validation {
    condition     = var.container_health_check_retries >= 1 && var.container_health_check_retries <= 10
    error_message = "container_health_check_retries must be between 1 and 10, inclusive."
  }
}
variable "container_health_check_start_period_seconds" {
  description = "Startup period during which failed ECS container health checks are ignored."
  type        = number
  default     = 60

  validation {
    condition     = var.container_health_check_start_period_seconds >= 0 && var.container_health_check_start_period_seconds <= 300
    error_message = "container_health_check_start_period_seconds must be between 0 and 300 seconds, inclusive."
  }
}
variable "core_health_check_grace_period_seconds" {
  description = "Grace period for Core to start before ECS evaluates ALB health checks."
  type        = number
  default     = 120

  validation {
    condition     = var.core_health_check_grace_period_seconds >= 0
    error_message = "core_health_check_grace_period_seconds must be zero or greater."
  }
}
variable "deployment_minimum_healthy_percent" {
  description = "Minimum healthy task percentage during deployment; 100 keeps desired-count-one services available."
  type        = number
  default     = 100

  validation {
    condition     = var.deployment_minimum_healthy_percent >= 0 && var.deployment_minimum_healthy_percent <= 100
    error_message = "deployment_minimum_healthy_percent must be between 0 and 100, inclusive."
  }
}
variable "deployment_maximum_percent" {
  description = "Maximum running task percentage during deployment; 200 permits a replacement alongside a desired-count-one task."
  type        = number
  default     = 200

  validation {
    condition     = var.deployment_maximum_percent >= 100 && var.deployment_maximum_percent <= 200
    error_message = "deployment_maximum_percent must be between 100 and 200, inclusive."
  }
}
variable "tags" {
  type    = map(string)
  default = {}
}
