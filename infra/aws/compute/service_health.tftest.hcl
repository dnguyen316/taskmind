mock_provider "aws" {
  mock_resource "aws_iam_role" {
    defaults = {
      arn = "arn:aws:iam::123456789012:role/taskmind-test"
    }
  }

  mock_resource "aws_service_discovery_service" {
    defaults = {
      arn = "arn:aws:servicediscovery:ap-southeast-2:123456789012:service/srv-00000000000000000"
    }
  }
}

override_data {
  target = data.aws_iam_policy_document.core_policy
  values = {
    json = "{\"Version\":\"2012-10-17\",\"Statement\":[]}"
  }
}

override_data {
  target = data.aws_iam_policy_document.relay_policy
  values = {
    json = "{\"Version\":\"2012-10-17\",\"Statement\":[]}"
  }
}

override_data {
  target = data.aws_iam_policy_document.nova_policy
  values = {
    json = "{\"Version\":\"2012-10-17\",\"Statement\":[]}"
  }
}

variables {
  environment            = "test"
  aws_region             = "ap-southeast-2"
  vpc_id                 = "vpc-00000000000000000"
  private_subnet_ids     = ["subnet-00000000000000000", "subnet-11111111111111111"]
  service_security_group_ids = {
    core  = "sg-00000000000000001"
    relay = "sg-00000000000000002"
    nova  = "sg-00000000000000003"
  }
  core_target_group_arn  = "arn:aws:elasticloadbalancing:ap-southeast-2:123456789012:targetgroup/core/0000000000000000"
  alb_listener_ready_arn = "arn:aws:elasticloadbalancing:ap-southeast-2:123456789012:listener/app/taskmind-test/0000000000000000/1111111111111111"
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
    condition     = toset(keys(aws_ecs_task_definition.service)) == toset(["core", "relay", "nova"])
    error_message = "The compute module must create task definitions for Core, Relay, and Nova."
  }

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

  assert {
    condition = alltrue([
      aws_ecs_service.service["core"].network_configuration[0].security_groups == ["sg-00000000000000001"],
      aws_ecs_service.service["relay"].network_configuration[0].security_groups == ["sg-00000000000000002"],
      aws_ecs_service.service["nova"].network_configuration[0].security_groups == ["sg-00000000000000003"],
    ])
    error_message = "Each ECS service must use its matching service security group."
  }
}

run "health_and_deployment_tuning_is_applied" {
  command = plan

  variables {
    container_health_check_interval_seconds     = 45
    container_health_check_timeout_seconds      = 10
    container_health_check_retries              = 5
    container_health_check_start_period_seconds = 90
    core_health_check_grace_period_seconds      = 180
    deployment_minimum_healthy_percent          = 50
    deployment_maximum_percent                  = 150
  }

  assert {
    condition = alltrue([
      for task in values(aws_ecs_task_definition.service) :
      jsondecode(task.container_definitions)[0].healthCheck.interval == 45 &&
      jsondecode(task.container_definitions)[0].healthCheck.timeout == 10 &&
      jsondecode(task.container_definitions)[0].healthCheck.retries == 5 &&
      jsondecode(task.container_definitions)[0].healthCheck.startPeriod == 90
    ])
    error_message = "Container health-check tuning must be applied to every service task definition."
  }

  assert {
    condition = alltrue([
      for service in values(aws_ecs_service.service) :
      service.deployment_minimum_healthy_percent == 50 && service.deployment_maximum_percent == 150
    ])
    error_message = "Deployment percentage tuning must be applied to every ECS service."
  }

  assert {
    condition     = aws_ecs_service.service["core"].health_check_grace_period_seconds == 180
    error_message = "Core ALB health-check grace-period tuning must be applied."
  }
}
