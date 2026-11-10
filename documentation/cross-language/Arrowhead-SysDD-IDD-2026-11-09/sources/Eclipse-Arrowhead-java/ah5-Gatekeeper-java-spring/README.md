# Gatekeeper 5.2.2 - independent Java-Spring distribution

> Current standalone packaging (November 2026): build and deploy from this folder. See [DEPLOYMENT.md](DEPLOYMENT.md).

This folder is the complete build and deployment unit for Gatekeeper. Gatekeeper is a Java 21 / Spring Boot 3.4 Arrowhead support system with the same layout as the other Spring systems (template: Blacklist):

- `eu.arrowhead.gatekeeper`: `GatekeeperMain`, `GatekeeperSystemInfo`, `GatekeeperConstants` and `GatekeeperDefaults`;
- `init/`, `api/http/`, `service/` (with `validation/`, `normalization/` and `dto/`), `jpa/entity|repository|service`;
- `relay/`: the MQTT relay client;
- `security/`: the caller filters;
- `migrate/`: the SQLite import.

The Arrowhead common library is a private copy under `vendor/` (common-utils and data-transfer-objects, see `vendor/PROVENANCE.json`). The POM inherits only the Spring Boot parent. The native JDK `HttpsServer` + SQLite implementation (`eu.arrowhead.port`) has been removed. A copy of its SysDD remains in `documentation/`, and the Python predecessor is in `legacy/`.

## What the common library now does

| Concern | Native 5.2.2 (removed) | Now |
|---|---|---|
| HTTPS, mutual TLS | `Service`/`Tls`, PEM | Spring Boot TLS: PKCS#12 keystore and truststore, `client-auth=need`, CRL check (`arrowhead.ssl.crl-file`) |
| Identity token bound to the certificate | `Service.caller()` | `OutsourcedFilter` + `TokenCertificateBinding` |
| Login, registration, re-registration | `Service.token()`, `register()` | `OutsourcedLoginJob`, `ApplicationInitListener` |
| Consuming services | `Service.orchestrate()/consume()` | `ArrowheadHttpService` + orchestration collector (ServiceRegistry lookup only for the bootstrap services) |
| sysMonitor (AMQPS) | `Messaging.nativeMonitor` | `SysMonitorPublisher` with a `SysMonitorHealthIndicator` (degraded while the relay link is down) |
| generalManagement | `status` only | `status`, plus the common `get-log` and `get-config` |
| Insecure configuration | checks spread over the code | `SecurityProfileGuard`: refuses to start unless `arrowhead.security.profile=development` |
| Storage | SQLite file | private MySQL schema `ah_gatekeeper` (JPA, `ddl-auto=validate`, DML-only account) |

These parts are Gatekeeper's own and were ported with unchanged behaviour:

- the global service discovery and inter-cloud negotiation protocol (`FederationService`), including the provider-side `authorizationManagement.check-policies` for foreign consumers;
- `PeerNames` and the federation profile check (`legacy-mtls` / `ah-gt-1`, never a silent downgrade);
- the **relay client** (`RelayClient`, Paho MQTT v3, with its own organisation-signed relay identity and CRL check). Its AH-GT-1 / relay topics and message fields stay byte-for-byte compatible with the Go and Rust Gatekeepers: `arrowhead/relay/<cloud>/gatekeeper/in/<sender CN>/<id>`, `correlationId`, `type: reply`, duplicate handling.

## Interfaces (unchanged)

These services are registered with the same definitions, versions, operations, paths, metadata and the `CERT_AUTH` policy:

- `globalServiceDiscovery` 5.2.2
- `interCloudNegotiation` 5.2.2
- `gatekeeperManagement` 5.2.2
- `generalManagement` 1.0.0, under `/gatekeeper/mgmt/general`
- the native `sysMonitor` AMQPS offer

The system listens on port 8449. Every path the native implementation served is still served, with the same JSON shapes and status codes:

| Method | Path | Callers |
|---|---|---|
| POST | `/gatekeeper/init_gsd`, `/gatekeeper/init_multi_gsd`, `/gatekeeper/init_icn` | DynamicServiceOrchestration, SimpleStoreServiceOrchestration, LocalCloudManager, Gatekeeper, Sysop |
| GET | `/gatekeeper/echo`, `/gatekeeper/monitor/echo` | same |
| GET, POST | `/gatekeeper/mgmt/clouds`, `/gatekeeper/mgmt/relays` | LocalCloudManager, Sysop |
| DELETE | `/gatekeeper/mgmt/clouds/{operator}/{name}` | LocalCloudManager, Sysop |
| GET | `/gatekeeper/mgmt/status`, `/gatekeeper/mgmt/general/status` | LocalCloudManager, Sysop |
| GET | `/health`, `/certificate-authority/health` | any trusted client certificate, no token; `{"status","system"}` only |

Only system (`sy`) and operator (`op`) certificates are accepted (403 otherwise). A missing, invalid or foreign identity token gives 401. Only the orchestrators, LocalCloudManager and Sysop may name another `consumerSystem` (403 otherwise). The native implementation never served unprefixed aliases of these paths, and none are added.

