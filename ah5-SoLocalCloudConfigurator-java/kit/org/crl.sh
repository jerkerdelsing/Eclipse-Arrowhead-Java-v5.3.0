#!/bin/sh
# SPDX-License-Identifier: EPL-2.0
# The organisation CA's CRL, checked fail-closed by the relay (crl-watch.sh: cross-certificates) and by every Gatekeeper and
# GateTunnel (the relay's server certificate).
#   crl.sh                       re-issue the CRL (ORG_CRL_DAYS, default 180); run well before its nextUpdate
#   crl.sh revoke <Cloud>        revoke a member's cross-certificate (the cloud leaves the SoLC), then re-issue
# After a change, distribute state/public/org-ca.crl to the member clouds (org/sign-cloud.sh --bundles rewrites their
# federation bundles; docs/OPERATIONS.md).
. "$(dirname -- "$0")/lib-org.sh"
need openssl
DB=$STATE/ca/crl-db
mkdir -p "$DB" "$STATE/public"
[ -f "$DB/index.txt" ] || : > "$DB/index.txt"
[ -f "$DB/crlnumber" ] || echo 1000 > "$DB/crlnumber"
cat > "$DB/openssl.cnf" <<CNF
[ ca ]
default_ca = org
[ org ]
database = $DB/index.txt
crlnumber = $DB/crlnumber
certificate = $STATE/ca/org-ca.crt
private_key = $STATE/ca/org-ca.key
default_md = sha256
default_crl_days = ${ORG_CRL_DAYS:-180}
crl_extensions = crl_ext
[ crl_ext ]
authorityKeyIdentifier = keyid:always
CNF
if [ "${1:-}" = revoke ]; then
  c=${2:?cloud}
  [ -f "$STATE/clouds/$c/ca-cross.crt" ] || die "no cross-certificate of $c"
  openssl ca -config "$DB/openssl.cnf" -revoke "$STATE/clouds/$c/ca-cross.crt" -batch 2>/dev/null
  echo "revoked the cross-certificate of $c"
fi
openssl ca -config "$DB/openssl.cnf" -gencrl -batch -out "$STATE/public/.org-ca.crl.tmp" 2>/dev/null
openssl crl -in "$STATE/public/.org-ca.crl.tmp" -CAfile "$STATE/ca/org-ca.crt" -noout 2>&1 | grep -q "verify OK" || die "CRL does not verify"
chmod 644 "$STATE/public/.org-ca.crl.tmp"
mv "$STATE/public/.org-ca.crl.tmp" "$STATE/public/org-ca.crl"
echo "org-ca.crl: $(openssl crl -in "$STATE/public/org-ca.crl" -noout -nextupdate)"
