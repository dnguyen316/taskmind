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
variable "tags" {
  type    = map(string)
  default = {}
}
