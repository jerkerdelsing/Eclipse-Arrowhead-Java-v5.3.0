#!/bin/sh
# SPDX-License-Identifier: EPL-2.0
# Install one member cloud of the SoLC on this host (run through clouds/<Cloud>/install.sh).
#   install-cloud.sh <cloud.json> [--skip-images]
# 1 checks the host (tools, Docker, CPU architecture), 2 provides the images (loads the package's archive or builds them
# from the pinned source), 3 writes the cloud's folder clouds/<Cloud>/work (configure.py), 4 generates this cloud's CA,
# certificates, passwords and stores there (nothing secret comes from the package), 5 starts and provisions the cloud in
# bootstrap order (deploy-cloud.sh), 6 in a federation, writes the federation request for the organisation host.
# Safe to re-run: existing keys, passwords and data are kept.
CLOUD_JSON=${1:?usage: install-cloud.sh <cloud.json> [--skip-images]}
. "$(dirname -- "$0")/lib-cloud.sh"
SKIP_IMAGES=${2:-}

say "1/6 host checks"
need docker python3 openssl jq curl tar
docker compose version >/dev/null 2>&1 || die "Docker Compose v2 is required"
python3 -c 'import sys; sys.exit(sys.version_info < (3, 9))' || die "python3 3.9 or newer is required"
host_arch=$(docker info --format '{{.Architecture}}' 2>/dev/null)
case "$host_arch" in x86_64|amd64) host_arch=amd64 ;; aarch64|arm64) host_arch=arm64 ;; esac
if [ "$host_arch" != "$ARCH" ]; then
  [ "${SOLC_ALLOW_EMULATION:-}" = 1 ] || die "this host runs $host_arch, the design places $CLOUD on $ARCH (SOLC_ALLOW_EMULATION=1 runs emulated images)"
  echo "warning: $ARCH images on a $host_arch host run emulated (slow)"
fi
# openssl ca refuses long database paths ("File name too long"); keep the package in a short path such as ~/arrowhead
[ "${#WORK}" -le 120 ] || die "the package path is too long (${#WORK} characters for $WORK); unpack it in a shorter folder such as ~/arrowhead"
mem=$(docker info --format '{{.MemTotal}}' 2>/dev/null || echo 0)
[ "$mem" -ge 4000000000 ] 2>/dev/null || echo "warning: Docker has less than 4 GB of memory; a full cloud needs about 6 GB"
echo "host ok: $host_arch, Docker $(docker version --format '{{.Server.Version}}' 2>/dev/null), $(python3 --version)"

say "2/6 configuration ($WORK)"
python3 "$KIT/bin/configure.py" "$PKG" "$CLOUD_JSON" "$WORK"
# Each MySQL with native AIO takes about 4096 of the Docker host kernel's AIO contexts (fs.aio-max-nr, often 65536); a
# database that gets none does not start ("io_setup() failed with EAGAIN"). Without the headroom the databases of this
# cloud use MySQL's own I/O threads; raise fs.aio-max-nr on a Linux server instead (docs/TROUBLESHOOTING.md).
dbs=$(python3 -c 'import json,sys; print(sum(1 for n in json.load(open(sys.argv[1]))["services"] if n.endswith("-db")))' "$WORK/compose.yaml")
probe=$(python3 -c 'import json,sys; print(next(s["image"] for n,s in json.load(open(sys.argv[1]))["services"].items() if n.endswith("-db")))' "$WORK/compose.yaml")
if [ -z "${SOLC_DB_NATIVE_AIO:-}" ] && [ ! -f "$WORK/host-settings.json" ] && aio=$(docker run --rm --network none --entrypoint sh "$probe" -c 'echo $(cat /proc/sys/fs/aio-nr) $(cat /proc/sys/fs/aio-max-nr)' 2>/dev/null); then
  set -- $aio
  if [ $(( $2 - $1 )) -lt $(( dbs * 4096 )) ]; then
    echo "warning: the Docker host has $(( $2 - $1 )) of $2 AIO contexts left (fs.aio-max-nr) for $dbs databases; they run without native AIO"
    echo "         (on a Linux server: sudo sysctl -w fs.aio-max-nr=1048576, persist in /etc/sysctl.d/, then re-run install.sh)"
    export SOLC_DB_NATIVE_AIO=off
    python3 "$KIT/bin/configure.py" "$PKG" "$CLOUD_JSON" "$WORK" >/dev/null
  fi
fi

if [ "$SKIP_IMAGES" != --skip-images ]; then
  say "3/6 images"
  "$KIT/bin/provide-images.sh" "$CLOUD_JSON"
fi

say "4/6 keys, certificates and passwords (generated on this host)"
cd "$WORK"
./scripts/init.sh
ARROWHEAD_CLOUD_NAME=$CLOUD ./scripts/generate-x509.sh
if [ ! -f federation/imported ]; then
  # Relay stand-ins until the federation is imported: Gatekeeper and GateTunnel load a relay identity at start-up; this
  # cloud's own CA stands in for the organisation CA, so no other cloud trusts them (GATEKEEPER_RELAY_ENABLED=false).
  cp security/x509/ca.crt security/x509/org-ca.crt
  for s in gatekeeper gatetunnel; do cat "security/x509/$s-relay.crt" security/x509/ca.crt > "security/x509/$s-relay-chain.crt"; done
  chmod 644 security/x509/org-ca.crt security/x509/*-relay-chain.crt
else
  "$KIT/bin/import-federation.sh" "$CLOUD_JSON" --reinstall
fi
./scripts/make-credential-bundles.sh >/dev/null
python3 "$KIT/bin/prepare.py" "$WORK"

say "5/6 start and provision"
"$KIT/bin/deploy-cloud.sh" "$CLOUD_JSON" up

if [ "$FEDERATION" = true ] && [ ! -f "$WORK/federation/imported" ]; then
  say "6/6 federation request"
  "$KIT/bin/request-federation.sh" "$CLOUD_JSON"
fi
echo
echo "$CLOUD is running (Compose project $PROJECT)."
if dc config --services | grep -qx cloud-monitor-api; then
  echo "Console: http://127.0.0.1:$(port 8490)  (also http://$(printf '%s' "$CLOUD" | tr 'A-Z' 'a-z').localhost:$(port 8490)); user sysop."
  if command -v security >/dev/null 2>&1; then
    echo "Console password: macOS keychain, service arrowhead-console-$PROJECT (security find-generic-password -s arrowhead-console-$PROJECT -w)."
  else
    echo "Console password: $WORK/operator/console-password (mode 0600)."
  fi
fi
