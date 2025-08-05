terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "Parking Garage Streams"
      Environment = var.environment
      ManagedBy   = "Terraform"
      cflt_managed_by = "user"
      cflt_managed_id = "sjacobs@confluent.io"
    }
  }
}