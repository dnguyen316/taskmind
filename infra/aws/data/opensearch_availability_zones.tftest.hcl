mock_provider "aws" {
  mock_resource "aws_db_instance" {
    defaults = {
      master_user_secret = [{
        kms_key_id    = "arn:aws:kms:ap-southeast-2:123456789012:key/test"
        secret_arn    = "arn:aws:secretsmanager:ap-southeast-2:123456789012:secret:test"
        secret_status = "active"
      }]
    }
  }
}

variables {
  environment                      = "test"
  account_suffix                   = "123456789012"
  vpc_id                           = "vpc-00000001"
  private_subnet_ids               = ["subnet-00000001", "subnet-00000002", "subnet-00000003"]
  rds_security_group_ids           = ["sg-00000001"]
  redis_security_group_ids         = ["sg-00000002"]
  opensearch_security_group_ids    = ["sg-00000003"]
  redis_auth_token                 = "test-redis-token-123456789"
  final_snapshot_identifier_suffix = "test-001"
}

run "single_zone_uses_one_subnet_without_zone_awareness" {
  command = plan

  variables {
    opensearch_availability_zone_count = 1
  }

  assert {
    condition     = !aws_opensearch_domain.activity.cluster_config[0].zone_awareness_enabled
    error_message = "A single-zone domain must disable zone awareness."
  }

  assert {
    condition     = length(aws_opensearch_domain.activity.vpc_options[0].subnet_ids) == 1
    error_message = "A single-zone domain must use exactly one private subnet."
  }
}

run "two_zones_use_two_subnets_and_zone_awareness" {
  command = plan

  variables {
    opensearch_availability_zone_count = 2
    opensearch_instance_count          = 4
  }

  assert {
    condition     = aws_opensearch_domain.activity.cluster_config[0].zone_awareness_enabled
    error_message = "A two-zone domain must enable zone awareness."
  }

  assert {
    condition     = aws_opensearch_domain.activity.cluster_config[0].zone_awareness_config[0].availability_zone_count == 2
    error_message = "The zone-awareness configuration must use the requested two zones."
  }

  assert {
    condition     = length(aws_opensearch_domain.activity.vpc_options[0].subnet_ids) == 2
    error_message = "A two-zone domain must use exactly two private subnets."
  }
}

run "rejects_too_few_private_subnets" {
  command = plan

  variables {
    private_subnet_ids                 = ["subnet-00000001"]
    opensearch_availability_zone_count = 2
  }

  expect_failures = [aws_opensearch_domain.activity]
}

run "rejects_uneven_node_distribution" {
  command = plan

  variables {
    opensearch_availability_zone_count = 3
    opensearch_instance_count          = 4
  }

  expect_failures = [aws_opensearch_domain.activity]
}
