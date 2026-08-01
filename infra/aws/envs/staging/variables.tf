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

variable "final_snapshot_identifier_suffix" {
  description = "Unique identifier for the current RDS replacement generation. Change this before intentionally replacing the database."
  type        = string
}

variable "vpc_cidr" {
  description = "IPv4 CIDR for the VPC; must be large enough for eight equal AWS-valid subnet ranges."
  type        = string
  default     = "10.40.0.0/16"

  validation {
    condition     = can(cidrsubnet(var.vpc_cidr, 4, 7)) && try(tonumber(split("/", var.vpc_cidr)[1]) >= 16 && tonumber(split("/", var.vpc_cidr)[1]) <= 24, false)
    error_message = "vpc_cidr must be a valid AWS IPv4 VPC CIDR from /16 through /24."
  }
}

variable "az_count" {
  description = "Number of availability zones; the network module generates four ranges per subnet tier."
  type        = number
  default     = 2

  validation {
    condition     = var.az_count >= 1 && var.az_count <= 4 && floor(var.az_count) == var.az_count
    error_message = "az_count must be a whole number between 1 and 4."
  }
}

variable "single_nat_gateway" {
  type    = bool
  default = true
}

variable "enable_alb_deletion_protection" {
  description = "Whether staging protects the Core ALB from deletion. Disable to reduce teardown friction for an intentionally disposable staging stack."
  type        = bool
  default     = false
}

variable "alb_access_log_retention_days" {
  description = "Number of days to retain staging Core ALB access logs."
  type        = number
  default     = 30

  validation {
    condition     = var.alb_access_log_retention_days >= 1 && floor(var.alb_access_log_retention_days) == var.alb_access_log_retention_days
    error_message = "alb_access_log_retention_days must be a positive whole number."
  }
}

variable "alb_certificate_arn" {
  description = "Regional ACM certificate ARN in ap-southeast-2 for the Core ALB HTTPS listener."
  type        = string
}

variable "cloudfront_certificate_arn" {
  description = "Optional ACM certificate ARN for CloudFront aliases. CloudFront viewer certificates are the us-east-1 ACM exception and do not change the main TaskMind deployment region."
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


variable "redis_auth_token" {
  description = "ElastiCache Redis AUTH token. Must match the TASKMIND_REDIS_PASSWORD secret injected into Relay and Nova."
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.redis_auth_token) >= 16 && length(var.redis_auth_token) <= 128
    error_message = "redis_auth_token must be 16 to 128 characters to satisfy ElastiCache AUTH token requirements."
  }
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
