DOCKER ?= docker
JAVA ?= java
# Maven runs in the build image unless a local mvn is given (MVN=mvn)
MVN ?= $(DOCKER) run --rm -v "$(CURDIR)":/src -v "$(HOME)/.m2":/root/.m2 -w /src maven:3.9-eclipse-temurin-21 mvn
CLOUD ?= $(abspath ../../OLD/Arrowhead/Arrowhead-test-cloud-java)
.PHONY: build test image up down deploy worker-start worker-stop worker-restart status plan
build:
	$(MVN) -B clean verify
test:
	$(MVN) -B test
image:
	$(DOCKER) compose build
# standalone stack of this folder (needs ./security, see DEPLOYMENT.md)
up:
	$(DOCKER) compose up -d --build
down:
	$(DOCKER) compose down
# changes a deployed cloud: only when an operator decides so
deploy:
	./scripts/deploy.sh
worker-start worker-restart:
	python3 scripts/install-direct.py --cloud-dir "$(CLOUD)" --compose-file "$(CLOUD)/compose.yaml"
worker-stop:
	launchctl bootout gui/$$(id -u)/eu.arrowhead.java-localcloud-direct
status:
	launchctl print gui/$$(id -u)/eu.arrowhead.java-localcloud-direct
plan:
	$(JAVA) -jar target/localcloudconfigurator.jar plan -catalog catalogs/javacloud.json -request $(REQUEST)
