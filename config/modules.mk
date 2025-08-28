# Module definitions for Parking Garage Streams
# This file defines all modules and their relationships

# Main module categories
MODULES := infrastructure datagen kafka-streams utils

# Infrastructure modules (Terraform-based)
INFRASTRUCTURE_MODULES := confluent-cloud aws

# Confluent Cloud sub-modules
CONFLUENT_CLOUD_MODULES := base-env ks-connectors

# Kafka Streams sub-modules
KAFKA_STREAMS_MODULES := kstreams-utils parking-space-status row-aggregates zone-statistics

KAFKA_STREAMS_TOPOLOGIES := parking-space-status row-aggregates zone-statistics



# Common actions for different module types
INFRA_ACTIONS := deploy destroy plan init
APP_ACTIONS := build run stop test clean
CONFIG_ACTIONS := generate validate

# Default configuration
CONFIG_DIR ?= $(HOME)/tools/parking-garage
ENV_FILE ?= .env
GRADLE_CMD ?= ./gradlew

# Module paths
INFRASTRUCTURE_PATH := .
CONFLUENT_CLOUD_PATH := confluent-cloud
AWS_PATH := aws
DATAGEN_PATH := datagen
KAFKA_STREAMS_PATH := kafka-streams

UTILS_PATH := utils
