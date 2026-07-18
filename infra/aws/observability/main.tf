terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

locals {
  name = "taskmind-${var.environment}"
  tags = merge(var.tags, {
    Project     = "taskmind"
    Environment = var.environment
    ManagedBy   = "opentofu"
  })
}

resource "aws_xray_sampling_rule" "default" {
  rule_name      = "${local.name}-default"
  priority       = 10000
  version        = 1
  reservoir_size = 1
  fixed_rate     = var.trace_sample_rate
  url_path       = "*"
  host           = "*"
  http_method    = "*"
  service_type   = "*"
  service_name   = "*"
  resource_arn   = "*"
  attributes     = {}
}

resource "aws_cloudwatch_metric_alarm" "alb_5xx" {
  alarm_name          = "${local.name}-alb-5xx"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "HTTPCode_ELB_5XX_Count"
  namespace           = "AWS/ApplicationELB"
  period              = 60
  statistic           = "Sum"
  threshold           = var.alb_5xx_threshold
  alarm_actions       = var.alarm_topic_arns
  dimensions = {
    LoadBalancer = var.alb_arn_suffix
  }
  tags = local.tags
}

resource "aws_cloudwatch_metric_alarm" "rds_cpu" {
  alarm_name          = "${local.name}-rds-cpu"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 3
  metric_name         = "CPUUtilization"
  namespace           = "AWS/RDS"
  period              = 300
  statistic           = "Average"
  threshold           = 80
  alarm_actions       = var.alarm_topic_arns
  dimensions = {
    DBInstanceIdentifier = var.rds_identifier
  }
  tags = local.tags
}

resource "aws_cloudwatch_metric_alarm" "redis_cpu" {
  alarm_name          = "${local.name}-redis-cpu"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 3
  metric_name         = "EngineCPUUtilization"
  namespace           = "AWS/ElastiCache"
  period              = 300
  statistic           = "Average"
  threshold           = 80
  alarm_actions       = var.alarm_topic_arns
  dimensions = {
    ReplicationGroupId = var.redis_replication_group_id
  }
  tags = local.tags
}

resource "aws_cloudwatch_metric_alarm" "opensearch_red" {
  alarm_name          = "${local.name}-opensearch-red"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "ClusterStatus.red"
  namespace           = "AWS/ES"
  period              = 60
  statistic           = "Maximum"
  threshold           = 0
  alarm_actions       = var.alarm_topic_arns
  dimensions = {
    DomainName = var.opensearch_domain_name
  }
  tags = local.tags
}

resource "aws_cloudwatch_dashboard" "this" {
  dashboard_name = local.name
  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        x      = 0
        y      = 0
        width  = 12
        height = 6
        properties = {
          title   = "TaskMind ECS CPU"
          region  = var.aws_region
          metrics = [["AWS/ECS", "CPUUtilization", "ClusterName", var.ecs_cluster_name]]
          stat    = "Average"
          period  = 300
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 0
        width  = 12
        height = 6
        properties = {
          title   = "Core ALB 5xx"
          region  = var.aws_region
          metrics = [["AWS/ApplicationELB", "HTTPCode_ELB_5XX_Count", "LoadBalancer", var.alb_arn_suffix]]
          stat    = "Sum"
          period  = 60
        }
      }
    ]
  })
}
