# SysMonitor 5.2.2 (Java-Spring)

> Since 8 November 2026 this system follows the Arrowhead Java-Spring pattern, like the other support systems. The previous native implementation (JDK HttpsServer and SQLite, package `eu.arrowhead.port`) is no longer built; older material is kept in `legacy/` and in the history of this folder. See [DEPLOYMENT.md](DEPLOYMENT.md) for deployment.

SysMonitor collects the `sysMonitor` observations (echo, power, status, ...) that the systems of one local cloud publish on the AMQP broker. It validates and stores them as a retention-bounded history, republishes them on the MQTT v5 output `sysMonitorData`, and offers the history and the management of its subscriptions over HTTPS.

## Structure

This folder is a complete, independent build and deployment unit with the structure of `ah5-Blacklist-java-spring` and `ah5-CertificateAuthority-java-spring`:

| Part | Where |
|---|---|
| Spring Boot application | `eu.arrowhead.sysmonitor.SysMonitorMain` |
| System and service models | `SysMonitorSystemInfo` |
| ServiceRegistry registration, CERT_AUTH interfaces, own sysMonitor observations | common `ApplicationInitListener` and `SysMonitorPublisher` |
| Authentication (identity token bound to the client certificate) | common `OutsourcedFilter` |
| Caller profile (`sy`, `op`) and access rules | `api/http/filter/CallerProfileFilter`, `SysMonitorAccessFilter` |
| HTTP APIs | `api/http/HistoryAPI`, `ManagementAPI`, `GeneralManagementAPI`, `HealthAPI` |
| AMQP intake, MQTT v5 output, orchestration, scheduled jobs | `service/AmqpIntake`, `MqttOutput`, `MonitoringService`, `SysMonitorJobs` |
| Storage | JPA entities `Subscription`, `AmqpBinding`, `Outbox`, `Observation`, `Audit`, `Logs` in the private MySQL schema `ah_sysmonitor` |
| Arrowhead common library | private source copy under `vendor/` (see `vendor/PROVENANCE.json`) |

## Services

Service definitions, operations, HTTP paths, AMQP routing keys, the observation format and the MQTT topics and payloads are unchanged from 5.2.2 (and compatible with the Go and Rust SysMonitor). The HTTPS port is 8475.

| Service | Operation | Method and path | Who may call |
|---|---|---|---|
| `sysMonitorHistory` | `list-systems` | GET `/sysmonitor/history/systems` | systems authorized by ConsumerAuthorization |
| | `latest` | GET `/sysmonitor/history/systems/{systemName}/latest` | as above |
| | `query` | GET `/sysmonitor/history/systems/{systemName}/observations?count&type&from&to` | as above |
| `sysMonitorManagement` | `status`, `providers`, `subscriptions` | GET `/sysmonitor/management/status`, `/providers`, `/subscriptions` | LocalCloudManager, Sysop |
| | `subscribe` / `unsubscribe` | PUT / DELETE `/sysmonitor/management/subscriptions/{providerSystem}` (`If-Match` revision, `ETag`) | LocalCloudManager, Sysop |
| | `reconcile` | POST `/sysmonitor/management/reconcile` (202) | LocalCloudManager, Sysop |
| `generalManagement` | `status`, `get-log`, `get-config` | `/sysmonitor/mgmt/general/status`, POST `/logs`, GET `/config` | Sysop, LocalCloudManager |
| — | liveness | GET `/health` (200 running, 503 degraded) | any authenticated system |
| `sysMonitorData` | subscribe (MQTT v5, QoS 1) | `arrowhead/<cloud>/sysmonitor/data/<observationType>/<providerSystem>` | broker ACL |
| `sysMonitor` (consumed) | AMQP intake | queue `SysMonitor.observations` bound on exchange `arrowhead` to `<cloud>.sysmonitor.<provider>.<instance>.<type>` | orchestrated providers |
| `sysMonitor` (own) | echo, power, status | routing key `<cloud>.sysmonitor.SysMonitor.SysMonitor.<type>` | — |

The monitored providers are orchestrated every `ORCHESTRATION_INTERVAL_SECONDS` (`sysMonitor`, `generic_amqps`, `CERT_AUTH`). Providers without a subscription follow `SYSMONITOR_DEFAULT_SUBSCRIPTION` (`all` or `none`). Observations are stored before they are acknowledged, deduplicated by `observationId`, published through an outbox (at least once) and pruned after `SYSMONITOR_RETENTION_DAYS` (every `SYSMONITOR_PRUNE_INTERVAL_SECONDS`).

### Differences from the native implementation

- Every path, including `/health`, now needs an identity token bound to the client certificate, as in all Spring systems.
- Error responses use the Arrowhead `ErrorMessageDTO` (`errorMessage`, `errorCode`, `exceptionType`, `origin`) instead of `{"error": ...}`. Status codes are unchanged; invalid `from`/`to` values now return 400 instead of 500, and an observation with an unparsable `sourceTime` is rejected instead of being requeued.
- `generalManagement` adds `get-log` and `get-config` next to `status`.
- The plain-HTTP health listener (port 8482) and the stray `/certificate-authority/health` alias of the shared native base class are gone. The container health check is a TCP check of port 8475.
- The system identity is a PKCS#12 keystore, like the other Spring systems. Broker certificates are still checked against the cloud CRL.
- The own sysMonitor observations come from the common-library publisher (same routing key; its `status` value carries the common fields).
- The SQLite history is not migrated; it is retention-bounded.

## Build and test

```sh
mvn -B clean verify
```

`scripts/verify.sh` also checks the contents of the JAR, and `scripts/verify-image.sh` builds the image and checks that it runs with a read-only root and refuses an insecure configuration.

The tests cover the observation format, the store, orchestration and binding selection, the intake acknowledgement rules, the ConsumerAuthorization cache, the secure-profile guard, and every 5.2.2 path and method behind the deployed filter chain (unauthenticated, wrong certificate, authorised). Two opt-in tests are skipped unless their system properties are given:

- `MySqlEndToEndTest` (`-Dsysmonitor.it.db.url=... -Dsysmonitor.it.db.password=...`): the whole application against a MySQL schema built from `deployment/database`, including the DML-only grants.
- `BrokerRoundTripTest` (additionally `-Dsysmonitor.it.broker.dir=<PKCS12 folder> -Dsysmonitor.it.amqp.host=... -Dsysmonitor.it.mqtt.host=...`): AMQP → store → MQTT round trip against RabbitMQ and Mosquitto with mutual TLS.

## Listening port

Default **8475** (`server.port` in the packaged `application.properties`). Override with `server.port` in `config/application.properties`, `SERVER_PORT=8475` or `--server.port=8475`.

See [LISTENING-PORTS.md](../LISTENING-PORTS.md) for precedence and all systems.
