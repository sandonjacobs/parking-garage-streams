resource "confluent_kafka_topic" "parking_space_status" {

  kafka_cluster {
    id = confluent_kafka_cluster.kafka_cluster.id
  }
  rest_endpoint = confluent_kafka_cluster.kafka_cluster.rest_endpoint

  topic_name = "parking-space-status"
  partitions_count = 6
  config = {
    "cleanup.policy" = "compact"
  }

  credentials {
    key    = confluent_api_key.app-manager-kafka-api-key.id
    secret = confluent_api_key.app-manager-kafka-api-key.secret
  }

  lifecycle {
    prevent_destroy = false
  }
}