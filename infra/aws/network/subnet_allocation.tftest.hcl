mock_provider "aws" {
  mock_data "aws_availability_zones" {
    defaults = {
      names = ["ap-southeast-2a", "ap-southeast-2b", "ap-southeast-2c", "ap-southeast-2d"]
    }
  }
}

run "default_subnets_are_distinct" {
  command = plan

  variables {
    environment = "test"
    aws_region  = "ap-southeast-2"
  }

  assert {
    condition = output.public_subnet_cidrs == [
      "10.40.0.0/20",
      "10.40.16.0/20",
    ]
    error_message = "The default public subnet CIDRs did not expand as expected."
  }

  assert {
    condition = output.private_subnet_cidrs == [
      "10.40.64.0/20",
      "10.40.80.0/20",
    ]
    error_message = "The default private subnet CIDRs did not expand as expected."
  }

  assert {
    condition = length(setintersection(
      toset(output.public_subnet_cidrs),
      toset(output.private_subnet_cidrs),
    )) == 0
    error_message = "A default public subnet CIDR overlaps a private subnet CIDR."
  }
}
