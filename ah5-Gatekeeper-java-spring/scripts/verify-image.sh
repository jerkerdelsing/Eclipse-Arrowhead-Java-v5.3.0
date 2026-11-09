#!/bin/sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
docker compose build
docker run --rm --network none --read-only --tmpfs /tmp:rw,noexec,nosuid,nodev,size=64m,mode=1777 arrowhead-gatekeeper-standalone:5.2.2 storage-selftest
