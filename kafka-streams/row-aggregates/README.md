# Parking Row Aggregation Processor

This Kafka Streams application aggregates parking space statuses by row to provide real-time occupancy information for each row in a parking garage.

## Overview

The row-aggregates module is a Kafka Streams application that:

- Consumes parking space status updates from a Kafka topic
- Joins this data with parking garage information
- Aggregates the status information by row
- Maintains counts of occupied spaces by vehicle type (car, handicap, motorcycle)
- Calculates capacity and availability for each row in a parking garage
- Produces aggregated row status updates to a dedicated Kafka topic

## Data Flow

```
┌─────────────────────┐     ┌─────────────────────┐     ┌─────────────────────┐
│                     │     │                     │     │                     │
│ parking-space-status│────▶│  RowAggregation     │────▶│ parking-row-        │
│      (topic)        │     │     Topology        │     │ aggregates (topic)  │
│                     │     │                     │     │                     │
└─────────────────────┘     └─────────────────────┘     └─────────────────────┘
          ▲                           │
          │                           │
          │                           ▼
┌─────────────────────┐     ┌─────────────────────┐
│                     │     │                     │
│   parking-garage    │────▶│  GlobalKTable       │
│      (topic)        │     │                     │
│                     │     │                     │
└─────────────────────┘     └─────────────────────┘
```

### Input

- **Primary Topic**: `parking-space-status`
  - **Key**: String (parking space ID)
  - **Value**: `ParkingSpaceStatus` protobuf message
  
- **Reference Topic**: `parking-garage`
  - **Key**: String (garage ID)
  - **Value**: `ParkingGarage` protobuf message

### Output

- **Topic**: `parking-row-aggregates`
- **Key**: String (row ID in format `{garageId}-{zoneId}-{rowId}`)
- **Value**: `ParkingGarageRowStatus` protobuf message containing:
  - Row identification (garage ID, zone ID, row ID)
  - Status for each vehicle type (car, handicap, motorcycle)
  - Capacity and occupied count for each vehicle type

## Topology

The Kafka Streams topology is defined in `RowAggregationTopology.kt` and performs the following operations:

1. **Stream**: Consumes status updates from the `parking-space-status` topic
2. **GlobalKTable**: Loads garage information from the `parking-garage` topic
3. **Join**: Joins the status stream with the garage table using the garage ID as the key
4. **Group**: Groups the joined data by row ID
5. **Aggregate**: Aggregates the status information by row, maintaining counts of occupied spaces by vehicle type
6. **Produce**: Sends aggregated row status updates to the `parking-row-aggregates` topic

## Implementation

### Key Classes

- **RowAggregationTopology**: Defines the Kafka Streams topology for processing status updates
- **CapacityCalculator**: Calculates the capacity of a row for different vehicle types
- **MainClassKt**: Entry point that configures and starts the Kafka Streams application

### Capacity Calculation

The application calculates the capacity of each row using the `CapacityCalculator`:

```kotlin
fun findCapacityOfRow(garage: ParkingGarage, space: ParkingSpace): RowCapacities {
    // Find the zone in the garage
    val zone = garage.getParkingZonesList().find { it.getId() == space.zoneId }
    
    // Find the row in the zone
    val row = zone?.getParkingRowsList()?.find { it.getId() == space.rowId }
    
    // Count the spaces of each type in the row
    return RowCapacities(
        carCapacity = row?.getParkingSpacesList()?.count { it.getType() == VehicleType.CAR } ?: 0,
        handicapCapacity = row?.getParkingSpacesList()?.count { it.getType() == VehicleType.HANDICAP } ?: 0,
        motorcycleCapacity = row?.getParkingSpacesList()?.count { it.getType() == VehicleType.MOTORCYCLE } ?: 0
    )
}
```

### Status Aggregation

The application updates the row status based on space status changes:

```kotlin
when (vehicleType) {
    VehicleType.CAR -> {
        if (spaceStatus.status == SpaceStatus.OCCUPIED) {
            carStatusBuilder.setOccupied(carStatusBuilder.occupied + 1)
        } else if (spaceStatus.status == SpaceStatus.VACANT && carStatusBuilder.occupied > 0) {
            carStatusBuilder.setOccupied(carStatusBuilder.occupied - 1)
        }
    }
    // Similar logic for HANDICAP and MOTORCYCLE
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
application.id=kstreams-row-aggregations
default.key.serde=org.apache.kafka.common.serialization.Serdes$StringSerde
default.value.serde=io.confluent.kafka.streams.serdes.protobuf.KafkaProtobufSerde
```

## Building and Running

### Build

To build the application:

```bash
./gradlew :kafka-streams:row-aggregates:build
```

This creates a fat JAR with all dependencies included.

### Run

To run the application locally:

```bash
java -jar kafka-streams/row-aggregates/build/libs/zone-aggregates.jar
```

With custom configuration:

```bash
java -jar kafka-streams/row-aggregates/build/libs/zone-aggregates.jar \
  --base /path/to/custom-config.properties
```

With Confluent Cloud configuration:

```bash
java -jar kafka-streams/row-aggregates/build/libs/zone-aggregates.jar \
  --cloud /path/to/cc-config.properties
```

## Testing

The application includes unit tests for the topology using Kafka Streams' `TopologyTestDriver`:

```bash
./gradlew :kafka-streams:row-aggregates:test
```

The tests verify:
- Correct joining of space status with garage information
- Proper aggregation of status information by row
- Accurate capacity calculation for different vehicle types
- Correct counting of occupied spaces

## Dependencies

The module has the following dependencies:

- **kstreams-utils**: Common utilities for Kafka Streams applications
- **utils**: Protocol Buffer definitions and data models
- **Kafka Streams**: Core Kafka Streams library
- **Confluent Kafka Streams Protobuf Serde**: For Protocol Buffer serialization
- **Kotlinx CLI**: For command-line argument parsing

## Integration with Other Modules

This module:
- Consumes status updates produced by the **parking-space-status** module
- Uses garage information that may be produced by the **datagen** module
- Uses common utilities from the **kstreams-utils** module
- Uses Protocol Buffer definitions from the **utils** module
- Integrates with **confluent-cloud** infrastructure for cloud deployment
- Can be used with **aws** infrastructure for storing aggregated results

## Use Cases

The aggregated row status information can be used for:

- Displaying available spaces by row in a parking garage
- Directing drivers to rows with available spaces of the appropriate type
- Analyzing parking patterns and optimizing space allocation
- Generating alerts when rows are near capacity