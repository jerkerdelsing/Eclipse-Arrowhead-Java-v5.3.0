#!/bin/sh
# SPDX-License-Identifier: EPL-2.0
# Images of one member cloud: prebuilt packages load images/<arch>/images.tar (checked against images.sha256);
# build-on-target packages build them from source/source.tar.gz or, without it, from a clone of the pinned commit.
CLOUD_JSON=${1:?cloud.json}
. "$(dirname -- "$0")/lib-cloud.sh"
MODE=$(cj imageMode)
missing() { for i in $(python3 -c 'import json,sys; print(" ".join(sorted({s["image"] for s in json.load(open(sys.argv[1]))["services"].values()})))' "$WORK/compose.yaml"); do
  docker image inspect "$i" >/dev/null 2>&1 || echo "$i"; done; }
if [ -z "$(missing)" ]; then echo "all images present"; exit 0; fi
if [ "$MODE" = prebuilt ] || [ -f "$PKG/images/$ARCH/images.tar" ]; then
  a=$PKG/images/$ARCH
  [ -f "$a/images.tar" ] || die "images/$ARCH/images.tar is missing: complete the package with kit/bin/assemble-package.sh --arch $ARCH (images/README.md)"
  (cd "$a" && sha256 -c images.sha256) || die "images/$ARCH/images.tar does not match images.sha256"
  docker load -i "$a/images.tar"
else
  src=$PKG/source/tree
  if [ ! -d "$src" ]; then
    if [ -f "$PKG/source/source.tar.gz" ]; then
      (cd "$PKG/source" && sha256 -c source.sha256) || die "source.tar.gz does not match source.sha256"
      mkdir -p "$src"; tar -xzf "$PKG/source/source.tar.gz" -C "$src"
    else
      "$KIT/bin/fetch-source.sh" "$PKG"
    fi
  fi
  python3 "$KIT/bin/build-images.py" --source "$src" --platform "linux/$ARCH" --tag "$(cj imageTag)" "$WORK/compose.yaml"
fi
left=$(missing)
[ -z "$left" ] || die "images still missing: $left"
echo "all images present"
