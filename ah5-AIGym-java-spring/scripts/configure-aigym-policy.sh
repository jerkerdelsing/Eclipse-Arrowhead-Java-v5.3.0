#!/bin/sh
set -eu
AUTHORIZATION_URL=${CONSUMER_AUTHORIZATION_URL:-https://127.0.0.1:8445}
AUTHENTICATION_URL=${AUTHENTICATION_URL:-https://127.0.0.1:8444}
CERT_DIR=${CERT_DIR:-security/x509}
token=$(curl -fsS --cacert "$CERT_DIR/ca.crt" --cert "$CERT_DIR/local-cloud-manager.crt" --key "$CERT_DIR/local-cloud-manager.key" \
  -H 'Content-Type: application/json' --data '{"systemName":"LocalCloudManager","credentials":{"password":"LCM-test-52"}}' \
  "$AUTHENTICATION_URL/authentication/identity/login" | jq -er '.token')
payload='{"list":[
 {"provider":"AIGym","targetType":"SERVICE_DEF","target":"aiGym","description":"LocalCloudManager test access to AIGym","defaultPolicy":{"policyType":"WHITELIST","policyList":["LocalCloudManager"]},"scopedPolicies":{"status":{"policyType":"WHITELIST","policyList":["LocalCloudManager"]},"add-sample":{"policyType":"WHITELIST","policyList":["LocalCloudManager"]},"predict":{"policyType":"WHITELIST","policyList":["LocalCloudManager"]},"observations":{"policyType":"WHITELIST","policyList":["LocalCloudManager"]}}},
 {"provider":"AIGym","targetType":"SERVICE_DEF","target":"sysMonitor","description":"SysMonitor may consume AIGym observations","defaultPolicy":{"policyType":"WHITELIST","policyList":["SysMonitor"]},"scopedPolicies":{"echo":{"policyType":"WHITELIST","policyList":["SysMonitor"]},"power":{"policyType":"WHITELIST","policyList":["SysMonitor"]},"status":{"policyType":"WHITELIST","policyList":["SysMonitor"]}}},
 {"provider":"SysMonitor","targetType":"SERVICE_DEF","target":"sysMonitorData","description":"AIGym may consume normalized monitoring data","defaultPolicy":{"policyType":"WHITELIST","policyList":["AIGym"]},"scopedPolicies":{"subscribe":{"policyType":"WHITELIST","policyList":["AIGym"]}}}
]}'
response=$(curl -sS --cacert "$CERT_DIR/ca.crt" --cert "$CERT_DIR/local-cloud-manager.crt" --key "$CERT_DIR/local-cloud-manager.key" \
  -H "Authorization: Bearer IDENTITY-TOKEN//$token" -H 'Content-Type: application/json' --data "$payload" \
  "$AUTHORIZATION_URL/consumerauthorization/authorization/mgmt/grant")
case "$response" in *'AIGym'*) echo 'AIGym policies created';; *'management level policy is already existing'*) echo 'AIGym policies already exist';; *) echo "Unable to configure AIGym policies: $response" >&2; exit 1;; esac
