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
variable "tags" {
  type    = map(string)
  default = {}
}
