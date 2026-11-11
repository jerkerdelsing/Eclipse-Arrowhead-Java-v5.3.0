#!/bin/sh
# Build and test from the primary source, update the cloud's Compose file, install the host worker and recreate only the
# configurator and its database. This changes a deployed cloud: run it only when an operator decides so.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
ECLIPSE=$(CDPATH= cd -- "$ROOT/../.." && pwd)
CLOUD=${JAVA_CLOUD_DIR:-$ECLIPSE/OLD/Arrowhead/Arrowhead-test-cloud-java}
PRIVATE=${JAVA_LCC_PRIVATE_DIR:-$HOME/.local/share/JavaLocalCloudConfigurator}
ENV_FILE=${JAVA_LCC_ENV_FILE:-$CLOUD/localcloudconfigurator.env}
DOCKER=${DOCKER:-docker}
cd "$ROOT"
"$DOCKER" run --rm -v "$ROOT:/src" -v "$HOME/.m2:/root/.m2" -w /src maven:3.9-eclipse-temurin-21 mvn -B clean verify
python3 scripts/configure-direct-cloud.py --cloud-dir "$CLOUD" --env-file "$ENV_FILE"
python3 scripts/install-direct.py --cloud-dir "$CLOUD" --compose-file "$CLOUD/compose.yaml" --private-dir "$PRIVATE" ${JAVA_LCC_CRL_FILE:+--crl-file "$JAVA_LCC_CRL_FILE"}
"$DOCKER" compose -f "$CLOUD/compose.yaml" up -d --build --no-deps --wait localcloudconfigurator-db localcloudconfigurator
# LocalCloudManager reaches the configurator only through orchestration, and the configurator verifies every call with
# ConsumerAuthorization: both need their grants (the cloud's policy scripts, applied as Sysop; re-running replaces them)
if [ -x "$CLOUD/scripts/configure-localcloudconfigurator-policy.sh" ]; then
  (cd "$CLOUD" && ./scripts/configure-orchestration-dependency-policies.sh && ./scripts/configure-localcloudconfigurator-policy.sh)
else
  echo "WARNING: $CLOUD has no scripts/configure-localcloudconfigurator-policy.sh: grant LocalCloudManager the localCloudConfiguration operations and LocalCloudConfigurator authorization.verify in ConsumerAuthorization" >&2
fi
printf '%s\n' 'Java LocalCloudConfigurator: Arrowhead support system on 8492 (mutual TLS), host worker on loopback 8493 (mutual TLS).'
