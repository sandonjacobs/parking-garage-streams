# Confluent Cloud Terraform Module

This module provides Terraform configurations for provisioning and managing Confluent Cloud resources required by the Parking Garage Streams project.

## Overview

The confluent-cloud module uses Terraform to automate the deployment and configuration of Confluent Cloud resources, including:

- Confluent Cloud environment with Stream Governance
- Kafka cluster in your preferred cloud provider and region
- Schema Registry with BACKWARD compatibility
- Service accounts with appropriate permissions
- API keys for authentication
- Kafka topics with proper configurations

This infrastructure-as-code approach ensures consistent, repeatable deployments and simplifies the management of Confluent Cloud resources.

## Prerequisites

Before using this module, you need:

1. [Terraform](https://www.terraform.io/downloads.html) (v1.0.0 or later)
2. A Confluent Cloud account
3. Confluent Cloud organization ID
4. [Confluent CLI](https://docs.confluent.io/confluent-cli/current/install.html) (optional, for additional management)

## Configuration

### Variables

The module can be configured using the following variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `org_id` | Confluent Cloud organization ID (required) | - |
| `cloud_provider` | Cloud provider for Confluent Cloud | `"AWS"` |
| `cloud_region` | Cloud provider region | `"us-east-2"` |
| `cc_env_display_name` | Name of Confluent Cloud environment | `"parking_garage_streams"` |
| `cc_kafka_cluster_name` | Name of Confluent Cloud Kafka cluster | `"parking_stuff"` |

### Outputs

The module provides the following outputs:

| Output | Description |
|--------|-------------|
| `ORG_ID` | Confluent Cloud organization ID |
| `CC_ENV_DISPLAY_NAME` | Environment display name |
| `CC_ENV_ID` | Environment ID |
| `CLOUD_PROVIDER` | Cloud provider |
| `CLOUD_REGION` | Cloud region |
| `CC_KAFKA_CLUSTER_ID` | Kafka cluster ID |
| `CC_BROKER` | Kafka broker endpoint (without protocol) |
| `CC_BROKER_URL` | Kafka REST endpoint |
| `CC_SCHEMA_REGISTRY_ID` | Schema Registry ID |
| `CC_SCHEMA_REGISTRY_URL` | Schema Registry REST endpoint |
| `KAFKA_KEY_ID` | Kafka API key ID |
| `KAFKA_KEY_SECRET` | Kafka API key secret |
| `SCHEMA_REGISTRY_KEY_ID` | Schema Registry API key ID |
| `SCHEMA_REGISTRY_KEY_SECRET` | Schema Registry API key secret |

## Usage

### Initial Setup

1. Clone the repository and navigate to the confluent-cloud directory:

```bash
cd parking-garage-streams/confluent-cloud
```

2. Create a `terraform.tfvars` file with your Confluent Cloud organization ID:

```hcl
org_id = "your-organization-id"
```

3. Initialize Terraform:

```bash
terraform init
```

### Deployment

1. Generate and review an execution plan:

```bash
terraform plan -out=tfplan
```

2. Apply the changes:

```bash
terraform apply tfplan
```

### Customization

To customize the deployment, you can modify the variables in your `terraform.tfvars` file:

```hcl
org_id = "your-organization-id"
cloud_provider = "GCP"  # AWS, GCP, or Azure
cloud_region = "us-central1"
cc_env_display_name = "my-parking-garage-env"
cc_kafka_cluster_name = "my-parking-cluster"
```

## Resources Created

### Confluent Cloud Environment

The module creates a Confluent Cloud environment with Stream Governance package set to "ADVANCED".

```hcl
resource "confluent_environment" "cc_env" {
  display_name = var.cc_env_display_name
  stream_governance {
    package = "ADVANCED"
  }
}
```

### Kafka Cluster

A basic Kafka cluster is provisioned in the specified cloud provider and region.

```hcl
resource "confluent_kafka_cluster" "kafka_cluster" {
  display_name = var.cc_kafka_cluster_name
  availability = "SINGLE_ZONE"
  cloud        = var.cloud_provider
  region       = var.cloud_region
  basic {}
  environment {
    id = confluent_environment.cc_env.id
  }
}
```

### Service Accounts and API Keys

The module creates service accounts with appropriate permissions:

- `app-manager`: For managing Kafka resources (CloudClusterAdmin role)
- `env-manager`: For managing environment resources (EnvironmentAdmin role)

API keys are created for both service accounts to authenticate with Kafka and Schema Registry.

### Kafka Topics

The following Kafka topics are created with appropriate configurations:

| Topic | Partitions | Retention | Description |
|-------|------------|-----------|-------------|
| `parking-events` | 6 | 7 days | Stores parking events (vehicle entry/exit) |
| `parking-garage` | 1 | 7 days | Stores parking garage information |
| `parking-space-status` | 6 | 7 days | Stores the status of parking spaces |
| `parking-row-aggregates` | 6 | 7 days | Stores aggregated status by row |

## Integration with Other Modules

The confluent-cloud module is designed to work seamlessly with other modules in the Parking Garage Streams project:

### datagen Module

The datagen module uses the Confluent Cloud resources to produce parking events and garage information. It connects to Confluent Cloud using the environment variables that match the outputs from this module:

```yaml
spring:
  kafka:
    bootstrap-servers: ${CC_BROKER}
    schema-registry:
      url: ${CC_SCHEMA_REGISTRY_URL}
    producer:
      properties:
        sasl.jaas.config: "org.apache.kafka.common.security.plain.PlainLoginModule required username='${KAFKA_KEY_ID}' password='${KAFKA_KEY_SECRET}';"
        basic.auth.user.info: "${SCHEMA_REGISTRY_KEY_ID}:${SCHEMA_REGISTRY_KEY_SECRET}"
```

### kafka-streams Module

The kafka-streams module processes the parking events using Kafka Streams. It also connects to Confluent Cloud using the same environment variables:

```kotlin
// Add Confluent Cloud broker configuration
cloud.getProperty(CC_BROKER)?.let {
    properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, it)
}
cloud.getProperty(KAFKA_KEY_ID)?.let { kafkaKey ->
    cloud.getProperty(KAFKA_KEY_SECRET)?.let { kafkaSecret ->
        properties.put("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username='$kafkaKey' password='$kafkaSecret';")
    }
}
```

## Common Operations

### Exporting Confluent Cloud Credentials

After applying the Terraform configuration, you can export the credentials as environment variables:

```bash
export CC_BROKER=$(terraform output -raw CC_BROKER)
export CC_SCHEMA_REGISTRY_URL=$(terraform output -raw CC_SCHEMA_REGISTRY_URL)
export KAFKA_KEY_ID=$(terraform output -raw KAFKA_KEY_ID)
export KAFKA_KEY_SECRET=$(terraform output -raw KAFKA_KEY_SECRET)
export SCHEMA_REGISTRY_KEY_ID=$(terraform output -raw SCHEMA_REGISTRY_KEY_ID)
export SCHEMA_REGISTRY_KEY_SECRET=$(terraform output -raw SCHEMA_REGISTRY_KEY_SECRET)
```

### Creating a Properties File for Applications

You can create a properties file for the applications to use:

```bash
cat > cc.properties << EOF
CC_BROKER=$(terraform output -raw CC_BROKER)
CC_SCHEMA_REGISTRY_URL=$(terraform output -raw CC_SCHEMA_REGISTRY_URL)
KAFKA_KEY_ID=$(terraform output -raw KAFKA_KEY_ID)
KAFKA_KEY_SECRET=$(terraform output -raw KAFKA_KEY_SECRET)
SCHEMA_REGISTRY_KEY_ID=$(terraform output -raw SCHEMA_REGISTRY_KEY_ID)
SCHEMA_REGISTRY_KEY_SECRET=$(terraform output -raw SCHEMA_REGISTRY_KEY_SECRET)
EOF
```

### Destroying Resources

To destroy all resources created by this module:

```bash
terraform destroy
```

**Note:** This will delete all Confluent Cloud resources created by this module, including all data in the Kafka topics.

## Troubleshooting

### API Key Issues

If you encounter issues with API keys, ensure that:

1. The service accounts have the correct permissions
2. The API keys are correctly exported as environment variables
3. The API keys are not expired or revoked

### Topic Creation Issues

If topic creation fails, check:

1. The Kafka cluster is in RUNNING state
2. The app-manager service account has CloudClusterAdmin role
3. The API key for the app-manager is valid

For more troubleshooting information, refer to the [Confluent Terraform Provider documentation](https://registry.terraform.io/providers/confluentinc/confluent/latest/docs).