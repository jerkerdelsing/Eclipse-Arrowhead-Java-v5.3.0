#!/bin/sh
# Build and test, then check that the Spring Boot JAR contains this system only and no native (eu.arrowhead.port) code.
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
mvn -B "$@" clean verify
jar tf target/arrowhead-sysmonitor-5.3.0.jar > target/package-contents.txt
grep -q '^BOOT-INF/classes/eu/arrowhead/sysmonitor/SysMonitorMain.class$' target/package-contents.txt
if grep -E '^BOOT-INF/classes/eu/arrowhead/(port|gatekeeper|gatetunnel|certificateauthority)/' target/package-contents.txt >/dev/null; then
  echo "Unexpected implementation in artifact" >&2
  exit 1
fi
echo "SysMonitor artifact verified"
