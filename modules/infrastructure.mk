# Infrastructure module - Terraform-based deployments
# Handles Confluent Cloud and AWS infrastructure

.PHONY: infrastructure infrastructure-deploy infrastructure-destroy infrastructure-plan
.PHONY: confluent-cloud confluent-cloud-deploy confluent-cloud-destroy
.PHONY: aws aws-deploy aws-destroy
.PHONY: ks-connectors ks-connectors-deploy ks-connectors-destroy
.PHONY: db-create-schema db-init

# Database schema configuration
# Use environment variable if set, otherwise default to parking
DB_SCHEMA_NAME ?= $(shell if [ -f .env ] && grep -q '^export DB_SCHEMA_NAME=' .env; then grep '^export DB_SCHEMA_NAME=' .env | cut -d'=' -f2; else echo "parking"; fi)

# Infrastructure targets
infrastructure: infrastructure-deploy

infrastructure-deploy: check-config confluent-cloud-deploy aws-deploy
	@echo "✅ All infrastructure deployed successfully"

infrastructure-destroy: check-config ks-connectors-destroy confluent-cloud-destroy aws-destroy
	@echo "✅ All infrastructure destroyed successfully"

infrastructure-plan: check-config confluent-cloud-plan aws-plan
	@echo "✅ Infrastructure plans generated"

# Confluent Cloud targets
confluent-cloud: _confluent-cloud-init confluent-cloud-plan _confluent-cloud-apply

_confluent-cloud-apply:
	@echo "🚀 Terraform Applying Confluent Cloud infrastructure..."
	cd $(CONFLUENT_CLOUD_PATH)/base-env && \
	terraform apply -auto-approve
	@echo "📝 Generating configuration files..."
	@echo "Generating cc.auto.tfvars file..."
	cd $(CONFLUENT_CLOUD_PATH)/base-env && \
	terraform output -json | \
	jq -r 'to_entries[] | "\(.key) = \"\(.value.value)\""' > \
	../ks-connectors/cc.auto.tfvars
	@echo "Generating cc.properties file..."
	cd $(CONFLUENT_CLOUD_PATH)/base-env && \
	terraform output -json | \
	jq -r 'to_entries[] | "\(.key)=\(.value.value)"' > \
	$(PG_CONFIG_HOME)/cc.properties
	@echo "✅ Confluent Cloud base-env created."

_confluent-cloud-init: check-config
	@echo "🚀 Initializing Terraform for Confluent Cloud infrastructure..."
	@echo "📝 Getting Confluent Cloud organization ID..."
	@if ! confluent organization list -o json | jq -r '.[] | select(.is_current) | .id' > /tmp/org_id.tmp; then \
		echo "❌ Failed to get Confluent Cloud organization ID. Please ensure you are logged in with 'confluent login'"; \
		exit 1; \
	fi; \
	if [ ! -s /tmp/org_id.tmp ]; then \
		echo "❌ No organization ID found. Please ensure you have access to a Confluent Cloud organization"; \
		rm -f /tmp/org_id.tmp; \
		exit 1; \
	fi; \
	echo "org_id = \"$$(cat /tmp/org_id.tmp)\"" > $(CONFLUENT_CLOUD_PATH)/base-env/terraform.tfvars && \
	echo "✅ Organization ID: $$(cat /tmp/org_id.tmp)"; \
	rm -f /tmp/org_id.tmp
	@echo "🚀 Deploying Confluent Cloud base environment..."
	cd $(CONFLUENT_CLOUD_PATH)/base-env && \
	terraform init
	@echo "✅ Terraform initialized for Confluent Cloud"

confluent-cloud-plan:
	@echo "🚀 Creating Terraform Plan for Confluent Cloud base environment..."
	cd $(CONFLUENT_CLOUD_PATH)/base-env && \
	terraform plan
	@echo "✅ Terraform Planned for Confluent Cloud"


confluent-cloud-destroy: check-config
	@echo "🛑 Destroying Confluent Cloud infrastructure..."
	@echo "Destroying Confluent Cloud connectors..."
	cd $(CONFLUENT_CLOUD_PATH)/ks-connectors && terraform destroy -auto-approve || true
	@echo "Destroying Confluent Cloud base environment..."
	cd $(CONFLUENT_CLOUD_PATH)/base-env && terraform destroy -auto-approve || true
	@echo "Removing generated configuration files..."
	@rm -f $(CONFLUENT_CLOUD_PATH)/ks-connectors/cc.auto.tfvars
	@rm -f $(PG_CONFIG_HOME)/cc.properties
	@rm -f $(CONFLUENT_CLOUD_PATH)/base-env/terraform.tfvars
	@rm -f $(CONFLUENT_CLOUD_PATH)/base-env/.env
	@rm -f $(CONFLUENT_CLOUD_PATH)/ks-connectors/.env
	@echo "✅ Confluent Cloud infrastructure destroyed"

# KS Connectors targets
ks-connectors: ks-connectors-init ks-connectors-plan ks-connectors-deploy

