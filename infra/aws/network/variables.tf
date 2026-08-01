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
variable "tags" {
  type    = map(string)
  default = {}
}
