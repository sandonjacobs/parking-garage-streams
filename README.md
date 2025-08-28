# Parking Garage Streams

A comprehensive multi-module project demonstrating real-time stream processing of parking garage events using modern streaming technologies like Kafka Streams.

## 🏗️ Project Overview

This project showcases a complete real-time parking garage management system that processes vehicle entry/exit events, tracks parking space status, and provides aggregated analytics at multiple levels (space, row, zone, and garage). The system uses Protocol Buffers for efficient data serialization and Confluent Schema Registry for schema management.

### Key Features

- **Real-time Event Processing**: Processes parking events as they occur
- **Multi-level Aggregation**: Provides analytics at space, row, zone, and garage levels
- **Scalable Architecture**: Built with Kafka Streams for horizontal scaling
- **Cloud-Native**: Supports both local development and Confluent Cloud deployment
- **Type-Safe Data**: Uses Protocol Buffers for efficient, schema-validated data exchange
- **Infrastructure as Code**: Terraform configurations for AWS and Confluent Cloud

## 📁 Project Structure

This project follows a modular architecture with the following components:

### Core Modules

#### `utils` - Shared Data Models and Utilities
- Protocol Buffer schema definitions for all data models
- Factory classes for generating test data
- DTO classes and conversion utilities
- Shared code used by all other modules

#### `datagen` - Data Generation Application
- Spring Boot application for simulating parking events
- Generates realistic parking garage events and sends them to Apache Kafka
- Provides REST API for controlling event generation
- Supports both local and Confluent Cloud deployment

### Stream Processing Modules

#### `kafka-streams` - Kafka Streams Implementation
Contains multiple sub-modules for different processing needs:

- **`kstreams-utils`**: Common utilities for Kafka Streams applications
- **`parking-space-status`**: Processes parking events and maintains real-time space status
- **`row-aggregates`**: Aggregates parking space statuses by row with capacity calculations
- **`zone-statistics`**: Aggregates parking space statuses by zone with statistics


- Provides SQL-like interface for stream processing
- Currently in development phase

### Infrastructure Modules

#### `confluent-cloud` - Confluent Cloud Infrastructure
- Terraform configurations for Confluent Cloud resources
- Automated setup of Kafka clusters, Schema Registry, and topics
- Service accounts and API key management
- Supports multiple cloud providers (AWS, GCP, Azure)

#### `aws` - AWS Infrastructure
- Terraform configurations for AWS RDS Aurora PostgreSQL
- Database infrastructure for storing aggregated results
- Supports both development and production configurations

## 🚀 Quick Start

### Prerequisites

- JDK 21
- Confluent Cloud account (recommended) or local Kafka/Schema Registry setup
- Terraform (for infrastructure deployment)

### Local Development Setup

**Note**: This project is designed to work with Confluent Cloud infrastructure. For local development, you'll need to set up your own Kafka and Schema Registry instances.

1. **Set up Local Infrastructure** (if needed):
   ```bash
   # You'll need to set up Kafka and Schema Registry locally
   # This could be done with Docker, local installations, or other methods
   # The project includes configuration for local development in kafka-local.properties
   ```

2. **Build the Project**:
   ```bash
   ./gradlew build
   ```

3. **Run Data Generator**:
   ```bash
   ./gradlew :datagen:bootRun
   ```

4. **Run Stream Processing Applications**:
   ```bash
   # Parking space status processor
   java -jar kafka-streams/parking-space-status/build/libs/parking-space-status.jar
   
   # Row aggregation processor
   java -jar kafka-streams/row-aggregates/build/libs/row-aggregates.jar
   
   # Zone statistics processor
   java -jar kafka-streams/zone-statistics/build/libs/zone-statistics.jar
   ```

### Confluent Cloud Deployment

1. **Deploy Confluent Cloud Infrastructure**:
   ```bash
   cd confluent-cloud
   terraform init
   terraform apply
   ```

2. **Export Credentials**:
   ```bash
   export CC_BROKER=$(terraform output -raw CC_BROKER)
   export CC_SCHEMA_REGISTRY_URL=$(terraform output -raw CC_SCHEMA_REGISTRY_URL)
   export KAFKA_KEY_ID=$(terraform output -raw KAFKA_KEY_ID)
   export KAFKA_KEY_SECRET=$(terraform output -raw KAFKA_KEY_SECRET)
   ```

3. **Run Applications with Cloud Configuration**:
   ```bash
   ./gradlew :datagen:bootRun --args='--spring.profiles.active=cc'
   ```

## 📊 Data Flow Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│                 │     │                 │     │                 │
│   datagen       │────▶│  parking-events │────▶│ parking-space-  │
│   (Spring Boot) │     │     (topic)     │     │ status (topic)  │
│                 │     │                 │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
         │                                                │
         │                                                ▼
         │                                        ┌─────────────────┐
         │                                        │                 │
         │                                        │ row-aggregates  │
         │                                        │     (topic)     │
         │                                        │                 │
         │                                        └─────────────────┘
         │                                                │
         │                                                ▼
         │                                        ┌─────────────────┐
         │                                        │                 │
         │                                        │ zone-statistics │
         │                                        │     (topic)     │
         │                                        │                 │
         │                                        └─────────────────┘
         │
         ▼
┌─────────────────┐
│                 │
│ parking-garage  │
│     (topic)     │
│                 │
└─────────────────┘
```

## 🛠️ Build and Development

### Gradle Commands

```bash
# Build entire project
./gradlew build

# Run all tests
./gradlew check

# Clean build outputs
./gradlew clean

# Build specific module
./gradlew :utils:build

# Run specific module
./gradlew :datagen:bootRun
```

### Testing

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :utils:test

# Run integration tests
./gradlew :datagen:test
```

## 🔧 Configuration

### Local Development
- Uses `kafka-local.properties` for local Kafka configuration (if you set up local infrastructure)
- Default configuration in `application.yml` for Spring Boot applications
- **Note**: Local infrastructure setup is not included in this project

### Confluent Cloud
- Uses `application-cc.yml` profile for cloud deployment
- Environment variables for sensitive credentials
- External configuration files for cloud-specific settings

## 📈 Monitoring and Observability

The project includes:
- Structured logging with Logback
- Metrics collection capabilities
- Health check endpoints in Spring Boot applications
- Kafka Streams monitoring through JMX

## 🔒 Security

- API key-based authentication for Confluent Cloud
- SASL_SSL for secure Kafka communication
- Environment variable-based credential management
- Terraform-managed service accounts and permissions

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For questions and support:
- Check the individual module README files for specific documentation
- Review the test files for usage examples
- Open an issue for bugs or feature requests

---

**Note**: This project uses the Gradle Wrapper (`./gradlew`) for consistent builds across different environments. The wrapper ensures that the correct version of Gradle is used regardless of what's installed on the system.