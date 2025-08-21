# Apache Flink Table API Module

This module provides an alternative stream processing implementation using Apache Flink's Table API and SQL interface for processing parking garage events.

## Overview

The flink-table-api module is designed to demonstrate how the same parking garage event processing can be implemented using Apache Flink instead of Kafka Streams. This provides a comparison between different stream processing approaches and showcases Flink's SQL-like interface for stream processing.

## Current Status

⚠️ **Development Phase**: This module is currently in early development and is not yet fully implemented.

## Planned Features

### Core Functionality
- **Parking Event Processing**: Process parking events using Flink's Table API
- **Space Status Tracking**: Maintain real-time status of parking spaces
- **Row Aggregation**: Aggregate parking space statuses by row
- **Zone Statistics**: Calculate zone-level occupancy statistics

### Technical Features
- **SQL-like Interface**: Use Flink SQL for stream processing queries
- **Protocol Buffer Support**: Integration with Protocol Buffer schemas
- **Kafka Connector**: Read from and write to Kafka topics
- **Confluent Schema Registry**: Schema evolution support
- **State Management**: Flink's stateful processing capabilities

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│                 │     │                 │     │                 │
│  parking-events │────▶│   Flink Table   │────▶│ parking-space-  │
│   (Kafka Topic) │     │      API        │     │ status (Topic)  │
│                 │     │                 │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                │
                                ▼
                        ┌─────────────────┐     ┌─────────────────┐
                        │                 │     │                 │
                        │   Row/Zone      │────▶│ Aggregated      │
                        │ Aggregation     │     │ Results         │
                        │                 │     │                 │
                        └─────────────────┘     └─────────────────┘
```

## Dependencies

The module includes the following dependencies:

- **Apache Flink Table API**: Core Flink Table API and SQL functionality
- **Confluent Flink Table API**: Integration with Confluent's Kafka and Schema Registry
- **Kotlin**: Programming language support
- **utils**: Shared Protocol Buffer definitions and data models

## Configuration

### Flink Configuration
- **Checkpointing**: Configured for fault tolerance
- **State Backend**: RocksDB for scalable state storage
- **Parallelism**: Configurable parallelism for horizontal scaling

### Kafka Integration
- **Source Connector**: Read from Kafka topics
- **Sink Connector**: Write results back to Kafka topics
- **Schema Registry**: Protocol Buffer schema management

## Development Roadmap

### Phase 1: Basic Setup ✅
- [x] Module structure and dependencies
- [x] Basic Flink Table API configuration
- [ ] Kafka source and sink connectors

### Phase 2: Core Processing
- [ ] Parking event processing pipeline
- [ ] Space status tracking
- [ ] Basic aggregation queries

### Phase 3: Advanced Features
- [ ] Row-level aggregation
- [ ] Zone-level statistics
- [ ] Complex SQL queries
- [ ] Performance optimization

### Phase 4: Production Ready
- [ ] Error handling and monitoring
- [ ] Configuration management
- [ ] Testing and documentation
- [ ] Deployment guides

## Comparison with Kafka Streams

| Feature | Kafka Streams | Apache Flink |
|---------|---------------|--------------|
| **Processing Model** | Stream processing library | Full stream processing framework |
| **API Style** | Java/Kotlin DSL | Table API, SQL, DataStream API |
| **State Management** | Built-in state stores | Flexible state backends |
| **Fault Tolerance** | At-least-once semantics | Exactly-once semantics |
| **Scaling** | Partition-based | Dynamic scaling |
| **SQL Support** | Limited | Full SQL support |
| **Deployment** | Embedded in applications | Standalone cluster |

## Building and Running

### Build
```bash
./gradlew :flink-table-api:build
```

### Run (Future)
```bash
# Submit to Flink cluster
flink run flink-table-api/build/libs/flink-table-api.jar

# Run locally
./gradlew :flink-table-api:run
```

## Testing

### Unit Tests
```bash
./gradlew :flink-table-api:test
```

### Integration Tests (Planned)
- Flink MiniCluster for testing
- Kafka integration tests
- End-to-end pipeline tests

## Integration with Other Modules

This module will integrate with:
- **utils**: Use shared Protocol Buffer definitions
- **datagen**: Consume parking events from Kafka topics
- **confluent-cloud**: Deploy to cloud infrastructure

## Contributing

To contribute to this module:

1. Check the current development phase
2. Review the planned features
3. Implement functionality following Flink best practices
4. Add comprehensive tests
5. Update documentation

## Resources

- [Apache Flink Documentation](https://flink.apache.org/docs/)
- [Flink Table API & SQL](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/table/)
- [Confluent Flink Connectors](https://docs.confluent.io/kafka-connectors/flink/current/overview.html)

---

**Note**: This module is under active development. Check the project issues and pull requests for the latest updates and contribution opportunities.
