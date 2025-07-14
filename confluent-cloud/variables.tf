variable "cloud_provider" {
  type = string
  description = "cloud provider for Confluent Cloud"
  default = "AWS"
}

variable "cloud_region" {
  type = string
  description = "cloud provider region"
  default = "us-east-2"
}

variable "org_id" {
  type = string
}

variable "cc_env_display_name" {
  type = string
  description = "Name of Confluent Cloud Environment to Manage"
  default = "parking_garage_streams"
}

variable "cc_kafka_cluster_name" {
  type = string
  description = "Name of Confluent Cloud Kafka Cluster to Manage"
  default = "parking_stuff"
}