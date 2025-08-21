# Parking Garage Data Generator

This module provides a data generation application for simulating parking events in a multi-level parking garage environment. It generates realistic parking events (vehicle entries and exits) and sends them to Apache Kafka for processing by stream processing applications.

## Overview

The datagen module is a Spring Boot application that:

- Loads parking garage configurations from YAML files
- Creates realistic parking garage structures with zones, rows, and spaces
- Generates random parking events (vehicle entries and exits) with realistic timing patterns
- Sends both garage information and parking events to Kafka topics
- Provides a REST API for controlling event generation and accessing garage information
- Supports both local development and Confluent Cloud deployment

The application simulates realistic traffic patterns by adjusting the event generation rate based on the time of day (higher during rush hours, lower during off-peak hours).

## Application Structure

### Key Components

- **DataGeneratorApplication**: Main application class that initializes the application and starts event generation
- **ParkingGarageService**: Creates and manages parking garage structures based on configuration
- **ParkingEventGenerator**: Generates parking events with realistic timing patterns
- **ParkingEventProducer**: Sends parking events to Kafka
- **ParkingGarageProducer**: Sends parking garage information to Kafka
- **REST Controllers**:
  - **DataGeneratorController**: Provides endpoints for accessing garage information
  - **ParkingEventController**: Provides endpoints for controlling event generation

### Data Flow

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│                 │     │                 │     │                 │
│  Configuration  │────▶│  ParkingGarage  │────▶│  parking-garage │
│  (YAML files)   │     │     Service     │     │     (topic)     │
│                 │     │                 │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                │
                                ▼
                        ┌─────────────────┐     ┌─────────────────┐
                        │                 │     │                 │
                        │ ParkingEvent    │────▶│  parking-events │
                        │   Generator     │     │     (topic)     │
                        │                 │     │                 │
                        └─────────────────┘     └─────────────────┘
                                │
                                ▼
                        ┌─────────────────┐
                        │                 │
                        │    REST API     │
                        │                 │
                        └─────────────────┘
```

## Configuration

### Application Configuration

The application is configured using Spring Boot's configuration system:

- **application.yml**: Default configuration for local development
- **application-cc.yml**: Configuration for Confluent Cloud deployment
- **parking-garages.yml**: Parking garage definitions

#### Local Development Configuration (application.yml)

```yaml
server:
  port: 8080

spring:
  application:
    name: parking-garage-data-generator
  config:
    import: classpath:parking-garages.yml
  kafka:
    bootstrap-servers: localhost:9092
    schema-registry:
      url: http://localhost:8081

app:
  kafka:
    topic:
      parking-garage: parking-garage
      parking-events: parking-events
```

#### Confluent Cloud Configuration (application-cc.yml)

For Confluent Cloud deployment, the application uses the `cc` profile which:

1. Imports external configuration from `${user.home}/tools/parking-garage/cc.properties`
2. Uses environment variables for sensitive information:
   - `CC_BROKER`: Confluent Cloud broker URL
   - `CC_SCHEMA_REGISTRY_URL`: Confluent Cloud Schema Registry URL
   - `KAFKA_KEY_ID` and `KAFKA_KEY_SECRET`: Kafka API credentials
   - `SCHEMA_REGISTRY_KEY_ID` and `SCHEMA_REGISTRY_KEY_SECRET`: Schema Registry credentials

To run with Confluent Cloud configuration:

```bash
./gradlew :datagen:bootRun --args='--spring.profiles.active=cc'
```

### Parking Garage Configuration

Parking garages are configured in `parking-garages.yml` with a hierarchical structure:

```yaml
parking-garages:
  garages:
    - id: "garage-1"
      name: "Downtown Parking Garage"
      location:
        latitude: "35.7796"
        longitude: "-78.6382"
        address: "123 Main Street"
      zones:
        - id: "ground-floor"
          name: "Ground Floor"
          spaces:
            handicap: 8
            motorcycle: 4
            default: 88
        - id: "level-1"
          name: "Level 1"
          rows:
            - id: "row-a"
              name: "Row A"
              spaces:
                handicap: 3
                motorcycle: 2
                default: 25
```

Each garage has:
- A unique ID and name
- Geographic location information
- One or more zones (floors or sections)
- Each zone has either spaces directly or rows of spaces
- Spaces are categorized by type: handicap, motorcycle, and default (regular car)

## REST API

The application provides a REST API for controlling event generation and accessing garage information.

### Garage Information Endpoints

#### Get All Garages

```
GET /api/data-generator/garages
```

Returns a list of all configured parking garages with their structure.

#### Get Garage by ID

```
GET /api/data-generator/garages/{garageId}
```

Returns a specific parking garage by ID.

#### Get Garage Statistics

```
GET /api/data-generator/garages/{garageId}/statistics
```

Returns statistics about a specific parking garage, including counts of different types of spaces.

#### Get All Garage Statistics

```
GET /api/data-generator/statistics
```

Returns aggregated statistics for all parking garages.

### Event Generation Control Endpoints

#### Stop Event Generation for a Garage

```
POST /api/parking-events/stop/{garageId}
```

Stops generating parking events for a specific garage.

#### Stop All Event Generation

```
POST /api/parking-events/stop
```

Stops generating parking events for all garages.

## Building and Running

### Prerequisites

- JDK 21
- Apache Kafka (local or Confluent Cloud)
- Confluent Schema Registry

### Build

To build the application:

```bash
./gradlew :datagen:build
```

### Run Locally

To run the application with local Kafka:

```bash
./gradlew :datagen:bootRun
```

### Run with Confluent Cloud

To run with Confluent Cloud:

```bash
./gradlew :datagen:bootRun --args='--spring.profiles.active=cc'
```

Make sure to set the required environment variables:

```bash
export CC_BROKER=<confluent-cloud-broker-url>
export CC_SCHEMA_REGISTRY_URL=<confluent-cloud-schema-registry-url>
export KAFKA_KEY_ID=<kafka-api-key>
export KAFKA_KEY_SECRET=<kafka-api-secret>
export SCHEMA_REGISTRY_KEY_ID=<schema-registry-api-key>
export SCHEMA_REGISTRY_KEY_SECRET=<schema-registry-api-secret>
```

## Testing

The module includes integration tests that verify the Kafka producer functionality:

```bash
./gradlew :datagen:test
```

The tests use Spring Kafka's test utilities to verify that:
- Parking garage information is correctly sent to Kafka
- Parking events are correctly generated and sent to Kafka

## Integration with Other Modules

This module:
- Uses Protocol Buffer definitions from the **utils** module
- Produces data consumed by the **kafka-streams** modules
  - Garage information is consumed by the **row-aggregates** and **zone-statistics** modules
  - Parking events are consumed by the **parking-space-status** module
- Will provide data for the **flink-table-api** module (when implemented)
- Integrates with **confluent-cloud** infrastructure for cloud deployment

## Dependencies

The module has the following dependencies:

- **Spring Boot**: Core framework and web server
- **Spring Kafka**: For Kafka integration
- **Confluent Schema Registry**: For Protocol Buffer serialization
- **utils**: For Protocol Buffer definitions and factory classes
- **Kotlin Coroutines**: For asynchronous event generation