variable "environment" {
  type = string
}
variable "aws_region" {
  type = string
}
variable "ecs_cluster_name" {
  type = string
}
variable "alb_arn_suffix" {
  type = string
}
variable "rds_identifier" {
  type = string
}
variable "redis_replication_group_id" {
  type = string
}
variable "opensearch_domain_name" {
  type = string
}
variable "alarm_topic_arns" {
  type    = list(string)
  default = []
}
variable "trace_sample_rate" {
  type    = number
  default = 0.05
}
variable "alb_5xx_threshold" {
  type    = number
  default = 10
}
variable "tags" {
  type    = map(string)
  default = {}
}
