# Parking Garage Streams

A multi-module project for processing parking garage events using stream processing technologies like Kafka Streams and Apache Flink.

## Overview

This project demonstrates real-time processing of parking garage events, including vehicle entry/exit tracking, parking space status updates, and aggregated statistics. It uses Protocol Buffers for data serialization and Confluent Schema Registry for schema management.

## Build and Run

This project uses [Gradle](https://gradle.org/).
To build and run the application, use the *Gradle* tool window by clicking the Gradle icon in the right-hand toolbar,
or run it directly from the terminal:

* Run `./gradlew build` to build the application.
* Run `./gradlew check` to run all checks, including tests.
* Run `./gradlew clean` to clean all build outputs.

Note the usage of the Gradle Wrapper (`./gradlew`).
This is the suggested way to use Gradle in production projects.

[Learn more about the Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html).

[Learn more about Gradle tasks](https://docs.gradle.org/current/userguide/command_line_interface.html#common_tasks).

## Project Structure

This project follows a multi-module setup with the following modules:

### utils

Common utilities and data models using Protocol Buffers:
- Protocol Buffer definitions for parking events, spaces, garages, and vehicles
- Data model factories for generating test data
- DTO classes and conversion utilities
- Shared code used by all other modules

### datagen

Data generation application for simulating parking events:
- Generates realistic parking garage events and sends them to Apache Kafka
- Uses Confluent's Protobuf serializer for efficient message serialization
- Provides a REST API to control event generation
- Built as a Spring Boot application

### kafka-streams

Kafka Streams implementation for processing parking events, with the following sub-modules:

#### kstreams-utils

Utility functions for Kafka Streams applications:
- Configuration loading utilities for local and Confluent Cloud environments
- Serialization/deserialization providers for Protocol Buffers
- Common functionality shared by Kafka Streams applications

#### parking-space-status

Kafka Streams application for processing parking space status:
- Processes parking events and maintains a materialized view of parking space statuses
- Provides a REST API for querying the current state of parking spaces
- Uses state stores for fast, real-time queries

#### row-aggregates

Kafka Streams application for aggregating parking space statuses by row:
- Aggregates parking space statuses to provide row-level occupancy information
- Maintains counts of occupied spaces by vehicle type (car, handicap, motorcycle)
- Calculates capacity and availability for each row in a parking garage

### confluent-cloud

Terraform configurations for Confluent Cloud:
- Setting up Kafka clusters in Confluent Cloud
- Creating topics with appropriate configurations
- Configuring Schema Registry

## Dependencies

This project uses a version catalog (see `gradle/libs.versions.toml`) to declare and version dependencies
and both a build cache and a configuration cache (see `gradle.properties`).

The shared build logic was extracted to a convention plugin located in `buildSrc`.