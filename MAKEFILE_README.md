# Parking Garage Streams - Modular Makefile

This document describes the refactored, modular Makefile structure for the Parking Garage Streams project.

## Overview

The Makefile has been refactored from a monolithic structure to a modular, maintainable system that makes it easy to add new modules and actions. The new structure provides:

- **Modularity**: Each module has its own makefile fragment
- **Scalability**: Easy to add new modules without touching the main Makefile
- **Consistency**: Standardized patterns across all modules
- **Maintainability**: Clear separation of concerns
- **Documentation**: Built-in help system for each module

## Directory Structure

```
Makefile                    # Main Makefile (entry point)
├── config/
│   └── modules.mk         # Module definitions and configuration
└── modules/
    ├── infrastructure.mk  # Infrastructure (Terraform) operations
    ├── datagen.mk         # Data generator application
    ├── kafka-streams.mk   # Kafka Streams applications

    ├── utils.mk           # Utility modules
    └── config.mk          # Configuration management
```

## Module Definitions

### Main Modules

- **infrastructure**: Terraform-based deployments (Confluent Cloud, AWS)
- **datagen**: Spring Boot data generator application
- **kafka-streams**: Kafka Streams processing applications

- **utils**: Shared utilities and common functionality

### Sub-modules

#### Infrastructure
- `confluent-cloud`: Confluent Cloud environment and connectors
  - `base-env`: Base Confluent Cloud environment (topics, etc.)
  - `ks-connectors`: Kafka Streams connectors and configurations
- `aws`: AWS infrastructure (RDS, etc.)

#### Kafka Streams
- `kstreams-utils`: Kafka Streams utilities
- `parking-space-status`: Parking space status processor
- `row-aggregates`: Row aggregation processor
- `zone-statistics`: Zone statistics processor



## Available Targets

### Project Management

- **help**: Show all available targets
- **init-project**: Initialize project configuration directory
- **check-config**: Validate project configuration
- **all**: Run complete deployment pipeline (infrastructure + datagen)
- **clean**: Clean all build artifacts and infrastructure

### Infrastructure Targets

#### Confluent Cloud
- **confluent-cloud**: Initialize, plan, and apply Confluent Cloud infrastructure
- **confluent-cloud-init**: Initialize Terraform for Confluent Cloud
- **confluent-cloud-plan**: Generate Terraform plan for Confluent Cloud
- **confluent-cloud-apply**: Apply Confluent Cloud infrastructure
- **confluent-cloud-destroy**: Destroy Confluent Cloud infrastructure

#### KS Connectors
- **ks-connectors**: Initialize, plan, and deploy Kafka Streams connectors
- **ks-connectors-init**: Initialize Terraform for KS Connectors
- **ks-connectors-plan**: Generate Terraform plan for KS Connectors
- **ks-connectors-deploy**: Deploy Kafka Streams connectors
- **ks-connectors-destroy**: Destroy Kafka Streams connectors

#### AWS
- **aws**: Deploy AWS infrastructure
- **aws-deploy**: Deploy AWS infrastructure (RDS, etc.)
- **aws-plan**: Generate Terraform plan for AWS
- **aws-destroy**: Destroy AWS infrastructure

#### Infrastructure Aggregates
- **infrastructure**: Deploy all infrastructure (Confluent Cloud + AWS)
- **infrastructure-deploy**: Deploy all infrastructure
- **infrastructure-destroy**: Destroy all infrastructure
- **infrastructure-plan**: Generate plans for all infrastructure

### Datagen Targets

- **datagen**: Build and run data generator application
- **datagen-build**: Build datagen application
- **datagen-run**: Run datagen application in background
- **datagen-stop**: Stop datagen application
- **datagen-test**: Test datagen application
- **datagen-clean**: Clean datagen build artifacts

### Kafka Streams Targets

#### Individual Applications
- **parking-space-status-build**: Build parking space status processor
- **parking-space-status-run**: Run parking space status processor
- **parking-space-status-stop**: Stop parking space status processor
- **parking-space-status-test**: Test parking space status processor
- **parking-space-status-clean**: Clean parking space status processor

- **row-aggregates-build**: Build row aggregates processor
- **row-aggregates-run**: Run row aggregates processor
- **row-aggregates-stop**: Stop row aggregates processor
- **row-aggregates-test**: Test row aggregates processor
- **row-aggregates-clean**: Clean row aggregates processor

- **zone-statistics-build**: Build zone statistics processor
- **zone-statistics-run**: Run zone statistics processor
- **zone-statistics-stop**: Stop zone statistics processor
- **zone-statistics-test**: Test zone statistics processor
- **zone-statistics-clean**: Clean zone statistics processor

- **kstreams-utils-build**: Build Kafka Streams utilities
- **kstreams-utils-test**: Test Kafka Streams utilities
- **kstreams-utils-clean**: Clean Kafka Streams utilities

