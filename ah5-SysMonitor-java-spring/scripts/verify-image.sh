#!/bin/sh
# Build the image, then start it with a read-only root and no network using the packaged development sample: the
# secure-profile guard must refuse that configuration before any listener starts.
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
docker compose build sysmonitor
if docker run --rm --network none --read-only --tmpfs /tmp:rw,noexec,nosuid,nodev,size=64m,mode=1777 \
    --entrypoint java arrowhead-sysmonitor-standalone:5.2.2 -jar /app/app.jar --spring.config.location=classpath:/application.properties \
    > target/image-guard.log 2>&1; then
  echo "The image started with an insecure configuration" >&2
  exit 1
fi
grep -q "Refusing to start" target/image-guard.log
echo "SysMonitor image verified: runs read-only and refuses insecure settings"
