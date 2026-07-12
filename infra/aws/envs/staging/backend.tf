terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket         = "taskmind-tfstate-staging"
    key            = "infra/aws/envs/staging/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "taskmind-tf-locks-staging"
    encrypt        = true
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = local.tags
  }
}
