variable "CLOUD_PROVIDER" {
  type = string
  description = "cloud provider for Confluent Cloud"
  default = "AWS"
}

variable "CLOUD_REGION" {
  type = string
  description = "cloud provider region"
  default = "us-east-2"
}

variable "ORG_ID" {
  type = string
}

variable "CC_ENV_ID" {
  type = string
}

variable "CC_ENV_DISPLAY_NAME" {
  type = string
  description = "Name of Confluent Cloud Environment to Manage"
  default = "parking_garage_streams"
}

variable "CC_KAFKA_CLUSTER_ID" {
  type = string
  description = "Name of Confluent Cloud Kafka Cluster to Manage"
}

variable "TOPIC_ROW_AGGREGATES_ID" {
  type = string
  description = "ID of row aggregates topic"
}

variable "TOPIC_ZONE_AGGREGATES_ID" {
  type = string
  description = "ID of zone aggregates topic"
}

variable "TOPIC_ROW_AGGREGATES_NAME" {
  type = string
  description = "Name of row aggregates topic"
}

variable "TOPIC_ZONE_AGGREGATES_NAME" {
  type = string
  description = "Name of zone aggregates topic"
}

variable "postgres_cluster_endpoint" {
  type = string
  description = "Host Name of Postgres Instance"
}

variable "postgres_cluster_port" {
  type = number
  description = "Post of Postgres Instance"
  default = 5432
}

variable "postgres_database_name" {
  type = string
  description = "Database name"
  default = "parkingstreams"
}

variable "db_master_user" {
  type = string
  description = "Database username"
  default = "postgres"
}

variable "db_master_password" {
  type = string
}

variable "sink_connector_tasks_max" {
  type = number
  description = "Value for task.max for most sink connectors"
  default = 2
}