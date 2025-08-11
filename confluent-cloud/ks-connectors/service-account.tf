resource "confluent_service_account" "connector_admin" {
  display_name = "${var.CC_ENV_DISPLAY_NAME}-connector-admin-sa"
  description  = "Service Account for Kafka Connectors"

  lifecycle {
    prevent_destroy = false
  }

}

resource "confluent_api_key" "connector_admin" {
  display_name = "${var.CC_ENV_DISPLAY_NAME}-connector-api-key"
  description  = "Kafka API Key that is owned by 'connector_admin' service account"
  owner {
    id          = confluent_service_account.connector_admin.id
    api_version = confluent_service_account.connector_admin.api_version
    kind        = confluent_service_account.connector_admin.kind
  }

  managed_resource {
    id          = data.confluent_kafka_cluster.kafka_cluster.id
    api_version = data.confluent_kafka_cluster.kafka_cluster.api_version
    kind        = data.confluent_kafka_cluster.kafka_cluster.kind

    environment {
      id = data.confluent_environment.cc_env.id
    }
  }

  lifecycle {
    prevent_destroy = false
  }
}

resource "confluent_role_binding" "connector_admin" {
  principal   = "User:${confluent_service_account.connector_admin.id}"
  role_name   = "CloudClusterAdmin"
  crn_pattern = data.confluent_kafka_cluster.kafka_cluster.rbac_crn
}


resource "confluent_service_account" "connector" {
  display_name = "${var.CC_ENV_DISPLAY_NAME}-connectors-sa"
  description  = "Service Account for Kafka Connectors"

  lifecycle {
    prevent_destroy = false
  }

}

resource "confluent_api_key" "connector-api-key" {
  display_name = "${var.CC_ENV_DISPLAY_NAME}-connector-api-key"
  description  = "Kafka API Key that is owned by 'app-manager' service account"
  owner {
    id          = confluent_service_account.connector.id
    api_version = confluent_service_account.connector.api_version
    kind        = confluent_service_account.connector.kind
  }

  managed_resource {
    id          = data.confluent_kafka_cluster.kafka_cluster.id
    api_version = data.confluent_kafka_cluster.kafka_cluster.api_version
    kind        = data.confluent_kafka_cluster.kafka_cluster.kind

    environment {
      id = data.confluent_environment.cc_env.id
    }
  }

  lifecycle {
    prevent_destroy = false
  }
}

resource "confluent_role_binding" "connector_resource_owner" {
  principal   = "User:${confluent_service_account.connector.id}"
  role_name   = "ResourceOwner"
  crn_pattern = "${data.confluent_kafka_cluster.kafka_cluster.rbac_crn}/connector=*"
}


resource "confluent_role_binding" "connector_topic_developer_read" {
  principal   = "User:${confluent_service_account.connector.id}"
  role_name   = "DeveloperRead"
  crn_pattern = "${data.confluent_kafka_cluster.kafka_cluster.rbac_crn}/kafka=${data.confluent_kafka_cluster.kafka_cluster.id}/topic=*"
}

resource "confluent_role_binding" "connector_topic_developer_write" {
  principal   = "User:${confluent_service_account.connector.id}"
  role_name   = "DeveloperWrite"
  crn_pattern = "${data.confluent_kafka_cluster.kafka_cluster.rbac_crn}/kafka=${data.confluent_kafka_cluster.kafka_cluster.id}/topic=*"
}