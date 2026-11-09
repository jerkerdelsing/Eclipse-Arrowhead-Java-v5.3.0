# PlantDescriptionEngine 5.2.2

> Current standalone packaging (25 October 2026): build and deploy from this folder. See [DEPLOYMENT.md](DEPLOYMENT.md). Any combined-tree build or live-cloud examples below are historical integration context.

PlantDescriptionEngine (PDE) is an Eclipse Arrowhead 5.2.2 support system. It keeps the local cloud's orchestration consistent with an engineered *plant description*: the systems of the plant, the services each one consumes or produces (ports), and the connections between consumer and producer ports. It succeeds the v4.6 `core-java-spring/plantdescriptionengine`.

One plant description is active at a time, together with the descriptions it includes. The PDE:

- turns the active description's connections into an atomic DynamicServiceOrchestration (DSO) plant-rule snapshot;
- reconciles that snapshot every `plant.sync.interval` and immediately after each change;
- raises an alarm whenever the running cloud differs from the description.

Liveness comes from the observations SysMonitor stores (`sysMonitorHistory`). This replaces the v4.6 ping of `monitorable` services. The PDE also publishes the 5.2.2 `sysMonitor` service natively.

## Services

| Service | Version | Operations (HTTP) |
| --- | --- | --- |
| `plantDescriptionManagement` | 1.0.0 | `query` `GET`, `get` `GET {id}`, `create` `POST`, `replace` `PUT {id}`, `update` `PATCH {id}`, `remove` `DELETE {id}` on `/plantdescriptionengine/mgmt/plant-descriptions` |
| `plantDescriptionMonitor` | 1.0.0 | `query-plant-descriptions`, `get-plant-description` on `/plantdescriptionengine/monitor/plant-descriptions[/{id}]`; `query-alarms`, `get-alarm`, `acknowledge-alarm` (`PATCH`) on `/plantdescriptionengine/monitor/alarms[/{id}]` |
| `generalManagement` | 1.0.0 | `get-log`, `get-config` on `/plantdescriptionengine/general/mgmt` |
| `sysMonitor` | 1.0.0 | `echo`, `power`, `status` over AMQP (`publisherMode=native`); `echo` is `degraded` while synchronization fails or alarms are active |

A plant description:

```json
{"plantDescription":"Line 1","active":true,"include":[1],
 "systems":[
  {"systemId":"ctrl","systemName":"LineController",
   "ports":[{"portName":"temp","serviceDefinition":"temperature","consumer":true}]},
  {"systemId":"sensor","systemName":"TemperatureSensor",
   "ports":[{"portName":"out","serviceDefinition":"temperature","consumer":false}]}],
 "connections":[{"consumer":{"systemId":"ctrl","portName":"temp"},
                 "producer":{"systemId":"sensor","portName":"out"},"priority":1}]}
```

The v4.6 validation rules apply: includes, unique and identifiable systems, ports, and connections. Service definitions must be camelCase and system names PascalCase.

A plant system matches a registered system when the names are equal (if a name is given) and the plant system's metadata is a subset of the registered system's metadata. A provider is selected only when both systems match exactly one registered system. Named consumers (including temporarily unregistered consumers) and metadata-matched consumers remain managed even when the producer is unavailable or ambiguous: their scope has an empty provider list until a match is available. In that case there is one rule per producer service instance with the port's service definition, port metadata and interface. Colliding priorities for the same consumer and service definition move to the next free priority.

Alarm causes:

- `SYSTEM_NOT_REGISTERED`
- `MULTIPLE_MATCHES`
- `SYSTEM_NOT_IN_DESCRIPTION`, except for the systems in `plant.alarm.ignored.systems`
- `NOT_MONITORED`
- `NO_RECENT_OBSERVATION`

Alarms are stored, cleared automatically, and acknowledged by operators.

## Configuration

| Property | Default | Meaning |
| --- | --- | --- |
| `server.port` | `8432` | as in v4.6 |
| `plant.description.max.size` | `200000` | characters of a description's JSON document |
| `plant.sync.interval` | `15000` | ms between synchronizations |
| `plant.rule.default.priority` | `1` | priority of a connection without one |
| `plant.monitoring.enabled` | `true` | evaluate `NOT_MONITORED`/`NO_RECENT_OBSERVATION` via `sysMonitorHistory` |
| `plant.monitoring.stale.after.seconds` | `120` | age at which observations are stale |
| `plant.alarm.ignored.systems` | framework systems | exempt from `SYSTEM_NOT_IN_DESCRIPTION` |
| `service.authorization.enabled` | `false` | per-call ConsumerAuthorization check of every `plantDescriptionMonitor` operation (cached 30 s, fail closed) |
| `sysmonitor.*` | see TimeManager | native `sysMonitor` publisher |

`plantDescriptionManagement` lives under `/mgmt/`, so the management filter (`management.policy`) guards it. In the test clouds the policy is `whitelist` with `LocalCloudManager`, whose dashboard (**PD** view) edits the plant descriptions; system operators are always allowed.

