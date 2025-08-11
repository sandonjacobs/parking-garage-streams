resource "confluent_service_account" "connector_admin" {
  display_name = "${var.CC_ENV_DISPLAY_NAME}-connector-admin-sa"
  description  = "Service Account for Kafka Connectors"

  lifecycle {
    prevent_destroy = false
  }
}

resource "confluent_role_binding" "connector_admin" {
  principal   = "User:${confluent_service_account.connector_admin.id}"
  role_name   = "CloudClusterAdmin"
  crn_pattern = data.confluent_kafka_cluster.kafka_cluster.rbac_crn
}

# Kafka API Key for connector_admin to manage ACLs and topics
resource "confluent_api_key" "connector_admin" {
  display_name = "connector-admin-kafka-api-key"
  description  = "Kafka API Key owned by connector_admin SA"
  owner {
    id          = confluent_service_account.connector_admin.id
    api_version = confluent_service_account.connector_admin.api_version
    kind        = confluent_service_account.connector_admin.kind
  }

  managed_resource {
    id          = data.confluent_kafka_cluster.kafka_cluster.id
    api_version = data.confluent_kafka_cluster.kafka_cluster.api_version
    kind        = data.confluent_kafka_cluster.kafka_cluster.kind

    environment { id = data.confluent_environment.cc_env.id }
  }

  depends_on = [confluent_role_binding.connector_admin]
}

resource "confluent_service_account" "connector" {
  display_name = "${var.CC_ENV_DISPLAY_NAME}-connectors-sa"
  description  = "Service Account for Kafka Connectors"

  lifecycle {
    prevent_destroy = false
  }
}