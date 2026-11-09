#!/bin/sh
set -eu
AUTHENTICATION_URL=${AUTHENTICATION_URL:-https://127.0.0.1:8444}
CERT_DIR=${CERT_DIR:-security/x509}
SYSOP_PASSWORD=${ARROWHEAD_SYSOP_PASSWORD:-xFNHbh2mQyIp1gmimeTC}
token=$(curl -fsS --cacert "$CERT_DIR/ca.crt" --cert "$CERT_DIR/local-cloud-manager.crt" \
  --key "$CERT_DIR/local-cloud-manager.key" -H 'Content-Type: application/json' \
  --data "{\"systemName\":\"Sysop\",\"credentials\":{\"password\":\"$SYSOP_PASSWORD\"}}" \
  "$AUTHENTICATION_URL/authentication/identity/login" | jq -er '.token')
response=$(curl -sS --cacert "$CERT_DIR/ca.crt" --cert "$CERT_DIR/local-cloud-manager.crt" \
  --key "$CERT_DIR/local-cloud-manager.key" -H "Authorization: Bearer IDENTITY-TOKEN//$token" \
  -H 'Content-Type: application/json' \
  --data "{\"authenticationMethod\":\"PASSWORD\",\"identities\":[{\"systemName\":\"GateTunnel\",\"credentials\":{\"password\":\"${AH_PW_GT:?set AH_PW_GT}\"},\"sysop\":false}]}" \
  "$AUTHENTICATION_URL/authentication/mgmt/identities")
case "$response" in
  *'GateTunnel'*) echo "GateTunnel Authentication identity provisioned" ;;
  *'already exists'*|*'already registered'*) echo "GateTunnel Authentication identity already exists" ;;
  *) echo "Unable to provision GateTunnel identity: $response" >&2; exit 1 ;;
esac
