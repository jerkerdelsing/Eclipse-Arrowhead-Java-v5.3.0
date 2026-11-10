DOCKER ?= docker
# Maven runs in the build image unless a local mvn is given (MVN=mvn)
MVN ?= $(DOCKER) run --rm -v "$(CURDIR)":/src -v "$(HOME)/.m2":/root/.m2 -w /src maven:3.9-eclipse-temurin-21 mvn
JAVA ?= java
CATALOG ?= catalogs/sos.json
JAR := target/sosconfigurator.jar
STACK := $(DOCKER) compose -f deploy/colonyos/compose.yaml
SECRETS := deploy/colonyos/secrets
COLONY := $(shell jq -r .colony $(CATALOG))
ROLES := $(shell jq -r '"relay", (.clouds[] | "cloud:" + .name)' $(CATALOG))
.PHONY: build test image keygen stack-up register up executors-start executors-stop executor down status plan smoke clean

build:
	$(MVN) -B -q clean verify
test:
	$(MVN) -B test
$(JAR):
	$(MVN) -B -q clean verify -DskipTests
image:
	SOS_COLONY=$(COLONY) $(STACK) build sosconfigurator

# ColonyOS stack: secrets, server + database + API, then the colony, the API identity and one identity per executor role.
keygen: $(JAR)
	$(JAVA) -jar $(JAR) keygen -dir $(SECRETS) -catalog $(CATALOG)
stack-up: keygen image
	SOS_COLONY=$(COLONY) $(STACK) up -d --wait timescaledb
	SOS_COLONY=$(COLONY) $(STACK) up -d colonies-server
	@for i in $$(seq 1 40); do curl -fsS -o /dev/null http://127.0.0.1:50081/ 2>/dev/null && break; \
	  $(STACK) logs colonies-server 2>/dev/null | grep -qi 'started\|listening' && break; sleep 1; done
register: $(JAR)
	$(JAVA) -jar $(JAR) register -dir $(SECRETS) -catalog $(CATALOG) -port 50081
up: stack-up register
	SOS_COLONY=$(COLONY) $(STACK) up -d sosconfigurator
	@echo "Console: http://127.0.0.1:8495  (password: $(SECRETS)/sos_console_password)"

# Executors: one per local cloud and one for the relay, here all on this host (they need docker compose, curl, jq,
# openssl and the cloud and relay directories). ROLE=cloud:GoCloud make executor runs one in the foreground.
executor_env = COLONIES_SERVER_HOST=127.0.0.1 COLONIES_SERVER_PORT=50081 COLONIES_COLONY_NAME=$(COLONY) \
	COLONIES_PRVKEY_FILE=$(SECRETS)/sos_executor_$(1)_prvkey
role_key = $(if $(filter relay,$(1)),relay,$(shell printf '%s' '$(patsubst cloud:%,%,$(1))' | tr 'A-Z' 'a-z'))
start_executor = if [ -f state/$(2).pid ] && kill -0 $$(cat state/$(2).pid) 2>/dev/null; then echo "executor $(1) already running"; \
	else $(call executor_env,$(2)) nohup $(JAVA) -Xmx128m -XX:+UseSerialGC -jar $(JAR) executor --sos.catalog.file=$(CATALOG) --sos.role=$(1) \
	>state/$(2).log 2>&1 & echo $$! > state/$(2).pid; echo "executor $(1) started (state/$(2).log)"; fi;
executor: $(JAR)
	$(call executor_env,$(call role_key,$(ROLE))) $(JAVA) -Xmx128m -jar $(JAR) executor --sos.catalog.file=$(CATALOG) --sos.role=$(ROLE)
executors-start: $(JAR)
	@mkdir -p state && chmod 700 state
	@$(foreach r,$(ROLES),$(call start_executor,$(r),$(call role_key,$(r))))
executors-stop:
	@for f in state/*.pid; do [ -f "$$f" ] || continue; kill $$(cat "$$f") 2>/dev/null || true; rm -f "$$f"; done

status:
	$(STACK) ps
plan: $(JAR)
	$(JAVA) -jar $(JAR) plan -catalog $(CATALOG) -request $(or $(REQUEST),requests/testcloud-gocloud.json)
smoke: $(JAR)
	./scripts/smoke.sh
down: executors-stop
	$(STACK) down
clean:
	rm -rf target
