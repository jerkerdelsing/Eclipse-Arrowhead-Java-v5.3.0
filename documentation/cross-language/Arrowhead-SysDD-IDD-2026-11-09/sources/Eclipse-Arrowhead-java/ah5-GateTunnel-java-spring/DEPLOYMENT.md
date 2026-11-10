# GateTunnel - independent deployment

This folder owns the complete source, build, application image, database image, configuration and Compose deployment of GateTunnel 5.2.2 (Java-Spring). No sibling folder or shared support-system data is used.

## Build

`mvn -B clean verify` (Java 21, Maven 3.9) or `docker compose build`, which runs the tests and creates `arrowhead-gatetunnel-standalone:5.2.2` and `arrowhead-gatetunnel-standalone-db:5.2.2`. `-o` builds offline from a populated Maven cache.

## Provision

1. Copy `.env.example` to `.env`. Select the existing cloud network, the Service Registry, a unique Compose project name, host port and private database subnet (`DB_SUBNET`).
2. Provision under `security/` (or set the paths in `.env`), readable by container UID 10001:
   - `GateTunnel.p12`: the system identity, alias `GateTunnel.<LOCAL_CLOUD>.<ORGANIZATION>.arrowhead.eu`, profile (dnQualifier) `sy`. The start is refused with any other name or profile, since peers pin that name. `truststore.p12` holds the cloud CA.
   - `crl/ca.crl`: the cloud CA's CRL. Client certificates of the API and of the tunnel listeners, and the certificates of called providers, are checked against it (fail closed).
   - `store-password`, `identity-password` (the system's password in Authentication), `db-password`, `db-root-password`: one value per file, passed as Compose secrets.
   - `db.crt`, `db.key`, `db-ca.crt`: TLS of the private database. The system connects with `sslMode=verify-full`, so `db.crt` must name `gatetunnel-db`.
   - `gatetunnel-relay-chain.crt` (relay certificate + cross-certificate of the cloud CA), `gatetunnel-relay.key`, `org-ca.crt`: the separate relay identity (`RELAY_*`). It is used for the MQTT relay link and, in `ah-gt-1`, for signing the key exchange.
3. Register the identity in Authentication and the authorization and orchestration rules for the callers in README.md. Configure `NEIGHBOR_CLOUDS` and, for other organisations, `FEDERATION_PEER_ORGANIZATIONS`. Both peers must use the same `FEDERATION_PROFILE`; `legacy-mtls` is an explicit compatibility choice, never a fallback.
4. Run `docker compose config --quiet`, then `docker compose up -d --build`.

The configuration is `config/application.properties` (secure profile: mutual TLS with PKCS12 stores, `authentication.policy=outsourced`, `CERT_AUTH`, CRL, sysMonitor over AMQPS, `ddl-auto=validate`). The 5.2.2 environment names stay as placeholders: `LOCAL_CLOUD`, `ORGANIZATION`, `ADVERTISED_ADDRESS`, `NEIGHBOR_CLOUDS`, `FEDERATION_PROFILE`, `FEDERATION_PEER_ORGANIZATIONS`, `RELAY_HOST`, `RELAY_PORT`, `RELAY_CHAIN_CERT`, `RELAY_KEY`, `RELAY_CA_CERT`, `MIN_TUNNEL_PORT`/`MAX_TUNNEL_PORT`, `INTERCLOUD_MIN_PORT`/`INTERCLOUD_MAX_PORT`, `SESSION_MAX_AGE_SECONDS`, `SESSION_IDLE_SECONDS`, `SESSION_CLOCK_SKEW_SECONDS`, `AMQP_HOST`, `AMQP_PORT`, `CRL_FILE`. `SecurityProfileGuard` refuses an insecure configuration unless `arrowhead.security.profile=development` is selected for an isolated test.

## Database

`gatetunnel-db` is MySQL 8.4.11 with TLS required, on an internal network. `deployment/database/init.sql` creates the schema `ah_gatetunnel` (tables `session`, `logs`); `least-privilege.sh` leaves the application account SELECT, INSERT, UPDATE and DELETE only. The schema is validated at start, never changed. Inter-cloud sessions are kept in memory and end with the process. Control sessions closed longer than `SESSION_MAX_AGE_SECONDS` are removed by a scheduled job, which returns their ports to the pool. There is no migration from the 5.2.2 SQLite store, because sessions are ephemeral.

## Ports

8453 is the Arrowhead API, published on loopback by default. The inter-cloud consumer tunnel listeners (8200-8299) are reached by local consumers on the cloud network under `ADVERTISED_ADDRESS`. The control pool (8000-8099) is bookkeeping only.

## Operation

`docker compose ps` shows health (a TCP probe of 8453). `GET /gatetunnel/mgmt/status` (LocalCloudManager, Sysop) shows the relay link and the sessions; sysMonitor reports `degraded` while the relay is not connected. `docker compose down` keeps the database volume; `down -v` erases it. No live cloud was changed by this packaging work; the Spring build has not yet been run in a live federation with the Go and Rust GateTunnels.

The [distribution deployment guide](../DEPLOYMENT-GUIDE.md) covers selecting several systems.
