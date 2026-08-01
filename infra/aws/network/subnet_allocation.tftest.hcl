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
    # Module outputs are lists; tolist avoids comparing a list to a tuple,
    # which is always false even when every CIDR string is identical.
    condition = output.public_subnet_cidrs == tolist([
      "10.40.0.0/20",
      "10.40.16.0/20",
    ])
    error_message = "The default public subnet CIDRs did not expand as expected."
  }

  assert {
    condition = output.private_subnet_cidrs == tolist([
      "10.40.64.0/20",
      "10.40.80.0/20",
    ])
    error_message = "The default private subnet CIDRs did not expand as expected."
  }

  assert {
    # Every generated range has the same prefix length, so two ranges overlap
    # if and only if their CIDR strings are equal.
    condition = length(setintersection(
      toset(output.public_subnet_cidrs),
      toset(output.private_subnet_cidrs),
    )) == 0
    error_message = "A default public subnet CIDR overlaps a private subnet CIDR."
  }
}

run "maximum_supported_azs_use_all_distinct_ranges" {
  command = plan

  variables {
    environment = "test"
    aws_region  = "ap-southeast-2"
    az_count    = 4
  }

  assert {
    condition = length(setunion(
      toset(output.public_subnet_cidrs),
      toset(output.private_subnet_cidrs),
    )) == 8
    error_message = "The maximum supported AZ count must allocate eight distinct subnet CIDRs."
  }
}
