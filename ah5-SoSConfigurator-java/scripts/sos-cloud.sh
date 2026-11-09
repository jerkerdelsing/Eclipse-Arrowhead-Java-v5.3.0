#!/bin/sh
# SoSConfigurator cloud steps, run by the cloud's ColonyOS executor with the cloud directory as working directory.
#
#   sos-cloud.sh <action> <spec.json>
#
# The executor writes spec.json after it has evaluated the SoS request against its own catalog (scripts never decide
# what is allowed). Actions:
#   status       Gatekeeper/GateTunnel state, relay PKI state and current neighbour clouds (JSON on stdout)
#   cloud-check  security and authorization preconditions; changes nothing
#   neighbours   Gatekeeper neighbour clouds = the SoS peers of this cloud (gatekeeperManagement)
#   grants       ConsumerAuthorization policies for the consumers of peer clouds; grants of the catalogued exports to
#                other catalogued clouds are revoked
#   verify       ConsumerAuthorization checks: granted exchanges are granted, revoked ones denied; neighbours as declared
#   discover     global service discovery from this (consumer) cloud's Gatekeeper over the relay
#
# Management calls are made as the operator Sysop with sysop.crt (J6), through the cloud's own lib-policies.sh. No
# secret, certificate or key is created, changed or printed.
set -eu

action=${1:?action}
spec=${2:?spec file}
: "${CLOUD_DIR:?}" "${COMPOSE_PROJECT:?}" "${AUTHENTICATION_URL:?}" "${CONSUMER_AUTHORIZATION_URL:?}" "${GATEKEEPER_URL:?}"
cd "$CLOUD_DIR"
CERT_DIR=security/x509
AUTHORIZATION_URL=$CONSUMER_AUTHORIZATION_URL
export SECRETS_ENV_FILE=${SECRETS_ENV_FILE:-$CLOUD_DIR/.env}
. "$CLOUD_DIR/scripts/lib-secrets.sh"
. "$CLOUD_DIR/scripts/lib-policies.sh"

s() { jq -r "$1" "$spec"; }
CLOUD=$(s .cloud)
ORG=$(s .organization)
fail() { echo "FAIL [$CLOUD] $*" >&2; exit 1; }

running_services() { docker compose -p "$COMPOSE_PROJECT" ps --status running --format '{{.Service}}' 2>/dev/null; }
is_running() { running_services | grep -qx "$1"; }

epoch() { # openssl date ("Nov  4 10:01:28 2026 GMT") -> seconds; BSD or GNU date
  d=$(printf '%s' "$1" | tr -s ' ')
  date -j -u -f '%b %d %H:%M:%S %Y GMT' "$d" +%s 2>/dev/null || date -u -d "$d" +%s
}

# Relay PKI of this cloud (Arrowhead-relay README): cloud CA name, cross-certificate name constraint, relay client
# certificates with the catalogued subject that verify against the organization CA, are not expired and not revoked,
# and a CRL that the relay accepts (fail closed when it is missing or expired). Prints problems, one per line.
pki_problems() {
  for f in ca.crt ca-cross.crt org-ca.crt gatekeeper-relay.crt gatetunnel-relay.crt gatekeeper-relay-chain.crt gatetunnel-relay-chain.crt; do
    [ -f "$CERT_DIR/$f" ] || echo "missing $CERT_DIR/$f (run the cloud's generate-x509.sh, then make -C Arrowhead-relay pki)"
  done
  [ -f "$CERT_DIR/ca.crt" ] && [ -f "$CERT_DIR/ca-cross.crt" ] || return 0
  ca_cn=$(openssl x509 -in "$CERT_DIR/ca.crt" -noout -subject -nameopt RFC2253 | sed -n 's/.*CN=\([^,]*\).*/\1/p')
  [ "$ca_cn" = "$CLOUD.$ORG.arrowhead.eu" ] || echo "cloud CA is $ca_cn, expected $CLOUD.$ORG.arrowhead.eu"
  openssl x509 -in "$CERT_DIR/ca-cross.crt" -noout -ext nameConstraints 2>/dev/null | grep -q "O = $ORG, OU = $CLOUD\$" \
    || echo "cross-certificate is not name-constrained to O=$ORG, OU=$CLOUD"
  crl=security/crl/ca.crl
  if [ ! -f "$crl" ]; then
    echo "missing $crl: the relay refuses this cloud's certificates"
  else
    next=$(openssl crl -in "$crl" -noout -nextupdate | sed 's/^nextUpdate=//')
    [ "$(epoch "$next")" -gt "$(date -u +%s)" ] || echo "CRL expired ($next): the relay refuses this cloud's certificates"
  fi
  for sys in Gatekeeper GateTunnel; do
    lower=$(printf '%s' "$sys" | tr 'A-Z' 'a-z')
    cert="$CERT_DIR/$lower-relay.crt"
    [ -f "$cert" ] || continue
    want=$(s ".subjects.$sys")
    got=$(openssl x509 -in "$cert" -noout -subject -nameopt RFC2253 | sed 's/^subject=//')
    [ "$got" = "$want" ] || echo "$sys relay certificate subject is $got, expected $want"
    openssl x509 -in "$cert" -noout -checkend 86400 >/dev/null || echo "$sys relay certificate expires within a day"
    openssl verify -CAfile "$CERT_DIR/org-ca.crt" -untrusted "$CERT_DIR/ca-cross.crt" "$cert" >/dev/null 2>&1 \
      || echo "$sys relay certificate does not verify against the organization CA through the constrained cross-certificate"
    [ ! -f "$crl" ] || openssl verify -crl_check -CRLfile "$crl" -CAfile "$CERT_DIR/ca.crt" "$cert" >/dev/null 2>&1 \
      || echo "$sys relay certificate is revoked or does not verify against the cloud CA"
  done
}

