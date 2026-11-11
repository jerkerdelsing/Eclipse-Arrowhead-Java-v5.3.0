# SPDX-License-Identifier: EPL-2.0
# Relay broker (SOS-5, 2026-11-10): the official Mosquitto image plus the openssl command, which crl-watch.sh uses to
# check each cloud's cross-certificate against the organisation CA's CRL. Mosquitto itself checks only the client's
# leaf certificate against crlfile (X509_V_FLAG_CRL_CHECK), so it cannot enforce the organisation CRL on its own.
FROM eclipse-mosquitto:2.0.22@sha256:81a6193ae693942a3231cd3ac52875b02b7e3a19da671b2fb4b55cc874c5b279
USER root
# pinned: also brings libssl3/libcrypto3 from 3.5.8-r0 to 3.5.9-r0 (same Alpine branch)
RUN apk add --no-cache openssl=3.5.9-r0 libssl3=3.5.9-r0 libcrypto3=3.5.9-r0 && openssl version
USER mosquitto
LABEL eu.arrowhead.change="SOS-5: openssl for the organisation CRL check in crl-watch.sh, 2026-11-10; SOS-16: rebuilt Mosquitto 2.0.22 base (Alpine 3.23.8), 2026-11-10"
