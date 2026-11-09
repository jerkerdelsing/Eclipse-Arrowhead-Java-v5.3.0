# Configuration 5.2.2

> Current standalone packaging (25 October 2026): build and deploy from this folder. See [DEPLOYMENT.md](DEPLOYMENT.md). Any combined-tree build or live-cloud examples below are historical integration context.

Configuration is an Eclipse Arrowhead 5.2.2 support system for centralised configuration management: an operator stores one configuration file per system, and each system fetches its own file at run time. It is the v5.2.2 successor to the v4.6 `core-java-spring/configuration` core system, and it publishes the Arrowhead 5.2.2 `sysMonitor` service natively (`echo` is `degraded` while the configuration store cannot be read).

## Services

| Service | Version | Operations (method and path) |
| --- | --- | --- |
| `configuration` | 1.0.0 | `get-configuration`: `GET /configuration/config/{systemName}`; `get-raw-configuration`: `GET /configuration/config/raw/{systemName}` |
| `configurationProxy` | 1.0.0 | `get-configuration`: `GET /configuration/proxy/config/{systemName}`; `store-configuration`: `PUT /configuration/proxy/save-config` |
| `configurationManagement` | 1.0.0 | `list-configurations`: `GET /configuration/mgmt/config`; `store-configuration`: `PUT /configuration/mgmt/config/{systemName}`; `remove-configuration`: `DELETE /configuration/mgmt/config/{systemName}` |
| `generalManagement` | 1.0.0 | `get-log`: `POST /configuration/general/mgmt/logs`; `get-config`: `GET /configuration/general/mgmt/get-config` |
| `sysMonitor` | 1.0.0 | `echo`, `power`, `status` on `<localCloud>.sysmonitor.Configuration.Configuration.<type>` (exchange `arrowhead`) (`generic_amqps`, `CERT_AUTH`, `publisherMode=native`) |

The URLs are those of v4.6. A configuration is exchanged as:

```json
{"systemName":"Thermostat","fileName":"thermostat.json","contentType":"application/json",
 "data":"eyJzZXRwb2ludCI6MjEsInVuaXQiOiJDIn0="}
```

Responses add `id`, `size`, `createdAt` and `updatedAt`. `get-raw-configuration` returns the file itself with its `Content-Type` and `Content-Disposition: attachment; filename="..."`.

## Access rules

| Service | Who may call it |
| --- | --- |
| `configuration` | Any authenticated system, for its own configuration only (path name = authenticated system, bound to its X.509 certificate with TLS) |
| `configurationProxy` | Sysop, and with `service.authorization.enabled=true` systems granted `configurationProxy` (scope = operation) by ConsumerAuthorization |
| `configurationManagement`, `generalManagement` | The common management filter (`management.policy`) |

## Configuration

| Property | Default | Meaning |
| --- | --- | --- |
| `server.port` | `8459` | HTTP(S) port (as in v4.6) |
| `configuration.max.size` | `1048576` | Maximum decoded file size in bytes |
| `service.authorization.enabled` | `false` | ConsumerAuthorization check of `configurationProxy` |
| `management.policy` | `authorization` | Common management filter policy |
| `sysmonitor.enabled`, `sysmonitor.broker.*`, `sysmonitor.ssl.*` | `true`, `amqp-broker:5671`, `server.ssl` stores | Native `sysMonitor` publisher of the common library |

When TLS requires client certificates (`server.ssl.client-auth=need`), the HTTP offers are re-registered with interface policy `CERT_AUTH`.

## Database

Configuration owns the `ah_configuration` schema (`configuration_data` with one row per system, and `logs`). Create it with `src/main/resources/database/create_empty_db.sql` and `grant_privileges.sql`; Hibernate validates the schema at start-up.

## Build

Install `ah5-common-java-spring` and the `arrowhead-core` 5.2.2 parent pom (from `ah5-core-java-spring`) into the local Maven repository, then run from this directory:

```sh
mvn clean test
```

`docker/Dockerfile-Configuration` builds an image from the workspace root (it needs `ah5-common-java-spring`, `ah5-core-java-spring/pom.xml` and this folder).

## Test-cloud deployment

Both 5.2.2 test clouds (`Arrowhead-test-cloud`, `Arrowhead-test-cloud-go`) deploy Configuration as service `configuration` with its own `configuration-db` (`make up`, or `make up-configuration` on a running cloud). It runs with mTLS (`server.ssl.client-auth=need`), outsourced authentication with Authentication identity tokens and `management.policy=sysop-only`, and registers through the bootstrap-proxy listener on port 8512. When TLS requires a client certificate, the init listener re-registers the HTTP offers with interface policy `CERT_AUTH`. The `configuration`, `configurationProxy` and `sysMonitor` grants are set by the cloud's `scripts/configure-configuration-policy.sh`.

## Migration from v4.6

- Core system `CONFIGURATION` becomes support system `Configuration`; services `conf`/`confraw` become `configuration` (`get-configuration`, `get-raw-configuration`), `conf-by-proxy`/`save-conf-by-proxy` become `configurationProxy`, and the management endpoints become `configurationManagement`. The URLs are unchanged.
- The proxy operations were open to every certified system in v4.6; they now need a ConsumerAuthorization grant or sysop.
- `GET /configuration/echo` is removed; liveness is published through `sysMonitor`.
- System names follow the Arrowhead 5 rules and are case-sensitive (v4.6 lower-cased them).
- Fixed: v4.6 never returned `fileName` and sent raw files with `filename=null`.
- Content is validated (Base64, media type, plain file name, size limit) and stored as bytes in Configuration's own schema instead of Base64 text in the shared v4 database.

## Documentation

[SysD, SysDD and an SD and IDD for each produced service](documentation/v5.2.2/README.md) (`configuration`, `configurationProxy`, `configurationManagement`, `generalManagement`, `sysMonitor`), generated from the implementation with the official Arrowhead LaTeX templates by [`documentation-tools`](../documentation-tools/README.md); the PDFs are beside the sources. The earlier `documentation/v5.2` set is in `../../_superseded/2026-10-22/`.

## Listening port

Default **8459** (`server.port` in the packaged `application.properties`). Override with `server.port` in `config/application.properties`, `SERVER_PORT=8459` or `--server.port=8459`. Note: this folder's deployment package (`config/application.properties`, Dockerfile and compose) uses 8450.

See [LISTENING-PORTS.md](../LISTENING-PORTS.md) for precedence and all systems.
