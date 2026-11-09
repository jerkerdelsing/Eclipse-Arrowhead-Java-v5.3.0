#!/bin/sh
# Regenerates the LaTeX documentation of all core systems from the Java sources and builds the PDFs.
# Usage: documentation-tools/regenerate.sh [--no-pdf]
set -eu
TOOLS="$(cd "$(dirname "$0")" && pwd)"
CORE="$(dirname "$TOOLS")"
ROOT="$(dirname "$CORE")"
MODEL="$TOOLS/model.json"

for dir in "$ROOT/ah5-common-java-spring" "$ROOT/roadmap/Documentation_templates/LaTeX"; do
  [ -d "$dir" ] || { echo "missing $dir" >&2; exit 1; }
done

python3 "$TOOLS/extract.py" "$ROOT" > "$MODEL"
python3 "$TOOLS/generate.py" "$ROOT" "$MODEL"

if [ "${1:-}" != "--no-pdf" ]; then
  for module in serviceregistry authentication consumer-authorization serviceorchestration-dynamic serviceorchestration-simple; do
    sh "$CORE/$module/documentation/v5.2.2/build-docs.sh"
  done
fi
