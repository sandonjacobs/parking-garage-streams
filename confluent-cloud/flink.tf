data "confluent_flink_region" "flink_parking_region" {
  cloud   = var.cloud_provider
  region  = var.cloud_region
}

resource "confluent_flink_compute_pool" "parking_streams_pool" {
  display_name     = "standard_compute_pool"
  cloud            = data.confluent_flink_region.flink_parking_region.cloud
  region           = data.confluent_flink_region.flink_parking_region.region
  max_cfu          = 5
  environment {
    id = confluent_environment.cc_env.id
  }

  lifecycle {
    prevent_destroy = false
  }
}

resource "confluent_api_key" "env-manager-flink-api-key" {
  display_name = "env-manager-flink-api-key"
  description  = "Flink API Key that is owned by 'env-manager' service account"
  owner {
    id          = confluent_service_account.env-manager.id
    api_version = confluent_service_account.env-manager.api_version
    kind        = confluent_service_account.env-manager.kind
  }

  managed_resource {
    id          = data.confluent_flink_region.flink_parking_region.id
    api_version = data.confluent_flink_region.flink_parking_region.api_version
    kind        = data.confluent_flink_region.flink_parking_region.kind

    environment {
      id = confluent_environment.cc_env.id
    }
  }

  lifecycle {
    prevent_destroy = false
  }
}