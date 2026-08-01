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
