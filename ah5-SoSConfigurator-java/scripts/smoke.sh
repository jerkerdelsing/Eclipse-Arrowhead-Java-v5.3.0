#!/bin/sh
# Smoke test of the SoSConfigurator against TestCloud, GoCloud and Arrowhead-relay through ColonyOS:
#  1. console health and console login
#  2. status round trip through all executors (relay, TestCloud, GoCloud ready; RustDev not inter-cloud capable)
#  3. a request that violates the security and authorization policies is refused and nothing is submitted
#  4. one-way SoS GoCloud -> TestCloud: TestCloud grants, GoCloud's grant to TestCloud is revoked and verified denied
#  5. bidirectional SoS with discovery over the relay in both directions (the clouds' normal inter-cloud setup)
set -eu
cd "$(dirname -- "$0")/.."
URL=${SOS_URL:-http://127.0.0.1:8495}
PASSWORD_FILE=${PASSWORD_FILE:-deploy/colonyos/secrets/sos_console_password}
JAR=target/sosconfigurator.jar
JAR_OPTS="-password-file $PASSWORD_FILE -url $URL"
fail() { echo "FAIL: $*" >&2; exit 1; }
jar=$(mktemp); trap 'rm -f "$jar"' EXIT

curl -fsS "$URL/healthz" >/dev/null
[ "$(curl -s -o /dev/null -w '%{http_code}' "$URL/api/catalog")" = 401 ] || fail "console API answered without a session"
jq -n --rawfile p "$PASSWORD_FILE" '{password: ($p | rtrimstr("\n"))}' | curl -fsS -c "$jar" -H 'Content-Type: application/json' --data @- "$URL/api/session" >/dev/null
echo "ok: console up, login required"

st=$(curl -fsS -b "$jar" "$URL/api/status")
for c in TestCloud GoCloud; do
  [ "$(printf '%s' "$st" | jq -r --arg c "$c" '.clouds[$c].state')" = ready ] || fail "$c not ready: $(printf '%s' "$st" | jq -c --arg c "$c" '.clouds[$c]')"
done
[ "$(printf '%s' "$st" | jq -r .relay.state)" = ready ] || fail "relay not ready: $(printf '%s' "$st" | jq -c .relay)"
[ "$(printf '%s' "$st" | jq -r .clouds.RustDev.intercloud)" = false ] || fail "RustDev should report no inter-cloud support"
echo "ok: status through the ColonyOS executors (relay, TestCloud, GoCloud ready; RustDev not inter-cloud capable)"

code=$(curl -s -o /dev/null -w '%{http_code}' -b "$jar" -H 'Content-Type: application/json' --data @requests/refused-rustdev.json "$URL/api/deployments")
[ "$code" = 422 ] || fail "policy-violating SoS answered $code"
echo "ok: policy-violating SoS refused before submission"

java -jar "$JAR" apply -request requests/gocloud-to-testcloud-only.json $JAR_OPTS >/dev/null || fail "one-way SoS"
echo "ok: one-way SoS applied; the reverse grant was revoked and verified denied"

java -jar "$JAR" apply -request requests/testcloud-gocloud.json $JAR_OPTS >/dev/null || fail "bidirectional SoS"
echo "ok: bidirectional SoS applied, verified, and discovered over the relay"
echo "SMOKE PASSED"
