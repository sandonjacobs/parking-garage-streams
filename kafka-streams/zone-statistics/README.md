# Parking Zone Statistics Processor

This Kafka Streams application aggregates parking space statuses by zone to provide real-time occupancy information for each zone in a parking garage.

## Overview

The zone-statistics module is a Kafka Streams application that:

- Consumes parking space status updates from a Kafka topic
- Joins this data with parking garage information
- Aggregates the status information by zone
- Maintains counts of occupied spaces by vehicle type (car, handicap, motorcycle)
- Calculates capacity and availability for each zone in a parking garage
- Produces aggregated zone status updates to a dedicated Kafka topic

## Data Flow

```
┌─────────────────────┐     ┌─────────────────────┐     ┌─────────────────────┐
│                     │     │                     │     │                     │
│ parking-space-status│────▶│  ZoneStatistics     │────▶│ parking-zone-       │
│      (topic)        │     │     Topology        │     │ statistics (topic)  │
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

- **Topic**: `parking-zone-statistics`
- **Key**: String (zone ID in format `{garageId}-{zoneId}`)
- **Value**: `ParkingGarageRowStatus` protobuf message containing:
  - Zone identification (garage ID, zone ID)
  - Status for each vehicle type (car, handicap, motorcycle)
  - Capacity and occupied count for each vehicle type

## Topology

The Kafka Streams topology is defined in `ZoneStatisticsTopology.kt` and performs the following operations:

1. **Stream**: Consumes status updates from the `parking-space-status` topic
2. **GlobalKTable**: Loads garage information from the `parking-garage` topic
3. **Join**: Joins the status stream with the garage table using the garage ID as the key
4. **Group**: Groups the joined data by zone ID
5. **Aggregate**: Aggregates the status information by zone, maintaining counts of occupied spaces by vehicle type
6. **Produce**: Sends aggregated zone status updates to the `parking-zone-statistics` topic

## Implementation

### Key Classes

- **ZoneStatisticsTopology**: Defines the Kafka Streams topology for processing status updates
- **ZoneCalculator**: Calculates the capacity of a zone for different vehicle types
- **MainClassKt**: Entry point that configures and starts the Kafka Streams application

### Capacity Calculation

The application calculates the capacity of each zone using the `ZoneCalculator`:

```kotlin
fun findCapacityOfZone(garage: ParkingGarage, space: ParkingSpace): ZoneCapacities {
    // Find the zone in the garage
    val zone = garage.getParkingZonesList().find { it.getId() == space.zoneId }
    
    // Count the spaces of each type across all rows in the zone
    var carCapacity = 0
    var handicapCapacity = 0
    var motorcycleCapacity = 0

    // Iterate through all rows in the zone
    for (row in zone?.getParkingRowsList() ?: emptyList()) {
        // Count spaces by type in this row
        carCapacity += row.getParkingSpacesList().count { it.getType() == VehicleType.CAR }
        handicapCapacity += row.getParkingSpacesList().count { it.getType() == VehicleType.HANDICAP }
        motorcycleCapacity += row.getParkingSpacesList().count { it.getType() == VehicleType.MOTORCYCLE }
    }
    
    return ZoneCapacities(
        carCapacity = carCapacity,
        handicapCapacity = handicapCapacity,
        motorcycleCapacity = motorcycleCapacity
    )
}
```

### Status Aggregation

The application updates the zone status based on space status changes:

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
application.id=kstreams-zone-statistics
default.key.serde=org.apache.kafka.common.serialization.Serdes$StringSerde
default.value.serde=io.confluent.kafka.streams.serdes.protobuf.KafkaProtobufSerde
```

## Building and Running

### Build

To build the application:

```bash
./gradlew :kafka-streams:zone-statistics:build
```

This creates a fat JAR with all dependencies included.

### Run

To run the application locally:

```bash
java -jar kafka-streams/zone-statistics/build/libs/zone-statistics.jar
```

With custom configuration:

```bash
java -jar kafka-streams/zone-statistics/build/libs/zone-statistics.jar \
  --base /path/to/custom-config.properties
```

With Confluent Cloud configuration:

```bash
java -jar kafka-streams/zone-statistics/build/libs/zone-statistics.jar \
  --cloud /path/to/cc-config.properties
```

## Testing

The application includes unit tests for the topology using Kafka Streams' `TopologyTestDriver`:

```bash
./gradlew :kafka-streams:zone-statistics:test
```

The tests verify:
- Correct joining of space status with garage information
- Proper aggregation of status information by zone
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

## Use Cases

The aggregated zone status information can be used for:

- Displaying available spaces by zone in a parking garage
- Directing drivers to zones with available spaces of the appropriate type
- Analyzing parking patterns at the zone level
- Generating alerts when zones are near capacity
- Optimizing zone allocation and planning
- Providing zone-level statistics for parking garage management