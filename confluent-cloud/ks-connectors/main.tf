# Configure the Confluent Provider
terraform {
  backend "local" {
    workspace_dir = ".tfstate/terraform.state"
  }

  required_providers {
    confluent = {
      source  = "confluentinc/confluent"
      version = "2.37.0"
    }
  }
}

provider "confluent" {
}

data "confluent_environment" "cc_env" {
  id = var.CC_ENV_ID
}

data "confluent_kafka_cluster" "kafka_cluster" {
  id = var.CC_KAFKA_CLUSTER_ID
  environment {
    id = var.CC_ENV_ID
  }
}
