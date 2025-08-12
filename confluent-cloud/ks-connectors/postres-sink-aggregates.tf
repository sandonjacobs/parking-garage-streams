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
    "auto.create"                     = "true"
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
    "pk.mode"                         = "record_value"
    "pk.fields"                       = "id"
    "table.name.format"               = "parking.$${topic}"
    "tasks.max"                       = tostring(var.sink_connector_tasks_max)
    "topics"                          = "${var.TOPIC_ROW_AGGREGATES_NAME},${var.TOPIC_ZONE_AGGREGATES_NAME}"
    "transforms"                      = "Flattener,RenameFields,DropUnusedFlattenedFields"
    "transforms.Flattener.delimiter"  = "_"
    "transforms.Flattener.type"       = "org.apache.kafka.connect.transforms.Flatten$Value"
    "transforms.RenameFields.renames" = "carStatus_capacity:car_capacity,carStatus_occupied:car_occupied,handicapStatus_capacity:handicap_capacity,handicapStatus_occupied:handicap_occupied,motorcycleStatus_capacity:motorcycle_capacity,motorcycleStatus_occupied:motorcycle_occupied"
    "transforms.RenameFields.type"    = "io.confluent.connect.transforms.ReplaceField$Value"
    "transforms.DropUnusedFlattenedFields.type"      = "org.apache.kafka.connect.transforms.ReplaceField$Value"
    "transforms.DropUnusedFlattenedFields.exclude"   = "carStatus_vehicleType,handicapStatus_vehicleType,motorcycleStatus_vehicleType"
    # DLQ configuration
    "errors.tolerance"                         = "all"
    "errors.deadletterqueue.topic.name"        = confluent_kafka_topic.postgres_sink_aggregates_dlq.topic_name
    "errors.deadletterqueue.context.headers.enable" = "true"
  }

  depends_on = [
    confluent_service_account.connector,
    confluent_service_account.connector_admin,
    confluent_kafka_acl.connector_consumer_group_read,
    confluent_kafka_acl.source_topic_row_aggregates_describe,
    confluent_kafka_acl.source_topic_row_aggregates_read,
    confluent_kafka_acl.source_topic_zone_aggregates_describe,
    confluent_kafka_acl.source_topic_zone_aggregates_read,
    confluent_kafka_acl.dlq_describe, confluent_kafka_acl.dlq_write,
    confluent_kafka_topic.postgres_sink_aggregates_dlq
  ]

  lifecycle {
    prevent_destroy = false
  }
}