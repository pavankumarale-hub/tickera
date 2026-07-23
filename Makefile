# Tickera developer shortcuts
.DEFAULT_GOAL := help

.PHONY: help build test test-unit test-contract test-integration up down logs demo demo-fail pacts seed clean ui

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

build: ## Compile all modules and package jars (skips tests)
	./mvnw -q -DskipTests package

test: ## Run all tests — unit + contract + integration (Testcontainers needs Docker)
	./mvnw test

test-unit: ## Run unit tests only (@Tag("unit")) — no Docker required
	./mvnw test -Dgroups=unit

test-contract: ## Generate consumer pact then verify it on the provider (@Tag("contract"))
	./mvnw -pl payment-service test -Dtest=BookingEventsConsumerPactTest \
		-Dsurefire.failIfNoSpecifiedTests=false
	./mvnw -pl booking-service test -Dtest=BookingEventsProviderPactTest \
		-Dsurefire.failIfNoSpecifiedTests=false

test-integration: ## Run Testcontainers integration tests (@Tag("integration")) — needs Docker
	./mvnw test -Dgroups=integration

pacts: test-contract ## Alias for test-contract

up: ## Build images and start the whole stack (detached)
	docker compose up --build -d

down: ## Stop the stack and remove volumes
	docker compose down -v

logs: ## Tail the three service logs
	docker compose logs -f booking-service payment-service notification-service

demo: ## Run the end-to-end happy-path demo (booking → PAID)
	./scripts/demo.sh

demo-fail: ## Run the compensation-path demo (amount > $1000 → DECLINED → CANCELLED)
	./scripts/demo.sh --fail

seed: ## Seed 9 realistic bookings (4 PAID, 2 CANCELLED, 3 CREATED) for UI demo
	./scripts/seed.sh

ui: ## Start the React dev server (Vite) — services must be running on ports 808x
	cd ui && npm install && npm run dev

clean: ## Remove build output
	./mvnw -q clean
