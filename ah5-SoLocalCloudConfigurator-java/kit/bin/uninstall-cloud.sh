#!/bin/sh
# SPDX-License-Identifier: EPL-2.0
# Member host: stop the cloud. Keeps its data, keys and passwords unless --purge (then the Compose volumes and the work
# folder are deleted; this cannot be undone).
#   uninstall-cloud.sh <cloud.json> [--purge]
CLOUD_JSON=${1:?cloud.json}
. "$(dirname -- "$0")/lib-cloud.sh"
if [ "${2:-}" = --purge ]; then
  dc down --remove-orphans --volumes
  rm -rf "$WORK"
  command -v security >/dev/null 2>&1 && security delete-generic-password -s "arrowhead-console-$PROJECT" >/dev/null 2>&1 || true
  echo "$CLOUD removed with its data"
else
  dc down --remove-orphans
  echo "$CLOUD stopped; data, keys and passwords kept in $WORK and the Compose volumes"
fi
