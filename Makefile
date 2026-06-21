SHELL := /usr/bin/env bash

ROOT_DIR := $(abspath $(dir $(lastword $(MAKEFILE_LIST))))
FRONTEND_DIR := $(ROOT_DIR)/apps/frontend
LOG_DIR := $(ROOT_DIR)/.taskmind/logs
PID_DIR := $(ROOT_DIR)/.taskmind/pids
ENV_FILE := $(ROOT_DIR)/infra/env/.env

MVN ?= mvn
NPM ?= npm
SPRING_PROFILES_ACTIVE ?= local
BACKEND_PORT ?= 8080
RELAY_PORT ?= 8081
AI_PORT ?= 8082
FRONTEND_PORT ?= 5173
IMAGE_REGISTRY ?= taskmind
IMAGE_TAG ?= local
VITE_API_BASE_URL ?= /api

.DEFAULT_GOAL := help

.PHONY: help bootstrap install frontend-install build test vibe-verify verify \
	run-backend run-relay run-ai run-frontend \
	start start-backend start-relay start-ai start-frontend stop status logs clean-dev \
	env-example infra-up infra-down frontend-typecheck frontend-build frontend-format \
	image-build image-build-backend image-build-relay image-build-ai image-build-frontend \
	vibe-token-record vibe-token-report

help: ## Show available Makefile targets.
	@awk 'BEGIN {FS = ":.*##"; printf "\nTaskMind development shortcuts\n\n"} /^[a-zA-Z0-9_-]+:.*##/ {printf "  \033[36m%-22s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)
	@printf "\nFast start:\n"
	@printf "  make bootstrap      # install frontend deps\n"
	@printf "  make start          # start Backend, Relay, Nova AI, and Frontend in the background\n"
	@printf "  make status         # show service health/process status\n"
	@printf "  make logs           # tail all background logs\n"
	@printf "  make stop           # stop background services\n\n"

bootstrap: install ## Install local development dependencies.

install: frontend-install ## Alias for installing all non-Maven workspace dependencies.

frontend-install: ## Install Vue frontend npm dependencies.
	cd "$(FRONTEND_DIR)" && $(NPM) install

build: ## Build all Java modules without tests.
	$(MVN) clean install -DskipTests

test: ## Run Java tests.
	$(MVN) test

vibe-verify: ## Run the required quality gate.
	./scripts/vibe-verify.sh

verify: vibe-verify ## Alias for the required quality gate.

frontend-typecheck: ## Run the Vue/TypeScript typecheck.
	cd "$(FRONTEND_DIR)" && $(NPM) run typecheck

frontend-build: ## Build the Vue frontend.
	cd "$(FRONTEND_DIR)" && $(NPM) run build

frontend-format: ## Format the Vue frontend.
	cd "$(FRONTEND_DIR)" && $(NPM) run format:all

image-build: image-build-backend image-build-relay image-build-ai image-build-frontend ## Build all production application images.

image-build-backend: ## Build the Core API production image.
	docker build -f apps/backend/Dockerfile -t $(IMAGE_REGISTRY)/taskmind-backend:$(IMAGE_TAG) .

image-build-relay: ## Build the Relay production image.
	docker build -f apps/relay/Dockerfile -t $(IMAGE_REGISTRY)/taskmind-relay:$(IMAGE_TAG) .

image-build-ai: ## Build the Nova AI production image.
	docker build -f apps/ai/Dockerfile -t $(IMAGE_REGISTRY)/taskmind-ai:$(IMAGE_TAG) .

image-build-frontend: ## Build the Vue SPA production image.
	docker build -f apps/frontend/Dockerfile --build-arg VITE_API_BASE_URL="$(VITE_API_BASE_URL)" -t $(IMAGE_REGISTRY)/taskmind-frontend:$(IMAGE_TAG) .

run-backend: ## Run Core API in the foreground on BACKEND_PORT (default 8080).
	bash -lc 'set -a; [ ! -f "$(ENV_FILE)" ] || source "$(ENV_FILE)"; set +a; exec env SPRING_PROFILES_ACTIVE="$(SPRING_PROFILES_ACTIVE)" SERVER_PORT="$(BACKEND_PORT)" $(MVN) -pl apps/backend spring-boot:run'

run-relay: ## Run Relay in the foreground on RELAY_PORT (default 8081).
	bash -lc 'set -a; [ ! -f "$(ENV_FILE)" ] || source "$(ENV_FILE)"; set +a; exec env SPRING_PROFILES_ACTIVE="$(SPRING_PROFILES_ACTIVE)" SERVER_PORT="$(RELAY_PORT)" $(MVN) -pl apps/relay spring-boot:run'

run-ai: ## Run Nova AI in the foreground on AI_PORT (default 8082).
	bash -lc 'set -a; [ ! -f "$(ENV_FILE)" ] || source "$(ENV_FILE)"; set +a; exec env SPRING_PROFILES_ACTIVE="$(SPRING_PROFILES_ACTIVE)" SERVER_PORT="$(AI_PORT)" $(MVN) -pl apps/ai spring-boot:run'

run-frontend: ## Run the Vue dev server in the foreground on FRONTEND_PORT (default 5173).
	cd "$(FRONTEND_DIR)" && $(NPM) run dev -- --host 0.0.0.0 --port "$(FRONTEND_PORT)"

start: start-backend start-relay start-ai start-frontend ## Start all app services in the background.
	@$(MAKE) --no-print-directory status

