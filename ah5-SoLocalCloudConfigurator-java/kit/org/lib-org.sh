# SPDX-License-Identifier: EPL-2.0
# Shared settings of the organisation scripts (POSIX sh; sourced). The organisation host keeps its state in org/state/:
#   ca/       organisation CA key (0600; never leaves this host), certificate, CRL database
#   relay/    relay server key and certificate, generated ACL, relay Compose file
#   clouds/<Cloud>/  what a member cloud sent (CA certificate, CRL, relay client certificates) and its cross-certificate
#   public/   organisation CA certificate and CRL (readable by the relay)
# org/org.json (from the package) names the organisation, the SoLC, the relay address and the member clouds.
set -eu
ORG_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PKG_DIR=$(CDPATH= cd -- "$ORG_DIR/.." && pwd)
STATE=${ORG_STATE:-$ORG_DIR/state}
OUT=$ORG_DIR/out
j() { python3 -c 'import json,sys
v=json.load(open(sys.argv[1]))
for k in sys.argv[2].split("."): v=v[k]
print(v)' "$ORG_DIR/org.json" "$1"; }
ORG=$(j organization)
SOLC=$(j solc)
RELAY_HOST=$(j relay.host)
RELAY_PORT=$(j relay.port)
MEMBERS=$(python3 -c 'import json,sys; print(" ".join(json.load(open(sys.argv[1]))["clouds"]))' "$ORG_DIR/org.json")
RELAY_PROJECT=arrowhead-relay-$(printf '%s' "$SOLC" | tr 'A-Z' 'a-z')
die() { echo "error: $*" >&2; exit 1; }
need() { for c in "$@"; do command -v "$c" >/dev/null 2>&1 || die "$c is required (see docs/INSTALL-*.md)"; done; }
is_member() { for m in $MEMBERS; do [ "$m" = "$1" ] && return 0; done; return 1; }
umask 077
