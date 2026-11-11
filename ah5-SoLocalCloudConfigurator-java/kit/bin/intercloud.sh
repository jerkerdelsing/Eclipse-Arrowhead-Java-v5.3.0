#!/bin/sh
# SPDX-License-Identifier: EPL-2.0
# Member host: apply and verify this cloud's part of the SoLC plan (clouds/<Cloud>/federation-spec.json, derived by
# SoLocalCloudConfigurator from the design and the organisation policy): preconditions, Gatekeeper neighbours,
# inter-cloud ConsumerAuthorization grants and revocations, verification, and global service discovery over the relay.
#   intercloud.sh <cloud.json> [check|neighbours|grants|verify|discover|status]   (default: all in that order)
CLOUD_JSON=${1:?cloud.json}
. "$(dirname -- "$0")/lib-cloud.sh"
need jq curl openssl
SPEC=$HOME_DIR/federation-spec.json
[ -f "$SPEC" ] || die "no federation-spec.json: $CLOUD takes part in no inter-cloud exchange"
[ -f "$WORK/federation/imported" ] || die "import the federation bundle first (import-federation.sh)"
step() { sh "$KIT/bin/sos-cloud.sh" "$1" "$SPEC"; }
case "${2:-all}" in
all)
  step cloud-check; step neighbours; step grants; step verify
  if [ "$(jq '.config.discovery | length' "$SPEC")" -gt 0 ]; then
    echo "-- global service discovery (needs the provider clouds to have run intercloud.sh)"
    step discover || echo "discovery failed: run 'intercloud.sh <cloud.json> discover' again once every member is federated" >&2
  fi ;;
check) step cloud-check ;;
neighbours|grants|verify|discover|status) step "$2" ;;
*) die "usage: intercloud.sh <cloud.json> [check|neighbours|grants|verify|discover|status]" ;;
esac