neighbours_json() { policy_call "$GATEKEEPER_URL/gatekeeper/mgmt/clouds"; }

# ConsumerAuthorization checks: $1 = JSON array of {provider, consumer, cloud, service, scope, expected}
run_checks() {
  checks=$1
  [ "$(printf '%s' "$checks" | jq length)" -gt 0 ] || return 0
  body=$(printf '%s' "$checks" | jq -c '{list: [.[] | {provider, consumer, cloud, targetType: "SERVICE_DEF", target: .service}
    + (if .scope then {scope} else {} end)]}')
  answer=$(policy_call --data "$body" "$AUTHORIZATION_URL/consumerauthorization/authorization/mgmt/check")
  printf '%s' "$answer" | jq -e '.entries | type == "array"' >/dev/null 2>&1 || fail "authorization check refused: $answer"
  bad=0
  i=0
  n=$(printf '%s' "$checks" | jq length)
  while [ "$i" -lt "$n" ]; do
    c=$(printf '%s' "$checks" | jq -c ".[$i]")
    want=$(printf '%s' "$c" | jq -r .expected)
    got=$(printf '%s' "$answer" | jq -r ".entries[$i].granted // false")
    line=$(printf '%s' "$c" | jq -r '"\(.cloud)/\(.consumer) -> \(.provider).\(.service) \(.scope // "")"')
    if [ "$want" = "$got" ]; then
      echo "  ok   $line: granted=$got"
    else
      echo "  FAIL $line: granted=$got, expected $want" >&2
      bad=1
    fi
    i=$((i + 1))
  done
  [ "$bad" = 0 ]
}

case "$action" in
status)
  gk=false; gt=false; nb=null
  is_running "$(s .gatekeeperService)" && gk=true
  is_running "$(s .gatetunnelService)" && gt=true
  problems=$(pki_problems | jq -Rsc 'split("\n") | map(select(length > 0))')
  if [ "$gk" = true ]; then
    nb=$( (policy_token && neighbours_json) 2>/dev/null | jq -c '[.entries[]? | "\(.name)|\(.operator)"]' 2>/dev/null || echo null)
  fi
  jq -nc --arg cloud "$CLOUD" --argjson gk "$gk" --argjson gt "$gt" --argjson pki "$problems" --argjson nb "${nb:-null}" \
    '{cloud: $cloud, intercloud: true, gatekeeper: $gk, gatetunnel: $gt, pkiProblems: $pki, neighbours: $nb,
      state: (if $gk and $gt and ($pki | length) == 0 then "ready" else "not-ready" end)}'
  ;;

cloud-check)
  echo "SoS $(s .sos): preconditions of $CLOUD"
  for svc in "$(s .gatekeeperService)" "$(s .gatetunnelService)"; do
    is_running "$svc" || fail "service $svc is not running"
  done
  echo "  ok   Gatekeeper and GateTunnel run"
  for alts in $(s '.config.exportServices[]'); do
    found=""
    for svc in $(printf '%s' "$alts" | tr ',' ' '); do is_running "$svc" && found=$svc; done
    [ -n "$found" ] || fail "no provider service of [$alts] runs"
    echo "  ok   provider service $found runs"
  done
  problems=$(pki_problems)
  [ -z "$problems" ] || fail "relay PKI: $(printf '%s' "$problems" | tr '\n' ';')"
  echo "  ok   relay PKI: CA $CLOUD.$ORG.arrowhead.eu, constrained cross-certificate, relay certificates, fresh CRL"
  policy_token
  run_checks "$(jq -c .config.preconditions "$spec")" || fail "authorization preconditions not met (the cloud's policy scripts grant them: configure-intercloud.sh / configure-orchestration-dependency-policies.sh, configure-datamanager-policies.sh)"
  ;;

