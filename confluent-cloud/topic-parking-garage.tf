resource "confluent_kafka_topic" "parking_garage" {

  kafka_cluster {
    id = confluent_kafka_cluster.kafka_cluster.id
  }
  rest_endpoint = confluent_kafka_cluster.kafka_cluster.rest_endpoint

  topic_name = "parking-garage"
  partitions_count = 6
  config = {
    "cleanup.policy" = "compact"
  }

  lifecycle {
    prevent_destroy = false
  }
}