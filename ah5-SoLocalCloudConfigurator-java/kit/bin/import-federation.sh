#!/bin/sh
# SPDX-License-Identifier: EPL-2.0
# Member host: install the organisation's federation bundle and switch the cloud's Gatekeeper and GateTunnel to the relay.
#   import-federation.sh <cloud.json> <Cloud>-federation.tar.gz
#   import-federation.sh <cloud.json> --reinstall        (install.sh: re-apply the imported material after a re-run)
# Checks: the cross-certificate carries this cloud CA's subject and key and is name-constrained to this cloud; the relay
# client certificates verify under it and the organisation CA; the organisation CRL and each peer CRL are signed by
# their CA. Then: relay chains, organisation CRL, peer CRLs, credential bundles, Compose settings, restart.
CLOUD_JSON=${1:?cloud.json}
. "$(dirname -- "$0")/lib-cloud.sh"
need openssl tar
B=${2:?federation bundle or --reinstall}
if [ "$B" != --reinstall ]; then
  [ -f "$B" ] || die "no such bundle: $B"
  B=$(CDPATH= cd -- "$(dirname -- "$B")" && pwd)/$(basename -- "$B")
fi
cd "$WORK"
X=security/x509
F=$WORK/federation
if [ "$B" != --reinstall ]; then
  tmp=$(mktemp -d)
  tar -xzf "$B" -C "$tmp" --no-same-owner --no-same-permissions
  for f in federation.json org-ca.crt org-ca.crl ca-cross.crt; do [ -f "$tmp/$f" ] && [ ! -L "$tmp/$f" ] || die "bundle lacks $f"; done
  python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); assert d["cloud"]==sys.argv[2], "bundle is for "+d["cloud"]' "$tmp/federation.json" "$CLOUD" || die "wrong bundle"
  rm -rf "$F.new"; mkdir -p "$F.new"; cp -R "$tmp/." "$F.new/"; rm -rf "$tmp"
  src=$F.new
else
  [ -d "$F" ] || die "no imported federation"
  src=$F
fi
[ "$(openssl x509 -in "$src/ca-cross.crt" -noout -pubkey | openssl sha256)" = "$(openssl x509 -in $X/ca.crt -noout -pubkey | openssl sha256)" ] \
  || die "the cross-certificate is not for this cloud's CA key"
[ "$(openssl x509 -in "$src/ca-cross.crt" -noout -subject -nameopt RFC2253)" = "$(openssl x509 -in $X/ca.crt -noout -subject -nameopt RFC2253)" ] \
  || die "the cross-certificate subject differs from this cloud's CA"
openssl x509 -in "$src/ca-cross.crt" -noout -text | grep -q "DirName:C *= *SE, *O *= *$ORG, *OU *= *$CLOUD\|DirName:C=SE, O=$ORG, OU=$CLOUD" || die "cross-certificate is not constrained to OU=$CLOUD"
openssl crl -in "$src/org-ca.crl" -CAfile "$src/org-ca.crt" -noout 2>&1 | grep -q "verify OK" || die "organisation CRL not signed by the organisation CA"
for s in gatekeeper gatetunnel; do
  openssl verify -CAfile "$src/org-ca.crt" -untrusted "$src/ca-cross.crt" "$X/$s-relay.crt" >/dev/null || die "$s relay certificate does not verify under the cross-certificate"
done
mkdir -p security/org-crl security/peer-crl
for d in "$src"/peers/*/; do
  [ -d "$d" ] || continue
  p=$(basename "$d")
  openssl crl -in "$d/ca.crl" -CAfile "$d/ca.crt" -noout 2>&1 | grep -q "verify OK" || die "CRL of peer $p not signed by its CA"
  mkdir -p "security/peer-crl/$p"; cp "$d/ca.crl" "security/peer-crl/$p/.ca.crl.tmp"; mv "security/peer-crl/$p/.ca.crl.tmp" "security/peer-crl/$p/ca.crl"
done
cp "$src/org-ca.crt" $X/org-ca.crt
cp "$src/ca-cross.crt" $X/ca-cross.crt
for s in gatekeeper gatetunnel; do cat "$X/$s-relay.crt" $X/ca-cross.crt > "$X/$s-relay-chain.crt"; done
cp "$src/org-ca.crl" security/org-crl/.org-ca.crl.tmp; mv security/org-crl/.org-ca.crl.tmp security/org-crl/org-ca.crl
chmod 644 $X/org-ca.crt $X/ca-cross.crt $X/*-relay-chain.crt security/org-crl/org-ca.crl
chmod -R a+rX security/org-crl security/peer-crl
if [ "$src" = "$F.new" ]; then rm -rf "$F"; mv "$F.new" "$F"; fi
touch "$F/imported"
[ "$B" = --reinstall ] && exit 0
./scripts/make-credential-bundles.sh >/dev/null
# the peers the organisation has signed so far, as the cloud's GateTunnel neighbours
python3 - "$CLOUD_JSON" "$F/federation.json" <<'PY'
import json, sys
c = json.load(open(sys.argv[1])); f = json.load(open(sys.argv[2]))
c["peers"] = [p for p in c.get("peers", []) if p in f.get("peers", [])]
c["relay"] = f["relay"]
json.dump(c, open(sys.argv[1], "w"), indent=2)
PY
python3 "$KIT/bin/configure.py" "$PKG" "$CLOUD_JSON" "$WORK" >/dev/null
"$KIT/bin/deploy-cloud.sh" "$CLOUD_JSON" restart-federation
echo "$CLOUD federated with: $(cj peers) (relay $(cj relay.host):$(cj relay.port)). Next: clouds/$CLOUD/intercloud.sh"
