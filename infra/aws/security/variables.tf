variable "environment" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "vpc_cidr" {
  description = "VPC CIDR used to derive the Route 53 Resolver address (VPC base plus two)."
  type        = string
}

variable "vpc_endpoint_security_group_id" {
  description = "Security group attached to the AWS interface endpoints."
  type        = string
}

variable "s3_prefix_list_id" {
  description = "AWS-managed S3 prefix list exposed by the VPC gateway endpoint."
  type        = string
}

variable "tags" {
  type    = map(string)
  default = {}
}
