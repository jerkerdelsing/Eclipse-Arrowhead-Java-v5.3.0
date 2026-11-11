# SPDX-License-Identifier: EPL-2.0
# Shared settings of the member-cloud scripts (POSIX sh; sourced after CLOUD_JSON is set to clouds/<Cloud>/cloud.json).
#   PKG    the unpacked package          KIT   PKG/kit
#   HOME_DIR  clouds/<Cloud>             WORK  clouds/<Cloud>/work: the running cloud's folder (state: .env, secrets/,
#                                              security/; never copy it to another host)
set -eu
[ -n "${CLOUD_JSON:-}" ] && [ -f "$CLOUD_JSON" ] || { echo "error: cloud.json not found" >&2; exit 1; }
HOME_DIR=$(CDPATH= cd -- "$(dirname -- "$CLOUD_JSON")" && pwd)
CLOUD_JSON=$HOME_DIR/cloud.json
PKG=$(CDPATH= cd -- "$HOME_DIR/../.." && pwd)
KIT=$PKG/kit
WORK=$HOME_DIR/work
cj() { python3 -c 'import json,sys
v=json.load(open(sys.argv[1]))
for k in sys.argv[2].split("."): v=v[k]
print(" ".join(v) if isinstance(v,list) else ("true" if v is True else "false" if v is False else v))' "$CLOUD_JSON" "$1"; }
CLOUD=$(cj cloud)
ORG=$(cj organization)
PROJECT=$(cj composeProject)
ARCH=$(cj architecture)
OFFSET=$(cj portOffset)
FEDERATION=$(cj federation)
port() { echo $(( $1 + OFFSET )); }
export AUTHENTICATION_URL="https://127.0.0.1:$(port 8444)"
export CONSUMER_AUTHORIZATION_URL="https://127.0.0.1:$(port 8445)"
export SERVICE_REGISTRY_URL="https://127.0.0.1:$(port 8443)"
export GATEKEEPER_URL="https://127.0.0.1:$(port 8449)"
export CERTIFICATE_AUTHORITY_URL="https://127.0.0.1:$(port 8448)"
export COMPOSE_PROJECT=$PROJECT CLOUD_DIR=$WORK
die() { echo "error [$CLOUD]: $*" >&2; exit 1; }
say() { printf '\n== [%s] %s\n' "$CLOUD" "$*"; }
need() { for c in "$@"; do command -v "$c" >/dev/null 2>&1 || die "$c is required (docs/INSTALL-*.md)"; done; }
dc() { docker compose -p "$PROJECT" -f "$WORK/compose.yaml" --project-directory "$WORK" "$@"; }
sha256() { if command -v sha256sum >/dev/null 2>&1; then sha256sum "$@"; else shasum -a 256 "$@"; fi; }
# keytool from a JDK when installed, else from a pinned JRE image (the stores are then written by the container)
if ! command -v keytool >/dev/null 2>&1; then PATH=$KIT/bin/shims:$PATH; export PATH; fi
