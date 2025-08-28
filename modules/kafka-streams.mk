# Kafka Streams module - Stream processing applications

.PHONY: kafka-streams kafka-streams-build kafka-streams-run kafka-streams-stop kafka-streams-test kafka-streams-clean
.PHONY: $(KAFKA_STREAMS_MODULES:%=%-build) $(KAFKA_STREAMS_MODULES:%=%-run) $(KAFKA_STREAMS_MODULES:%=%-stop) $(KAFKA_STREAMS_MODULES:%=%-test)

# Kafka Streams targets
kafka-streams: kafka-streams-build

kafka-streams-build: check-config $(KAFKA_STREAMS_MODULES:%=%-build)
	@echo "✅ All Kafka Streams applications built successfully"

kafka-streams-run: check-config $(KAFKA_STREAMS_TOPOLOGIES:%=%-run)
	@echo "✅ All Kafka Streams applications started"

kafka-streams-stop: check-config $(KAFKA_STREAMS_TOPOLOGIES:%=%-stop)
	@echo "✅ All Kafka Streams applications stopped"

kafka-streams-test: check-config $(KAFKA_STREAMS_MODULES:%=%-test)
	@echo "✅ All Kafka Streams tests completed"

kafka-streams-clean: check-config $(KAFKA_STREAMS_MODULES:%=%-clean)
	@echo "✅ All Kafka Streams applications cleaned"

# Individual application targets
parking-space-status-build:
	@echo "🔨 Building parking-space-status..."
	$(GRADLE_CMD) :kafka-streams:parking-space-status:build

parking-space-status-run:
	@echo "🚀 Running parking-space-status..."
	@if [ -f $(KAFKA_STREAMS_PATH)/parking-space-status/parking-space-status.pid ]; then \
		echo "⚠️  parking-space-status already running. Stopping first..."; \
		make parking-space-status-stop; \
	fi
	$(GRADLE_CMD) :kafka-streams:parking-space-status:run --args="-c $(PG_CONFIG_HOME)/cc.properties" > $(KAFKA_STREAMS_PATH)/parking-space-status/parking-space-status.log 2>&1 & \
	echo $$! > $(KAFKA_STREAMS_PATH)/parking-space-status/parking-space-status.pid
	@echo "✅ parking-space-status started (PID: $$(cat $(KAFKA_STREAMS_PATH)/parking-space-status/parking-space-status.pid))"

parking-space-status-stop:
	@echo "🛑 Stopping parking-space-status..."
	@if [ -f $(KAFKA_STREAMS_PATH)/parking-space-status/parking-space-status.pid ]; then \
		kill $$(cat $(KAFKA_STREAMS_PATH)/parking-space-status/parking-space-status.pid) 2>/dev/null || true; \
		rm -f $(KAFKA_STREAMS_PATH)/parking-space-status/parking-space-status.pid; \
		echo "✅ parking-space-status stopped"; \
	else \
		echo "ℹ️  parking-space-status not running"; \
	fi

parking-space-status-test:
	@echo "🧪 Testing parking-space-status..."
	$(GRADLE_CMD) :kafka-streams:parking-space-status:test

parking-space-status-clean:
	@echo "🧹 Cleaning parking-space-status..."
	$(GRADLE_CMD) :kafka-streams:parking-space-status:clean
	@rm -f $(KAFKA_STREAMS_PATH)/parking-space-status/parking-space-status.pid
	@rm -f $(KAFKA_STREAMS_PATH)/parking-space-status/parking-space-status.log

row-aggregates-build:
	@echo "🔨 Building row-aggregates..."
	$(GRADLE_CMD) :kafka-streams:row-aggregates:build

row-aggregates-run:
	@echo "🚀 Running row-aggregates..."
	@if [ -f $(KAFKA_STREAMS_PATH)/row-aggregates/row-aggregates.pid ]; then \
		echo "⚠️  row-aggregates already running. Stopping first..."; \
		make row-aggregates-stop; \
	fi
	$(GRADLE_CMD) :kafka-streams:row-aggregates:run --args="-c $(PG_CONFIG_HOME)/cc.properties" > $(KAFKA_STREAMS_PATH)/row-aggregates/row-aggregates.log 2>&1 & \
	echo $$! > $(KAFKA_STREAMS_PATH)/row-aggregates/row-aggregates.pid
	@echo "✅ row-aggregates started (PID: $$(cat $(KAFKA_STREAMS_PATH)/row-aggregates/row-aggregates.pid))"

