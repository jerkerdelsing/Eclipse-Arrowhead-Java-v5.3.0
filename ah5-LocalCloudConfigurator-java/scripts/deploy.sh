#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
ECLIPSE=$(CDPATH= cd -- "$ROOT/../.." && pwd)
CLOUD=${JAVA_CLOUD_DIR:-$ECLIPSE/Arrowhead/Arrowhead-test-cloud-java}
PRIVATE=${JAVA_LCC_PRIVATE_DIR:-$HOME/.local/share/JavaLocalCloudConfigurator}
DOCKER=${DOCKER:-docker}
JAVA=${JAVA:-java}
SERVER_KEY=${JAVA_LCC_SERVER_KEY_FILE:-$ECLIPSE/Eclipse-Arrowhead-java/ah5-SoSConfigurator-java/deploy/colonyos/secrets/colonies_server_prvkey}
cd "$ROOT"
"$DOCKER" run --rm -v "$ROOT:/src" -v "$HOME/.m2:/root/.m2" -w /src maven:3.9-eclipse-temurin-21 mvn -B package
"$JAVA" -jar target/localcloudconfigurator.jar keygen -dir "$PRIVATE"
python3 scripts/install-java-cloud.py --cloud "$CLOUD" --private-dir "$PRIVATE"
"$JAVA" -jar target/localcloudconfigurator.jar register -dir "$PRIVATE" -catalog catalogs/javacloud.json -server-key-file "$SERVER_KEY" -port 50081
cd "$CLOUD"
"$DOCKER" compose -f compose.yaml build localcloudconfigurator cloud-monitor-api cloud-monitor-ui
"$DOCKER" compose -f compose.yaml up -d --no-deps --no-build --pull never --wait localcloudconfigurator cloud-monitor-api cloud-monitor-ui
python3 "$ROOT/scripts/executor.py" restart --cloud-dir "$CLOUD" --private-dir "$PRIVATE"
printf '%s\n' 'Java LocalCloudManager: http://testcloud.localhost:8490/ (LC view)'
