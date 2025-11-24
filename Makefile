.PHONY: help build build-% clean up down logs k8s-deploy k8s-stop k8s-restart k8s-status k8s-clean

REGISTRY := brewflow
SERVICES := user-service order-service payment-service notification-service api-gateway
HELM_DIR := helm-charts

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
	@echo "$(GREEN)Команды сборки:$(NC)"
	@echo "  make build              - Собрать все сервисы"
	@echo "  make build-SERVICE      - Собрать определенный сервис"
	@echo "  make clean              - Удалить все образы сервисов"
	@echo ""
	@echo "$(GREEN)Команды docker compose:$(NC)"
	@echo "  make up                 - Запустить все сервисы"
	@echo "  make down               - Остановить все сервисы"
	@echo "  make logs               - Посмотреть логи"
	@echo "  make logs-SERVICE       - Посмотреть логи определенного сервиса"
	@echo ""
	@echo "$(GREEN)Команды k8s:$(NC)"
	@echo "  make k8s-deploy                - Установка всей системы"
	@echo "  make k8s-stop                  - Удалить поды"
	@echo "  make k8s-restart               - Удалить поды"
	@echo "  make k8s-status                - Показать статусы всех подов"
	@echo "  make k8s-clean                 - Выполнить полную очистку вместе с namespace"
	@echo "  make k8s-logs-SERVICE          - Показать логи пода приложения"
	@echo "  make k8s-logs-infra-SERVICE    - Показать логи пода инфраструктуры"
	@echo "  make k8s-logs-obs-SERVICE      - Показать логи пода наблюдения"
	@echo ""
	@echo "$(YELLOW)Доступные сервисы:$(NC)"
	@for service in $(SERVICES); do echo "  - $$service"; done
	@echo ""
	@echo "$(YELLOW)Доступ k8s:$(NC)"
	@echo "  Grafana:     http://localhost:30300 (admin/admin)"
	@echo "  MailHog UI:  http://localhost:30080"
	@echo "  API Gateway: http://localhost:30000"


build: $(addprefix build-,$(SERVICES))
	@echo "$(GREEN)All services built successfully$(NC)"

build-%:
	@echo "$(BLUE)Сборка $* (port: $(PORT_$*))...$(NC)"
	@docker build \
		--build-arg SERVICE_NAME=$* \
		--build-arg SERVICE_PORT=$(PORT_$*) \
		-t $(REGISTRY)/$*:latest \
		-f Dockerfile.template \
		. \
	&& echo "$(GREEN)$* сборка успешна$(NC)" \
	|| { echo "$(RED)Сборка провалена $*$(NC)"; exit 1; }

clean:
	@echo "$(RED)Удаление всех образов...$(NC)"
	@for service in $(SERVICES); do \
		docker rmi $(REGISTRY)/$$service:latest 2>/dev/null || true; \
	done
	@echo "$(GREEN)Удаление завершено$(NC)"

rebuild: clean build

up:
	@echo "$(BLUE)Запуск всех сервисов через docker compose...$(NC)"
	@docker compose up -d
	@echo "$(GREEN)Сервисы запущены$(NC)"
	@echo "$(YELLOW)API Gateway: http://localhost:8080$(NC)"

down:
	@echo "$(BLUE)Остановка всех сервисов...$(NC)"
	@docker compose down
	@echo "$(GREEN)Сервисы остановлены$(NC)"

logs:
	@docker compose logs -f

logs-%:
	@docker compose logs -f $*

ps:
	@docker compose ps

k8s-deploy:
	@echo "$(BLUE) Развертывание BrewFlow в k8s...$(NC)"
	@cd $(HELM_DIR)/brewflow-app && helm dependency build
	@helm install -n brewflow-observability --create-namespace \
		brewflow-observability $(HELM_DIR)/brewflow-observability
	@helm install -n brewflow-infra --create-namespace \
		brewflow-infra $(HELM_DIR)/brewflow-infrastructure
	@echo "$(YELLOW) Подождем запуск инфраструктуры (30s)...$(NC)"
	@sleep 30
	@helm install -n brewflow --create-namespace \
		brewflow $(HELM_DIR)/brewflow-app
	@echo "$(GREEN) Развертывание завершено!$(NC)"
	@echo ""
	@echo "$(YELLOW)Доступ:$(NC)"
	@echo "  Grafana:     http://localhost:30300 (admin/admin)"
	@echo "  MailHog UI:  http://localhost:30080"
	@echo "  API Gateway: http://localhost:30000"
	@echo ""
	@make k8s-status

k8s-stop:
	@echo "$(RED) Остановка brewflow (удаление подов)$(NC)"
	@helm uninstall -n brewflow brewflow 2>/dev/null || true
	@helm uninstall -n brewflow-infra brewflow-infra 2>/dev/null || true
	@helm uninstall -n brewflow-observability brewflow-observability 2>/dev/null || true
	@echo "$(GREEN) Stopped$(NC)"

k8s-restart: k8s-stop k8s-deploy

k8s-status:
	@echo "$(BLUE) k8s статус:$(NC)"
	@echo ""
	@echo "$(YELLOW)observability:$(NC)"
	@kubectl get pods -n brewflow-observability -o wide 2>/dev/null || echo "  No pods"
	@echo ""
	@echo "$(YELLOW)infrastructure:$(NC)"
	@kubectl get pods -n brewflow-infra -o wide 2>/dev/null || echo "  No pods"
	@echo ""
	@echo "$(YELLOW)app:$(NC)"
	@kubectl get pods -n brewflow -o wide 2>/dev/null || echo "  No pods"

k8s-clean: k8s-stop
	@echo "$(RED) Полная очистка...$(NC)"
	@kubectl delete namespace brewflow brewflow-infra brewflow-observability 2>/dev/null || true
	@echo "$(GREEN) Очистка завершена$(NC)"

k8s-logs-%:
	@echo "kubectl logs -n brewflow -l app=$* --tail=100 -f"
	@kubectl logs -n brewflow -l app=$* --tail=100 -f

k8s-logs-infra-%:
	@echo "kubectl logs -n brewflow-infra -l app=$* --tail=100 -f"
	@kubectl logs -n brewflow -l app=$* --tail=100 -f

k8s-logs-obs-%:
	@echo "kubectl logs -n brewflow-observability -l app=$* --tail=100 -f"
	@kubectl logs -n brewflow -l app=$* --tail=100 -f

.DEFAULT_GOAL := help
