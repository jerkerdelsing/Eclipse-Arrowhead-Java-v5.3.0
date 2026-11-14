#!/bin/sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
mvn -B "$@" clean verify
jar tf target/arrowhead-timemanager-5.4.0.jar > target/package-contents.txt
grep -q '^BOOT-INF/classes/eu/arrowhead/' target/package-contents.txt
if grep -q 'JavaSupportRuntime\|eu/arrowhead/port/' target/package-contents.txt; then
  echo 'Unexpected combined support runtime' >&2; exit 1
fi
