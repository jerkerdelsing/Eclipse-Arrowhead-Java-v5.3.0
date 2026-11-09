#!/bin/sh
# Build and test, then check that the Spring Boot JAR contains this system only and no native (eu.arrowhead.port) code.
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
mvn -B "$@" clean verify
jar tf target/arrowhead-gatetunnel-5.2.2.jar > target/package-contents.txt
grep -q '^BOOT-INF/classes/eu/arrowhead/gatetunnel/GateTunnelMain.class$' target/package-contents.txt
if grep -E '^BOOT-INF/classes/eu/arrowhead/(port|gatekeeper|certificateauthority|sysmonitor)/' target/package-contents.txt >/dev/null; then
  echo "Unexpected implementation in artifact" >&2
  exit 1
fi
echo "GateTunnel artifact verified"
