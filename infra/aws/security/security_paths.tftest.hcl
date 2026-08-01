mock_provider "aws" {}

variables {
  environment           = "test"
  vpc_id                = "vpc-00000000000000000"
}

run "only_documented_network_paths_are_authorized" {
  command = plan

  assert {
    condition = toset(keys(output.service_security_group_ids)) == toset(["core", "relay", "nova"])
    error_message = "Security outputs must expose one group for each ECS service and no shared ECS group."
  }

  assert {
    condition = alltrue([
      aws_vpc_security_group_ingress_rule.core_to_relay.from_port == 8081,
      aws_vpc_security_group_ingress_rule.core_to_nova.from_port == 8082,
      aws_vpc_security_group_ingress_rule.nova_to_core.from_port == 8080,
      aws_vpc_security_group_ingress_rule.nova_to_relay.from_port == 8081,
      aws_vpc_security_group_ingress_rule.opensearch_from_relay.from_port == 443,
      aws_vpc_security_group_ingress_rule.opensearch_from_core.from_port == 443,
    ])
    error_message = "Service ingress must match the documented caller, destination, and port paths."
  }

  assert {
    condition = alltrue([
      toset(keys(aws_vpc_security_group_ingress_rule.rds_from_service)) == toset(["core", "relay", "nova"]),
      toset(keys(aws_vpc_security_group_ingress_rule.redis_from_service)) == toset(["core", "relay", "nova"]),
      alltrue([for rule in values(aws_vpc_security_group_ingress_rule.rds_from_service) : rule.from_port == 5432 && rule.to_port == 5432]),
      alltrue([for rule in values(aws_vpc_security_group_ingress_rule.redis_from_service) : rule.from_port == 6379 && rule.to_port == 6379]),
    ])
    error_message = "Only the three application services may reach RDS and Redis on their required ports."
  }
}
