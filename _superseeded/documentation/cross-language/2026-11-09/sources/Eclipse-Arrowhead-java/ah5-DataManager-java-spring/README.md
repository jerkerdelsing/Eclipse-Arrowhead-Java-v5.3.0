# DataManager 5.2

> Current standalone packaging (25 October 2026): build and deploy from this folder. See [DEPLOYMENT.md](DEPLOYMENT.md). Any combined-tree build or live-cloud examples below are historical integration context.

DataManager is an Arrowhead 5 support system that caches and persists SenML sensor and actuator data. This implementation is a port of the v4.6 DataManager to the Arrowhead 5.2.2 Java and Spring baseline.

## Arrowhead names

- System: `DataManager`
- Services: `dataProxy`, `dataHistorian`, and `sysMonitor`
- Interfaces: `generic_https` (or `generic_http`) for `dataProxy` and `dataHistorian`; `generic_amqps` (or `generic_amqp`) for `sysMonitor`
- Service versions: `1.0.0`
- Operations: `list-systems`, `list-services`, `get-data`, and `put-data` for the data services; `echo`, `power` and `status` for `sysMonitor`

The API normalizes and validates system names as PascalCase and service names as camelCase. SenML's abbreviated JSON field names are preserved because RFC 8428 is the governing external standard for that payload.

## HTTP API

Both services expose the same resource layout:

| Operation | Method | Proxy path | Historian path |
| --- | --- | --- | --- |
| `list-systems` | GET | `/datamanager/data-proxy/systems` | `/datamanager/data-historian/systems` |
| `list-services` | GET | `/datamanager/data-proxy/systems/{systemName}/services` | `/datamanager/data-historian/systems/{systemName}/services` |
| `get-data` | GET | `/datamanager/data-proxy/systems/{systemName}/services/{serviceName}/data` | `/datamanager/data-historian/systems/{systemName}/services/{serviceName}/data` |
| `put-data` | PUT | `/datamanager/data-proxy/systems/{systemName}/services/{serviceName}/data` | `/datamanager/data-historian/systems/{systemName}/services/{serviceName}/data` |

Historian queries accept `from`, `to`, and `count`. Optional repeated `signals` parameters select SenML signal names, with `signalCount` limiting the number returned for each signal.

## sysMonitor

The v4.6 HTTP `echo` service is replaced by the Arrowhead 5.2.2 `sysMonitor` service (contract `SysMonitor-AMQP-IDD-5.2.2`). DataManager does not offer `/datamanager/echo`. The common-library `SysMonitorPublisher` publishes `echo` (status `alive`, or `degraded` when the historian database cannot be queried; see `DataManagerHealthIndicator`), `power` and `status` observations every 30 seconds to:

```
exchange arrowhead, routing key <localCloud>.sysmonitor.DataManager.<serviceInstance>.{echo,power,status}
```

Publications go to the local-cloud AMQP broker; they are persistent and broker-confirmed and use mutual TLS with the `sysmonitor.ssl.*` stores (default: the `server.ssl` stores). The offer is registered with `generic_amqps`, `CERT_AUTH` and `publisherMode=native`. SysMonitor stores these observations, like those of every other microsystem, in its own history (`sysMonitorHistory`); DataManager's `dataProxy` and `dataHistorian` hold application data only and no `sysMonitor` data. Configure the publisher in `application.properties`:

| Property | Default | Meaning |
| --- | --- | --- |
| `sysmonitor.enabled` | `true` | Register and publish `sysMonitor` |
| `sysmonitor.broker.address` | `amqp-broker` | Broker host |
| `sysmonitor.exchange` | `arrowhead` | AMQP topic exchange |
| `sysmonitor.broker.port` | `5671` | Broker TLS port (AMQPS) |
| `sysmonitor.local.cloud` | `TestCloud` | Local-cloud topic segment |
| `sysmonitor.service.instance` | `DataManager` | Service-instance topic segment |
| `sysmonitor.publish.interval` | `30` | Seconds between publications |

The broker ACL must allow DataManager to write below its base topic.

## Service authorization

With `service.authorization.enabled=true` (and SSL), every `dataProxy` and `dataHistorian` call must come from a client certificate whose system name matches the authenticated system, and ConsumerAuthorization must permit the operation (`authorization.verify`, cached for 30 seconds, failing closed). The offers are then re-registered with `CERT_AUTH` interfaces. The shipped default is `false` so that a standalone development instance runs without ConsumerAuthorization.

Both Arrowhead 5.2.2 test clouds (`../Arrowhead-test-cloud` and `../Arrowhead-test-cloud-go`) deploy this DataManager with `authentication.policy=outsourced`, mutual TLS and service authorization enabled.

## Database

DataManager owns the `ah_datamanager` schema. Initialize MySQL from `src/main/resources/database/create_empty_db.sql` before starting the application. This separation is intentional in Arrowhead 5; the system no longer writes to the shared v4 database.

## Build

Install `ah5-common-java-spring` and the `arrowhead-core` 5.2.2 parent pom (from `ah5-core-java-spring`) into the local Maven repository, then run from this directory:

```sh
mvn clean test
```

## Specifications

[SysD, SysDD and an SD and IDD for each produced service](documentation/v5.2.2/README.md) (`dataProxy`, `dataHistorian`, `sysMonitor`), generated from the implementation with the official Arrowhead LaTeX templates by [`documentation-tools`](../documentation-tools/README.md); the PDFs are beside the sources. The earlier `documentation/v5.2` set is in `../../_superseded/2026-10-22/`.

## Listening port

Default **8461** (`server.port` in the packaged `application.properties`). Override with `server.port` in `config/application.properties`, `SERVER_PORT=8461` or `--server.port=8461`.

See [LISTENING-PORTS.md](../LISTENING-PORTS.md) for precedence and all systems.
