#!/bin/sh
# Builds and tests both artifacts (console API and Docker proxy) in the Maven container; MAVEN_REPO selects the repository
# (default ~/.m2). The browser models in ui/test are checked by Node when it is installed.
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
REPO=${MAVEN_REPO:-$HOME/.m2}
docker run --rm -v "$PWD:/src" -v "$REPO:/root/.m2" -w /src maven:3.9-eclipse-temurin-21 mvn -B ${MAVEN_OFFLINE:+-o} clean verify
docker run --rm -v "$PWD/docker-proxy:/src" -v "$REPO:/root/.m2" -w /src maven:3.9-eclipse-temurin-21 mvn -B ${MAVEN_OFFLINE:+-o} clean verify
if command -v node >/dev/null 2>&1; then
  node --check ui/public/app.js
  node --test ui/test/
fi
