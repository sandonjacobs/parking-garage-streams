# Kafka Streams Module

This module contains a Kafka Streams application for processing parking garage data streams with an embedded REST API for querying the materialized state store.

## Overview

The application processes parking events and maintains a materialized view of parking space statuses. It also provides a REST API for querying the current state of parking spaces in real-time.

## Features

- **Stream Processing**: Processes parking events and maintains state
- **State Store**: Materializes parking space statuses for fast queries
- **REST API**: Embedded HTTP endpoints for querying state
- **Real-time Queries**: Direct access to the materialized state store

## Architecture

- **ParkingSpaceStatusTopology**: Kafka Streams topology that processes events
- **ParkingSpaceStatusQueryService**: Service for querying the state store
- **ParkingSpaceStatusController**: REST controller exposing HTTP endpoints
- **MainClass**: Application entry point with Spring Boot integration

## API Endpoints

The application exposes REST endpoints on port 8080:

### Get specific parking space status
```
GET /api/parking-spaces/{spaceId}
```

### Get all parking space statuses
```
GET /api/parking-spaces
```

### Get parking spaces by status
```
GET /api/parking-spaces/status/{status}
GET /api/parking-spaces/occupied
GET /api/parking-spaces/vacant
```

### Get counts and statistics
```
GET /api/parking-spaces/counts
GET /api/parking-spaces/count
```

### Health check
```
GET /api/parking-spaces/health
```

## Running the Application

### Start with Confluent Cloud configuration:
```bash
./gradlew :kstreams-parking-space-status:bootRun --args="--cc-config config/application-cc.yml"
```

### Start with generic Kafka configuration:
```bash
./gradlew :kstreams-parking-space-status:bootRun --args="--kafka-config config/kafka.properties"
```

### Build and run JAR:
```bash
./gradlew :kstreams-parking-space-status:build
java -jar kstreams-parking-space-status/build/libs/kstreams-parking-space-status-*.jar --cc-config config/application-cc.yml
```

## State Store

The application materializes parking space statuses to a state store named `parking-space-status-store`. This provides:
- **Fast queries**: Sub-millisecond response times
- **Real-time data**: Always up-to-date with the latest events
- **Direct access**: No need to consume from output topics

## Dependencies

- **Kafka Streams**: Core streaming functionality
- **Spring Boot**: REST API framework
- **Kotlin**: Programming language
- **Protobuf**: Protocol buffer support
- **Utils Module**: Shared utilities and data models

## Building

```bash
# Build this module only
./gradlew :kstreams-parking-space-status:build

# Build all modules
./gradlew build
``` 