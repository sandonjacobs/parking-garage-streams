output "ORG_ID" {
  value = trim(var.org_id, "\"")
}

output "CC_ENV_DISPLAY_NAME" {
  value = confluent_environment.cc_env.display_name
}

output "CC_ENV_ID" {
  value = confluent_environment.cc_env.id
}

output "CLOUD_PROVIDER" {
  value = var.cloud_provider
}

output "CLOUD_REGION" {
  value = var.cloud_region
}

output "CC_KAFKA_CLUSTER_ID" {
  value = confluent_kafka_cluster.kafka_cluster.id
}

output "CC_BROKER" {
  value = replace(confluent_kafka_cluster.kafka_cluster.bootstrap_endpoint, "SASL_SSL://", "")
}

output "CC_BROKER_URL" {
  value = confluent_kafka_cluster.kafka_cluster.rest_endpoint
}

output "CC_SCHEMA_REGISTRY_ID" {
  value = data.confluent_schema_registry_cluster.advanced.id
}

output "CC_SCHEMA_REGISTRY_URL" {
  value = data.confluent_schema_registry_cluster.advanced.rest_endpoint
}

output "SCHEMA_REGISTRY_KEY_ID" {
  value = confluent_api_key.env-manager-schema-registry-api-key.id
  sensitive = false
}

output "SCHEMA_REGISTRY_KEY_SECRET" {
  value = nonsensitive(confluent_api_key.env-manager-schema-registry-api-key.secret)
}

output "KAFKA_KEY_ID" {
  value = confluent_api_key.app-manager-kafka-api-key.id
  sensitive = false
}

output "KAFKA_KEY_SECRET" {
  value = nonsensitive(confluent_api_key.app-manager-kafka-api-key.secret)
}

output "FLINK_KEY_ID" {
  value = confluent_api_key.env-manager-flink-api-key.id
}

output "FLINK_KEY_SECRET" {
  value = nonsensitive(confluent_api_key.env-manager-flink-api-key.secret)
}

output "FLINK_ENV_ID" {
  value = confluent_environment.cc_env.id
}

output "FLINK_COMPUTE_POOL_ID" {
  value = confluent_flink_compute_pool.parking_streams_pool.id
}
