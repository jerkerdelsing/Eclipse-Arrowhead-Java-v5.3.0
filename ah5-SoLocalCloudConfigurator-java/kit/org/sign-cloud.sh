#!/bin/sh
# SPDX-License-Identifier: EPL-2.0
# Organisation host, step 3: accept member clouds' federation requests and write every member's federation bundle.
#   sign-cloud.sh <Cloud>-federation-request.tar.gz ...   check, cross-sign and register each request
#   sign-cloud.sh --bundles                               only rewrite the bundles (after crl.sh or a member's new CRL)
# A request holds public material only: the cloud CA certificate, a certificate request made with the cloud CA key on
# its host (proof of possession; the key never leaves the host), its CRL and its relay client certificates. Checked:
# the cloud is a member of this SoLC, the CA subject is <Cloud>.<ORG>.arrowhead.eu, the request is signed by that CA's key,
# the relay certificates carry the subjects the ACL expects and verify under the name-constrained cross-certificate, and
# the CRL is signed by the cloud CA and current. Output: out/<Cloud>-federation.tar.gz for each signed member (organisation
# CA certificate and CRL, its cross-certificate, the CA certificates and CRLs of the other signed members).
. "$(dirname -- "$0")/lib-org.sh"
need openssl python3 tar
[ -f "$STATE/ca/org-ca.key" ] || die "run org/init-org.sh first"
mkdir -p "$OUT"

sign() {
  req=$1
  tmp=$(mktemp -d)
  trap 'rm -rf "$tmp"' EXIT
  tar -xzf "$req" -C "$tmp" --no-same-owner --no-same-permissions
  for f in request.json ca.crt ca-cross.csr ca.crl gatekeeper-relay.crt gatetunnel-relay.crt; do
    [ -f "$tmp/$f" ] && [ ! -L "$tmp/$f" ] || die "$req: $f missing"
  done
  cloud=$(python3 -c 'import json,sys,re; c=json.load(open(sys.argv[1]))["cloud"]; assert re.fullmatch(r"[A-Za-z][A-Za-z0-9]{0,62}",c); print(c)' "$tmp/request.json") \
    || die "$req: invalid request.json"
  is_member "$cloud" || die "$cloud is not a member cloud of $SOLC ($MEMBERS)"
  subject=$(openssl x509 -in "$tmp/ca.crt" -noout -subject -nameopt RFC2253 | sed 's/^subject=//')
  case "$subject" in *"CN=$cloud.$ORG.arrowhead.eu"*) ;; *) die "$cloud: CA subject $subject is not $cloud.$ORG.arrowhead.eu" ;; esac
  openssl req -in "$tmp/ca-cross.csr" -noout -verify 2>&1 | grep -q 'verify OK\|Certificate request self-signature verify OK' || die "$cloud: request signature"
  [ "$(openssl req -in "$tmp/ca-cross.csr" -noout -pubkey | openssl sha256)" = "$(openssl x509 -in "$tmp/ca.crt" -noout -pubkey | openssl sha256)" ] \
    || die "$cloud: the request is not made with the cloud CA's key"
  openssl crl -in "$tmp/ca.crl" -CAfile "$tmp/ca.crt" -noout 2>&1 | grep -q "verify OK" || die "$cloud: CRL not signed by its CA"
  d=$STATE/clouds/$cloud
  if [ -f "$d/ca.crt" ] && [ "$(openssl x509 -in "$d/ca.crt" -noout -pubkey | openssl sha256)" != "$(openssl x509 -in "$tmp/ca.crt" -noout -pubkey | openssl sha256)" ]; then
    [ "${REPLACE_CLOUD_CA:-}" = "$cloud" ] || die "$cloud already has another CA; revoke it (crl.sh revoke $cloud) and set REPLACE_CLOUD_CA=$cloud to replace it"
  fi
  mkdir -p "$d"
  cat > "$tmp/cross.ext" <<EXT
basicConstraints=critical,CA:TRUE,pathlen:0
keyUsage=critical,keyCertSign,cRLSign
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid,issuer
nameConstraints=critical,permitted;dirName:cloud_dn

[cloud_dn]
C=SE
O=$ORG
OU=$cloud
EXT
  openssl x509 -req -sha256 -days 1825 -in "$tmp/ca-cross.csr" -CA "$STATE/ca/org-ca.crt" -CAkey "$STATE/ca/org-ca.key" \
    -CAcreateserial -extfile "$tmp/cross.ext" -out "$tmp/ca-cross.crt" 2>/dev/null
  for s in Gatekeeper GateTunnel; do
    f=$tmp/$(printf '%s' "$s" | tr 'A-Z' 'a-z')-relay.crt
    want="dnQualifier=sy,CN=$s.$cloud.$ORG.arrowhead.eu,OU=$cloud,O=$ORG,C=SE"
    got=$(openssl x509 -in "$f" -noout -subject -nameopt RFC2253 | sed 's/^subject=//')
    [ "$got" = "$want" ] || die "$cloud: $s relay certificate subject $got, expected $want"
    openssl verify -CAfile "$STATE/ca/org-ca.crt" -untrusted "$tmp/ca-cross.crt" "$f" >/dev/null 2>&1 \
      || die "$cloud: $s relay certificate does not verify under the constrained cross-certificate"
  done
  cp "$tmp/ca.crt" "$tmp/ca-cross.crt" "$tmp/ca.crl" "$d/"
  chmod 755 "$d"; chmod 644 "$d"/*
  rm -rf "$tmp"; trap - EXIT
  echo "$cloud: cross-certificate issued, constrained to C=SE, O=$ORG, OU=$cloud"
}

bundles() {
  for c in $MEMBERS; do
    [ -f "$STATE/clouds/$c/ca-cross.crt" ] || { echo "$c: no request signed yet"; continue; }
    tmp=$(mktemp -d)
    cp "$STATE/public/org-ca.crt" "$STATE/public/org-ca.crl" "$STATE/clouds/$c/ca-cross.crt" "$tmp/"
    peers=
    for p in $MEMBERS; do
      [ "$p" != "$c" ] && [ -f "$STATE/clouds/$p/ca-cross.crt" ] || continue
      mkdir -p "$tmp/peers/$p"; cp "$STATE/clouds/$p/ca.crt" "$STATE/clouds/$p/ca.crl" "$tmp/peers/$p/"
      peers="$peers $p"
    done
    printf '{"cloud":"%s","organization":"%s","solc":"%s","relay":{"host":"%s","port":%s},"peers":[%s]}\n' "$c" "$ORG" "$SOLC" "$RELAY_HOST" "$RELAY_PORT" \
      "$(for p in $peers; do printf '"%s",' "$p"; done | sed 's/,$//')" > "$tmp/federation.json"
    (cd "$tmp" && tar -czf "$OUT/$c-federation.tar.gz" .)
    chmod 644 "$OUT/$c-federation.tar.gz"
    rm -rf "$tmp"
    echo "$c: $OUT/$c-federation.tar.gz (peers:${peers:- none yet})"
  done
  "$ORG_DIR/relay.sh" config
}

if [ "${1:-}" = --bundles ]; then bundles; exit 0; fi
[ $# -gt 0 ] || die "usage: sign-cloud.sh <request.tar.gz>... | --bundles"
for r in "$@"; do sign "$r"; done
bundles
echo "Copy each bundle to its member host and run clouds/<Cloud>/import-federation.sh there. Re-run sign-cloud.sh --bundles"
echo "after the last member's request, so that every bundle holds all peers."
