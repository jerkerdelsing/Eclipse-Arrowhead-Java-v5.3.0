#!/bin/sh
# Build and test, then check that the Spring Boot JAR contains Gatekeeper only and no native (eu.arrowhead.port) code.
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
mvn -B "$@" clean verify
jar tf target/arrowhead-gatekeeper-5.2.2.jar > target/package-contents.txt
grep -q '^BOOT-INF/classes/eu/arrowhead/gatekeeper/GatekeeperMain.class$' target/package-contents.txt
if grep -E '^BOOT-INF/classes/eu/arrowhead/(port|certificateauthority|gatetunnel|sysmonitor)/' target/package-contents.txt >/dev/null; then
  echo "Unexpected implementation in artifact" >&2
  exit 1
fi
echo "Gatekeeper artifact verified"
