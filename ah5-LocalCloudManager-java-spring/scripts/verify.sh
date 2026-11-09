#!/bin/sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
(cd api && npm ci && npm test && npm run check)
(cd docker-proxy && node --test)
node --check ui/public/app.js
