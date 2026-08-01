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
}

resource "aws_security_group" "core" {
  name   = "${local.name}-core"
  vpc_id = var.vpc_id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.tags
}

resource "aws_security_group" "relay" {
  name   = "${local.name}-relay"
  vpc_id = var.vpc_id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.tags
}

resource "aws_security_group" "nova" {
  name   = "${local.name}-nova"
  vpc_id = var.vpc_id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.tags
}

resource "aws_security_group" "rds" {
  name   = "${local.name}-rds"
  vpc_id = var.vpc_id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.tags
}

resource "aws_security_group" "redis" {
  name   = "${local.name}-redis"
  vpc_id = var.vpc_id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.tags
}

resource "aws_security_group" "opensearch" {
  name   = "${local.name}-opensearch"
  vpc_id = var.vpc_id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.tags
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
