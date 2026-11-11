#!/bin/sh
# SPDX-License-Identifier: EPL-2.0
# Organisation host, step 1: the organisation CA, its CRL and the relay server certificate. Idempotent: an existing CA
# is kept (re-running only refreshes the CRL and, when the relay address changed, the relay certificate).
#
#   <ORG> organisation CA (self-signed, dnQualifier=or, pathlen 1)
#     |- relay server certificate (SAN: the relay host of org.json, localhost, host.docker.internal)
#     |- cross-certificate of each member cloud CA (org/sign-cloud.sh), name-constrained to C=SE, O=<ORG>, OU=<Cloud>
. "$(dirname -- "$0")/lib-org.sh"
need openssl python3
mkdir -p "$STATE/ca" "$STATE/relay" "$STATE/clouds" "$STATE/public" "$OUT"
chmod 700 "$STATE" "$STATE/ca" "$STATE/relay"
if [ ! -f "$STATE/ca/org-ca.key" ]; then
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out "$STATE/ca/org-ca.key" 2>/dev/null
  openssl req -x509 -new -sha256 -days 3650 -key "$STATE/ca/org-ca.key" \
    -subj "/C=SE/O=$ORG/OU=Arrowhead/CN=$ORG.arrowhead.eu/dnQualifier=or" \
    -addext "basicConstraints=critical,CA:TRUE,pathlen:1" -addext "keyUsage=critical,keyCertSign,cRLSign" \
    -addext "subjectKeyIdentifier=hash" -out "$STATE/ca/org-ca.crt"
  echo "organisation CA created: $(openssl x509 -in "$STATE/ca/org-ca.crt" -noout -subject)"
fi
cp "$STATE/ca/org-ca.crt" "$STATE/public/org-ca.crt"
san="DNS:localhost,DNS:host.docker.internal,DNS:arrowhead-relay,IP:127.0.0.1"
case "$RELAY_HOST" in
  *[!0-9.]*) case "$RELAY_HOST" in *:*) san="$san,IP:$RELAY_HOST" ;; *) san="$san,DNS:$RELAY_HOST" ;; esac ;;
  *) san="$san,IP:$RELAY_HOST" ;;
esac
if [ ! -f "$STATE/relay/relay.crt" ] || [ "$(cat "$STATE/relay/san" 2>/dev/null)" != "$san" ]; then
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out "$STATE/relay/relay.key" 2>/dev/null
  printf 'basicConstraints=critical,CA:FALSE\nkeyUsage=critical,digitalSignature,keyEncipherment\nextendedKeyUsage=serverAuth\nsubjectAltName=%s\nsubjectKeyIdentifier=hash\nauthorityKeyIdentifier=keyid,issuer\n' "$san" > "$STATE/relay/relay.ext"
  openssl req -new -sha256 -key "$STATE/relay/relay.key" -subj "/C=SE/O=$ORG/OU=Arrowhead Relay/CN=Relay.$ORG.arrowhead.eu" -out "$STATE/relay/relay.csr"
  openssl x509 -req -sha256 -days 825 -in "$STATE/relay/relay.csr" -CA "$STATE/ca/org-ca.crt" -CAkey "$STATE/ca/org-ca.key" \
    -CAcreateserial -extfile "$STATE/relay/relay.ext" -out "$STATE/relay/relay.crt" 2>/dev/null
  rm -f "$STATE/relay/relay.csr"
  printf '%s' "$san" > "$STATE/relay/san"
  echo "relay certificate issued for $san"
fi
"$ORG_DIR/crl.sh"
chmod 644 "$STATE/public"/* "$STATE/relay/relay.crt"
echo "Organisation $ORG is ready. Next: org/relay.sh up, then sign each member's federation request with org/sign-cloud.sh."
