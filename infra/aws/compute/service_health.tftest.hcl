mock_provider "aws" {}

variables {
  environment            = "test"
  aws_region             = "ap-southeast-2"
  vpc_id                 = "vpc-00000000000000000"
  private_subnet_ids     = ["subnet-00000000000000000", "subnet-11111111111111111"]
  ecs_security_group_id  = "sg-00000000000000000"
  core_target_group_arn  = "arn:aws:elasticloadbalancing:ap-southeast-2:123456789012:targetgroup/core/0000000000000000"
  attachments_bucket_arn = "arn:aws:s3:::taskmind-test-attachments"
  opensearch_domain_arn  = "arn:aws:es:ap-southeast-2:123456789012:domain/taskmind-test"
  core_secret_arns       = []
  relay_secret_arns      = []
  nova_secret_arns       = []
  service_images = {
    core  = "example.invalid/taskmind/core:test"
    relay = "example.invalid/taskmind/relay:test"
    nova  = "example.invalid/taskmind/nova:test"
  }
}

run "services_are_health_checked_and_rollback_safe" {
  command = plan

  assert {
    condition = alltrue([
      for task in values(aws_ecs_task_definition.service) :
      try(length(jsondecode(task.container_definitions)[0].healthCheck.command) > 0, false)
    ])
    error_message = "Every service container must define a health check command."
  }

  assert {
    condition = alltrue([
      strcontains(jsondecode(aws_ecs_task_definition.service["core"].container_definitions)[0].healthCheck.command[1], "http://localhost:8080/api/health"),
      strcontains(jsondecode(aws_ecs_task_definition.service["relay"].container_definitions)[0].healthCheck.command[1], "http://localhost:8081/actuator/health"),
      strcontains(jsondecode(aws_ecs_task_definition.service["nova"].container_definitions)[0].healthCheck.command[1], "http://localhost:8082/api/health"),
    ])
    error_message = "Container checks must use the health endpoint supported by each owning service."
  }

  assert {
    condition = alltrue([
      for service in values(aws_ecs_service.service) :
      service.deployment_circuit_breaker[0].enable && service.deployment_circuit_breaker[0].rollback
    ])
    error_message = "Every ECS service must enable the deployment circuit breaker with rollback."
  }

  assert {
    condition = alltrue([
      for service in values(aws_ecs_service.service) :
      service.deployment_minimum_healthy_percent == 100 && service.deployment_maximum_percent == 200
    ])
    error_message = "Deployments must retain the existing task while allowing one replacement for desired-count-one services."
  }

  assert {
    condition     = aws_ecs_service.service["core"].health_check_grace_period_seconds == 120
    error_message = "Core must receive an ALB health-check grace period."
  }
}
