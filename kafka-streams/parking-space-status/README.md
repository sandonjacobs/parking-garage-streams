# Parking Space Status Processor

This Kafka Streams application processes parking events and maintains a real-time view of parking space statuses in a parking garage.

## Overview

The parking-space-status module is a Kafka Streams application that:

- Consumes parking events (vehicle entry/exit) from a Kafka topic
- Processes these events to track the status of each parking space
- Maintains a materialized view of all parking spaces with their current status
- Produces status updates to a dedicated Kafka topic

## Data Flow

```
┌─────────────────┐     ┌─────────────────────┐     ┌─────────────────────┐
│                 │     │                     │     │                     │
│  parking-events │────▶│ ParkingSpaceStatus  │────▶│ parking-space-status│
│     (topic)     │     │     Topology        │     │       (topic)       │
│                 │     │                     │     │                     │
└─────────────────┘     └─────────────────────┘     └─────────────────────┘
```

### Input

- **Topic**: `parking-events`
- **Key**: String (parking space ID)
- **Value**: `ParkingEvent` protobuf message containing:
  - Event type (ENTER/EXIT)
  - Parking space details
  - Vehicle information
  - Timestamp

### Output

- **Topic**: `parking-space-status`
- **Key**: String (parking space ID)
- **Value**: `ParkingSpaceStatus` protobuf message containing:
  - Parking space details
  - Current status (VACANT/OCCUPIED)
  - Vehicle information (for occupied spaces)
  - Last updated timestamp

## Topology

The Kafka Streams topology is defined in `ParkingSpaceStatusTopology.kt` and performs the following operations:

1. **Stream**: Consumes events from the `parking-events` topic
2. **Process**: Maps each event to a status update based on the event type:
   - ENTER events → OCCUPIED status (with vehicle information)
   - EXIT events → VACANT status (without vehicle information)
3. **Produce**: Sends status updates to the `parking-space-status` topic

## Implementation

### Key Classes

- **ParkingSpaceStatusTopology**: Defines the Kafka Streams topology for processing events
- **MainClass**: Entry point that configures and starts the Kafka Streams application

### Status Mapping Logic

The application maps parking events to space statuses using the following logic:

```kotlin
when (event.type) {
    ParkingEventType.ENTER -> {
        // Mark space as OCCUPIED with vehicle information
        event.mkStatus(SpaceStatus.OCCUPIED)
    }
    else -> {
        // Mark space as VACANT without vehicle information
        event.mkStatus(SpaceStatus.VACANT)
    }
}
```

## Configuration

The application can be configured using properties files and command-line arguments:

### Command-Line Arguments

- `-b, --base`: Path to base configuration file (default: `kafka-local.properties`)
- `-c, --cloud`: Path to Confluent Cloud configuration override file (optional)

### Configuration Files

- **Local Development**: `src/main/resources/kafka-local.properties`
- **Confluent Cloud**: External file specified with `-c` option

### Example Configuration

```properties
# Local Kafka configuration
bootstrap.servers=localhost:9092
schema.registry.url=http://localhost:8081

# Streams configuration
application.id=kstreams-parking-space-status
default.key.serde=org.apache.kafka.common.serialization.Serdes$StringSerde
default.value.serde=io.confluent.kafka.streams.serdes.protobuf.KafkaProtobufSerde
```

## Building and Running

### Build

To build the application:

```bash
./gradlew :kafka-streams:parking-space-status:build
```

This creates a fat JAR with all dependencies included.

### Run

To run the application locally:

```bash
java -jar kafka-streams/parking-space-status/build/libs/parking-space-status.jar
```

With custom configuration:

```bash
java -jar kafka-streams/parking-space-status/build/libs/parking-space-status.jar \
  --base /path/to/custom-config.properties
```

With Confluent Cloud configuration:

```bash
java -jar kafka-streams/parking-space-status/build/libs/parking-space-status.jar \
  --cloud /path/to/cc-config.properties
```

## Testing

The application includes unit tests for the topology using Kafka Streams' `TopologyTestDriver`:

```bash
./gradlew :kafka-streams:parking-space-status:test
```

The tests verify:
- Correct processing of ENTER events (space marked as OCCUPIED)
- Correct processing of EXIT events (space marked as VACANT)
- Proper inclusion of vehicle information for occupied spaces

## Dependencies

The module has the following dependencies:

- **kstreams-utils**: Common utilities for Kafka Streams applications
- **utils**: Protocol Buffer definitions and data models
- **Kafka Streams**: Core Kafka Streams library
- **Confluent Kafka Streams Protobuf Serde**: For Protocol Buffer serialization
- **Kotlinx CLI**: For command-line argument parsing

## Integration with Other Modules

This module:
- Consumes events produced by the **datagen** module
- Produces status updates consumed by the **row-aggregates** module
- Uses common utilities from the **kstreams-utils** module
- Uses Protocol Buffer definitions from the **utils** module