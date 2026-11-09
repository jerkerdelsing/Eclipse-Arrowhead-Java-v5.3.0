#!/bin/sh
set -eu

BASE_URL=${BASE_URL:-https://127.0.0.1:8448/certificate-authority}
CERT_DIR=${CERT_DIR:?set CERT_DIR to the test-cloud security/x509 directory}
WORK_DIR=$(mktemp -d)
trap 'rm -rf "$WORK_DIR"' EXIT

AUTHENTICATION_URL=${AUTHENTICATION_URL:-https://127.0.0.1:8444}
LCM_PASSWORD=${LCM_PASSWORD:?set LCM_PASSWORD (AH_PW_LCM of the cloud .env)}

# Every API call needs LocalCloudManager's identity token together with its own client certificate.
TOKEN=$(curl -fsS --cacert "$CERT_DIR/ca.crt" --cert "$CERT_DIR/local-cloud-manager.crt" --key "$CERT_DIR/local-cloud-manager.key" \
  -H 'Content-Type: application/json' \
  --data "{\"systemName\":\"LocalCloudManager\",\"credentials\":{\"password\":\"$LCM_PASSWORD\"}}" \
  "$AUTHENTICATION_URL/authentication/identity/login" | jq -er .token)

curl_ca() {
  curl -fsS --cacert "$CERT_DIR/ca.crt" \
    --cert "$CERT_DIR/local-cloud-manager.crt" \
    --key "$CERT_DIR/local-cloud-manager.key" \
    -H "Authorization: Bearer IDENTITY-TOKEN//$TOKEN" "$@"
}

curl_ca "$BASE_URL/health" | jq -e '.status == "ok" and .version == "5.2.2"' >/dev/null

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$WORK_DIR/client.key" >/dev/null 2>&1
openssl req -new -key "$WORK_DIR/client.key" -subj '/C=SE/O=Company/CN=IntegrationClient.TestCloud.Company.arrowhead.eu' \
  -out "$WORK_DIR/client.csr"
jq -n --rawfile csr "$WORK_DIR/client.csr" '{encodedCSR:$csr}' > "$WORK_DIR/sign.json"
curl_ca -H 'Content-Type: application/json' -d @"$WORK_DIR/sign.json" "$BASE_URL/sign" > "$WORK_DIR/signed.json"
jq -e '.id > 0 and (.certificateChain | length) == 2' "$WORK_DIR/signed.json" >/dev/null

unauthorized_status=$(curl -sS -o "$WORK_DIR/unauthorized.json" -w '%{http_code}' \
  --cacert "$CERT_DIR/ca.crt" --cert "$CERT_DIR/random-data-producer.crt" \
  --key "$CERT_DIR/random-data-producer.key" -H 'Content-Type: application/json' \
  -d @"$WORK_DIR/sign.json" "$BASE_URL/sign")
test "$unauthorized_status" = "403"
jq -e '.error | contains("authorized provisioning identity")' "$WORK_DIR/unauthorized.json" >/dev/null

jq -n --arg certificate "$(jq -r '.certificateChain[0]' "$WORK_DIR/signed.json")" '{certificate:$certificate}' > "$WORK_DIR/check.json"
curl_ca -H 'Content-Type: application/json' -d @"$WORK_DIR/check.json" "$BASE_URL/checkCertificate" \
  | jq -e '.status == "GOOD" and .valid == true' >/dev/null

openssl pkey -in "$WORK_DIR/client.key" -pubout -out "$WORK_DIR/client.pub"
jq -n --rawfile publicKey "$WORK_DIR/client.pub" '{publicKey:$publicKey,description:"integration test"}' > "$WORK_DIR/key.json"
curl_ca -X PUT -H 'Content-Type: application/json' -d @"$WORK_DIR/key.json" "$BASE_URL/mgmt/keys" > "$WORK_DIR/key-added.json"
jq -e '.trusted == true and .id > 0' "$WORK_DIR/key-added.json" >/dev/null
curl_ca -H 'Content-Type: application/json' -d @"$WORK_DIR/key.json" "$BASE_URL/checkTrustedKey" \
  | jq -e '.trusted == true' >/dev/null
curl_ca -X DELETE "$BASE_URL/mgmt/keys/$(jq -r '.id' "$WORK_DIR/key-added.json")" | jq -e '.success == true' >/dev/null

curl_ca -X DELETE "$BASE_URL/mgmt/certificates/$(jq -r '.id' "$WORK_DIR/signed.json")" | jq -e '.success == true' >/dev/null
curl_ca -H 'Content-Type: application/json' -d @"$WORK_DIR/check.json" "$BASE_URL/checkCertificate" \
  | jq -e '.status == "REVOKED" and .valid == false' >/dev/null

echo "CertificateAuthority integration lifecycle passed"
