# Kafka Streams Utilities Module

This module provides common utilities and configuration helpers for Kafka Streams applications in the Parking Garage Streams project.

## Overview

The kstreams-utils module serves as a foundation for all Kafka Streams applications in this project, providing:

- Configuration loading utilities for local and Confluent Cloud environments
- Serialization/deserialization providers for Protocol Buffers
- Common functionality shared by Kafka Streams applications

## Key Components

### KafkaConfigLoader

The `KafkaConfigLoader` class provides utilities for loading and configuring Kafka Streams applications:

- **Configuration Loading**: Load properties from files (absolute paths or classpath resources)
- **Cloud Configuration**: Set up Kafka Streams for Confluent Cloud with proper authentication
- **Serde Configuration**: Create configuration maps for serializers and deserializers

Key methods:

- `load(filePath: String?)`: Loads properties from a file path
- `setupCloudStreamsConfig(applicationId, base, cloud)`: Sets up Kafka Streams configuration with Confluent Cloud support
- `mkSerdeConfig(schemaRegistryUrl, otherProps)`: Creates a configuration map for serdes

### SerdeProvider

The `SerdeProvider` object provides utilities for creating Protocol Buffer serializers and deserializers:

- **Protobuf Serde Creation**: Create and configure Kafka Protobuf serdes for specific message types
- **Type Safety**: Generic implementation that works with any Protocol Buffer message type

Key methods:

- `createProtobufSerde<T: Message>(clazz, props, isKey)`: Creates a configured KafkaProtobufSerde for a specific message type

## Usage

### Including the Module

To use this module in other Kafka Streams applications, add it as a dependency in your `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":kafka-streams:kstreams-utils"))
}
```

### Loading Configuration

```kotlin
// Create a config loader
val configLoader = KafkaConfigLoader()

// Load local configuration
val localConfig = configLoader.load("kafka-local.properties")

// Load cloud configuration (optional)
val cloudConfig = configLoader.load(System.getProperty("user.home") + "/tools/parking-garage/cc.properties")

// Set up Streams configuration
val streamsConfig = configLoader.setupCloudStreamsConfig(
    applicationId = "parking-space-status",
    base = localConfig,
    cloud = cloudConfig // Pass null to use only local config
)
```

### Creating Protocol Buffer Serdes

```kotlin
// Create a config loader
val configLoader = KafkaConfigLoader()

// Load configuration
val config = configLoader.load("kafka-local.properties")

// Get Schema Registry URL
val schemaRegistryUrl = config.getProperty("schema.registry.url")

// Create serde configuration
val serdeConfig = configLoader.mkSerdeConfig(schemaRegistryUrl, config)

// Create serdes for specific message types
val parkingEventSerde = SerdeProvider.createProtobufSerde(
    ParkingEvent::class,
    serdeConfig
)

val parkingSpaceStatusSerde = SerdeProvider.createProtobufSerde(
    ParkingSpaceStatus::class,
    serdeConfig
)
```

## Configuration Files

The module includes a sample Kafka configuration file for local development:

- `src/main/resources/kafka-local.properties`: Default configuration for local Kafka and Schema Registry

Example configuration:

```properties
bootstrap.servers=localhost:9092
schema.registry.url=http://localhost:8081
```

## Confluent Cloud Support

The module provides built-in support for Confluent Cloud with secure authentication:

- **Environment Variables**: Uses environment variables for sensitive credentials
  - `CC_BROKER`: Confluent Cloud broker URL
  - `CC_SCHEMA_REGISTRY_URL`: Confluent Cloud Schema Registry URL
  - `KAFKA_KEY_ID` and `KAFKA_KEY_SECRET`: Kafka API credentials
  - `SCHEMA_REGISTRY_KEY_ID` and `SCHEMA_REGISTRY_KEY_SECRET`: Schema Registry credentials

- **Authentication**: Configures SASL_SSL authentication for Kafka and basic authentication for Schema Registry

## Dependencies

The module has the following dependencies:

- **Kafka Streams**: Core Kafka Streams library
- **Confluent Kafka Streams Protobuf Serde**: For Protocol Buffer serialization
- **SLF4J**: For logging

## Build

To build the module:

```bash
./gradlew :kafka-streams:kstreams-utils:build
```

To run tests:

```bash
./gradlew :kafka-streams:kstreams-utils:test
```

## Integration with Other Modules

This module provides utilities for all Kafka Streams applications in the project:

- **parking-space-status**: Uses configuration loading and serde utilities
- **row-aggregates**: Uses configuration loading and serde utilities  
- **zone-statistics**: Uses configuration loading and serde utilities
- **utils**: Depends on Protocol Buffer definitions and data models