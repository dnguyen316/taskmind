terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_caller_identity" "current" {}

locals {
  azs = slice(data.aws_availability_zones.available.names, 0, var.az_count)

  # Allocate all subnet tiers from one sequence so a CIDR can never be reused
  # between the public and private tiers. Eight equal-sized ranges support up to
  # four AZs while preserving the same per-AZ capacity for both tiers.
  subnet_newbits     = 4
  subnet_tier_count  = 2
  subnet_range_count = local.subnet_tier_count * 4
  subnet_cidrs = [
    for subnet_index in range(local.subnet_range_count) :
    cidrsubnet(var.vpc_cidr, local.subnet_newbits, subnet_index)
  ]
  public_subnets  = slice(local.subnet_cidrs, 0, var.az_count)
  private_subnets = slice(local.subnet_cidrs, local.subnet_range_count / 2, local.subnet_range_count / 2 + var.az_count)
  common_tags = merge(var.tags, {
    Project     = "taskmind"
    Environment = var.environment
    ManagedBy   = "opentofu"
  })
  flow_log_retention_days = coalesce(var.flow_log_retention_days, var.environment == "production" ? 365 : 30)
  flow_log_kms_key_id     = coalesce(var.flow_log_kms_key_id, try(aws_kms_key.flow_logs[0].arn, null))
}

data "aws_iam_policy_document" "flow_logs_kms" {
  count = var.flow_log_kms_key_id == null ? 1 : 0

  statement {
    sid     = "EnableAccountAdministration"
    actions = ["kms:*"]
    principals {
      type        = "AWS"
      identifiers = ["arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"]
    }
    resources = ["*"]
  }

  statement {
    sid = "AllowCloudWatchLogsEncryption"
    actions = [
      "kms:Decrypt",
      "kms:Encrypt",
      "kms:GenerateDataKey*",
      "kms:ReEncrypt*",
      "kms:DescribeKey",
    ]
    principals {
      type        = "Service"
      identifiers = ["logs.${var.aws_region}.amazonaws.com"]
    }
    resources = ["*"]
    condition {
      test     = "ArnLike"
      variable = "kms:EncryptionContext:aws:logs:arn"
      values   = ["arn:aws:logs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:log-group:/taskmind/${var.environment}/vpc-flow-logs"]
    }
  }
}

resource "aws_kms_key" "flow_logs" {
  count                   = var.flow_log_kms_key_id == null ? 1 : 0
  description             = "Encrypt TaskMind ${var.environment} VPC Flow Logs"
  deletion_window_in_days = 30
  enable_key_rotation     = true
  policy                  = data.aws_iam_policy_document.flow_logs_kms[0].json
  tags                    = local.common_tags
}

resource "aws_kms_alias" "flow_logs" {
  count         = var.flow_log_kms_key_id == null ? 1 : 0
  name          = "alias/taskmind-${var.environment}-vpc-flow-logs"
  target_key_id = aws_kms_key.flow_logs[0].key_id
}

resource "aws_cloudwatch_log_group" "vpc_flow_logs" {
  name              = "/taskmind/${var.environment}/vpc-flow-logs"
  retention_in_days = local.flow_log_retention_days
  kms_key_id        = local.flow_log_kms_key_id
  tags              = local.common_tags
}

data "aws_iam_policy_document" "flow_logs_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["vpc-flow-logs.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "flow_logs" {
  name               = "taskmind-${var.environment}-vpc-flow-logs"
  assume_role_policy = data.aws_iam_policy_document.flow_logs_assume_role.json
  tags               = local.common_tags
}

data "aws_iam_policy_document" "flow_logs_delivery" {
  statement {
    sid       = "DescribeLogGroups"
    actions   = ["logs:DescribeLogGroups"]
    resources = ["*"]
  }
  statement {
    sid = "DeliverToVpcFlowLogGroup"
    actions = [
      "logs:CreateLogStream",
      "logs:DescribeLogStreams",
      "logs:PutLogEvents",
    ]
    resources = ["${aws_cloudwatch_log_group.vpc_flow_logs.arn}:*"]
  }
}

resource "aws_iam_role_policy" "flow_logs_delivery" {
  name   = "deliver-vpc-flow-logs"
  role   = aws_iam_role.flow_logs.id
  policy = data.aws_iam_policy_document.flow_logs_delivery.json
}

