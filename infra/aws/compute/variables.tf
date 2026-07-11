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
variable "alb_security_group_id" {
  type = string
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
    condition     = alltrue([for arn in var.core_secret_arns : trimspace(arn) != "" && trimspace(arn) != "*"])
    error_message = "core_secret_arns must not contain empty strings or '*'."
  }
}
variable "relay_secret_arns" {
  type        = list(string)
  description = "Secrets Manager secret ARNs and SSM parameter ARNs readable by the Relay task role."

  validation {
    condition     = alltrue([for arn in var.relay_secret_arns : trimspace(arn) != "" && trimspace(arn) != "*"])
    error_message = "relay_secret_arns must not contain empty strings or '*'."
  }
}
variable "nova_secret_arns" {
  type        = list(string)
  description = "Secrets Manager secret ARNs and SSM parameter ARNs readable by the Nova task role."

  validation {
    condition     = alltrue([for arn in var.nova_secret_arns : trimspace(arn) != "" && trimspace(arn) != "*"])
    error_message = "nova_secret_arns must not contain empty strings or '*'."
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
variable "enable_execute_command" {
  type    = bool
  default = false
}
variable "tags" { type = map(string) default = {} }
