# Parking Garage Streams Project Guidelines

This document provides guidelines and instructions for developing and working with the Parking Garage Streams project.

## Build/Configuration Instructions

### Project Structure

The project is a multi-module Gradle project with the following modules:

- **utils**: Common utilities and data models using Protocol Buffers
- **datagen**: Data generation application for simulating parking events
- **kafka-streams**: Kafka Streams implementation for processing parking events
- **flink-table-api**: Apache Flink implementation for processing parking events

### Build Requirements

- JDK 21 (configured via jvmToolchain in Gradle)
- Gradle 8.x (included as Gradle wrapper)

### Building the Project

To build the entire project:

```bash
./gradlew build
```

To build a specific module:

```bash
./gradlew :<module-name>:build
```

For example:

```bash
./gradlew :kafka-streams:build
```

### Configuration

#### Local Development

The applications are configured for local development with:

- Kafka broker at localhost:9092
- Schema Registry at localhost:8081

Configuration files are located in `src/main/resources/application.yml` for each module.

#### Confluent Cloud Configuration

For Confluent Cloud deployment, the applications use the `application-cc.yml` configuration files which:

1. Import external configuration from `${user.home}/tools/parking-garage/cc.properties`
2. Use environment variables for sensitive information:
   - `CC_BROKER`: Confluent Cloud broker URL
   - `CC_SCHEMA_REGISTRY_URL`: Confluent Cloud Schema Registry URL
   - `KAFKA_KEY_ID` and `KAFKA_KEY_SECRET`: Kafka API credentials
   - `SCHEMA_REGISTRY_KEY_ID` and `SCHEMA_REGISTRY_KEY_SECRET`: Schema Registry credentials

To run with Confluent Cloud configuration:

```bash
./gradlew :<module-name>:bootRun --args='--spring.profiles.active=cc'
```

## Testing Information

### Test Framework

The project uses:
- JUnit 5 as the test framework
- Kotlin's test assertions (kotlin.test)
- Kafka Streams Test Utils for stream processing tests
- Spring Boot Test for Spring components

### Running Tests

To run all tests:

```bash
./gradlew test
```

To run tests for a specific module:

```bash
./gradlew :<module-name>:test
```

To run a specific test class:

```bash
./gradlew :<module-name>:test --tests "fully.qualified.ClassName"
```

For example:

```bash
./gradlew :utils:test --tests "io.sandonjacobs.streaming.parking.utils.VehicleFactoryTest"
```

### Adding New Tests

1. Create a test class in the appropriate module's `src/test/kotlin` directory
2. Use the `@Test` annotation for test methods
3. Use backtick-quoted names for readability (e.g., `` fun `test description here`() ``)
4. Use assertions from kotlin.test (assertEquals, assertTrue, etc.)

Example test class:

```kotlin
package io.sandonjacobs.streaming.parking.utils

import io.sandonjacobs.streaming.parking.model.*
import kotlin.test.*

class VehicleFactoryTest {

    @Test
    fun `createRandomVehicle should create a valid vehicle`() {
        val vehicle = VehicleFactory.createRandomVehicle()
        
        assertNotNull(vehicle.id)
        assertTrue(vehicle.id.startsWith("vehicle-"))
        assertNotNull(vehicle.licensePlate)
        assertNotNull(vehicle.state)
    }
}
```

### Testing Kafka Streams Topologies

For Kafka Streams topologies, use the `TopologyTestDriver` to test the topology without a running Kafka cluster:

```kotlin
@SpringBootTest
@ContextConfiguration(classes = [TestConfig::class])
class ParkingSpaceStatusTopologyTest {
    
    @Autowired
    private lateinit var topology: ParkingSpaceStatusTopology
    
    private lateinit var testDriver: TopologyTestDriver
    private lateinit var inputTopic: TestInputTopic<String, ParkingEvent>
    private lateinit var outputTopic: TestOutputTopic<String, ParkingSpaceStatus>
    
    @BeforeEach
    fun setUp() {
        val builder = StreamsBuilder()
        testDriver = TopologyTestDriver(topology.buildTopology(builder))
        
        inputTopic = testDriver.createInputTopic(
            "input-topic",
            Serdes.String().serializer(),
            parkingEventSerde.serializer()
        )
        
        outputTopic = testDriver.createOutputTopic(
            "output-topic",
            Serdes.String().deserializer(),
            parkingSpaceStatusSerde.deserializer()
        )
    }
    
    @AfterEach
    fun tearDown() {
        testDriver.close()
    }
    
    @Test
    fun `test topology behavior`() {
        // Given
        val inputEvent = createTestEvent()
        
        // When
        inputTopic.pipeInput("key", inputEvent)
        
        // Then
        val outputRecords = outputTopic.readRecordsToList()
        assertEquals(1, outputRecords.size)
        // Additional assertions...
    }
}
```

## Additional Development Information

### Code Style

- The project uses Kotlin with a functional programming style
- Use backtick-quoted names for test methods to improve readability
- Follow the existing code structure and naming conventions

### Protocol Buffers

The project uses Protocol Buffers for data serialization:

- Proto files are located in `utils/src/main/proto`
- Generated Java classes are used for data models
- Confluent Schema Registry is used for schema management

### Kafka Streams

The Kafka Streams application:

- Uses Spring Boot for the application framework
- Implements a topology for processing parking events
- Provides a REST API for querying the state of parking spaces

### Terraform for Confluent Cloud

The `confluent-cloud` directory contains Terraform configurations for:

- Setting up Kafka clusters in Confluent Cloud
- Creating topics with appropriate configurations
- Configuring Schema Registry

To apply the Terraform configuration:

```bash
cd confluent-cloud
terraform init
terraform apply
```

### Logging

The project uses SLF4J with Logback for logging:

- Log levels are configured in the application.yml files
- Default log levels:
  - io.sandonjacobs.parking.kstreams: DEBUG
  - org.apache.kafka.streams: INFO
  - org.springframework.web: INFO