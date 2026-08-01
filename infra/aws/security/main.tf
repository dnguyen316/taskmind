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
  tags = merge(var.tags, { Project = "taskmind", Environment = var.environment, ManagedBy = "opentofu" })
  service_security_groups = {
    core  = aws_security_group.core.id
    relay = aws_security_group.relay.id
    nova  = aws_security_group.nova.id
  }
}

resource "aws_security_group" "core" {
  name   = "${local.name}-core"
  vpc_id = var.vpc_id

  tags = local.tags
}

resource "aws_security_group" "relay" {
  name   = "${local.name}-relay"
  vpc_id = var.vpc_id

  tags = local.tags
}

resource "aws_security_group" "nova" {
  name   = "${local.name}-nova"
  vpc_id = var.vpc_id

  tags = local.tags
}

resource "aws_security_group" "rds" {
  name   = "${local.name}-rds"
  vpc_id = var.vpc_id

  tags = local.tags
}

resource "aws_security_group" "redis" {
  name   = "${local.name}-redis"
  vpc_id = var.vpc_id

  tags = local.tags
}

resource "aws_security_group" "opensearch" {
  name   = "${local.name}-opensearch"
  vpc_id = var.vpc_id

  tags = local.tags
}

resource "aws_vpc_security_group_egress_rule" "dns_udp" {
  for_each = local.service_security_groups

  description       = "DNS to the VPC Route 53 Resolver"
  security_group_id = each.value
  cidr_ipv4         = "${cidrhost(var.vpc_cidr, 2)}/32"
  from_port         = 53
  to_port           = 53
  ip_protocol       = "udp"
}

resource "aws_vpc_security_group_egress_rule" "dns_tcp" {
  for_each = local.service_security_groups

  description       = "TCP DNS fallback to the VPC Route 53 Resolver"
  security_group_id = each.value
  cidr_ipv4         = "${cidrhost(var.vpc_cidr, 2)}/32"
  from_port         = 53
  to_port           = 53
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "interface_endpoints" {
  for_each = local.service_security_groups

  description                  = "HTTPS to ECR, Logs, Secrets Manager, SSM, KMS, and X-Ray endpoints"
  security_group_id            = each.value
  referenced_security_group_id = var.vpc_endpoint_security_group_id
  from_port                    = 443
  to_port                      = 443
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "core_s3" {
  description                = "Core attachment access through the S3 gateway endpoint"
  security_group_id          = aws_security_group.core.id
  destination_prefix_list_id = var.s3_prefix_list_id
  from_port                  = 443
  to_port                    = 443
  ip_protocol                = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "database" {
  for_each = local.service_security_groups

  description                  = "${title(each.key)} PostgreSQL access"
  security_group_id            = each.value
  referenced_security_group_id = aws_security_group.rds.id
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "redis" {
  for_each = local.service_security_groups

  description                  = "${title(each.key)} Redis access"
  security_group_id            = each.value
  referenced_security_group_id = aws_security_group.redis.id
  from_port                    = 6379
  to_port                      = 6379
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "opensearch" {
  for_each = {
    core  = aws_security_group.core.id
    relay = aws_security_group.relay.id
  }

  description                  = "${title(each.key)} OpenSearch access"
  security_group_id            = each.value
  referenced_security_group_id = aws_security_group.opensearch.id
  from_port                    = 443
  to_port                      = 443
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "core_to_relay" {
  description                  = "Core context calls to Relay"
  security_group_id            = aws_security_group.core.id
  referenced_security_group_id = aws_security_group.relay.id
  from_port                    = 8081
  to_port                      = 8081
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "core_to_nova" {
  description                  = "Core AI calls to Nova"
  security_group_id            = aws_security_group.core.id
  referenced_security_group_id = aws_security_group.nova.id
  from_port                    = 8082
  to_port                      = 8082
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "nova_to_core" {
  description                  = "Nova tool callbacks to Core internal APIs"
  security_group_id            = aws_security_group.nova.id
  referenced_security_group_id = aws_security_group.core.id
  from_port                    = 8080
  to_port                      = 8080
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "nova_to_relay" {
  description                  = "Nova context calls to Relay"
  security_group_id            = aws_security_group.nova.id
  referenced_security_group_id = aws_security_group.relay.id
  from_port                    = 8081
  to_port                      = 8081
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "nova_external_ai" {
  description       = "Unavoidable HTTPS through NAT to configured external AI providers"
  security_group_id = aws_security_group.nova.id
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 443
  to_port           = 443
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "core_external_integrations" {
  description       = "Unavoidable HTTPS through NAT for configured OAuth, integration, and notification providers"
  security_group_id = aws_security_group.core.id
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 443
  to_port           = 443
  ip_protocol       = "tcp"
}


resource "aws_vpc_security_group_ingress_rule" "core_to_relay" {
  description                  = "Core context calls to Relay"
  security_group_id            = aws_security_group.relay.id
  referenced_security_group_id = aws_security_group.core.id
  from_port                    = 8081
  to_port                      = 8081
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "core_to_nova" {
  description                  = "Core AI calls to Nova"
  security_group_id            = aws_security_group.nova.id
  referenced_security_group_id = aws_security_group.core.id
  from_port                    = 8082
  to_port                      = 8082
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "nova_to_core" {
  description                  = "Nova tool callbacks to Core internal APIs"
  security_group_id            = aws_security_group.core.id
  referenced_security_group_id = aws_security_group.nova.id
  from_port                    = 8080
  to_port                      = 8080
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "nova_to_relay" {
  description                  = "Nova context calls to Relay"
  security_group_id            = aws_security_group.relay.id
  referenced_security_group_id = aws_security_group.nova.id
  from_port                    = 8081
  to_port                      = 8081
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "rds_from_service" {
  for_each = {
    core  = aws_security_group.core.id
    relay = aws_security_group.relay.id
    nova  = aws_security_group.nova.id
  }

  description                  = "${title(each.key)} database access"
  security_group_id            = aws_security_group.rds.id
  referenced_security_group_id = each.value
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "redis_from_service" {
  for_each = {
    core  = aws_security_group.core.id
    relay = aws_security_group.relay.id
    nova  = aws_security_group.nova.id
  }

  description                  = "${title(each.key)} Redis access"
  security_group_id            = aws_security_group.redis.id
  referenced_security_group_id = each.value
  from_port                    = 6379
  to_port                      = 6379
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "opensearch_from_relay" {
  description                  = "Relay indexing and search access"
  security_group_id            = aws_security_group.opensearch.id
  referenced_security_group_id = aws_security_group.relay.id
  from_port                    = 443
  to_port                      = 443
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "opensearch_from_core" {
  description                  = "Core activity search reads"
  security_group_id            = aws_security_group.opensearch.id
  referenced_security_group_id = aws_security_group.core.id
  from_port                    = 443
  to_port                      = 443
  ip_protocol                  = "tcp"
}
