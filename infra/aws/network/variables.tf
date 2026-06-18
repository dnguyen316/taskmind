variable "environment" {
  type = string
}
variable "aws_region" {
  type = string
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
