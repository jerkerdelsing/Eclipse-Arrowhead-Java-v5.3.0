# LocalCloudManager independent console deployment

This project is Node.js/nginx, despite its Java-style folder name. A Java build/JAR is not applicable. It owns its API/UI source, npm build/tests, Dockerfiles, configuration and compose.yaml. No JavaSupportRuntime is used.

Copy .env.example to .env; select an existing cloud network, unique Compose project/host port and non-overlapping CONTROL_SUBNET. Provision its PEM system identity, password, public CA/CRL, a scrypt console password hash and a random proxy secret in the named files. The system certificate CN is LocalCloudManager.<Cloud>.<Organization>.arrowhead.eu with profile sy. File paths are configured in .env and read by the intended unprivileged user.

Run `docker compose config --quiet`, `docker compose build`, and `docker compose up -d` from this folder. The UI is loopback-published on port 8490; the API is internal on 8491. API/UI state is volatile and no shared runtime state is required. Run `scripts/verify.sh` with Node.js 24 to test the sources.

The selected deployment is console-only: it has no Docker host access. Container inventory/log/lifecycle features are unavailable here. Core/provider access still needs registry, identity, authorization and orchestration configuration; broker-backed observations need the AMQP broker. The existing docker-proxy source is historical optional integration material and is not included in this Compose definition.

The console isolation test uses generated test credentials and a temporary network and checks API/UI startup and access control. No production credential or live cloud is changed. See ../ALL-JAVA-SUPPORT-VERIFICATION-2026-10-25.md.