row-aggregates-stop:
	@echo "🛑 Stopping row-aggregates..."
	@if [ -f $(KAFKA_STREAMS_PATH)/row-aggregates/row-aggregates.pid ]; then \
		kill $$(cat $(KAFKA_STREAMS_PATH)/row-aggregates/row-aggregates.pid) 2>/dev/null || true; \
		rm -f $(KAFKA_STREAMS_PATH)/row-aggregates/row-aggregates.pid; \
		echo "✅ row-aggregates stopped"; \
	else \
		echo "ℹ️  row-aggregates not running"; \
	fi

row-aggregates-test:
	@echo "🧪 Testing row-aggregates..."
	$(GRADLE_CMD) :kafka-streams:row-aggregates:test

row-aggregates-clean:
	@echo "🧹 Cleaning row-aggregates..."
	$(GRADLE_CMD) :kafka-streams:row-aggregates:clean
	@rm -f $(KAFKA_STREAMS_PATH)/row-aggregates/row-aggregates.pid
	@rm -f $(KAFKA_STREAMS_PATH)/row-aggregates/row-aggregates.log

zone-statistics-build:
	@echo "🔨 Building zone-statistics..."
	$(GRADLE_CMD) :kafka-streams:zone-statistics:build

zone-statistics-run:
	@echo "🚀 Running zone-statistics..."
	@if [ -f $(KAFKA_STREAMS_PATH)/zone-statistics/zone-statistics.pid ]; then \
		echo "⚠️  zone-statistics already running. Stopping first..."; \
		make zone-statistics-stop; \
	fi
	$(GRADLE_CMD) :kafka-streams:zone-statistics:run --args="-c $(PG_CONFIG_HOME)/cc.properties" > $(KAFKA_STREAMS_PATH)/zone-statistics/zone-statistics.log 2>&1 & \
	echo $$! > $(KAFKA_STREAMS_PATH)/zone-statistics/zone-statistics.pid
	@echo "✅ zone-statistics started (PID: $$(cat $(KAFKA_STREAMS_PATH)/zone-statistics/zone-statistics.pid))"

zone-statistics-stop:
	@echo "🛑 Stopping zone-statistics..."
	@if [ -f $(KAFKA_STREAMS_PATH)/zone-statistics/zone-statistics.pid ]; then \
		kill $$(cat $(KAFKA_STREAMS_PATH)/zone-statistics/zone-statistics.pid) 2>/dev/null || true; \
		rm -f $(KAFKA_STREAMS_PATH)/zone-statistics/zone-statistics.pid; \
		echo "✅ zone-statistics stopped"; \
	else \
		echo "ℹ️  zone-statistics not running"; \
	fi

zone-statistics-test:
	@echo "🧪 Testing zone-statistics..."
	$(GRADLE_CMD) :kafka-streams:zone-statistics:test

zone-statistics-clean:
	@echo "🧹 Cleaning zone-statistics..."
	$(GRADLE_CMD) :kafka-streams:zone-statistics:clean
	@rm -f $(KAFKA_STREAMS_PATH)/zone-statistics/zone-statistics.pid
	@rm -f $(KAFKA_STREAMS_PATH)/zone-statistics/zone-statistics.log

kstreams-utils-build:
	@echo "🔨 Building kstreams-utils..."
	$(GRADLE_CMD) :kafka-streams:kstreams-utils:build

kstreams-utils-test:
	@echo "🧪 Testing kstreams-utils..."
	$(GRADLE_CMD) :kafka-streams:kstreams-utils:test

kstreams-utils-clean:
	@echo "🧹 Cleaning kstreams-utils..."
	$(GRADLE_CMD) :kafka-streams:kstreams-utils:clean
