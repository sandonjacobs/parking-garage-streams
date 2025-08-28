# Utils module - Shared utilities and common functionality

.PHONY: utils utils-build utils-test utils-clean

# Utils targets
utils: utils-build

utils-build: check-config
	@echo "🔨 Building utility modules..."
	$(GRADLE_CMD) :utils:build
	@echo "✅ Utility modules built successfully"

utils-test: check-config
	@echo "🧪 Testing utility modules..."
	$(GRADLE_CMD) :utils:test
	@echo "✅ Utility tests completed"

utils-clean: check-config
	@echo "🧹 Cleaning utility modules..."
	$(GRADLE_CMD) :utils:clean
	@echo "✅ Utilities cleaned"
