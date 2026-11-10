# Gatekeeper - independent deployment

This folder owns the complete source, build, executable JAR, application image, database image, configuration and Compose deployment for Gatekeeper 5.2.2 (Java-Spring). No sibling source folder, shared runtime or shared support-system data is used. The Arrowhead common library is compiled from the private copy in `vendor/`.

## Build

With Java 21 and Maven 3.9, run `mvn -B clean verify` here. `scripts/verify.sh` also checks the artifact contents. The first build needs Maven Central; `-o` works with a populated cache. The output is `target/arrowhead-gatekeeper-5.2.2.jar`. `docker compose build` builds both images from this folder and runs the tests:

- `arrowhead-gatekeeper-standalone:5.2.2`
- `arrowhead-gatekeeper-standalone-db:5.2.2`

## Provision and select this system

1. Copy `.env.example` to `.env`. Select an existing cloud network and the ServiceRegistry address. Use a unique Compose project name, host port and `DB_SUBNET` for each deployment on one Docker host (default `10.252.212.0/24`). The project does not create or start core systems or other support systems.
2. Register Gatekeeper's identity and password in Authentication, and provision its authorizations:
   - `authorizationManagement.check-policies` at ConsumerAuthorization;
   - `gateTunnelConnection`;
   - `service.discovery.direct.access` in the ServiceRegistry.
3. Provision these files under `security/`, or set their paths in `.env`. They must be readable by UID 10001.

| File | Content |
|---|---|
| `Gatekeeper.p12` | system identity, alias `Gatekeeper.<LOCAL_CLOUD>.<ORGANIZATION>.arrowhead.eu`, DNQ `sy` |
| `truststore.p12` | cloud trust |
| `store-password` | password of both PKCS#12 files |
| `identity-password` | Authentication password of Gatekeeper |
| `crl/ca.crl` | CRL of the cloud CA (checked on every handshake, fail closed) |
| `db-password`, `db-root-password` | MySQL application and root passwords |
| `db-ca.crt`, `db.crt`, `db.key` | MySQL server TLS (`sslMode=verify-full` from Gatekeeper) |
| `gatekeeper-relay-chain.crt`, `gatekeeper-relay.key` | organisation-signed relay identity (PEM), separate from the system certificate |
| `org-ca.crt` | organisation trust for the relay broker |
| `org-ca.crl` | CRL of the organisation CA: the relay's server certificate is checked against it, fail closed (renew before its nextUpdate) |

4. Run `docker compose config --quiet`, then `docker compose up -d --build`.

What Compose starts:
- `gatekeeper-db`: MySQL 8.4.11, secure transport required, on the internal `database` network only. Its first start creates the `ah_gatekeeper` schema (`deployment/database/init.sql`). `least-privilege.sh` then restricts the `gatekeeper` account to `SELECT, INSERT, UPDATE, DELETE`.
- `gatekeeper`: starts with `ddl-auto=validate` once the database is healthy.

The container runs unprivileged (UID 10001), with a read-only root, no capabilities, and tmpfs `/tmp` and `/app/logs`.

The configuration is `config/application.properties`. Secrets are read through `configtree:/run/secrets/`. Environment names are unchanged from the native packaging:
- `LOCAL_CLOUD`, `ORGANIZATION`
- `SERVICE_REGISTRY_ADDRESS`, `SERVICE_REGISTRY_PORT`
- `AMQP_HOST`, `AMQP_PORT`, `SYSMONITOR_ENABLED`
- `CRL_FILE`
- `FEDERATION_PROFILE`, `FEDERATION_PEER_ORGANIZATIONS`
- `RELAY_HOST`, `RELAY_PORT`, `RELAY_CHAIN_CERT`, `RELAY_KEY`, `RELAY_CA_CERT`

`SERVICE_REGISTRY_URL`, `AUTHENTICATION_PASSWORD_FILE`, `DB_PATH`, `HEALTH_PORT` and the PEM `TLS_*` names of the native packaging are no longer used. Provision the PKCS#12 files instead.

The system refuses to start when the configuration is insecure (TLS off, declared identities, or service authorization off). `arrowhead.security.profile=development` is an explicit exception for isolated tests only.

## Importing a native SQLite store

The new packaging has not been deployed anywhere yet, so no live data needs moving. To carry over the clouds and relays of an old `gatekeeper.db` once, run:

```sh
docker compose run --rm -v /path/to/old:/old:ro gatekeeper migrate-sqlite /old/gatekeeper.db --spring.config.additional-location=file:/app/config/application.properties
```

The mode has these properties:
- It reads the file read-only and checks every row as a management request would.
- It keeps rows that already exist in MySQL and keeps the relays' creation times.
- It prints a summary and exits.
- It starts no HTTP listener and registers nothing in the ServiceRegistry.
- It disables the relay and sysMonitor, and with TLS uses the certificate policy, so no Authentication login is needed.

## Operational dependencies and verification

The HTTPS API uses port 8449, published on loopback by default. The container health check is a TCP check of 8449; the native plain-HTTP health port 8480 is gone. `GET /health` on the mutual-TLS port answers token-free with the status only. `/gatekeeper/mgmt/status` gives the detail (registration, relay link) to LocalCloudManager and Sysop.

Normal Arrowhead protocol dependencies remain:
- ServiceRegistry and Authentication are needed for registration and identity.
- DynamicServiceOrchestration, ConsumerAuthorization and GateTunnel are needed for federation.
- The AMQPS broker is needed for sysMonitor, and the relay broker for inter-cloud traffic.

Without a relay connection, Gatekeeper serves but federation requests fail, and the sysMonitor echo reports degraded.

Verification of this packaging (November 2026):
- `mvn -o clean verify`: route contract, provider-side policy checks, federation protocol, management, peer names, and the insecure-start refusal.
- The application against a fresh database container: the schema validates under the DML-only grants, plus a `migrate-sqlite` run of the JAR.
- A relay round trip against a TLS Mosquitto container.

A live federation run against the Go and Rust Gatekeepers has not been repeated for this packaging.

`docker compose down` stops only this project and keeps the database volume. Back up the database before upgrades. `down -v` erases this project's private state.

The SysDD is in `documentation/v5.2.2`. The [distribution deployment guide](../DEPLOYMENT-GUIDE.md) covers selecting multiple systems.
