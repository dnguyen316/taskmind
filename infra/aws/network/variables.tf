variable "environment" {
  type = string
}
variable "aws_region" {
  type = string
}
variable "vpc_cidr" {
  description = "IPv4 CIDR for the VPC. It must be /24 or larger to provide eight AWS-valid equal-sized subnet ranges."
  type        = string
  default     = "10.40.0.0/16"

  validation {
    condition = (
      can(regex("^[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+/[0-9]+$", var.vpc_cidr)) &&
      can(cidrsubnet(var.vpc_cidr, 4, 7)) &&
      try(tonumber(split("/", var.vpc_cidr)[1]) >= 16, false) &&
      try(tonumber(split("/", var.vpc_cidr)[1]) <= 24, false)
    )
    error_message = "vpc_cidr must be a valid AWS IPv4 VPC CIDR from /16 through /24 so eight /+4 subnets are at least AWS's minimum /28 size."
  }
}
variable "az_count" {
  description = "Number of availability zones. The allocation reserves public and private ranges for at most four AZs."
  type        = number
  default     = 2

  validation {
    condition     = var.az_count >= 1 && var.az_count <= 4 && floor(var.az_count) == var.az_count
    error_message = "az_count must be a whole number between 1 and 4, matching the four generated ranges per subnet tier."
  }
}
variable "single_nat_gateway" {
  type    = bool
  default = false
}
variable "interface_endpoints" {
  type    = list(string)
  default = ["ecr.api", "ecr.dkr", "logs", "secretsmanager", "ssm", "ssmmessages", "xray", "kms"]
}
variable "flow_log_retention_days" {
  description = "CloudWatch retention for VPC Flow Logs. When null, production retains 365 days and other environments retain 30 days."
  type        = number
  default     = null

  validation {
    condition     = var.flow_log_retention_days == null || contains([1, 3, 5, 7, 14, 30, 60, 90, 120, 150, 180, 365, 400, 545, 731, 1096, 1827, 2192, 2557, 2922, 3288, 3653], var.flow_log_retention_days)
    error_message = "flow_log_retention_days must be a retention period supported by CloudWatch Logs."
  }
}
variable "flow_log_kms_key_id" {
  description = "Optional customer-managed KMS key ID or ARN for VPC Flow Logs. A dedicated key is created when omitted."
  type        = string
  default     = null

  validation {
    condition     = var.flow_log_kms_key_id == null || trimspace(var.flow_log_kms_key_id) != ""
    error_message = "flow_log_kms_key_id must be null or a non-empty KMS key ID or ARN."
  }
}
variable "rejected_traffic_alarm_threshold" {
  description = "Rejected VPC flows in a five-minute period that trigger the unusual rejected traffic alarm."
  type        = number
  default     = 100

  validation {
    condition     = var.rejected_traffic_alarm_threshold > 0
    error_message = "rejected_traffic_alarm_threshold must be greater than zero."
  }
}
variable "alarm_topic_arns" {
  description = "SNS topic ARNs notified when unusual rejected VPC traffic is detected."
  type        = list(string)
  default     = []
}
variable "tags" {
  type    = map(string)
  default = {}
}
