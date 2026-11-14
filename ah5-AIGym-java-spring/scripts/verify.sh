#!/bin/sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
mvn -B "$@" clean verify
jar tf target/arrowhead-ai-gym-5.4.0.jar > target/package-contents.txt
grep -q '^BOOT-INF/classes/eu/arrowhead/aigym/' target/package-contents.txt
