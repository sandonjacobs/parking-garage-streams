resource "confluent_kafka_acl" "source_topic_row_aggregates_read" {
  kafka_cluster { id = data.confluent_kafka_cluster.kafka_cluster.id }
  resource_type = "TOPIC"
  resource_name = var.TOPIC_ROW_AGGREGATES_NAME
  pattern_type  = "LITERAL"
  principal     = "User:${confluent_service_account.connector.id}"
  operation     = "READ"
  permission    = "ALLOW"
  host          = "*"
  rest_endpoint = data.confluent_kafka_cluster.kafka_cluster.rest_endpoint
  credentials {
    key    = confluent_api_key.connector_admin.id
    secret = confluent_api_key.connector_admin.secret
  }

  depends_on = [
    confluent_api_key.connector_admin
  ]
}

resource "confluent_kafka_acl" "source_topic_row_aggregates_describe" {
  kafka_cluster { id = data.confluent_kafka_cluster.kafka_cluster.id }
  resource_type = "TOPIC"
  resource_name = var.TOPIC_ROW_AGGREGATES_NAME
  pattern_type  = "LITERAL"
  principal     = "User:${confluent_service_account.connector.id}"
  operation     = "DESCRIBE"
  permission    = "ALLOW"
  host          = "*"
  rest_endpoint = data.confluent_kafka_cluster.kafka_cluster.rest_endpoint
  credentials {
    key    = confluent_api_key.connector_admin.id
    secret = confluent_api_key.connector_admin.secret
  }

  depends_on = [
    confluent_api_key.connector_admin
  ]
}

resource "confluent_kafka_acl" "source_topic_zone_aggregates_read" {
  kafka_cluster { id = data.confluent_kafka_cluster.kafka_cluster.id }
  resource_type = "TOPIC"
  resource_name = var.TOPIC_ZONE_AGGREGATES_NAME
  pattern_type  = "LITERAL"
  principal     = "User:${confluent_service_account.connector.id}"
  operation     = "READ"
  permission    = "ALLOW"
  host          = "*"
  rest_endpoint = data.confluent_kafka_cluster.kafka_cluster.rest_endpoint
  credentials {
    key    = confluent_api_key.connector_admin.id
    secret = confluent_api_key.connector_admin.secret
  }

  depends_on = [
    confluent_api_key.connector_admin
  ]
}

resource "confluent_kafka_acl" "source_topic_zone_aggregates_describe" {
  kafka_cluster { id = data.confluent_kafka_cluster.kafka_cluster.id }
  resource_type = "TOPIC"
  resource_name = var.TOPIC_ZONE_AGGREGATES_NAME
  pattern_type  = "LITERAL"
  principal     = "User:${confluent_service_account.connector.id}"
  operation     = "DESCRIBE"
  permission    = "ALLOW"
  host          = "*"
  rest_endpoint = data.confluent_kafka_cluster.kafka_cluster.rest_endpoint
  credentials {
    key    = confluent_api_key.connector_admin.id
    secret = confluent_api_key.connector_admin.secret
  }

  depends_on = [
    confluent_api_key.connector_admin
  ]
}

# Consumer Group READ permission for managed connector groups (connect-*)
resource "confluent_kafka_acl" "connector_consumer_group_read" {
  kafka_cluster { id = data.confluent_kafka_cluster.kafka_cluster.id }
  resource_type = "GROUP"
  resource_name = "connect-"
  pattern_type  = "PREFIXED"
  principal     = "User:${confluent_service_account.connector.id}"
  operation     = "READ"
  permission    = "ALLOW"
  host          = "*"
  rest_endpoint = data.confluent_kafka_cluster.kafka_cluster.rest_endpoint
  credentials {
    key    = confluent_api_key.connector_admin.id
    secret = confluent_api_key.connector_admin.secret
  }

  depends_on = [
    confluent_api_key.connector_admin
  ]
}

# DLQ topic ACLs (WRITE/DESCRIBE)
resource "confluent_kafka_acl" "dlq_write" {
  kafka_cluster { id = data.confluent_kafka_cluster.kafka_cluster.id }
  resource_type = "TOPIC"
  resource_name = confluent_kafka_topic.postgres_sink_aggregates_dlq.topic_name
  pattern_type  = "LITERAL"
  principal     = "User:${confluent_service_account.connector.id}"
  operation     = "WRITE"
  permission    = "ALLOW"
  host          = "*"
  rest_endpoint = data.confluent_kafka_cluster.kafka_cluster.rest_endpoint
  credentials {
    key    = confluent_api_key.connector_admin.id
    secret = confluent_api_key.connector_admin.secret
  }

  depends_on = [
    confluent_api_key.connector_admin,
    confluent_kafka_topic.postgres_sink_aggregates_dlq
  ]
}

resource "confluent_kafka_acl" "dlq_describe" {
  kafka_cluster { id = data.confluent_kafka_cluster.kafka_cluster.id }
  resource_type = "TOPIC"
  resource_name = confluent_kafka_topic.postgres_sink_aggregates_dlq.topic_name
  pattern_type  = "LITERAL"
  principal     = "User:${confluent_service_account.connector.id}"
  operation     = "DESCRIBE"
  permission    = "ALLOW"
  host          = "*"
  rest_endpoint = data.confluent_kafka_cluster.kafka_cluster.rest_endpoint
  credentials {
    key    = confluent_api_key.connector_admin.id
    secret = confluent_api_key.connector_admin.secret
  }

  depends_on = [
    confluent_api_key.connector_admin,
    confluent_kafka_topic.postgres_sink_aggregates_dlq
  ]
}