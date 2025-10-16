.PHONY: help build build-% clean up down logs

REGISTRY := brewflow
SERVICES := user-service order-service payment-service notification-service api-gateway

PORT_user-service := 8081
PORT_order-service := 8082
PORT_payment-service := 8083
PORT_notification-service := 8084
PORT_api-gateway := 8080

GREEN  := \033[0;32m
BLUE   := \033[0;34m
YELLOW := \033[0;33m
RED    := \033[0;31m
NC     := \033[0m

export DOCKER_BUILDKIT=1

help:
	@echo "$(BLUE)BrewFlow Docker Build System$(NC)"
	@echo ""
	@echo "$(GREEN)Build Commands:$(NC)"
	@echo "  make build              - Build all services"
	@echo "  make build-SERVICE      - Build specific service"
	@echo "  make clean              - Remove all built images"
	@echo ""
	@echo "$(GREEN)Docker Compose Commands:$(NC)"
	@echo "  make up                 - Start all services"
	@echo "  make down               - Stop all services"
	@echo "  make logs               - View logs"
	@echo ""
	@echo "$(YELLOW)Available Services:$(NC)"
	@for service in $(SERVICES); do echo "  - $$service"; done

build: $(addprefix build-,$(SERVICES))
	@echo "$(GREEN)All services built successfully$(NC)"

build-%:
	@echo "$(BLUE)Building $* (port: $(PORT_$*))...$(NC)"
	@docker build \
		--build-arg SERVICE_NAME=$* \
		--build-arg SERVICE_PORT=$(PORT_$*) \
		-t $(REGISTRY)/$*:latest \
		-f Dockerfile.template \
		. \
	&& echo "$(GREEN)$* built successfully$(NC)" \
	|| { echo "$(RED)Failed to build $*$(NC)"; exit 1; }

up:
	@echo "$(BLUE)Starting all services...$(NC)"
	@docker compose up -d
	@echo "$(GREEN)Services started$(NC)"
	@echo "$(YELLOW)API Gateway: http://localhost:8080$(NC)"
	@echo "$(YELLOW)Zipkin UI: http://localhost:9411$(NC)"

down:
	@echo "$(BLUE)Stopping all services...$(NC)"
	@docker compose down
	@echo "$(GREEN)Services stopped$(NC)"

logs:
	@docker compose logs -f

logs-%:
	@docker compose logs -f $*

clean:
	@echo "$(RED)Removing all built images...$(NC)"
	@for service in $(SERVICES); do \
		docker rmi $(REGISTRY)/$$service:latest 2>/dev/null || true; \
	done
	@echo "$(GREEN)Cleanup complete$(NC)"

rebuild: clean build

ps:
	@docker compose ps

.DEFAULT_GOAL := help
