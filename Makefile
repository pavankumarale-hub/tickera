# TicketHub developer shortcuts
.DEFAULT_GOAL := help

.PHONY: help build test up down logs demo pacts clean

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-10s\033[0m %s\n", $$1, $$2}'

build: ## Compile all modules and package jars (skips tests)
	./mvnw -q -DskipTests package

test: ## Run unit, contract, and integration tests (Testcontainers needs Docker)
	./mvnw test

pacts: ## Generate consumer pacts, then verify them against the provider
	./mvnw -q -pl payment-service test -Dtest=BookingEventsConsumerPactTest
	./mvnw -q -pl booking-service test -Dtest=BookingEventsProviderPactTest

up: ## Build images and start the whole stack
	docker compose up --build -d

down: ## Stop the stack and remove volumes
	docker compose down -v

logs: ## Tail the three service logs
	docker compose logs -f booking-service payment-service notification-service

demo: ## Run the end-to-end happy-path demo against the running stack
	./scripts/demo.sh

clean: ## Remove build output
	./mvnw -q clean
