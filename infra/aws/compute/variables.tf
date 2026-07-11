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
variable "secret_arns" {
  type = list(string)
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
  description = "Optional KMS key ARN or ID used to encrypt ECS service CloudWatch log groups."
  type        = string
  default     = null
}
variable "enable_execute_command" {
  type    = bool
  default = false
}
variable "tags" { type = map(string) default = {} }
