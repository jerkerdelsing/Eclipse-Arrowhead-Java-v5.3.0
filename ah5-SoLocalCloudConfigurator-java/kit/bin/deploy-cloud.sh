#!/bin/sh
# SPDX-License-Identifier: EPL-2.0
# Start a member cloud in bootstrap order and provision it: identities, ConsumerAuthorization policies, database
# migrations; each step only for the services the cloud runs (its local-cloud design). Order as the v5.3.0 test cloud's
# deploy.sh, whose result smoke-test.sh and smoke-security.sh verify.
#   deploy-cloud.sh <cloud.json> up | down | restart-federation
CLOUD_JSON=${1:?cloud.json}
. "$(dirname -- "$0")/lib-cloud.sh"
cd "$WORK"
SERVICES=$(dc config --services)
has() { printf '%s\n' "$SERVICES" | grep -qx "$1"; }
up() { dc up -d --no-build --pull never --wait --wait-timeout "${WAIT:-300}" "$@"; }
run() { echo "-- $*"; "$@"; }
present() { out=; for s in "$@"; do has "$s" && out="$out $s"; done; echo "$out"; }

case "${2:?up|down|restart-federation}" in
up)
  has amqp-broker && run python3 scripts/generate-amqp-definitions.py
  up serviceregistry authentication-db authentication
  run ./scripts/provision-authentication-identities.sh
  run ./scripts/configure-authentication-mtls-registry.sh
  dc restart authentication
  up authentication
  run ./scripts/register-authentication-services.sh
  has amqp-broker && run ./scripts/register-amqp-interface-templates.sh
  for s in gatekeeper gatetunnel sysmonitor timemanager plantdescriptionengine configuration choreographer aigym; do
    has "$s" && run "./scripts/provision-$s-identity.sh"
  done
  run ./scripts/provision-smoke-client-identity.sh
  # shellcheck disable=SC2046
  up $(present amqp-broker mqtt-broker consumerauthorization datamanager)
  run ./scripts/configure-orchestration-security-policies.sh
  run ./scripts/configure-orchestration-dependency-policies.sh
  has random-data-producer && run ./scripts/configure-random-producer-policy.sh
  up serviceorchestration-dynamic-db
  run ./scripts/migrate-dso-plant-rules-db.sh
  WAIT=600 up $(printf '%s\n' "$SERVICES" | grep -v '^random-data-producer$')
  for s in gatekeeper gatetunnel sysmonitor; do has "$s" && run "./scripts/configure-$s-policy.sh"; done
  has sysmonitor && run ./scripts/configure-all-sysmonitor-policies.sh
  for s in timemanager eventhandler plantdescriptionengine configuration choreographer onboarding aigym; do
    if has "$s"; then
      [ "$s" = onboarding ] && run ./scripts/migrate-onboarding-db.sh
      run "./scripts/configure-$s-policy.sh"
    fi
  done
  has datamanager && has sysmonitor && run ./scripts/configure-datamanager-monitoring-policy.sh
  has datamanager && run ./scripts/configure-datamanager-policies.sh
  has graphdbmanager && run ./scripts/configure-graphdbmanager-policies.sh
  has random-data-producer && up random-data-producer
  dc ps --format 'table {{.Service}}\t{{.Status}}' ;;
restart-federation)
  # after import-federation.sh: recreate the containers whose federation settings changed
  # shellcheck disable=SC2046
  dc up -d --no-build --pull never --wait $(present gatekeeper gatetunnel serviceorchestration-dynamic random-data-producer) ;;
down)
  dc down --remove-orphans ;;
*) die "usage: deploy-cloud.sh <cloud.json> up|down|restart-federation" ;;
esac
