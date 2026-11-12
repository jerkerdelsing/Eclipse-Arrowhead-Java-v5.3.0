# GateTunnel 5.2.2 - independent Java-Spring distribution

> Spring port (8 November 2026): GateTunnel now follows the Arrowhead Java-Spring system pattern (as Blacklist and EventHandler). The native JDK `HttpsServer` + SQLite implementation (package `eu.arrowhead.port`) has been removed. Wire contracts are unchanged. See [DEPLOYMENT.md](DEPLOYMENT.md).

This folder is the complete build and deployment unit for GateTunnel: Java 21, Spring Boot 3.4, Maven, a private MySQL schema `ah_gatetunnel` through JPA, and the Arrowhead common library as a private vendored source copy under `vendor/` (see `vendor/PROVENANCE.json`; the same snapshot as the other vendored systems). The POM inherits only the Spring Boot parent. Nothing is loaded from a sibling folder at build or run time.

## What GateTunnel does

GateTunnel carries an inter-cloud service session between two local clouds over the organisation's MQTT relay.

- **Inter-cloud sessions** (`gateTunnelConnection` operations `open-provider-session` and `open-consumer-session`, called only by the local Gatekeeper after a successful inter-cloud negotiation). On the consumer side it opens a mutual-TLS tunnel listener for the negotiated consumer in the pool 8200-8299. On the provider side it executes relayed requests against the negotiated provider, within the negotiated operations only.
- **AH-GT-1** (`FEDERATION_PROFILE=ah-gt-1`, default in Compose): the request and reply are sealed end to end (X25519, HKDF-SHA256, AES-256-GCM; key exchange signed with the relay identity and verified against the organisation CA). The relay sees ciphertext only. `legacy-mtls` keeps the earlier relay-visible JSON protocol. There is no fallback between the profiles: a peer on the other profile, or an AH-GT-1 session offered to a legacy GateTunnel, is refused.
- **4.x-compatible control sessions** (`connect-provider`, `connect-consumer`, `close-sessions`): bookkeeping of the control port pool 8000-8099 in the database.
- **Management** (`gateTunnelManagement`): session list, close, relay and session status. **Key** (`gateTunnelKey`): the public key of the system certificate.

AH-GT-1 framing, encryption, relay topics (`arrowhead/relay/<cloud>/gatetunnel/in/<sender CN>/<id>`), `PeerNames` and `SessionSecurity` are ported verbatim in behaviour and stay byte-for-byte compatible with the Go and Rust GateTunnels; the reference vector test (`src/test/resources/ahgt1`) proves it.

## Layout

`eu.arrowhead.gatetunnel`: `GateTunnelMain`, `GateTunnelSystemInfo` (registered services), `GateTunnelConstants`, `GateTunnelDefaults`, `init/` (startup checks), `api/http/` (controllers), `security/` (caller rules, TLS outside the web server, identity tokens), `service/` (inter-cloud sessions, `SessionSecurity`, relay client, provider client, control sessions, scheduled jobs, `validation/`, `normalization/`, `dto/`), `jpa/` (`Session`, `Logs`).

From the common library: service registration (`ApplicationInitListener`), the authentication filter (`OutsourcedFilter`: identity token verified by Authentication and bound to the client certificate), the management filter, `ArrowheadHttpService` with orchestration, the login job, `SysMonitorPublisher` (sysMonitor over AMQPS), `generalManagement` log/config, the CRL check of the web server, and `SecurityProfileGuard`.

## Served paths

Each operation is served under `/gatetunnel`, under the 4.x name `/gateway`, and without a base path:

| Method | Path | Callers | Success |
|---|---|---|---|
| POST | `connect_provider`, `connect_consumer` | Gatekeeper, DynamicServiceOrchestration, SimpleStoreServiceOrchestration, LocalCloudManager, Sysop | 201 |
| POST | `close_sessions` | same | 200 |
| POST | `intercloud/provider`, `intercloud/consumer` | Gatekeeper | 201 |
| GET | `publickey`, `echo`, `monitor/echo` | Gatekeeper, DSO, SimpleStoreServiceOrchestration, LocalCloudManager, Sysop | 200 |
| GET | `mgmt/sessions`, `mgmt/status`; POST `mgmt/sessions/close` | LocalCloudManager, Sysop | 200 (close: 404 if nothing was open) |
| GET | `/gatetunnel/mgmt/general/status`; POST `.../logs`; GET `.../get-config` | LocalCloudManager, Sysop | 200 |
| GET | `/health` (liveness only) | any authenticated system | 200 |

Success bodies are unchanged. Errors keep their 5.2.2 status codes (400, 401, 403, 404, 413, 502, 503) and now use the Arrowhead error body (`errorMessage`, `errorCode`, `exceptionType`, `origin`). Changes against 5.2.2: `/health` now needs an identity token like every other path (mutual TLS was already required); the loopback plain-HTTP health port 8481 is gone (the container health check is a TCP probe); a wrong HTTP method is 405 instead of 404. The unprefixed aliases are new: the native implementation served only `/gatetunnel` and `/gateway`.

## Build and test

```sh
mvn -B clean verify          # or: sh scripts/verify.sh (also checks the JAR contents)
```

The output is `target/arrowhead-gatetunnel-5.3.0.jar` (Spring Boot JAR). Tests: AH-GT-1 reference vector and key exchange, peer names, operation scope, TLS pinning and token renewal, the refused insecure start, and a MockMvc contract test of every served path and method through the real authentication filter (unauthenticated, certificate of another system, non-system certificate, unauthorised system, authorised caller). Two integration tests run only when their containers are given:

- `it.MySqlEndToEndTest`: `-Dgt.it.db.url=jdbc:mariadb://<host>:3306/ah_gatetunnel?... -Dgt.it.db.password=...` against a database container built from `deployment/database/Dockerfile` (schema validated, DML-only grants).
- `it.RelayRoundTripTest`: `-Dgt.it.relay.host=<broker> -Dgt.it.relay.ca=<org CA + broker CA bundle>` against an MQTT broker with mutual TLS that trusts the organisation CA in `src/test/resources/ahgt1`.

## Deploy

See [DEPLOYMENT.md](DEPLOYMENT.md). `legacy/` preserves an earlier implementation as historical material; it is not built or deployed.

## Documentation

The SysDD and interface contracts are in `documentation/v5.2.2` (`make docs`).

## Listening port

Default **8453** (`server.port` in the packaged `application.properties`). Override with `server.port` in `config/application.properties`, `SERVER_PORT=8453` or `--server.port=8453`.

See [LISTENING-PORTS.md](../LISTENING-PORTS.md) for precedence and all systems.
