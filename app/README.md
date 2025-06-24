# Parking Garage Data Generator with Kafka Producer

This application generates realistic parking garage events and sends them to Apache Kafka using Confluent's Protobuf serializer.

## Features

- **Kafka Producer**: Uses `KafkaTemplate<String, ParkingEvent>` to send messages
- **Protobuf Serialization**: Confluent Protobuf serializer for efficient message serialization
- **Real-time Event Generation**: Generates parking events with realistic timing patterns
- **REST API**: Control event generation via HTTP endpoints
- **Spring Boot Integration**: Full Spring Boot application with auto-configuration

## Configuration

### Kafka Settings

The application is configured to connect to:
- **Bootstrap Servers**: `localhost:9092` (configurable via `spring.kafka.bootstrap-servers`)
- **Schema Registry**: `http://localhost:8081` (configurable via `spring.kafka.schema-registry.url`)
- **Topic**: `parking-events` (configurable via `app.kafka.topic.parking-events`)

### Message Format

- **Key**: String (parking space ID)
- **Value**: `ParkingEvent` protobuf message containing:
  - Event type (ENTER/EXIT)
  - Parking space details
  - Vehicle information
  - Timestamp

## API Endpoints

### Start Event Generation

```bash
# Start generation for all garages
POST /api/parking-events/start?eventsPerMinute=10

# Start generation for specific garage
POST /api/parking-events/start/{garageId}?eventsPerMinute=5
```

### Stop Event Generation

```bash
# Stop generation for specific garage
POST /api/parking-events/stop/{garageId}

# Stop all generators
POST /api/parking-events/stop
```

### Check Status

```bash
# Get status of all garages
GET /api/parking-events/status

# Get status of specific garage
GET /api/parking-events/status/{garageId}
```

### Send Single Event

```bash
# Send a single event for testing
POST /api/parking-events/send-single?garageId=garage-1&spaceId=space-1
```

## Running the Application

1. **Start Kafka and Schema Registry**:
   ```bash
   # Using Docker Compose or your preferred method
   docker-compose up -d kafka schema-registry
   ```

2. **Run the Application**:
   ```bash
   ./gradlew :app:bootRun
   ```

3. **Verify Kafka Connection**:
   The application will automatically start generating events and sending them to Kafka.

## Testing

Run the integration tests with embedded Kafka:

```bash
./gradlew :app:test
```

The tests verify:
- Kafka producer configuration
- Message serialization
- Topic creation and message delivery

## Dependencies

- **Spring Kafka**: For KafkaTemplate and producer functionality
- **Confluent Protobuf Serializer**: For Protobuf message serialization
- **Confluent Schema Registry Client**: For schema management
- **Spring Boot**: For application framework and auto-configuration

## Message Flow

1. **Event Generation**: `ParkingEventGenerator` creates realistic parking events
2. **Kafka Production**: `KafkaParkingEventProducer` sends events to Kafka
3. **Serialization**: Confluent Protobuf serializer handles message serialization
4. **Delivery**: Messages are sent to the configured Kafka topic

## Monitoring

The application exposes Spring Boot Actuator endpoints for monitoring:
- Health checks: `/actuator/health`
- Metrics: `/actuator/metrics`
- Application info: `/actuator/info` 