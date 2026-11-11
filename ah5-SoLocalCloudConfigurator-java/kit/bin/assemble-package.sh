#!/bin/sh
# SPDX-License-Identifier: EPL-2.0
# Complete an SoLC package with the pinned source and/or image archives (see assemble.py for the options). Run it on a
# machine with Docker (buildx) and the Eclipse-Arrowhead-java checkout; SoLocalCloudConfigurator itself has no Docker.
#   kit/bin/assemble-package.sh <package.tar.gz|dir> [--source <checkout>] [--arch amd64,arm64] [--out <file>] [--verify-only]
exec python3 "$(dirname -- "$0")/assemble.py" "$@"
