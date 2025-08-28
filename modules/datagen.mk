# Datagen module - Spring Boot data generator application

.PHONY: datagen datagen-build datagen-run datagen-stop datagen-test datagen-clean

# Datagen targets
datagen: datagen-build datagen-run

datagen-build: check-config
	@echo "🔨 Building datagen application..."
	$(GRADLE_CMD) :datagen:build
	@echo "✅ Datagen application built successfully"

datagen-run: check-config
	@echo "🚀 Building and running datagen Spring Boot application in background..."
	@echo "Using profile: cc"
	@echo "Configuration directory: $(PG_CONFIG_HOME)"
	@echo "Application will run in background. Use 'make datagen-stop' to stop it."
	@if [ -f $(DATAGEN_PATH)/datagen.pid ]; then \
		echo "⚠️  Datagen application already running. Stopping first..."; \
		make datagen-stop; \
	fi
	nohup $(GRADLE_CMD) :datagen:bootRun --args='--spring.profiles.active=cc' > $(DATAGEN_PATH)/datagen.log 2>&1 & \
	echo $$! > $(DATAGEN_PATH)/datagen.pid
	@echo "✅ Datagen application started in background (PID: $$(cat $(DATAGEN_PATH)/datagen.pid))"
	@echo "📋 Logs available at: $(DATAGEN_PATH)/datagen.log"

datagen-stop:
	@echo "🛑 Stopping datagen application..."
	@if [ -f $(DATAGEN_PATH)/datagen.pid ]; then \
		kill $$(cat $(DATAGEN_PATH)/datagen.pid) 2>/dev/null || true; \
		rm -f $(DATAGEN_PATH)/datagen.pid; \
		echo "✅ Datagen application stopped"; \
	else \
		echo "ℹ️  No datagen application running (no PID file found)"; \
	fi

datagen-test: check-config
	@echo "🧪 Testing datagen application..."
	$(GRADLE_CMD) :datagen:test
	@echo "✅ Datagen tests completed"

datagen-clean: datagen-stop
	@echo "🧹 Cleaning datagen build artifacts..."
	$(GRADLE_CMD) :datagen:clean
	@rm -f $(DATAGEN_PATH)/datagen.pid
	@rm -f $(DATAGEN_PATH)/datagen.log
	@echo "✅ Datagen cleaned"
