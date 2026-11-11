#!/bin/sh
# SPDX-License-Identifier: EPL-2.0
# Member host: checks of the cloud. smoke-test.sh (every system the cloud runs answers over mutual TLS with an identity
# token) and, in a federation, the verification of this cloud's inter-cloud grants and neighbours. The security suite of
# the reference cloud (smoke-security.sh, sections H1-J23) probes with the console, SysMonitor, CertificateAuthority and
# both brokers; it runs when the cloud has them (or with FULL_SECURITY=1) and is reported as skipped otherwise.
CLOUD_JSON=${1:?cloud.json}
. "$(dirname -- "$0")/lib-cloud.sh"
cd "$WORK"
rc=0
./scripts/smoke-test.sh || rc=1
services=$(dc config --services)
missing=
for s in cloud-monitor-api sysmonitor certificate-authority mqtt-broker amqp-broker datamanager; do
  printf '%s\n' "$services" | grep -qx "$s" || missing="$missing $s"
done
if [ -z "$missing" ] || [ "${FULL_SECURITY:-}" = 1 ]; then
  ./scripts/smoke-security.sh || rc=1
else
  echo "skip  security suite smoke-security.sh: it needs$missing (not in this cloud's design; FULL_SECURITY=1 runs it anyway)"
fi
if [ -f "$HOME_DIR/federation-spec.json" ] && [ -f federation/imported ]; then
  sh "$KIT/bin/sos-cloud.sh" verify "$HOME_DIR/federation-spec.json" || rc=1
fi
[ "$rc" = 0 ] && echo "$CLOUD: checks passed" || echo "$CLOUD: checks FAILED"
exit $rc
