# Kafka Streams Module

This module contains Kafka Streams applications for processing parking garage data streams.

## Structure

```
kafka-streams/
├── build.gradle.kts          # Gradle build configuration
├── src/
│   ├── main/
│   │   ├── kotlin/           # Kotlin source code
│   │   ├── resources/        # Configuration files and resources
│   │   └── proto/            # Protocol buffer definitions (if any)
│   └── test/
│       ├── kotlin/           # Kotlin test code
│       └── resources/        # Test resources
└── README.md                 # This file
```

## Dependencies

- **Kafka Streams**: Core streaming functionality
- **Kafka Clients**: Kafka client libraries
- **Kotlin**: Programming language
- **Kotlin Serialization**: JSON serialization
- **Protobuf**: Protocol buffer support
- **Utils Module**: Shared utilities and data models

## Usage

This module will contain Kafka Streams applications that process:
- Parking garage events
- Vehicle entry/exit data
- Real-time analytics
- Data transformations

## Building

```bash
# Build this module only
./gradlew :kafka-streams:build

# Build all modules
./gradlew build
``` 