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
    tags = merge(var.other_tags, {
      Project     = "Parking Garage Streams"
      Environment = var.environment
      ManagedBy   = "Terraform"
    })
  }
}

