#!/bin/sh
# SPDX-License-Identifier: EPL-2.0
# Self-test of the deployment kit; changes nothing outside a temporary folder. Run in an unpacked package
# (kit/bin/selftest.sh) or in the module (kit/bin/selftest.sh --module, which lays out a package from kit/).
# Checks: shell syntax of every script (and shellcheck when installed), Python syntax, the Compose template, no key material
# or host paths, and configure.py for a lean and a full cloud with a port offset (and `docker compose config` when Docker
# is available).
set -eu
HERE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
if [ "${1:-}" = --module ]; then
  M=$(CDPATH= cd -- "$HERE/../.." && pwd)
  PKG=$tmp/pkg; mkdir -p "$PKG/kit"
  cp -R "$M/kit/cloud/." "$PKG/kit/"; cp -R "$M/kit/bin" "$PKG/kit/bin"; cp -R "$M/kit/org" "$PKG/org"
  printf '{"systems":[{"id":"serviceregistry","services":["serviceregistry-db","serviceregistry"],"internalAssets":[]},{"id":"authentication","services":["authentication-db","authentication"],"internalAssets":[]},{"id":"consumerauthorization","services":["consumerauthorization-db","consumerauthorization"],"internalAssets":[]},{"id":"serviceorchestration-dynamic","services":["serviceorchestration-dynamic-db","serviceorchestration-dynamic"],"internalAssets":[]},{"id":"gatekeeper","services":["gatekeeper"],"internalAssets":[]},{"id":"gatetunnel","services":["gatetunnel"],"internalAssets":[]},{"id":"datamanager","services":["datamanager-db","datamanager"],"internalAssets":[]},{"id":"sysmonitor","services":["sysmonitor"],"internalAssets":[]},{"id":"localcloudmanager","services":["cloud-monitor-api"],"internalAssets":[]}]}\n' > "$PKG/kit/lc-catalog.json"
else
  PKG=$(CDPATH= cd -- "$HERE/../.." && pwd)
fi
fail=0
ok() { printf '  ok   %s\n' "$1"; }
bad() { printf '  FAIL %s\n' "$1"; fail=1; }

echo "shell syntax"
n=0
for f in $(find "$PKG/kit" "$PKG/org" -name '*.sh' -type f) "$PKG/kit/bin/shims/keytool"; do
  sh -n "$f" || bad "$f"; n=$((n + 1))
done
ok "$n scripts parse"
if command -v shellcheck >/dev/null 2>&1; then
  # shellcheck -S error: errors only (the reference cloud's scripts carry style warnings)
  # shellcheck disable=SC2046
  shellcheck -S error -s sh $(find "$PKG/kit/bin" "$PKG/org" -name '*.sh' -type f) && ok "shellcheck (errors)" || bad "shellcheck"
else
  echo "  skip shellcheck (not installed)"
fi

echo "python"
for f in $(find "$PKG/kit" -name '*.py' -type f); do python3 -c 'import ast,sys; ast.parse(open(sys.argv[1]).read())' "$f" || bad "$f"; done
ok "python sources parse"

echo "content"
python3 -c 'import json,sys; json.load(open(sys.argv[1]))' "$PKG/kit/templates/compose-template.json" && ok "compose template is JSON"
if grep -rlE 'BEGIN (RSA |EC |ENCRYPTED )?PRIVATE KEY' "$PKG/kit" "$PKG/org" 2>/dev/null; then bad "key material in the kit"; else ok "no key material"; fi
if grep -rlE '/(Users|home)/[A-Za-z]' "$PKG/kit/templates" "$PKG/kit/bin" "$PKG/org" 2>/dev/null; then bad "host paths in the kit"; else ok "no host paths"; fi

echo "configure.py"
for spec in "Lean:0:serviceregistry authentication consumerauthorization serviceorchestration-dynamic" \
            "Full:3000:serviceregistry authentication consumerauthorization serviceorchestration-dynamic gatekeeper gatetunnel datamanager sysmonitor localcloudmanager"; do
  name=${spec%%:*}; rest=${spec#*:}; off=${rest%%:*}; comps=${rest#*:}
  d=$tmp/clouds/$name; mkdir -p "$d"
  python3 - "$d/cloud.json" "$name" "$off" "$comps" <<'PY'
import json, sys
json.dump({"cloud": sys.argv[2], "organization": "Acme", "solc": "Selftest", "composeProject": "arrowhead-" + sys.argv[2].lower(),
           "host": "localhost", "architecture": "arm64", "portOffset": int(sys.argv[3]), "components": sys.argv[4].split(),
           "lcDesign": {"id": "x", "name": "x", "revision": 1}, "federation": True, "relay": {"host": "relay.example.org", "port": 8893},
           "peers": [], "intercloudConsumers": [], "imageMode": "prebuilt", "imageTag": "selftest"}, open(sys.argv[1], "w"))
PY
  if python3 "$PKG/kit/bin/configure.py" "$PKG" "$d/cloud.json" "$d/work" >/dev/null; then
    python3 - "$d/work/compose.yaml" "$off" <<'PY' && ok "$name: services, ports +$off, subnets, names" || bad "$name compose content"
import json, re, sys
c = json.load(open(sys.argv[1])); off = int(sys.argv[2])
ports = [int(p["published"]) for s in c["services"].values() for p in s.get("ports", [])]
assert ports and min(ports) >= 5671 + off, ports
text = json.dumps(c)
assert "TestCloud" not in text and not re.search(r"@[A-Z_]+@", text), "placeholders or reference names left"
for n in c["networks"].values():
    for x in (n.get("ipam") or {}).get("config", []) or []:
        assert x["subnet"].startswith(f"10.{200 + off // 1000}."), x
PY
    grep -rq 'TestCloud' "$d/work/scripts" && bad "$name: reference cloud name left in scripts" || true
    if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
      (cd "$d/work" && docker compose config -q) && ok "$name: docker compose config" || bad "$name: docker compose config"
    fi
  else
    bad "$name: configure.py"
  fi
done
[ "$fail" = 0 ] && echo "selftest passed" || { echo "selftest FAILED"; exit 1; }