ks-connectors-init: check-config
	@echo "🔧 Initializing KS Connectors Terraform..."
	cd $(CONFLUENT_CLOUD_PATH)/ks-connectors && \
	terraform init
	@echo "✅ KS Connectors Terraform initialized"

ks-connectors-plan: check-config
	@echo "📋 Generating KS Connectors plan..."
	@ln -sf .env $(CONFLUENT_CLOUD_PATH)/ks-connectors/.env
	cd $(CONFLUENT_CLOUD_PATH)/ks-connectors && \
	terraform plan -out=tfplan
	@echo "✅ KS Connectors plan generated"

ks-connectors-deploy: check-config
	@echo "🚀 Deploying KS Connectors..."
	cd $(CONFLUENT_CLOUD_PATH)/ks-connectors && \
	terraform apply -auto-approve
	@echo "✅ KS Connectors deployed successfully"

ks-connectors-destroy: check-config
	@echo "🛑 Destroying KS Connectors..."
	cd $(CONFLUENT_CLOUD_PATH)/ks-connectors && \
	terraform destroy -auto-approve || true
	@echo "✅ KS Connectors destroyed"

# AWS targets
aws: aws-deploy

aws-deploy: check-config
	@echo "🚀 Deploying AWS infrastructure..."
	@ln -sf .env $(AWS_PATH)/.env
	cd $(AWS_PATH) && \
	terraform init && \
	terraform plan && \
	terraform apply -auto-approve
	@echo "📝 Generating AWS configuration files..."
	@mkdir -p $(CONFLUENT_CLOUD_PATH)/ks-connectors
	@echo "Generating aws.auto.tfvars file..."
	cd $(AWS_PATH) && \
	terraform output -json | \
	jq -r 'to_entries[] | "\(.key) = \"\(.value.value)\""' > \
	../$(CONFLUENT_CLOUD_PATH)/ks-connectors/aws.auto.tfvars
	@echo "Generating aws.properties file..."
	cd $(AWS_PATH) && \
	terraform output -json | \
	jq -r 'to_entries[] | "\(.key)=\(.value.value)"' > \
	$(PG_CONFIG_HOME)/aws.properties
	@echo "✅ AWS infrastructure deployed"

aws-destroy: check-config
	@echo "🛑 Destroying AWS infrastructure..."
	@echo "⚠️  WARNING: This will destroy all AWS infrastructure. This is a critical operation."
	@echo "   If terraform destroy fails, the process will stop to prevent orphaned infrastructure."
	cd $(AWS_PATH) && terraform destroy -auto-approve
	@echo "Removing Terraform state and plan files..."
	@rm -rf $(AWS_PATH)/.terraform
	@rm -f $(AWS_PATH)/terraform.tfstate*
	@rm -f $(AWS_PATH)/.terraform.lock.hcl
	@rm -f $(AWS_PATH)/tfplan
	@echo "Removing generated configuration files..."
	@rm -f $(CONFLUENT_CLOUD_PATH)/ks-connectors/aws.auto.tfvars
	@rm -f $(PG_CONFIG_HOME)/aws.properties
	@rm -f $(AWS_PATH)/.env
	@echo "✅ AWS infrastructure destroyed"

aws-plan: check-config
	@echo "📋 Generating AWS plan..."
	@ln -sf .env $(AWS_PATH)/.env
	cd $(AWS_PATH) && \
	terraform init && \
	terraform plan -out=tfplan

# Database management targets
db-create-schema: check-config
	@echo "🗄️  Creating database schema '$(DB_SCHEMA_NAME)' using Docker..."
	@if [ ! -f "$(PG_CONFIG_HOME)/aws.properties" ]; then \
		echo "❌ AWS properties file not found. Please run 'make aws-deploy' first."; \
		exit 1; \
	fi
	@echo "📝 Executing schema creation with PostgreSQL Docker container..."
	@docker run --rm \
		-e PGPASSWORD=$$(grep 'TF_VAR_db_master_password' .env | cut -d'=' -f2) \
		postgres:16-alpine \
		psql -h $$(grep 'postgres_cluster_endpoint' $(PG_CONFIG_HOME)/aws.properties | cut -d'=' -f2) \
		-p $$(grep 'postgres_cluster_port' $(PG_CONFIG_HOME)/aws.properties | cut -d'=' -f2) \
		-U $$(grep 'cluster_master_username' $(PG_CONFIG_HOME)/aws.properties | cut -d'=' -f2) \
		-d $$(grep 'postgres_database_name' $(PG_CONFIG_HOME)/aws.properties | cut -d'=' -f2) \
		-c "CREATE SCHEMA IF NOT EXISTS $(DB_SCHEMA_NAME); GRANT USAGE ON SCHEMA $(DB_SCHEMA_NAME) TO PUBLIC; GRANT CREATE ON SCHEMA $(DB_SCHEMA_NAME) TO PUBLIC;"
	@echo "✅ Schema '$(DB_SCHEMA_NAME)' created successfully"

db-init: check-config
	@echo "🔧 Initializing database management..."
	@echo "✅ Database management initialized (Docker-based)"
