#!/bin/sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
mvn -B "$@" clean verify
java -jar target/arrowhead-gatetunnel-5.2.2.jar storage-selftest
jar tf target/arrowhead-gatetunnel-5.2.2.jar > target/package-contents.txt
for other in CertificateAuthority Gatekeeper GateTunnel SysMonitor; do
  [ "$other" = "GateTunnel" ] && continue
  if grep -E "^eu/arrowhead/port/$other(\\$|\\.)" target/package-contents.txt >/dev/null; then
    echo "Unexpected system implementation in artifact: $other" >&2
    exit 1
  fi
done
