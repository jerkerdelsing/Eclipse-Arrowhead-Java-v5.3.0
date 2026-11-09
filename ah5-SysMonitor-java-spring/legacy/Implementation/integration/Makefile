SHELL := /bin/sh
COMPOSE := docker compose

.PHONY: init validate pull up up-simple up-auth up-monitor up-datamanager up-random-producer up-sysmonitor up-all status smoke smoke-all configure-orchestration-security-policies configure-gatekeeper-policy configure-gatetunnel-policy configure-sysmonitor-policy configure-datamanager-monitoring-policy logs down clean

init:
	@./scripts/init.sh

validate:
	@./scripts/preflight.sh
	@$(COMPOSE) config --quiet

pull: validate
	@$(COMPOSE) --profile all pull

up:
	@./scripts/generate-x509.sh
	@$(MAKE) validate
	@$(COMPOSE) up -d --wait serviceregistry authentication-db authentication-registry-mtls authentication authentication-identity-bridge
	@./scripts/configure-authentication-mtls-registry.sh
	@$(COMPOSE) restart authentication
	@./scripts/register-authentication-services.sh
	@./scripts/provision-authentication-identities.sh
	@./scripts/provision-gatekeeper-identity.sh
	@./scripts/provision-gatetunnel-identity.sh
	@./scripts/provision-sysmonitor-identity.sh
	@$(COMPOSE) up -d --build --wait
	@./scripts/configure-orchestration-security-policies.sh
	@./scripts/configure-gatekeeper-policy.sh
	@./scripts/configure-gatetunnel-policy.sh
	@$(COMPOSE) restart serviceorchestration-dynamic
	@./scripts/configure-random-producer-policy.sh
	@./scripts/configure-sysmonitor-policy.sh
	@./scripts/configure-datamanager-monitoring-policy.sh

up-simple:
	@./scripts/generate-x509.sh
	@$(MAKE) validate
	@$(COMPOSE) --profile simple up -d --wait

up-auth: up

up-monitor:
	@./scripts/generate-x509.sh
	@$(MAKE) validate
	@$(COMPOSE) up -d --build --wait cloud-monitor-api cloud-monitor-ui

up-datamanager: validate
	@$(COMPOSE) up -d --build --wait datamanager cloud-monitor-api cloud-monitor-ui

up-random-producer:
	@./scripts/generate-x509.sh
	@$(MAKE) validate
	@$(COMPOSE) up -d --build --wait datamanager consumerauthorization serviceorchestration-dynamic
	@./scripts/configure-random-producer-policy.sh
	@$(COMPOSE) up -d --build --wait datamanager random-data-producer cloud-monitor-api cloud-monitor-ui

up-sysmonitor:
	@./scripts/generate-x509.sh
	@$(MAKE) validate
	@./scripts/provision-sysmonitor-identity.sh
	@$(COMPOSE) up -d --build --wait datamanager mqtt-broker sysmonitor
	@./scripts/configure-sysmonitor-policy.sh
	@./scripts/configure-datamanager-monitoring-policy.sh
	@$(COMPOSE) restart sysmonitor
	@$(COMPOSE) up -d --build --wait random-data-producer cloud-monitor-api cloud-monitor-ui

up-all:
	@./scripts/generate-x509.sh
	@$(MAKE) validate
	@$(COMPOSE) --profile all up -d --wait

status:
	@$(COMPOSE) --profile all ps

smoke:
	@./scripts/smoke-test.sh

smoke-all:
	@SMOKE_ALL=1 ./scripts/smoke-test.sh

configure-orchestration-security-policies:
	@./scripts/configure-orchestration-security-policies.sh

configure-gatekeeper-policy:
	@./scripts/configure-gatekeeper-policy.sh

configure-gatetunnel-policy:
	@./scripts/configure-gatetunnel-policy.sh

configure-sysmonitor-policy:
	@./scripts/configure-sysmonitor-policy.sh

configure-datamanager-monitoring-policy:
	@./scripts/configure-datamanager-monitoring-policy.sh

logs:
	@$(COMPOSE) --profile all logs -f --tail=100

down:
	@$(COMPOSE) --profile all down

# Explicitly destructive: removes all database volumes as well as containers.
clean:
	@$(COMPOSE) --profile all down --volumes --remove-orphans