start-backend: ## Start Core API in the background.
	@$(MAKE) --no-print-directory _start NAME=backend CMD='env SPRING_PROFILES_ACTIVE="$(SPRING_PROFILES_ACTIVE)" SERVER_PORT="$(BACKEND_PORT)" $(MVN) -pl apps/backend spring-boot:run'

start-relay: ## Start Relay in the background.
	@$(MAKE) --no-print-directory _start NAME=relay CMD='env SPRING_PROFILES_ACTIVE="$(SPRING_PROFILES_ACTIVE)" SERVER_PORT="$(RELAY_PORT)" $(MVN) -pl apps/relay spring-boot:run'

start-ai: ## Start Nova AI in the background.
	@$(MAKE) --no-print-directory _start NAME=ai CMD='env SPRING_PROFILES_ACTIVE="$(SPRING_PROFILES_ACTIVE)" SERVER_PORT="$(AI_PORT)" $(MVN) -pl apps/ai spring-boot:run'

start-frontend: ## Start the Vue dev server in the background.
	@$(MAKE) --no-print-directory _start NAME=frontend CMD='cd "$(FRONTEND_DIR)" && $(NPM) run dev -- --host 0.0.0.0 --port "$(FRONTEND_PORT)"'

_start:
	@mkdir -p "$(LOG_DIR)" "$(PID_DIR)"
	@if [ -f "$(PID_DIR)/$(NAME).pid" ] && kill -0 "$$(cat "$(PID_DIR)/$(NAME).pid")" 2>/dev/null; then \
		echo "$(NAME) is already running (pid $$(cat "$(PID_DIR)/$(NAME).pid"))."; \
	else \
		echo "Starting $(NAME); logs: $(LOG_DIR)/$(NAME).log"; \
		bash -lc 'cd "$(ROOT_DIR)" && set -a; [ ! -f "$(ENV_FILE)" ] || source "$(ENV_FILE)"; set +a; exec $(CMD)' >"$(LOG_DIR)/$(NAME).log" 2>&1 & \
		echo $$! >"$(PID_DIR)/$(NAME).pid"; \
	fi

stop: ## Stop background services started by this Makefile.
	@mkdir -p "$(PID_DIR)"
	@for name in frontend ai relay backend; do \
		pid_file="$(PID_DIR)/$$name.pid"; \
		if [ -f "$$pid_file" ]; then \
			pid="$$(cat "$$pid_file")"; \
			if kill -0 "$$pid" 2>/dev/null; then \
				echo "Stopping $$name (pid $$pid)"; \
				kill "$$pid" 2>/dev/null || true; \
			else \
				echo "$$name is not running."; \
			fi; \
			rm -f "$$pid_file"; \
		fi; \
	done

status: ## Show process and HTTP health status for local app services.
	@mkdir -p "$(PID_DIR)"
	@for entry in "backend:$(BACKEND_PORT):/api/health" "relay:$(RELAY_PORT):/api/health" "ai:$(AI_PORT):/actuator/health" "frontend:$(FRONTEND_PORT):/"; do \
		IFS=: read -r name port path <<<"$$entry"; \
		pid_file="$(PID_DIR)/$$name.pid"; \
		if [ -f "$$pid_file" ] && kill -0 "$$(cat "$$pid_file")" 2>/dev/null; then proc="running pid $$(cat "$$pid_file")"; else proc="not started"; fi; \
		if command -v curl >/dev/null 2>&1 && curl -fsS "http://localhost:$$port$$path" >/dev/null 2>&1; then http="http ok"; else http="http pending"; fi; \
		printf "%-10s %-18s %s\n" "$$name" "$$proc" "$$http"; \
	done

logs: ## Tail logs for services started in the background.
	@mkdir -p "$(LOG_DIR)"
	tail -n 120 -F "$(LOG_DIR)"/*.log

clean-dev: stop ## Remove local Makefile runtime logs and pid files.
	rm -rf "$(ROOT_DIR)/.taskmind"

env-example: ## Create infra/env/.env from infra/env/.env.example when available.
	@if [ -f "$(ROOT_DIR)/infra/env/.env.example" ]; then \
		mkdir -p "$(ROOT_DIR)/infra/env"; \
		cp -n "$(ROOT_DIR)/infra/env/.env.example" "$(ROOT_DIR)/infra/env/.env"; \
		echo "Prepared infra/env/.env"; \
	else \
		echo "infra/env/.env.example does not exist yet; skipping."; \
	fi

infra-up: ## Start local infrastructure if compose files exist.
	@if [ -f "$(ROOT_DIR)/docker-compose.yml" ] || [ -f "$(ROOT_DIR)/compose.yml" ]; then \
		docker compose up -d; \
	else \
		echo "No compose file exists yet; nothing to start."; \
	fi

infra-down: ## Stop local infrastructure if compose files exist.
	@if [ -f "$(ROOT_DIR)/docker-compose.yml" ] || [ -f "$(ROOT_DIR)/compose.yml" ]; then \
		docker compose down; \
	else \
		echo "No compose file exists yet; nothing to stop."; \
	fi

vibe-token-record: ## Record optional local AI coding telemetry.
	python3 scripts/vibe-token-usage.py record $(filter-out $@,$(MAKECMDGOALS))

vibe-token-report: ## Report optional local AI coding telemetry.
	python3 scripts/vibe-token-usage.py report $(filter-out $@,$(MAKECMDGOALS))

%:
	@:
