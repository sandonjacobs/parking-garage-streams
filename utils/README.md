# Parking Garage Utils Module

This module provides common utilities and data models for the Parking Garage Streams project using Protocol Buffers for efficient serialization and data exchange. It serves as the foundational layer that all other modules depend on.

## Overview

The utils module serves as the foundation for the entire project, providing:

- Protocol Buffer schema definitions for all data models
- Generated Java classes from Protocol Buffer schemas
- Factory classes for generating test data
- DTO classes for REST API representations
- Conversion utilities between Protocol Buffer objects and DTOs

## Protocol Buffer Schemas

The module defines the following Protocol Buffer schemas in the `src/main/proto` directory:

### commons.proto

Defines common data structures and events:
- `Location`: Geographic coordinates with latitude and longitude
- `ParkingEventType`: Enum for event types (ENTER, EXIT)
- `ParkingEvent`: Core event message containing event type, parking space, vehicle, and timestamp

### vehicle.proto

Defines vehicle-related data structures:
- `Vehicle`: Vehicle information including ID, license plate, state, and type
- `VehicleType`: Enum for vehicle types (CAR, HANDICAP, MOTORCYCLE)

### parking-space.proto

Defines parking space structure:
- `ParkingSpace`: Individual parking space with ID, row ID, zone ID, garage ID, and vehicle type

### parking-garage.proto

Defines the hierarchical structure of a parking garage:
- `ParkingGarage`: Top-level structure with ID, zones, and location
- `ParkingZone`: Zone within a garage containing rows
- `ParkingRow`: Row within a zone containing spaces

### parking-space-status.proto

Defines the status tracking for parking spaces:
- `SpaceStatus`: Enum for space status (VACANT, OCCUPIED)
- `ParkingSpaceStatus`: Status message with space, status, optional vehicle, and timestamp

### row-aggregate.proto

Defines aggregated status for parking rows:
- `ParkingGarageRowStatus`: Aggregated status for a row with separate counts for each vehicle type
- `RowStatus`: Status for a specific vehicle type with capacity and occupancy counts

## Code Generation

The module uses the Protocol Buffers compiler (protoc) to generate Java classes from the `.proto` files. The generated classes provide:

- Builders for creating instances
- Serialization/deserialization methods
- Immutable data structures
- Type-safe access to fields

Configuration for the code generation is in `build.gradle.kts`:

```kotlin
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
}

sourceSets {
    main {
        proto {
            srcDir("src/main/proto")
        }
    }
}
```

The generated classes are placed in the `build/generated/source/proto/main/java` directory and are automatically included in the compilation.

## Utility Classes

### Factory Classes

The module provides factory classes for generating test data:

#### VehicleFactory

Creates realistic vehicle instances with:
- Random vehicle types based on realistic distribution (85% cars, 9% handicap, 6% motorcycles)
- Realistic license plates with different formats based on vehicle type
- Random state selection from US states
- Unique vehicle IDs

```kotlin
// Create a random vehicle
val vehicle = VehicleFactory.createRandomVehicle()

// Create a vehicle of a specific type
val handicapVehicle = VehicleFactory.createVehicle(VehicleType.HANDICAP)
```

#### ParkingGarageFactory

Creates parking garage structures with:
- Configurable zones, rows, and spaces
- Support for different vehicle type capacities
- Proper hierarchical relationships
- Consistent ID patterns

```kotlin
// Create a parking zone with specific capacities
val zone = ParkingGarageFactory.createParkingZone(
    zoneId = "zone-1",
    handicapCapacity = 10,
    motorcycleCapacity = 5,
    defaultCapacity = 85,
    garageId = "garage-1"
)

// Create a multi-row parking zone
val multiRowZone = ParkingGarageFactory.createParkingZoneWithRows(
    zoneId = "zone-2",
    numRows = 5,
    handicapCapacityPerRow = 2,
    motorcycleCapacityPerRow = 1,
    defaultCapacityPerRow = 17,
    garageId = "garage-1"
)
```

