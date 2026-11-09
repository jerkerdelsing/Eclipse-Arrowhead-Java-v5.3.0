#!/bin/sh
set -eu

AUTHORIZATION_URL=${CONSUMER_AUTHORIZATION_URL:-https://127.0.0.1:8445}
AUTHENTICATION_URL=${AUTHENTICATION_URL:-https://127.0.0.1:8444}
CERT_DIR=${CERT_DIR:-security/x509}
PAYLOAD='{"list":[{"provider":"DataManager","targetType":"SERVICE_DEF","target":"sysMonitor","description":"Allow SysMonitor to orchestrate and consume DataManager sysMonitor observations","defaultPolicy":{"policyType":"WHITELIST","policyList":["SysMonitor"]},"scopedPolicies":{"observe":{"policyType":"WHITELIST","policyList":["SysMonitor"]}}}]}'

token=$(curl -fsS --cacert "$CERT_DIR/ca.crt" --cert "$CERT_DIR/local-cloud-manager.crt" --key "$CERT_DIR/local-cloud-manager.key" \
  -H 'Content-Type: application/json' --data '{"systemName":"LocalCloudManager","credentials":{"password":"LCM-test-52"}}' \
  "$AUTHENTICATION_URL/authentication/identity/login" | jq -er '.token')
response=$(curl -sS --cacert "$CERT_DIR/ca.crt" --cert "$CERT_DIR/local-cloud-manager.crt" --key "$CERT_DIR/local-cloud-manager.key" \
  -H "Authorization: Bearer IDENTITY-TOKEN//$token" -H 'Content-Type: application/json' --data "$PAYLOAD" \
  "$AUTHORIZATION_URL/consumerauthorization/authorization/mgmt/grant")
case "$response" in
  *'MGMT|LOCAL|DataManager|SERVICE_DEF|sysMonitor'*) echo "DataManager sysMonitor policy created" ;;
  *'management level policy is already existing'*) echo "DataManager sysMonitor policy already exists" ;;
  *) echo "Unable to configure DataManager sysMonitor policy: $response" >&2; exit 1 ;;
esac
