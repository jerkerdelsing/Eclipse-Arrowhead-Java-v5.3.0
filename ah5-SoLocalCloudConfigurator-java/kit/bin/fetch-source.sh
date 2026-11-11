#!/bin/sh
# SPDX-License-Identifier: EPL-2.0
# Clone the pinned source of the package (source/SOURCE.json: repository and commit) into source/tree and check the commit.
#   fetch-source.sh <package-dir>
set -eu
PKG=$(CDPATH= cd -- "${1:?package dir}" && pwd)
repo=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["repository"])' "$PKG/source/SOURCE.json")
commit=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["commit"] or "")' "$PKG/source/SOURCE.json")
[ -n "$commit" ] || { echo "error: the package pins no commit" >&2; exit 1; }
command -v git >/dev/null 2>&1 || { echo "error: git is required, or add source/source.tar.gz with assemble-package.sh --source" >&2; exit 1; }
tmp=$PKG/source/.tree.tmp
rm -rf "$tmp"
git init -q "$tmp"
git -C "$tmp" fetch -q --depth 1 "$repo" "$commit"
git -C "$tmp" checkout -q --detach FETCH_HEAD
[ "$(git -C "$tmp" rev-parse HEAD)" = "$commit" ] || { echo "error: fetched commit differs from $commit" >&2; exit 1; }
rm -rf "$tmp/.git"
mv "$tmp" "$PKG/source/tree"
echo "source $repo at $commit -> source/tree"
