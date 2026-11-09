#!/bin/sh
# ConsumerAuthorization policy for SysMonitor's sysMonitorHistory service. SysMonitor stores the sysMonitor
# observations of all monitored microsystems itself and checks every history call against this policy
# (authorization.verify); the operations console (LocalCloudManager) reads it. Re-running replaces the policy.
set -eu

AUTHORIZATION_URL=${CONSUMER_AUTHORIZATION_URL:-https://127.0.0.1:8445}
AUTHENTICATION_URL=${AUTHENTICATION_URL:-https://127.0.0.1:8444}
CERT_DIR=${CERT_DIR:-security/x509}
INSTANCE='MGMT|LOCAL|SysMonitor|SERVICE_DEF|sysMonitorHistory'
PAYLOAD='{"list":[
  {"provider":"SysMonitor","targetType":"SERVICE_DEF","target":"sysMonitorHistory","description":"Stored sysMonitor observations of all microsystems; the operations console reads them","defaultPolicy":{"policyType":"WHITELIST","policyList":["LocalCloudManager"]}}
]}'

ca() {
  curl -sS --cacert "$CERT_DIR/ca.crt" --cert "$CERT_DIR/local-cloud-manager.crt" --key "$CERT_DIR/local-cloud-manager.key" \
    -H "Authorization: Bearer IDENTITY-TOKEN//$token" -H 'Content-Type: application/json' "$@"
}

token=$(curl -fsS --cacert "$CERT_DIR/ca.crt" \
  --cert "$CERT_DIR/local-cloud-manager.crt" --key "$CERT_DIR/local-cloud-manager.key" \
  -H 'Content-Type: application/json' \
  --data '{"systemName":"LocalCloudManager","credentials":{"password":"LCM-test-52"}}' \
  "$AUTHENTICATION_URL/authentication/identity/login" | jq -er '.token')

grant() { ca --data "$PAYLOAD" "$AUTHORIZATION_URL/consumerauthorization/authorization/mgmt/grant"; }

response=$(grant)
case "$response" in
  *'management level policy is already existing'*)
    ca -X DELETE "$AUTHORIZATION_URL/consumerauthorization/authorization/mgmt/revoke?instanceIds=$(printf %s "$INSTANCE" | sed 's/|/%7C/g')" >/dev/null
    response=$(grant)
    ;;
esac
case "$response" in
  *"$INSTANCE"*) echo "SysMonitor sysMonitorHistory policy configured" ;;
  *) echo "Unable to configure the SysMonitor sysMonitorHistory policy: $response" >&2; exit 1 ;;
esac
