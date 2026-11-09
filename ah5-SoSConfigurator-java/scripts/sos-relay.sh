#!/bin/sh
# SoSConfigurator relay checks, run by the relay's ColonyOS executor in the Arrowhead-relay directory.
#
#   sos-relay.sh status|check <spec.json>      spec: {members: [{name, composeProject, gatekeeperService, subjects: [DN, DN]}]}
#
# Security conditions for every member cloud (Arrowhead-relay README):
#   - the broker runs, refuses anonymous clients, requires client certificates and uses the verified subject DN as the
#     MQTT user name;
#   - mqtt/acl has a block per relay subject (Gatekeeper and GateTunnel) that reads only the own cloud's inbox of its
#     role and writes into other inboxes only below its own CN; no ACL line grants a topic to everybody;
#   - the relay watches the cloud's CRL (CRL_SOURCES) and shares a Docker network with the cloud's Gatekeeper.
# The relay configuration is only checked, never changed: adding a cloud means editing mqtt/acl and compose.yaml,
# rerunning make pki and restarting the relay, which is an operator decision.
set -eu

action=${1:?action}
spec=${2:?spec file}
: "${RELAY_DIR:?}" "${RELAY_PROJECT:?}" "${RELAY_SERVICE:?}" "${RELAY_ACL:?}"
cd "$RELAY_DIR"
problems=""
problem() { problems="$problems$*
"; }

cid=$(docker compose -p "$RELAY_PROJECT" ps -q --status running "$RELAY_SERVICE" 2>/dev/null | head -n 1)
[ -n "$cid" ] || problem "relay service $RELAY_SERVICE of $RELAY_PROJECT is not running"
conf=mqtt/mosquitto.conf
for line in "allow_anonymous false" "require_certificate true" "use_subject_as_username true"; do
  grep -qx "$line" "$conf" || problem "$conf lacks '$line'"
done
grep -q '^crlfile ' "$conf" || problem "$conf has no crlfile (revocation is not checked)"
grep -Eq '^[[:space:]]*(pattern|topic)[[:space:]]+(read|write|readwrite)?[[:space:]]*#[[:space:]]*$' "$RELAY_ACL" \
  && problem "$RELAY_ACL grants # to a client"
crl_sources=""
relay_networks=""
if [ -n "$cid" ]; then
  crl_sources=$(docker inspect "$cid" --format '{{range .Config.Env}}{{println .}}{{end}}' | sed -n 's/^CRL_SOURCES=//p')
  relay_networks=$(docker inspect "$cid" --format '{{range $k, $v := .NetworkSettings.Networks}}{{println $k}}{{end}}')
fi

# The ACL block of one subject: the lines after "user <subject>" up to the next user line.
acl_block() { awk -v u="user $1" '$0 == u {on = 1; next} /^user / {on = 0} on' "$RELAY_ACL"; }

members=$(jq -c '.members[]' "$spec")
summary="[]"
IFS='
'
for m in $members; do
  IFS=' 	
'
  name=$(printf '%s' "$m" | jq -r .name)
  ok=true
  for subject in $(printf '%s' "$m" | jq -r '.subjects[]'); do
    cn=$(printf '%s' "$subject" | sed -n 's/.*CN=\([^,]*\).*/\1/p')
    role=$(printf '%s' "$cn" | cut -d. -f1 | tr 'A-Z' 'a-z')
    block=$(acl_block "$subject")
    if [ -z "$block" ]; then
      problem "$name: no ACL user $subject"; ok=false; continue
    fi
    printf '%s\n' "$block" | grep -Eq "^topic read arrowhead/relay/$name/$role/" || { problem "$name: $cn may not read its own $role inbox"; ok=false; }
    printf '%s\n' "$block" | grep -Eq "^topic write arrowhead/relay/[^ ]*/$role/in/$cn/" || { problem "$name: $cn may not write below its own CN"; ok=false; }
    printf '%s\n' "$block" | grep -E '^topic (read|readwrite) ' | grep -Ev "arrowhead/relay/$name/$role/" >/dev/null \
      && { problem "$name: $cn may read inboxes of other clouds or roles"; ok=false; }
    printf '%s\n' "$block" | grep -E '^topic (write|readwrite) ' | grep -v "/in/$cn/" >/dev/null \
      && { problem "$name: $cn may write outside its own CN"; ok=false; }
  done
  if [ -n "$cid" ]; then
    printf '%s' "$crl_sources" | tr ' ' '\n' | grep -q "^/crl/$name/" || { problem "$name: the relay does not watch its CRL (CRL_SOURCES)"; ok=false; }
    gk=$(docker compose -p "$(printf '%s' "$m" | jq -r .composeProject)" ps -q "$(printf '%s' "$m" | jq -r .gatekeeperService)" 2>/dev/null | head -n 1)
    shared=""
    if [ -n "$gk" ]; then
      for net in $(docker inspect "$gk" --format '{{range $k, $v := .NetworkSettings.Networks}}{{println $k}}{{end}}'); do
        printf '%s\n' "$relay_networks" | grep -qx "$net" && shared=$net
      done
    fi
    [ -n "$shared" ] || { problem "$name: the relay shares no network with its Gatekeeper"; ok=false; }
  fi
  summary=$(printf '%s' "$summary" | jq -c --arg n "$name" --argjson ok "$ok" '. + [{cloud: $n, served: $ok}]')
done
IFS=' 	
'

case "$action" in
status)
  jq -nc --argjson running "$([ -n "$cid" ] && echo true || echo false)" --argjson clouds "$summary" \
    --argjson problems "$(printf '%s' "$problems" | jq -Rsc 'split("\n") | map(select(length > 0))')" \
    '{relay: true, running: $running, clouds: $clouds, problems: $problems,
      state: (if $running and ($problems | length) == 0 then "ready" else "not-ready" end)}'
  ;;
check)
  if [ -n "$problems" ]; then
    printf 'FAIL relay: %s' "$problems" >&2
    exit 1
  fi
  echo "relay serves $(printf '%s' "$summary" | jq -r '[.[].cloud] | join(", ")'): mTLS, subject user names, CRLs, per-subject ACL, shared networks"
  ;;
*)
  echo "unknown action $action" >&2
  exit 1
  ;;
esac
