#!/bin/sh
# SPDX-License-Identifier: EPL-2.0
# Member host: write the federation request for the organisation host, out/<Cloud>-federation-request.tar.gz. Public
# material only: the cloud CA certificate, a certificate request signed with the cloud CA key (the key stays here), the
# cloud's CRL and the relay client certificates of Gatekeeper and GateTunnel (their keys stay here).
CLOUD_JSON=${1:?cloud.json}
. "$(dirname -- "$0")/lib-cloud.sh"
need openssl tar
cd "$WORK"
X=security/x509
tmp=$(mktemp -d)
# a request with exactly the CA certificate's subject, signed with the CA key (proof of possession)
openssl x509 -x509toreq -sha256 -in $X/ca.crt -signkey security/ca/ca.key -out "$tmp/ca-cross.csr" 2>/dev/null
[ "$(openssl req -in "$tmp/ca-cross.csr" -noout -subject -nameopt RFC2253 | sed 's/^subject=//')" = "$(openssl x509 -in $X/ca.crt -noout -subject -nameopt RFC2253 | sed 's/^subject=//')" ] || die "request subject"
[ "$(openssl req -in "$tmp/ca-cross.csr" -noout -pubkey | openssl sha256)" = "$(openssl x509 -in $X/ca.crt -noout -pubkey | openssl sha256)" ] || die "request key"
cp $X/ca.crt security/crl/ca.crl $X/gatekeeper-relay.crt $X/gatetunnel-relay.crt "$tmp/"
printf '{"cloud":"%s","organization":"%s","solc":"%s","created":"%s"}\n' "$CLOUD" "$ORG" "$(cj solc)" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" > "$tmp/request.json"
mkdir -p "$HOME_DIR/out"
(cd "$tmp" && tar -czf "$HOME_DIR/out/$CLOUD-federation-request.tar.gz" request.json ca.crt ca-cross.csr ca.crl gatekeeper-relay.crt gatetunnel-relay.crt)
rm -rf "$tmp"
chmod 644 "$HOME_DIR/out/$CLOUD-federation-request.tar.gz"
echo "Federation request: $HOME_DIR/out/$CLOUD-federation-request.tar.gz"
echo "Copy it to the organisation host and run org/sign-cloud.sh there; bring back out/$CLOUD-federation.tar.gz and run"
echo "clouds/$CLOUD/import-federation.sh <file>."
