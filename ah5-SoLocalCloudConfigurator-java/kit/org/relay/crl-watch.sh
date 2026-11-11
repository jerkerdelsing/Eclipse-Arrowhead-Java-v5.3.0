#!/bin/sh
# Mosquitto entrypoint with certificate revocation (security findings H3 and SOS-5).
# Concatenates the CRLs named by CRL_SOURCES (space-separated paths, globs allowed) into CRL_FILE, which
# mosquitto.conf names as crlfile, then starts Mosquitto. A watcher re-evaluates every CRL_POLL_SECONDS and, on a
# change, rebuilds CRL_FILE and sends SIGHUP: Mosquitto reloads the CRL without dropping connections.
# Fail closed: Mosquitto is not started without any CRL, and CRL_FILE is never emptied or removed (a missing file
# after a reload would disable the check). An expired CRL makes Mosquitto refuse every client.
#
# Organisation CRL (SOS-5, 2026-11-10). Mosquitto checks only the client's leaf certificate against crlfile, so a
# cloud whose cross-certificate the organisation revokes would still connect. With ORG_CRL set, each cross-certificate
# in CROSS_CERTIFICATES is verified against ORG_CA and ORG_CRL (openssl verify -crl_check: signature, validity,
# revocation, CRL expiry). The CA CRL of a cloud whose cross-certificate fails is left out of CRL_FILE; Mosquitto then
# refuses every relay certificate of that cloud ("unable to get certificate CRL"). An organisation CRL that is missing,
# expired or not signed by ORG_CA fails every cross-certificate. The check runs on every poll, so an expiring CRL takes
# effect without a file change. As for any revocation, connections already open are not dropped.
set -eu
CRL_FILE=${CRL_FILE:-/tmp/arrowhead-crl.pem}
CRL_SOURCES=${CRL_SOURCES:-/crl/ca.crl}
CRL_POLL_SECONDS=${CRL_POLL_SECONDS:-10}
ORG_CA=${ORG_CA:-/certs/org-ca.crt}
ORG_CRL=${ORG_CRL:-}
CROSS_CERTIFICATES=${CROSS_CERTIFICATES:-}

# Every configured cross-certificate: "subject=..." (RFC 2253), a tab, and OK or the reason the organisation CRL does not
# accept it. Exits non-zero when a configured cross-certificate cannot be read.
cross_status() {
  [ -n "$ORG_CRL" ] || return 0
  for c in $CROSS_CERTIFICATES; do
    subject=$(openssl x509 -in "$c" -noout -subject -nameopt RFC2253 2>/dev/null) || return 1
    result=$(openssl verify -crl_check -CAfile "$ORG_CA" -CRLfile "$ORG_CRL" "$c" 2>&1 || true)
    case "$result" in
      *": OK") printf '%s\tOK\n' "$subject" ;;
      *)
        # anything but OK refuses; the reason is for the log only (a missing CRL file has no "lookup:" line)
        reason=$(printf '%s\n' "$result" | sed -n 's/^error [0-9]* at [0-9]* depth lookup: //p' | head -1)
        printf '%s\t%s\n' "$subject" "${reason:-organisation CRL unreadable}" ;;
    esac
  done
}

sources_digest() {
  for f in $CRL_SOURCES $ORG_CRL $CROSS_CERTIFICATES; do [ -f "$f" ] && cksum "$f"; done 2>/dev/null
  cross_status 2>/dev/null || echo "cross-certificate unreadable"
  true
}

combine() {
  tmp="$CRL_FILE.tmp"
  : > "$tmp"
  loaded=
  if ! status=$(cross_status); then
    echo "crl-watch: a cross-certificate in CROSS_CERTIFICATES is missing or unreadable" >&2
    rm -f "$tmp"; return 1
  fi
  for f in $CRL_SOURCES; do
    if [ ! -s "$f" ]; then rm -f "$tmp"; return 1; fi
    issuer=$(openssl crl -in "$f" -noout -issuer -nameopt RFC2253 2>/dev/null | sed 's/^issuer=//') || issuer=
    if [ -n "$ORG_CRL" ]; then
      # fail closed: a CRL source whose CA has no configured cross-certificate is not checked, so it is left out too
      reason=$(printf '%s\n' "$status" | awk -F '\t' -v s="subject=$issuer" '$1 == s { r = ($2 == "" ? "not verified" : $2) } END { print (r == "" ? "no cross-certificate configured" : r) }')
    else
      reason=OK
    fi
    if [ "$reason" != OK ]; then
      echo "crl-watch: $f left out, its cross-certificate is not accepted by the organisation CRL ($reason): relay certificates of $issuer are refused" >&2
      continue
    fi
    cat "$f" >> "$tmp"
    loaded="$loaded $f"
  done
  if grep -q "BEGIN X509 CRL" "$tmp"; then chmod 644 "$tmp"; mv "$tmp" "$CRL_FILE"; return 0; fi
  rm -f "$tmp"; return 1
}

until combine; do
  echo "crl-watch: no usable CRL in $CRL_SOURCES; waiting" >&2
  sleep 5
done
echo "crl-watch: CRL loaded from$loaded${ORG_CRL:+ (organisation CRL $ORG_CRL checked)}"

broker=$$
(
  last=$(sources_digest)
  while sleep "$CRL_POLL_SECONDS"; do
    current=$(sources_digest)
    if [ "$current" != "$last" ]; then
      if ! combine; then
        echo "crl-watch: required CRL missing; stopping broker" >&2
        kill -TERM "$broker" 2>/dev/null || true
        exit 1
      fi
      last=$current
      echo "crl-watch: CRL changed; reloading the broker"
      kill -HUP "$broker" 2>/dev/null || exit 0
    fi
  done
) &

exec /docker-entrypoint.sh "$@"
