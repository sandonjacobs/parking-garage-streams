# Parking Garage Streams - Refactored Makefile
# This Makefile provides a modular, maintainable structure for managing
# the parking garage streaming platform

# Include environment variables if .env file exists
-include .env

# Include module definitions and configurations
-include config/modules.mk
-include modules/*.mk

# Default target
.PHONY: help all clean init-project check-config

help: ## Show this help message
	@echo "🚗 Parking Garage Streams - Available targets:"
	@echo ""
	@echo "🔧 Project Management:"
	@echo "  help             - Show this help message"
	@echo "  init-project     - Initialize project configuration directory"
	@echo "  check-config     - Validate project configuration"
	@echo "  all              - Run complete deployment pipeline"
	@echo "  clean            - Clean all build artifacts and infrastructure"
	@echo ""
	@echo "🏗️  Infrastructure:"
	@echo "  infrastructure   - Deploy all infrastructure (Confluent Cloud + AWS)"
	@echo "  infrastructure-deploy    - Deploy all infrastructure"
	@echo "  infrastructure-destroy   - Destroy all infrastructure"
	@echo "  infrastructure-plan      - Generate plans for all infrastructure"
	@echo ""
	@echo "  Confluent Cloud:"
	@echo "    confluent-cloud        - Initialize, plan, and apply Confluent Cloud"
	@echo "    confluent-cloud-init   - Initialize Terraform for Confluent Cloud"
	@echo "    confluent-cloud-plan   - Generate Terraform plan for Confluent Cloud"
	@echo "    confluent-cloud-apply  - Apply Confluent Cloud infrastructure"
	@echo "    confluent-cloud-destroy - Destroy Confluent Cloud infrastructure"
	@echo ""
	@echo "  KS Connectors:"
	@echo "    ks-connectors          - Initialize, plan, and deploy KS Connectors"
	@echo "    ks-connectors-init     - Initialize Terraform for KS Connectors"
	@echo "    ks-connectors-plan     - Generate Terraform plan for KS Connectors"
	@echo "    ks-connectors-deploy   - Deploy Kafka Streams connectors"
	@echo "    ks-connectors-destroy  - Destroy Kafka Streams connectors"
	@echo ""
	@echo "  AWS:"
	@echo "    aws                    - Deploy AWS infrastructure"
	@echo "    aws-deploy             - Deploy AWS infrastructure (RDS, etc.)"
	@echo "    aws-plan               - Generate Terraform plan for AWS"
	@echo "    aws-destroy            - Destroy AWS infrastructure"
	@echo ""
	@echo "📊 Applications:"
	@echo "  datagen          - Build and run data generator"
	@echo "  datagen-build    - Build datagen application"
	@echo "  datagen-run      - Run datagen application in background"
	@echo "  datagen-stop     - Stop datagen application"
	@echo "  datagen-test     - Test datagen application"
	@echo "  datagen-clean    - Clean datagen build artifacts"
	@echo ""
	@echo "  Kafka Streams:"
	@echo "    kafka-streams          - Build all Kafka Streams applications"
	@echo "    kafka-streams-build    - Build all Kafka Streams applications"
	@echo "    kafka-streams-run      - Run all Kafka Streams applications"
	@echo "    kafka-streams-stop     - Stop all Kafka Streams applications"
	@echo "    kafka-streams-test     - Test all Kafka Streams applications"
	@echo "    kafka-streams-clean    - Clean all Kafka Streams applications"
	@echo ""
	@echo "    Individual Apps:"
	@echo "      parking-space-status-* - Parking space status processor"
	@echo "      row-aggregates-*       - Row aggregation processor"
	@echo "      zone-statistics-*      - Zone statistics processor"
	@echo "      kstreams-utils-*       - Kafka Streams utilities"
	@echo ""

	@echo ""
	@echo "🔧 Utilities:"
	@echo "  utils            - Build utility modules"
	@echo "  utils-build      - Build utility modules"
	@echo "  utils-test       - Test utility modules"
	@echo "  utils-clean      - Clean utility modules"
	@echo ""
	@echo "⚙️  Configuration:"
	@echo "  config-generate  - Generate configuration files from Terraform outputs"
	@echo "  config-validate  - Validate configuration setup"
	@echo "  config-clean     - Clean configuration files"
	@echo ""
	@echo "💡 Use 'make <target>' to run a specific target"
	@echo "💡 Use 'make help-<module>' for module-specific help"

# Configuration validation
check-config:
	@if [ -z "$(PG_CONFIG_HOME)" ]; then \
		echo "❌ PG_CONFIG_HOME not set. Run 'make init-project' first."; \
		exit 1; \
	fi
	@echo "✅ Configuration validated"

# Initialize project configuration directory
init-project:
	@echo "🔧 Initializing project configuration..."
	@rm -f .env
	@read -p "Specify configuration directory (default: $$HOME/tools/parking-garage): " config_dir; \
	if [ -z "$$config_dir" ]; then \
		config_dir="$$HOME/tools/parking-garage"; \
	fi; \
	echo "export PG_CONFIG_HOME=$$config_dir" >> .env; \
	if [ ! -d "$$config_dir" ]; then \
		read -p "Directory does not exist. Create it? [Y/n]: " create_dir; \
		if [ -z "$$create_dir" ] || [ "$$create_dir" = "Y" ] || [ "$$create_dir" = "y" ]; then \
			mkdir -p "$$config_dir"; \
			echo "✅ Created directory: $$config_dir"; \
		else \
			echo "❌ Directory creation skipped. Please create $$config_dir manually."; \
			exit 1; \
		fi; \
	else \
		echo "✅ Directory already exists: $$config_dir"; \
	fi; \
	read -s -p "Enter database master password: " db_password; \
	echo ""; \
	echo "export TF_VAR_db_master_password=$$db_password" >> .env; \
	echo "✅ Project configuration initialized. PG_CONFIG_HOME set to: $$config_dir"

# Complete pipeline
all: check-config infrastructure datagen
	@echo "🎉 All tasks completed successfully!"

# Clean everything
clean: check-config
	@echo "🧹 Cleaning all build artifacts and infrastructure..."
	@make infrastructure-destroy
	@make datagen-stop
	@make kafka-streams-clean

	@make utils-clean
	@make config-clean
	@echo "✅ Complete cleanup finished"

# Module-specific help targets
help-infrastructure:
	@echo "🏗️  Infrastructure targets:"
	@echo ""
	@echo "  Aggregate targets:"
	@echo "    infrastructure         - Deploy all infrastructure (Confluent Cloud + AWS)"
	@echo "    infrastructure-deploy  - Deploy all infrastructure"
	@echo "    infrastructure-destroy - Destroy all infrastructure"
	@echo "    infrastructure-plan    - Generate plans for all infrastructure"
	@echo ""
	@echo "  Confluent Cloud:"
	@echo "    confluent-cloud        - Initialize, plan, and apply Confluent Cloud"
	@echo "    confluent-cloud-init   - Initialize Terraform for Confluent Cloud"
	@echo "    confluent-cloud-plan   - Generate Terraform plan for Confluent Cloud"
	@echo "    confluent-cloud-apply  - Apply Confluent Cloud infrastructure"
	@echo "    confluent-cloud-destroy - Destroy Confluent Cloud infrastructure"
	@echo ""
	@echo "  KS Connectors:"
	@echo "    ks-connectors          - Initialize, plan, and deploy KS Connectors"
	@echo "    ks-connectors-init     - Initialize Terraform for KS Connectors"
	@echo "    ks-connectors-plan     - Generate Terraform plan for KS Connectors"
	@echo "    ks-connectors-deploy   - Deploy Kafka Streams connectors"
	@echo "    ks-connectors-destroy  - Destroy Kafka Streams connectors"
	@echo ""
	@echo "  AWS:"
	@echo "    aws                    - Deploy AWS infrastructure"
	@echo "    aws-deploy             - Deploy AWS infrastructure (RDS, etc.)"
	@echo "    aws-plan               - Generate Terraform plan for AWS"
	@echo "    aws-destroy            - Destroy AWS infrastructure"

help-datagen:
	@echo "📊 Datagen targets:"
	@echo ""
	@echo "  Main targets:"
	@echo "    datagen         - Build and run data generator"
	@echo "    datagen-build   - Build datagen application"
	@echo "    datagen-run     - Run datagen application in background"
	@echo "    datagen-stop    - Stop datagen application"
	@echo "    datagen-test    - Test datagen application"
	@echo "    datagen-clean   - Clean datagen build artifacts"

help-kafka-streams:
	@echo "🔄 Kafka Streams targets:"
	@echo ""
	@echo "  Aggregate targets:"
	@echo "    kafka-streams         - Build all Kafka Streams applications"
	@echo "    kafka-streams-build   - Build all Kafka Streams applications"
	@echo "    kafka-streams-run     - Run all Kafka Streams applications"
	@echo "    kafka-streams-stop    - Stop all Kafka Streams applications"
	@echo "    kafka-streams-test    - Test all Kafka Streams applications"
	@echo "    kafka-streams-clean   - Clean all Kafka Streams applications"
	@echo ""
	@echo "  Individual applications:"
	@echo "    parking-space-status-build  - Build parking space status processor"
	@echo "    parking-space-status-run    - Run parking space status processor"
	@echo "    parking-space-status-stop   - Stop parking space status processor"
	@echo "    parking-space-status-test   - Test parking space status processor"
	@echo "    parking-space-status-clean  - Clean parking space status processor"
	@echo ""
	@echo "    row-aggregates-build        - Build row aggregates processor"
	@echo "    row-aggregates-run          - Run row aggregates processor"
	@echo "    row-aggregates-stop         - Stop row aggregates processor"
	@echo "    row-aggregates-test         - Test row aggregates processor"
	@echo "    row-aggregates-clean        - Clean row aggregates processor"
	@echo ""
	@echo "    zone-statistics-build       - Build zone statistics processor"
	@echo "    zone-statistics-run         - Run zone statistics processor"
	@echo "    zone-statistics-stop        - Stop zone statistics processor"
	@echo "    zone-statistics-test        - Test zone statistics processor"
	@echo "    zone-statistics-clean       - Clean zone statistics processor"
	@echo ""
	@echo "    kstreams-utils-build        - Build Kafka Streams utilities"
	@echo "    kstreams-utils-test         - Test Kafka Streams utilities"
	@echo "    kstreams-utils-clean        - Clean Kafka Streams utilities"

help-utils:
	@echo "🔧 Utils targets:"
	@echo ""
	@echo "  Main targets:"
	@echo "    utils         - Build utility modules"
	@echo "    utils-build   - Build utility modules"
	@echo "    utils-test    - Test utility modules"
	@echo "    utils-clean   - Clean utility modules"

help-config:
	@echo "⚙️  Configuration targets:"
	@echo ""
	@echo "  Main targets:"
	@echo "    config-generate  - Generate configuration files from Terraform outputs"
	@echo "    config-validate  - Validate configuration setup"
	@echo "    config-clean     - Clean configuration files"
