variable "aws_region" {
  description = "AWS region used for regional TaskMind resources. Must match the S3 backend region unless backend configuration is overridden during init."
  type        = string
  default     = "ap-southeast-2"
}

variable "environment" {
  description = "Deployment environment name."
  type        = string
  default     = "staging"

  validation {
    condition     = var.environment == "staging"
    error_message = "The staging root may only be used with environment = staging."
  }
}

variable "account_suffix" {
  description = "Globally unique suffix for S3 bucket names, usually the AWS account ID."
  type        = string
}

variable "vpc_cidr" {
  type    = string
  default = "10.40.0.0/16"
}

variable "az_count" {
  type    = number
  default = 2
}

variable "single_nat_gateway" {
  type    = bool
  default = true
}

variable "alb_certificate_arn" {
  description = "Regional ACM certificate ARN for the Core ALB HTTPS listener."
  type        = string
}

variable "cloudfront_certificate_arn" {
  description = "Optional us-east-1 ACM certificate ARN for CloudFront aliases."
  type        = string
  default     = null
}

variable "frontend_aliases" {
  type    = list(string)
  default = []
}

variable "service_images" {
  description = "Container image URIs keyed by core, relay, and nova."
  type        = map(string)
}

variable "service_environment" {
  type    = map(list(object({ name = string, value = string })))
  default = {}
}

variable "service_secrets" {
  type    = map(list(object({ name = string, valueFrom = string })))
  default = {}
}

variable "core_secret_arns" {
  type = list(string)
}

variable "relay_secret_arns" {
  type = list(string)
}

variable "nova_secret_arns" {
  type = list(string)
}

variable "alarm_topic_arns" {
  type    = list(string)
  default = []
}

variable "tags" {
  type    = map(string)
  default = {}
}
