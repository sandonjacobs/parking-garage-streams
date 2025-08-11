resource "confluent_kafka_topic" "postgres_sink_aggregates_dlq" {
  kafka_cluster { id = data.confluent_kafka_cluster.kafka_cluster.id }
  rest_endpoint = data.confluent_kafka_cluster.kafka_cluster.rest_endpoint

  topic_name       = "postgres_sink_aggregates_dlq"
  partitions_count = 3
  config = {
    "cleanup.policy" = "delete"
  }

  credentials {
    key    = confluent_api_key.connector_admin.id
    secret = confluent_api_key.connector_admin.secret
  }

  lifecycle {
    prevent_destroy = false
  }
}
