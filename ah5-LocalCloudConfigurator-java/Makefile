DOCKER ?= docker
JAVA ?= java
MVN ?= $(DOCKER) run --rm -v "$(CURDIR)":/src -v "$(HOME)/.m2":/root/.m2 -w /src maven:3.9-eclipse-temurin-21 mvn
.PHONY: build test image deploy executor-start executor-stop executor-restart status
build:
	$(MVN) -B package
test:
	$(MVN) -B test
image:
	$(DOCKER) build -t eclipse-arrowhead/localcloudconfigurator-java:5.2.2 .
deploy:
	./scripts/deploy.sh
executor-start:
	python3 scripts/executor.py start
executor-stop:
	python3 scripts/executor.py stop
executor-restart:
	python3 scripts/executor.py restart
status:
	python3 scripts/executor.py status
