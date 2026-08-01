mock_provider "aws" {}

variables {
  environment                   = "production"
  account_suffix                = "123456789012"
  vpc_id                        = "vpc-00000001"
  private_subnet_ids            = ["subnet-00000001", "subnet-00000002"]
  rds_security_group_ids        = ["sg-00000001"]
  redis_security_group_ids      = ["sg-00000002"]
  opensearch_security_group_ids = ["sg-00000003"]
  redis_auth_token              = "test-redis-token-123456789"
  deletion_protection           = true
  skip_final_snapshot           = false
}

run "first_replacement_uses_first_unique_suffix" {
  command = plan

  variables {
    final_snapshot_identifier_prefix = "taskmind-final-snapshot"
    final_snapshot_identifier_suffix = "replacement-001"
  }

  assert {
    condition     = aws_db_instance.postgres.final_snapshot_identifier == "taskmind-final-snapshot-production-replacement-001"
    error_message = "The first replacement did not include its unique suffix."
  }
}

run "successive_replacement_uses_a_different_identifier" {
  command = plan

  variables {
    final_snapshot_identifier_prefix = "taskmind-final-snapshot"
    final_snapshot_identifier_suffix = "replacement-002"
  }

  assert {
    condition     = aws_db_instance.postgres.final_snapshot_identifier == "taskmind-final-snapshot-production-replacement-002"
    error_message = "A successive replacement reused the previous final snapshot identifier."
  }

  assert {
    condition     = aws_db_instance.postgres.deletion_protection && !aws_db_instance.postgres.skip_final_snapshot
    error_message = "Production must retain deletion protection and require a final snapshot."
  }
}
