# SOS-16 (2026-11-10): gosu rebuilt with a current Go (the image's own is built with Go 1.24.6), MySQL Shell removed (not
# used by the entrypoint or the deployments; it bundles Python packages with known vulnerabilities) and the Oracle Linux
# packages updated.
FROM golang:1.27.4-alpine@sha256:0a5f7fe796599ff487605987432e7619ae45beeb2fba4c2ce0b907879932aebf AS gosu
RUN --mount=type=cache,target=/go/pkg/mod CGO_ENABLED=0 GOBIN=/out go install -trimpath -ldflags=-s github.com/tianon/gosu@v0.0.0-20261025094355-29b939aa5bd4
FROM mysql:8.4.13@sha256:a81d043930e51745522bd5c430961cf8286f0a23f0a9a2a6e0108f7ea71b0902
COPY --from=gosu /out/gosu /usr/local/bin/gosu
RUN microdnf -y remove mysql-shell && microdnf -y update && microdnf clean all && gosu nobody true
COPY deployment/database/init.sql /docker-entrypoint-initdb.d/10-schema.sql
COPY deployment/database/least-privilege.sh /docker-entrypoint-initdb.d/20-least-privilege.sh