resource "aws_flow_log" "this" {
  vpc_id                   = aws_vpc.this.id
  traffic_type             = "ALL"
  log_destination_type     = "cloud-watch-logs"
  log_destination          = aws_cloudwatch_log_group.vpc_flow_logs.arn
  iam_role_arn             = aws_iam_role.flow_logs.arn
  max_aggregation_interval = 60
  log_format               = "$${version} $${account-id} $${interface-id} $${srcaddr} $${dstaddr} $${srcport} $${dstport} $${protocol} $${packets} $${bytes} $${start} $${end} $${action} $${log-status} $${flow-direction} $${tcp-flags} $${reject-reason}"

  depends_on = [aws_iam_role_policy.flow_logs_delivery]
}

resource "aws_cloudwatch_log_metric_filter" "rejected_flows" {
  name           = "taskmind-${var.environment}-rejected-vpc-flows"
  log_group_name = aws_cloudwatch_log_group.vpc_flow_logs.name
  pattern        = "[version, account_id, interface_id, srcaddr, dstaddr, srcport, dstport, protocol, packets, bytes, start, end, action = REJECT, log_status, flow_direction, tcp_flags, reject_reason]"

  metric_transformation {
    name      = "RejectedVpcFlows-${var.environment}"
    namespace = "TaskMind/Network"
    value     = "1"
  }
}

resource "aws_cloudwatch_metric_alarm" "unusual_rejected_traffic" {
  alarm_name          = "taskmind-${var.environment}-unusual-rejected-vpc-traffic"
  alarm_description   = "Rejected VPC flows exceeded the expected five-minute threshold."
  namespace           = "TaskMind/Network"
  metric_name         = "RejectedVpcFlows-${var.environment}"
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = var.rejected_traffic_alarm_threshold
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = var.alarm_topic_arns
  tags                = local.common_tags
}

resource "aws_vpc" "this" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = merge(local.common_tags, { Name = "taskmind-${var.environment}" })
}

resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id

  tags = merge(local.common_tags, { Name = "taskmind-${var.environment}-igw" })
}

resource "aws_subnet" "public" {
  count                   = var.az_count
  vpc_id                  = aws_vpc.this.id
  availability_zone       = local.azs[count.index]
  cidr_block              = local.public_subnets[count.index]
  map_public_ip_on_launch = false

  tags = merge(local.common_tags, {
    Name = "taskmind-${var.environment}-public-${count.index + 1}"
    Tier = "public"
  })
}

resource "aws_subnet" "private" {
  count             = var.az_count
  vpc_id            = aws_vpc.this.id
  availability_zone = local.azs[count.index]
  cidr_block        = local.private_subnets[count.index]

  tags = merge(local.common_tags, {
    Name = "taskmind-${var.environment}-private-${count.index + 1}"
    Tier = "private"
  })
}

resource "aws_eip" "nat" {
  count  = var.single_nat_gateway ? 1 : var.az_count
  domain = "vpc"

  tags = merge(local.common_tags, { Name = "taskmind-${var.environment}-nat-${count.index + 1}" })
}

resource "aws_nat_gateway" "this" {
  count         = var.single_nat_gateway ? 1 : var.az_count
  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public[count.index].id

  tags = merge(local.common_tags, { Name = "taskmind-${var.environment}-nat-${count.index + 1}" })

  depends_on = [aws_internet_gateway.this]
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }

  tags = merge(local.common_tags, { Name = "taskmind-${var.environment}-public" })
}

resource "aws_route_table_association" "public" {
  count          = var.az_count
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table" "private" {
  count  = var.az_count
  vpc_id = aws_vpc.this.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.this[var.single_nat_gateway ? 0 : count.index].id
  }

  tags = merge(local.common_tags, { Name = "taskmind-${var.environment}-private-${count.index + 1}" })
}

resource "aws_route_table_association" "private" {
  count          = var.az_count
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private[count.index].id
}

resource "aws_security_group" "vpc_endpoints" {
  name        = "taskmind-${var.environment}-vpc-endpoints"
  description = "HTTPS access to interface VPC endpoints"
  vpc_id      = aws_vpc.this.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.common_tags
}

resource "aws_vpc_endpoint" "s3" {
  vpc_id            = aws_vpc.this.id
  service_name      = "com.amazonaws.${var.aws_region}.s3"
  vpc_endpoint_type = "Gateway"
  route_table_ids   = aws_route_table.private[*].id

  tags = merge(local.common_tags, { Name = "taskmind-${var.environment}-s3" })
}

resource "aws_vpc_endpoint" "interface" {
  for_each = toset(var.interface_endpoints)

  vpc_id              = aws_vpc.this.id
  service_name        = "com.amazonaws.${var.aws_region}.${each.value}"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = aws_subnet.private[*].id
  security_group_ids  = [aws_security_group.vpc_endpoints.id]
  private_dns_enabled = true

  tags = merge(local.common_tags, { Name = "taskmind-${var.environment}-${each.value}" })
}
