#!/bin/sh
set -eu
AUTHORIZATION_URL=${CONSUMER_AUTHORIZATION_URL:-https://127.0.0.1:8445}
AUTHENTICATION_URL=${AUTHENTICATION_URL:-https://127.0.0.1:8444}
CERT_DIR=${CERT_DIR:-security/x509}
token=$(curl -fsS --cacert "$CERT_DIR/ca.crt" --cert "$CERT_DIR/local-cloud-manager.crt" \
  --key "$CERT_DIR/local-cloud-manager.key" -H 'Content-Type: application/json' \
  --data "{\"systemName\":\"LocalCloudManager\",\"credentials\":{\"password\":\"${AH_PW_LCM:?set AH_PW_LCM}\"}}" \
  "$AUTHENTICATION_URL/authentication/identity/login" | jq -er '.token')
grant() {
  target=$1 consumers=$2
  response=$(curl -sS --cacert "$CERT_DIR/ca.crt" --cert "$CERT_DIR/local-cloud-manager.crt" \
    --key "$CERT_DIR/local-cloud-manager.key" -H "Authorization: Bearer IDENTITY-TOKEN//$token" \
    -H 'Content-Type: application/json' \
    --data "{\"list\":[{\"provider\":\"GateTunnel\",\"targetType\":\"SERVICE_DEF\",\"target\":\"$target\",\"description\":\"GateTunnel v5.2.2 authorized consumers\",\"defaultPolicy\":{\"policyType\":\"WHITELIST\",\"policyList\":$consumers},\"scopedPolicies\":{}}]}" \
    "$AUTHORIZATION_URL/consumerauthorization/authorization/mgmt/grant")
  case "$response" in
    *"MGMT|LOCAL|GateTunnel|SERVICE_DEF|$target"*) echo "GateTunnel $target policy created" ;;
    *'management level policy is already existing'*) echo "GateTunnel $target policy already exists" ;;
    *) echo "Unable to configure GateTunnel $target policy: $response" >&2; exit 1 ;;
  esac
}
grant gateTunnelConnection '["Gatekeeper","DynamicServiceOrchestration","SimpleStoreServiceOrchestration","LocalCloudManager"]'
grant gateTunnelKey '["Gatekeeper","DynamicServiceOrchestration","SimpleStoreServiceOrchestration","LocalCloudManager"]'
grant gateTunnelManagement '["LocalCloudManager"]'
