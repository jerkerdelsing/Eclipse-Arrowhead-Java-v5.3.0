#!/bin/sh
set -eu
AUTHENTICATION_URL=${AUTHENTICATION_URL:-https://127.0.0.1:8444}
CERT_DIR=${CERT_DIR:-security/x509}
SYSOP_PASSWORD=${ARROWHEAD_SYSOP_PASSWORD:-xFNHbh2mQyIp1gmimeTC}
token=$(curl -fsS --cacert "$CERT_DIR/ca.crt" --cert "$CERT_DIR/local-cloud-manager.crt" --key "$CERT_DIR/local-cloud-manager.key" \
  -H 'Content-Type: application/json' --data "{\"systemName\":\"Sysop\",\"credentials\":{\"password\":\"$SYSOP_PASSWORD\"}}" \
  "$AUTHENTICATION_URL/authentication/identity/login" | jq -er '.token')
response=$(curl -sS --cacert "$CERT_DIR/ca.crt" --cert "$CERT_DIR/local-cloud-manager.crt" --key "$CERT_DIR/local-cloud-manager.key" \
  -H "Authorization: Bearer IDENTITY-TOKEN//$token" -H 'Content-Type: application/json' \
  --data '{"authenticationMethod":"PASSWORD","identities":[{"systemName":"AIGym","credentials":{"password":"AIG-test-52"},"sysop":false}]}' \
  "$AUTHENTICATION_URL/authentication/mgmt/identities")
case "$response" in *'AIGym'*) echo 'AIGym identity provisioned';; *'already exists'*|*'already registered'*) echo 'AIGym identity already exists';; *) echo "Unable to provision AIGym identity: $response" >&2; exit 1;; esac
