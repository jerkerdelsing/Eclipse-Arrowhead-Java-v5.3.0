#!/bin/sh
set -eu
: "${ECLIPSE_HOME:?Set ECLIPSE_HOME to the Eclipse directory containing plugins}"
: "${SYSML_STANDARD_LIBRARY:?Set SYSML_STANDARD_LIBRARY to the matching sysml.library directory}"
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
MODEL_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
CHECK_BUILD=$(mktemp -d)
trap 'rm -rf "$CHECK_BUILD"' EXIT HUP INT TERM
javac -cp "$ECLIPSE_HOME/plugins/*" -d "$CHECK_BUILD" "$SCRIPT_DIR/Validate.java"
java -Xmx4g -cp "$CHECK_BUILD:$ECLIPSE_HOME/plugins/*" Validate "$SYSML_STANDARD_LIBRARY" "$MODEL_DIR"