The PDE consumes several management services. It needs:

- ServiceRegistry `serviceRegistryManagement` (`system-query`, `service-query`);
- DynamicServiceOrchestration `serviceOrchestrationPlantManagement` (`query`, `replace`). Grant PDE those operations through ConsumerAuthorization; the endpoint also restricts replacement to the authenticated PDE identity or Sysop. The client pins the DSO provider;
- a ConsumerAuthorization grant for `sysMonitorHistory`.

The rules take effect in **DSO**, not SimpleStore. DSO restricts each managed `(consumer, serviceDefinition)` to the listed service-instance IDs, then runs its usual authorization, blacklist, lock, interface and QoS checks. For matchmaking, the lowest nonnegative priority among surviving providers wins; without matchmaking, all surviving providers are returned in priority order. Caller preferences can narrow the engineered set, but cannot add a provider outside it. No eligible provider means no result, including for `ONLY_INTERCLOUD`/`ALLOW_INTERCLOUD`. Consumers/services outside the snapshot retain normal DSO behavior.

PDE queries the current snapshot and replaces it only when changed. Replacement is atomic in DSO's MySQL table, survives restart, and is acknowledged before PDE reports successful synchronization. No active plant sends `{"rules":[]}` to clear the snapshot. Existing consumer orchestration caches (normally 300 s) may delay observable changes; the 15 s PDE interval does not invalidate those caches. Monitoring alarms remain independent of snapshot replacement failures.

## DSO contract and upgrade

This is a **local 5.2.2 extension**, not an API available in unmodified upstream DSO. The local Java, Go and native Rust implementations now provide it; see the [Go/Rust deployment report](../../security-evaluation/pde-dso-go-rust-2026-10-23/README.md).

| Operation | HTTP | Body |
|---|---|---|
| `query` | `POST /serviceorchestration/mgmt/plant-rules/query` | none |
| `replace` | `PUT /serviceorchestration/mgmt/plant-rules/replace` | complete `OrchestrationPlantRulesDTO` snapshot |

Both operations return the snapshot:

```json
{"rules":[{"consumer":"LineController","serviceDefinition":"temperature",
           "providers":[{"serviceInstanceId":"TemperatureSensor|temperature|1.0.0","priority":1}]}]}
```

`{"providers":[]}` keeps a scope managed but unavailable; `{"rules":[]}` removes every managed scope. The API rejects null/duplicate scopes, duplicate provider IDs and invalid priorities without replacing existing state. LocalCloudManager has query permission only; it edits the plant through PDE rather than overwriting DSO rules.

1. Build the common DTOs, DSO and PDE together, and rebuild ServiceRegistry to activate the reservation of the new management service definition. On an existing DSO database, apply `ah5-core-java-spring/serviceorchestration-dynamic/src/main/resources/database/patch/database_patch_pde_dso.sql` before starting the upgraded DSO (`ddl-auto=validate`). Fresh databases include the table in `create_tables.sql`.
2. In the Java test cloud, `make up` and `make up-plantdescriptionengine` run `scripts/migrate-dso-plant-rules-db.sh` and configure the scoped dependency policies. These targets require the rebuilt DSO and PDE images. They do not migrate another language implementation.
3. Wait until PDE reports the active plant in force, then pull from `DynamicServiceOrchestration` as the consumer to verify the engineered provider and authorization. Test loss of the primary provider, fallback priority, an unauthorized provider and deactivation.
4. Existing SimpleStore rows are not imported or automatically deleted. After verification, optionally remove only the legacy rows with `createdBy=PlantDescriptionEngine`; retain operator-owned SSO rules if still used. SimpleStore can remain available independently, but PDE no longer requires it. Avoid rolling back DSO alone while relying on these managed scopes; coordinate rollback of PDE and its orchestration backend.

## Database

The PDE owns the `ah_plantdescriptionengine` schema, which holds the `plant_description`, `pde_alarm` and `logs` tables. Create it with `src/main/resources/database/create_empty_db.sql` and `grant_privileges.sql`.

## Build

Install `ah5-common-java-spring` and the `arrowhead-core` 5.2.2 parent pom (from `ah5-core-java-spring`) into the local Maven repository, then run from this directory:

```sh
mvn clean test
```

`docker/Dockerfile-PlantDescriptionEngine` builds the image (build context: the workspace root).

## Documentation

[SysD, SysDD and an SD and IDD for each produced service](documentation/v5.2.2/README.md) (`plantDescriptionManagement`, `plantDescriptionMonitor`, `generalManagement`, `sysMonitor`), generated from the implementation with the official Arrowhead LaTeX templates by [`documentation-tools`](../documentation-tools/README.md); the PDFs are beside the sources. The earlier `documentation/v5.2` set is in `../../_superseded/2026-10-22/`.
