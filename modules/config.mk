# Configuration module - Environment and configuration management

.PHONY: config-generate config-validate config-clean

# Configuration targets
config-generate: check-config
	@echo "📝 Generating configuration files..."
	@if [ -f "$(CONFLUENT_CLOUD_PATH)/base-env/terraform.tfstate" ]; then \
		echo "Generating Confluent Cloud configuration..."; \
		cd $(CONFLUENT_CLOUD_PATH)/base-env && \
		terraform output -json | \
		jq -r 'to_entries[] | "\(.key)=\(.value.value)"' > \
		$(PG_CONFIG_HOME)/cc.properties; \
	fi
	@if [ -f "$(AWS_PATH)/terraform.tfstate" ]; then \
		echo "Generating AWS configuration..."; \
		cd $(AWS_PATH) && \
		terraform output -json | \
		jq -r 'to_entries[] | "\(.key)=\(.value.value)"' > \
		$(PG_CONFIG_HOME)/aws.properties; \
	fi
	@echo "✅ Configuration files generated"

config-validate: check-config
	@echo "🔍 Validating configuration..."
	@if [ ! -d "$(PG_CONFIG_HOME)" ]; then \
		echo "❌ Configuration directory does not exist: $(PG_CONFIG_HOME)"; \
		exit 1; \
	fi
	@if [ -z "$(TF_VAR_db_master_password)" ]; then \
		echo "❌ Database password not set"; \
		exit 1; \
	fi
	@echo "✅ Configuration validated"

config-clean:
	@echo "🧹 Cleaning configuration files..."
	@rm -f $(PG_CONFIG_HOME)/cc.properties
	@rm -f $(PG_CONFIG_HOME)/aws.properties
	@rm -f $(CONFLUENT_CLOUD_PATH)/ks-connectors/cc.auto.tfvars
	@rm -f $(CONFLUENT_CLOUD_PATH)/ks-connectors/aws.auto.tfvars
	@rm -f $(CONFLUENT_CLOUD_PATH)/base-env/terraform.tfvars
	@echo "✅ Configuration files cleaned"
