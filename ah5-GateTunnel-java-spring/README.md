# GateTunnel 5.2.2 - independent Java distribution

> Current standalone packaging (25 October 2026): build and deploy from this folder. See [DEPLOYMENT.md](DEPLOYMENT.md). Any combined-tree build or live-cloud examples below are historical integration context.

This folder is the complete build and deployment unit for GateTunnel. It uses Java 21 and Maven, JDK HttpsServer (not Spring), and its own shaded JAR. The folder name follows the existing Java repository layout. No source, build parent, image, database or configuration is loaded from JavaSupportRuntime or a sibling support folder. Shared helper implementations are private copies under `src/main/java/eu/arrowhead/port`; security fixes must be applied to each affected copy.

## Build and test

From this folder with Java 21 and Maven 3.9 installed:

```sh
mvn -B clean verify
java -jar target/arrowhead-gatetunnel-5.2.2.jar storage-selftest
```

Alternatively `docker compose build` runs all tests and creates `arrowhead-gatetunnel-standalone:5.2.2` using only this folder as the context. `Main` always starts GateTunnel; there is no runtime role selector. The artifact excludes the other three former runtime systems. Dependencies are resolved from Maven repositories, or a populated Maven cache for offline builds.

`scripts/verify.sh` repeats the build, SQLite check and artifact isolation check (`-o` selects offline Maven use). `scripts/verify-image.sh` also checks the actual image with a read-only root and non-executable temporary storage.

## Deploy into a selected local cloud

1. Copy `.env.example` to `.env` and select the existing Docker network and registry URL. Use a distinct `COMPOSE_PROJECT_NAME` and host port for each deployment when operating multiple clouds on one Docker host. If creating a new cloud network, create it before starting this service; configure the core systems and brokers on that network separately.
2. Provision `security/gatetunnel.crt`, `gatetunnel.key`, `ca.crt`, `identity-password`, and `crl/ca.crl`, or set their file paths in `.env`. Use a system certificate with CN `GateTunnel.<LOCAL_CLOUD>.<ORGANIZATION>.arrowhead.eu` and DNQ `sy`, and register the matching identity/password and authorizations with the cloud. The CRL must be valid for the trusted cloud issuer. Private material must be readable by container UID 10001; the CRL directory must exist. No development credentials are bundled.
3. Run `docker compose config --quiet`, then `docker compose up -d --build` from this folder. `docker compose ps` shows health and `docker compose logs` shows registration/dependency errors. Health is system-specific: SysMonitor reports HTTP 503 without its AMQP/MQTT connections, while the other three return process liveness. Examine registration, broker and orchestration status before accepting operational readiness.
4. Repeat only for other support folders you choose. Each Compose project owns its own `state` volume; stopping one does not remove another's state. `docker compose down` preserves the volume. Back up the private SQLite database before upgrades. Do not use `down -v` unless deliberately erasing this system's state.

The HTTPS service is published on loopback port 8453 by default; the internal health port 8481 is not published. The container is unprivileged, read-only except `/data` and `/tmp`. Certificates and password are mounted individually, not as a shared credentials bundle. The image installs the platform SQLite native library under `/app/lib` at build time, so `/tmp` can remain non-executable. The SQLite volume is never shared between systems. Do not attach an old cloud's database to a second running instance.

## Arrowhead service dependencies

ServiceRegistry is the bootstrap URL. Authentication and DynamicServiceOrchestration are discovered through the registry; ConsumerAuthorization and other consumed services are obtained through DSO. Provision the corresponding identity, service registrations, orchestration and authorization rules. AMQPS uses `AMQP_HOST`/`AMQP_PORT` and the system certificate for EXTERNAL authentication. These are network service dependencies, not a dependency on a common support runtime. Starting this process does not start other support systems or provision the cloud.

Federation uses the MQTT relay (`RELAY_HOST`, `RELAY_PORT`), a separate organization-signed relay identity (`RELAY_CHAIN_FILE`, `RELAY_KEY_FILE`) and organization trust (`ORGANIZATION_CA_FILE`). Explicitly configure `NEIGHBOR_CLOUDS` and `FEDERATION_PEER_ORGANIZATIONS` for foreign organizations. The default Compose profile is `ah-gt-1`; both peers must support it. `legacy-mtls` is an explicit compatibility choice and must not be used as a silent downgrade. Local inter-cloud service orchestration also requires authorized Gatekeeper/GateTunnel services; their code and processes remain separately selectable.

## Documentation and history

The current SysDD and interface contracts are in `documentation/v5.2.2`. `make docs` rebuilds the SysDD with TeX Live/latexmk and the included assets. The enclosing distribution's `roadmap/5.2/Arrowhead SoS architecture/WhiteBox_5.2.2` contains the updated support GSoSDD.

`legacy/` preserves the previous folder's implementation and instructions as historical material; it is excluded from the image and is not part of this Java build. No live cloud deployment has been replaced by this packaging change.