#### Kafka Streams Aggregates
- **kafka-streams**: Build all Kafka Streams applications
- **kafka-streams-build**: Build all Kafka Streams applications
- **kafka-streams-run**: Run all Kafka Streams applications
- **kafka-streams-stop**: Stop all Kafka Streams applications
- **kafka-streams-test**: Test all Kafka Streams applications
- **kafka-streams-clean**: Clean all Kafka Streams applications



### Utils Targets

- **utils**: Build utility modules
- **utils-build**: Build utility modules
- **utils-test**: Test utility modules
- **utils-clean**: Clean utility modules

### Configuration Targets

- **config-generate**: Generate configuration files from Terraform outputs
- **config-validate**: Validate configuration setup
- **config-clean**: Clean configuration files

## Usage

### Basic Commands

```bash
# Show all available targets
make help

# Initialize project configuration
make init-project

# Deploy complete infrastructure
make infrastructure

# Build and run data generator
make datagen

# Build all Kafka Streams applications
make kafka-streams



# Build utility modules
make utils

# Run complete pipeline
make all

# Clean everything
make clean
```

### Module-Specific Help

```bash
# Show infrastructure targets
make help-infrastructure

# Show datagen targets
make help-datagen

# Show Kafka Streams targets
make help-kafka-streams


```

### Individual Module Actions

```bash
# Infrastructure
make confluent-cloud-deploy
make aws-deploy
make infrastructure-destroy

# Datagen
make datagen-build
make datagen-run
make datagen-stop
make datagen-test

# Kafka Streams
make kafka-streams-build
make kafka-streams-run
make kafka-streams-stop
make kafka-streams-test

# Individual Kafka Streams applications
make parking-space-status-build
make row-aggregates-run
make zone-statistics-stop



# Utils
make utils-build
make utils-test
```

## Configuration

### Environment Variables

The Makefile uses the following environment variables:

- `PG_CONFIG_HOME`: Configuration directory (set by `init-project`)
- `TF_VAR_db_master_password`: Database password (set by `init-project`)
- `GRADLE_CMD`: Gradle command (defaults to `./gradlew`)

### Configuration Validation

The `check-config` target validates that required configuration is present:

```bash
make check-config
```

## Adding New Modules

To add a new module:

1. **Define the module** in `config/modules.mk`:
   ```makefile
   NEW_MODULE := my-new-module
   MODULES += new-module
   ```

2. **Create a module makefile** in `modules/new-module.mk`:
   ```makefile
   # New module - Description
   
   .PHONY: new-module new-module-build new-module-run new-module-stop new-module-test new-module-clean
   
   new-module: new-module-build
   
   new-module-build: check-config
   	@echo "🔨 Building new module..."
   	$(GRADLE_CMD) :new-module:build
   
   new-module-run: check-config
   	@echo "🚀 Running new module..."
   	$(GRADLE_CMD) :new-module:bootRun
   
   # ... other targets
   ```

3. **Add help target** to the main Makefile:
   ```makefile
   help-new-module:
   	@echo "🆕 New Module targets:"
   	@echo "  new-module-build    - Build new module"
   	@echo "  new-module-run      - Run new module"
   ```

## Benefits of the New Structure

### Before (Monolithic)
- All targets in one large file
- Hard to find specific functionality
- Difficult to add new modules
- Repetitive code patterns
- No clear separation of concerns

### After (Modular)
- Clear module separation
- Easy to locate functionality
- Simple to add new modules
- Reusable patterns
- Consistent interface across modules
- Built-in documentation
- Configuration validation

## Migration from Old Makefile

The old Makefile targets have been mapped to new targets:

| Old Target | New Target |
|------------|------------|
| `init-project` | `init-project` |
| `deploy-base-env` | `confluent-cloud-deploy` |
| `init-cc` | `confluent-cloud-deploy` |
| `init-aws` | `aws-deploy` |
| `run-datagen` | `datagen` |
| `stop-datagen` | `datagen-stop` |
| `destroy-all` | `clean` |
| `all` | `all` |

## Troubleshooting

### Common Issues

1. **Configuration not set**: Run `make init-project` first
2. **Module not found**: Check that the module is defined in `config/modules.mk`
3. **Target not found**: Use `make help` to see available targets
4. **Permission denied**: Ensure Gradle wrapper is executable (`chmod +x gradlew`)

### Debugging

To see what targets are available for a module:
```bash
make help-<module-name>
```

To see the actual commands being run:
```bash
make -n <target>
```

## Future Enhancements

Potential improvements for the future:

1. **Dependency management**: Automatic dependency resolution between modules
2. **Parallel execution**: Run independent modules in parallel
3. **Configuration profiles**: Support for different deployment environments
4. **Health checks**: Built-in health checking for running applications
5. **Logging**: Centralized logging and monitoring
6. **CI/CD integration**: Targets for continuous integration pipelines
