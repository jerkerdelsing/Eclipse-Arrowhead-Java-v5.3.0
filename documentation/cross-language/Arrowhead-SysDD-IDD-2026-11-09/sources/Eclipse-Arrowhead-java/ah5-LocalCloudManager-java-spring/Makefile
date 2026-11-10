DOCKER ?= docker
# Maven runs in the build image unless a local mvn is given (MVN=mvn)
MVN ?= $(DOCKER) run --rm -v "$(CURDIR)":/src -v "$(HOME)/.m2":/root/.m2 -w /src maven:3.9-eclipse-temurin-21 mvn
PROXY_MVN ?= $(DOCKER) run --rm -v "$(CURDIR)/docker-proxy":/src -v "$(HOME)/.m2":/root/.m2 -w /src maven:3.9-eclipse-temurin-21 mvn
.PHONY: build test image deploy stop
build:
	$(MVN) -B clean verify
	$(PROXY_MVN) -B clean verify
test:
	sh scripts/verify.sh
image:
	$(DOCKER) compose build
deploy:
	$(DOCKER) compose up -d --build
stop:
	$(DOCKER) compose down
