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
variable "ecs_security_group_id" {
  description = "Security group applied to ECS tasks. Created by the shared security module so data can authorize it without depending on compute."
  type        = string
}
variable "core_target_group_arn" {
  type = string
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
variable "tags" { type = map(string) default = {} }
