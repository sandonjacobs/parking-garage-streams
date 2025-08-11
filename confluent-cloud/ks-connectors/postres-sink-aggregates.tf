
resource "confluent_kafka_topic" "postgres_sink_aggregates_dlq" {
  kafka_cluster {
    id = data.confluent_kafka_cluster.kafka_cluster.id
  }
  rest_endpoint = data.confluent_kafka_cluster.kafka_cluster.rest_endpoint

  topic_name = "dlq-pg-sink-ggregates"
  partitions_count = 3
  config = {
    "cleanup.policy" = "delete"
    "retention.ms" = 604800000
  }

  credentials {
    key    = confluent_api_key.connector_admin.id
    secret = confluent_api_key.connector_admin.secret
  }

  lifecycle {
    prevent_destroy = false
  }

  depends_on = [
    confluent_service_account.connector_admin,
    confluent_service_account.connector
  ]

}

resource "confluent_connector" "postgres_sink_aggregates" {
  environment {
    id = data.confluent_environment.cc_env.id
  }

  kafka_cluster {
    id = data.confluent_kafka_cluster.kafka_cluster.id
  }

  config_sensitive = {
    "connection.password"             = var.database_password
  }

  config_nonsensitive = {
    "name"                            = "KS-AggregationsSinkToPostgres"
    "connector.class"                 = "PostgresSink"
    "connection.host"                 = var.postgres_cluster_endpoint
    "connection.port"                 = var.postgres_cluster_port
    "connection.user"                 = var.database_user
    "auto.create"                     = true
    "auto.evolve"                     = "true"
    "cloud.environment"               = "prod"
    "cloud.provider"                  = data.confluent_kafka_cluster.kafka_cluster.cloud
    "db.name"                         = var.postgres_database_name
    "input.data.format"               = "PROTOBUF"
    "input.key.format"                = "STRING"
    "insert.mode"                     = "UPSERT"
    "kafka.auth.mode"                 = "SERVICE_ACCOUNT"
    "kafka.endpoint"                  = data.confluent_kafka_cluster.kafka_cluster.bootstrap_endpoint
    "kafka.region"                    = data.confluent_kafka_cluster.kafka_cluster.region
    "kafka.service.account.id"        = confluent_service_account.connector.id
    "pk.mode"                         = "record_key"
    "table.name.format"               = "parking.$${topic}"
    "tasks.max"                       = var.sink_connector_tasks_max
    "topics"                          = "${var.TOPIC_ROW_AGGREGATES_NAME},${var.TOPIC_ZONE_AGGREGATES_NAME}"
    "transforms"                      = "Flattener,RenameFields,DropUnusedFlattenedFields"
    "transforms.Flattener.delimiter"  = "_"
    "transforms.Flattener.type"       = "org.apache.kafka.connect.transforms.Flatten$Value"
    "transforms.RenameFields.renames" = "carStatus_capacity:car_capacity,carStatus_occupied:car_occupied,handicapStatus_capacity:handicap_capacity,handicapStatus_occupied:handicap_occupied,motorcycleStatus_capacity:motorcycle_capacity,motorcycleStatus_occupied:motorcycle_occupied"
    "transforms.RenameFields.type"    = "io.confluent.connect.transforms.ReplaceField$Value"
    "transforms.DropUnusedFlattenedFields.type"      = "org.apache.kafka.connect.transforms.ReplaceField$Value"
    "transforms.DropUnusedFlattenedFields.exclude"   = "carStatus_vehicleType,handicapStatus_vehicleType,motorcycleStatus_vehicleType"
    "errors.deadletterqueue.topic.name" = confluent_kafka_topic.postgres_sink_aggregates_dlq.topic_name
  }

  # depends_on = [
  #   confluent_service_account.connector,
  #   confluent_role_binding.connector_resource_owner,
  #   confluent_role_binding.connector_topic_developer_read,
  #   confluent_role_binding.connector_topic_developer_write
  # ]
  depends_on = [
    confluent_service_account.connector,
    confluent_kafka_topic.postgres_sink_aggregates_dlq
  ]

}

resource "confluent_kafka_acl" "postgres_sink_aggregates_group_read" {
  kafka_cluster {
    id            = data.confluent_kafka_cluster.kafka_cluster.id
  }
  credentials {
    key    = confluent_api_key.connector_admin.id
    secret = confluent_api_key.connector_admin.secret
  }

  resource_type   = "GROUP"
  resource_name   = "connect-lcc-${confluent_connector.postgres_sink_aggregates.id}"  # interpolate the connector's ID
  pattern_type    = "LITERAL"
  principal       = "User:${confluent_service_account.connector.id}"
  host            = "*"
  operation       = "READ"
  permission      = "ALLOW"

}

resource "confluent_kafka_acl" "cpostgres_sink_aggregates_group_describe" {
  kafka_cluster {
    id            = data.confluent_kafka_cluster.kafka_cluster.id
  }
  credentials {
    key    = confluent_api_key.connector_admin.id
    secret = confluent_api_key.connector_admin.secret
  }
  resource_type   = "GROUP"
  resource_name   = "connect-lcc-${confluent_connector.postgres_sink_aggregates.id}"  # interpolate the connector's ID
  pattern_type    = "LITERAL"
  principal       = "User:${confluent_service_account.connector.id}"
  host            = "*"
  operation       = "DESCRIBE"
  permission      = "ALLOW"

}

resource "confluent_kafka_acl" "postgres_sink_aggregates_group_delete" {
  kafka_cluster {
    id            = data.confluent_kafka_cluster.kafka_cluster.id
  }
  credentials {
    key    = confluent_api_key.connector_admin.id
    secret = confluent_api_key.connector_admin.secret
  }
  resource_type   = "GROUP"
  resource_name   = "connect-lcc-${confluent_connector.postgres_sink_aggregates.id}"  # interpolate the connector's ID
  pattern_type    = "LITERAL"
  principal       = "User:${confluent_service_account.connector.id}"
  host            = "*"
  operation       = "DELETE"
  permission      = "ALLOW"

}

resource "confluent_kafka_acl" "connector-create-on-dlq-lcc-topics" {
  kafka_cluster {
    id = data.confluent_kafka_cluster.kafka_cluster.id
  }
  resource_type = "TOPIC"
  resource_name = confluent_kafka_topic.postgres_sink_aggregates_dlq.topic_name
  pattern_type  = "PREFIXED"
  principal     = "User:${confluent_service_account.connector.id}"
  host          = "*"
  operation     = "CREATE"
  permission    = "ALLOW"
  rest_endpoint = data.confluent_kafka_cluster.kafka_cluster.rest_endpoint
  credentials {
    key    = confluent_api_key.connector_admin.id
    secret = confluent_api_key.connector_admin.secret
  }

}

resource "confluent_kafka_acl" "connector-write-on-dlq-lcc-topics" {
  kafka_cluster {
    id = data.confluent_kafka_cluster.kafka_cluster.id
  }
  resource_type = "TOPIC"
  resource_name = confluent_kafka_topic.postgres_sink_aggregates_dlq.topic_name
  pattern_type  = "PREFIXED"
  principal     = "User:${confluent_service_account.connector.id}"
  host          = "*"
  operation     = "WRITE"
  permission    = "ALLOW"
  rest_endpoint = data.confluent_kafka_cluster.kafka_cluster.rest_endpoint
  credentials {
    key    = confluent_api_key.connector_admin.id
    secret = confluent_api_key.connector_admin.secret
  }
}