#### ParkingEventFactory

Creates parking event instances:
- Entry events with appropriate vehicle types
- Exit events for specific vehicles
- Random events with realistic distribution (70% entry, 30% exit)
- Current timestamps

```kotlin
// Create an entry event
val entryEvent = ParkingEventFactory.createEntryEvent(garageId, parkingSpace)

// Create an exit event
val exitEvent = ParkingEventFactory.createExitEvent(garageId, zoneId, spaceId, vehicle)

// Create a random event
val randomEvent = ParkingEventFactory.createRandomEvent(garageId, parkingSpace)
```

### DTO Classes and Conversion Utilities

The module provides DTO classes for REST API representations:

- `ParkingSpaceStatusDto`: DTO for parking space status
- `ParkingSpaceDto`: DTO for parking space
- `VehicleDto`: DTO for vehicle
- `SpaceStatusDto`: Enum for space status
- `VehicleTypeDto`: Enum for vehicle type

Extension functions in `ProtobufToDtoExtensions.kt` provide easy conversion from Protocol Buffer objects to DTOs:

```kotlin
// Convert a ParkingSpaceStatus to DTO
val statusDto = parkingSpaceStatus.toDto()

// Convert a list of ParkingSpaceStatus to DTOs
val statusDtoList = parkingSpaceStatusList.toDtoList()
```

## Usage

### Including the Module

To use this module in other modules of the project, add it as a dependency in your `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":utils"))
}
```

### Using Protocol Buffer Classes

```kotlin
// Create a vehicle using the builder pattern
val vehicle = Vehicle.newBuilder()
    .setId("vehicle-123")
    .setLicensePlate("ABC123")
    .setState("CA")
    .setType(VehicleType.CAR)
    .build()

// Create a parking space
val space = ParkingSpace.newBuilder()
    .setId("space-1")
    .setRowId("row-1")
    .setZoneId("zone-1")
    .setGarageId("garage-1")
    .setType(VehicleType.CAR)
    .build()

// Create a parking event
val event = ParkingEvent.newBuilder()
    .setType(ParkingEventType.ENTER)
    .setSpace(space)
    .setVehicle(vehicle)
    .setTimestamp(Timestamp.newBuilder()
        .setSeconds(System.currentTimeMillis() / 1000)
        .setNanos(((System.currentTimeMillis() % 1000) * 1_000_000).toInt())
        .build())
    .build()
```

### Using Factory Classes

```kotlin
// Create a random vehicle
val vehicle = VehicleFactory.createRandomVehicle()

// Create a parking garage structure
val zone = ParkingGarageFactory.createParkingZone(
    zoneId = "zone-1",
    handicapCapacity = 10,
    motorcycleCapacity = 5,
    defaultCapacity = 85,
    garageId = "garage-1"
)

val garage = ParkingGarageFactory.createParkingGarage(
    id = "garage-1",
    parkingZones = arrayOf(zone)
)

// Create a parking event
val event = ParkingEventFactory.createEntryEvent("garage-1", space)
```

## Build and Dependencies

The module uses the following dependencies:

- **Kotlin Serialization**: For serialization support
- **Protocol Buffers**: For data model definition and code generation
- **Jackson Annotations**: For JSON serialization support in DTOs

### Build Commands

```bash
# Build the module
./gradlew :utils:build

# Run tests
./gradlew :utils:test

# Clean build outputs
./gradlew :utils:clean
```

### Integration with Other Modules

This module is a dependency for all other modules in the project:

- **datagen**: Uses Protocol Buffer definitions and factory classes
- **kafka-streams**: All sub-modules use the shared data models


## Generated Classes

The Protocol Buffer compiler generates Java classes for all messages and enums defined in the `.proto` files. These classes are used throughout the project for data representation and serialization.

Key features of the generated classes:

- **Builder Pattern**: All message classes have a builder for creating instances
- **Immutability**: Once built, message instances are immutable
- **Serialization**: Methods for serializing to/from binary format
- **Type Safety**: Strongly typed fields and enums