variable "environment" {
  type = string
}
variable "vpc_id" {
  type = string
}
variable "public_subnet_ids" {
  type = list(string)
}
variable "frontend_bucket_regional_domain_name" {
  type = string
}
variable "alb_certificate_arn" {
  type    = string
  default = null

  validation {
    condition     = var.alb_certificate_arn == null || can(regex("^arn:aws:acm:", var.alb_certificate_arn))
    error_message = "alb_certificate_arn must be an AWS ACM certificate ARN when provided."
  }
}
variable "cloudfront_certificate_arn" {
  type    = string
  default = null
}
variable "frontend_aliases" {
  type    = list(string)
  default = []
}
variable "waf_rate_limit" {
  type    = number
  default = 2000
}
variable "enable_deletion_protection" {
  description = "Prevents the Core ALB from being deleted through the AWS API. Disable only as part of an intentional teardown."
  type        = bool
  default     = true
}
variable "access_logs_bucket" {
  description = "Name of the same-region S3 bucket that receives Core ALB access logs. Null disables access logging."
  type        = string
  default     = null

  validation {
    condition     = var.access_logs_bucket == null || trimspace(var.access_logs_bucket) != ""
    error_message = "access_logs_bucket must be null or a non-empty S3 bucket name."
  }
}
variable "access_logs_prefix" {
  description = "Optional object-key prefix for Core ALB access logs."
  type        = string
  default     = null

  validation {
    condition     = var.access_logs_prefix == null || (trim(var.access_logs_prefix, "/") != "" && !contains(split("/", var.access_logs_prefix), "AWSLogs"))
    error_message = "access_logs_prefix must be null or a non-empty prefix that does not contain the reserved AWSLogs segment."
  }
}
variable "tags" {
  type    = map(string)
  default = {}
}