neighbours)
  policy_token
  add=$(jq -c '{list: .config.neighbours}' "$spec")
  if [ "$(printf '%s' "$add" | jq '.list | length')" -gt 0 ]; then
    response=$(policy_call --data "$add" "$GATEKEEPER_URL/gatekeeper/mgmt/clouds")
    printf '%s' "$response" | jq -e '.count > 0' >/dev/null 2>&1 || fail "unable to register neighbour clouds: $response"
  fi
  current=$(neighbours_json)
  for row in $(jq -r '.config.removeNeighbours[] | "\(.operator)/\(.name)"' "$spec"); do
    name=${row#*/}; operator=${row%%/*}
    if printf '%s' "$current" | jq -e --arg n "$name" --arg o "$operator" 'any(.entries[]?; .name == $n and .operator == $o)' >/dev/null; then
      policy_call -X DELETE "$GATEKEEPER_URL/gatekeeper/mgmt/clouds/$operator/$name" >/dev/null
      echo "  removed neighbour $name|$operator"
    fi
  done
  echo "Gatekeeper neighbour clouds of $CLOUD: $(neighbours_json | jq -r '[.entries[]? | "\(.name)|\(.operator)"] | join(", ")')"
  ;;

grants)
  n=$(jq '.config.grants | length' "$spec")
  if [ "$n" -gt 0 ]; then
    apply_policies "$(jq -c --arg sos "$(s .sos)" '{list: [.config.grants[] | {cloud, provider, targetType: "SERVICE_DEF", target: .service,
      description: "SoS \($sos): inter-cloud consumers of \(.cloud)",
      defaultPolicy: {policyType: "WHITELIST", policyList: .consumers}, scopedPolicies: {}}]}' "$spec")"
  fi
  policy_token
  for id in $(jq -r '.config.revokes[] | @uri' "$spec"); do
    policy_call -X DELETE "$AUTHORIZATION_URL/consumerauthorization/authorization/mgmt/revoke?instanceIds=$id" >/dev/null
    echo "  policy $(printf '%s' "$id" | sed 's/%7C/|/g') revoked (not part of the SoS)"
  done
  ;;

verify)
  policy_token
  echo "SoS $(s .sos): ConsumerAuthorization of $CLOUD"
  run_checks "$(jq -c .config.verify "$spec")" || fail "authorization does not match the SoS"
  current=$(neighbours_json)
  for n in $(jq -r '.config.neighbours[] | "\(.name)|\(.operator)"' "$spec"); do
    printf '%s' "$current" | jq -e --arg n "$n" 'any(.entries[]?; "\(.name)|\(.operator)" == $n)' >/dev/null || fail "neighbour $n missing"
  done
  for n in $(jq -r '.config.removeNeighbours[] | "\(.name)|\(.operator)"' "$spec"); do
    if printf '%s' "$current" | jq -e --arg n "$n" 'any(.entries[]?; "\(.name)|\(.operator)" == $n)' >/dev/null; then fail "neighbour $n still registered"; fi
  done
  echo "  ok   neighbours: $(printf '%s' "$current" | jq -r '[.entries[]? | "\(.name)|\(.operator)"] | join(", ")')"
  ;;

discover)
  policy_token
  jq -c '.config.discovery[]' "$spec" | while IFS= read -r d; do
    body=$(printf '%s' "$d" | jq -c '{consumerSystem: .consumer, serviceRequirement: {serviceDefinition: .service}, targetClouds: [.targetCloud]}')
    answer=$(policy_call --data "$body" "$GATEKEEPER_URL/gatekeeper/init_gsd")
    label=$(printf '%s' "$d" | jq -r '"\(.consumer) -> \(.targetCloud) \(.provider).\(.service)"')
    printf '%s' "$answer" | jq -e --argjson d "$d" 'any(.results[]?; .providerName == $d.provider)' >/dev/null \
      || fail "discovery $label: provider not found: $answer"
    echo "  ok   discovery $label over the relay"
  done
  ;;

*)
  fail "unknown action $action"
  ;;
esac
