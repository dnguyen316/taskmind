output "alb_dns_name" {
  value = aws_lb.core.dns_name
}
output "alb_security_group_id" {
  value = aws_security_group.alb.id
}
output "core_target_group_arn" {
  value = aws_lb_target_group.core.arn
}
output "cloudfront_distribution_id" {
  value = aws_cloudfront_distribution.spa.id
}
output "cloudfront_domain_name" {
  value = aws_cloudfront_distribution.spa.domain_name
}
