#!/bin/sh
# Builds the system and database images and checks that the system image holds the Spring Boot JAR and its config.
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
docker compose build
docker run --rm --network none --read-only --entrypoint sh arrowhead-gatetunnel-standalone:5.4.0 -c 'test -f /app/app.jar && test -f /app/config/application.properties && id -u | grep -qx 10001'
echo "GateTunnel image verified"
