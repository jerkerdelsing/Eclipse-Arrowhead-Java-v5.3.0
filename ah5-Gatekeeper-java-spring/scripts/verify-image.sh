#!/bin/sh
# Builds the image and checks that it refuses the packaged development configuration (secure profile), running with a
# read-only root and a non-executable /tmp as in compose.yaml.
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
docker compose build gatekeeper
if docker run --rm --network none --read-only --tmpfs /tmp:rw,noexec,nosuid,nodev,size=64m,mode=1777 \
    --entrypoint java arrowhead-gatekeeper-standalone:5.4.0 -jar /app/app.jar --spring.config.location=classpath:/application.properties \
    > target/image-refusal.log 2>&1; then
  echo "The image started with the insecure development configuration" >&2
  exit 1
fi
grep -q "Refusing to start" target/image-refusal.log
echo "Gatekeeper image refuses insecure settings"