Error bodies keep the native `{"error": "..."}` field. Errors raised by the common filters also carry the Arrowhead `ErrorMessageDTO` fields (`errorMessage`, `errorCode`, `exceptionType`, `origin`). `Cache-Control: no-store` and `X-Content-Type-Options: nosniff` are kept.

### Small deviations

- A wrong method on a known path answers 405, not 404.
- A path with a trailing slash is not matched (404).
- The 64-request concurrency limit is replaced by Tomcat's thread pool.
- The plain-HTTP container health listener (port 8480, loopback) is gone. The container health check is a TCP check of 8449. The token-free liveness on the mutual-TLS port remains.
- The timestamps of clouds and relays are stored with millisecond precision. The native store kept seconds as a floating-point number, and `relays[].createdAt` is still returned as seconds.

## Build and test

Run from this folder with Java 21 and Maven 3.9:

```sh
mvn -B clean verify
```

The output is `target/arrowhead-gatekeeper-5.2.2.jar`. `scripts/verify.sh` also checks that the JAR contains Gatekeeper only and no `eu.arrowhead.port` code. `scripts/verify-image.sh` builds the image and checks that it refuses the packaged development configuration.

The tests cover:

- the route contract: every native path and method through the deployed filter chain, with the authorised, unauthenticated, wrong-certificate, refused-profile, no-certificate and unauthorised statuses;
- the six provider-side `check-policies` tests and the federation protocol steps;
- management validation and views;
- `PeerNames` and the federation settings;
- the refusal of insecure settings.

Two integration tests run only when their properties are given:

- `MySqlEndToEndTest`: the whole application against a fresh `deployment/database` MySQL container, plus the SQLite import. Run it with `-Dgk.it.db.url=... -Dgk.it.db.password=...`.
- `RelayRoundTripIT`: two relay clients and an observer against a TLS Mosquitto with `require_certificate`. Run it with `-Dtest=RelayRoundTripIT -Dgk.it.relay.host=... -Dgk.it.relay.port=... -Dgk.it.relay.dir=...`, where the directory holds `ca.crt`, `a.crt`, `a.key`, `b.crt` and `b.key`.

## Deploy

See [DEPLOYMENT.md](DEPLOYMENT.md). In short:

1. Copy `.env.example` to `.env`.
2. Provision the following under `security/`:
   - the PKCS#12 system identity `Gatekeeper.p12` (alias `Gatekeeper.<LOCAL_CLOUD>.<ORGANIZATION>.arrowhead.eu`, DNQ `sy`) and `truststore.p12`;
   - the passwords;
   - the database TLS material;
   - the CRL;
   - the separate relay identity: `gatekeeper-relay-chain.crt`, `gatekeeper-relay.key` and `org-ca.crt`.
3. Run `docker compose up -d --build`.

Compose starts Gatekeeper and its private `gatekeeper-db` (MySQL 8.4.11, TLS required, internal network, DML-only account).

### Importing an old SQLite store

`migrate-sqlite <file>` imports the clouds and relays of a native `gatekeeper.db` into the MySQL schema. It keeps rows that already exist, keeps the relays' creation times, and exits. It starts no HTTP listener and registers nothing. It disables the relay and sysMonitor, and with TLS it does not log in to Authentication. See DEPLOYMENT.md.

## Arrowhead dependencies and provisioning

- ServiceRegistry is the bootstrap address. Authentication and the orchestrator are found there.
- ConsumerAuthorization (`authorizationManagement.check-policies`) and GateTunnel (`gateTunnelConnection`) are consumed through orchestration on behalf of Gatekeeper.
- Gatekeeper looks up local providers in the ServiceRegistry, so it must be in the registry property `service.discovery.direct.access`.

Provision the following in the provider cloud, or discovery returns no results:

- a ConsumerAuthorization grant for Gatekeeper on `authorizationManagement`, scope `check-policies`;
- grants for Gatekeeper on `gateTunnelConnection`.

The provider-side check carries the foreign cloud (`<cloud>|<organization>`), and every requested operation must be granted. An unreachable ConsumerAuthorization, a refusal or an incomplete answer denies the request, and the reason goes to the log.

Federation needs the following:

- the MQTT relay (`RELAY_HOST`, `RELAY_PORT`) and the organisation-signed relay identity;
- explicit `FEDERATION_PEER_ORGANIZATIONS` for foreign organisations;
- configured neighbour clouds (`/gatekeeper/mgmt/clouds`).

The default profile is `ah-gt-1`, which both peers must support.

## Documentation

The SysDD and interface contracts are in `documentation/v5.2.2`. `make docs` rebuilds the SysDD.

## Listening port

Default **8449** (`server.port` in the packaged `application.properties`). Override with `server.port` in `config/application.properties`, `SERVER_PORT=8449` or `--server.port=8449`.

See [LISTENING-PORTS.md](../LISTENING-PORTS.md) for precedence and all systems.
