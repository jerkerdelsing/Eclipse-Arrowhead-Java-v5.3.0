#!/bin/sh
# SPDX-License-Identifier: EPL-2.0
# Member host: copy this cloud's current CRL to out/<Cloud>-ca.crl for the organisation host (org/relay.sh crl <Cloud> <file>,
# then org/sign-cloud.sh --bundles for the peers). Needed before the CRL's nextUpdate and after every revocation.
CLOUD_JSON=${1:?cloud.json}
. "$(dirname -- "$0")/lib-cloud.sh"
mkdir -p "$HOME_DIR/out"
cp "$WORK/security/crl/ca.crl" "$HOME_DIR/out/$CLOUD-ca.crl"
chmod 644 "$HOME_DIR/out/$CLOUD-ca.crl"
echo "$HOME_DIR/out/$CLOUD-ca.crl: $(openssl crl -in "$HOME_DIR/out/$CLOUD-ca.crl" -noout -nextupdate)"
