# LocalCloudManager 5.2.2

Operator console of an Arrowhead 5.2.2 local cloud. It shows the cloud's state (containers, registry, orchestration,
consumers, DataManager data, SysMonitor history, inter-cloud exchange) and carries out operator actions (orchestration
rules and policies, plant descriptions, Systems of LocalClouds (SoLC view), sysMonitor subscriptions, local-cloud configuration, container
logs and lifecycle). It acts in the cloud as the Arrowhead system `LocalCloudManager` with its own system certificate
`LocalCloudManager.<Cloud>.<Organization>.arrowhead.eu` and an identity token bound to it.

**Java-Spring support system (8 November 2026).** The Node.js API, the nginx UI server and the Node.js Docker proxy were replaced
by two Spring Boot processes with the vendored Arrowhead common library. The `/api/*` routes and `/health` keep their paths,
JSON shapes, status codes and error bodies; the browser UI (`ui/public`) is unchanged. See [DEPLOYMENT.md](DEPLOYMENT.md).

| Part | Process / port | Content |
|---|---|---|
| Console connector | `cloud-monitor-api`, **8490** (plain HTTP, host loopback only) | the static UI, `/health`, `/api/session` and every `/api` route; password login in Spring Security (`console/`) |
| Arrowhead connector | `cloud-monitor-api`, **8491** (mutual TLS) | `localCloudManagement` (Sysop, `lcm.api.whitelist` or ConsumerAuthorization grant), `generalManagement` get-config |
| Consumer side | `cloud-monitor-api` | Providers selected by orchestration on behalf of LocalCloudManager (common service collector): DataManager, SysMonitor, PlantDescriptionEngine, DSO rules and history, ConsumerAuthorization, Gatekeeper/GateTunnel, LocalCloudConfigurator, ChipletFloorplanGenerator; ServiceRegistry management with the identity token; provider certificates checked against the cloud CRL |
| Docker proxy | `docker-proxy` (separate minimal Spring Boot process, 2375, internal network) | only `GET /containers/json`, `GET /containers/<id>/logs`, `POST /containers/<id>/(start\|stop\|restart)` for its own Compose project (lifecycle only for `MANAGED_SERVICES`); everything else 403 |

Produced services: `localCloudManagement` 1.0.0 (`generic_https`, CERT_AUTH), `generalManagement` (get-config) and `sysMonitor`
1.0.0 (`generic_amqps`, CERT_AUTH, common publisher; echo is degraded while Docker cannot be reached). LocalCloudManager has no
database: it keeps no persistent state (console sessions and the overview history are in memory).

Source layout (`eu.arrowhead.localcloudmanager`): `LocalCloudManagerMain`/`SystemInfo`/`Constants`, `console/` (ConsoleAuth,
ConsoleFilter, ConsoleSecurityConfig, ConsoleConnectorConfig), `api/http/` (ConsoleApiController: the native route order,
GeneralManagementAPI), `client/` (orchestrated provider gateway, TLS context with CRL, Docker gateway), `service/` (ports of the
native server, policy and rule-priority editors, CRL status), `security/` (localCloudManagement access filter),
`init/`. The Docker proxy is the separate Maven project `docker-proxy/`.

## Build and test

```sh
make build            # both artifacts: mvn clean verify in the Maven container
sh scripts/verify.sh  # the same, plus the browser-model tests in ui/test when Node.js is installed
```

The native Node tests of the API and the proxy were rewritten as JUnit/MockMvc tests (`src/test`, `docker-proxy/src/test`).

## Configuration notes

- `localCloudConfiguration` of LocalCloudConfigurator is consumed through orchestration with LocalCloudManager's own certificate and
  token (no `LOCAL_CLOUD_CONFIGURATOR_URL`, no shared token). Grant LocalCloudManager the operations of `localCloudConfiguration`.
- The console-only `session`/`login`/`logout` operations are no longer registered as `localCloudManagement` operations; they exist
  only on the console connector.

See [Graphical orchestration and security editor](GRAPHICAL-ORCHESTRATION.md), [System of LocalClouds view](SOLC-CONFIGURATION.md)
and [graphical local-cloud configuration](LOCAL-CLOUD-CONFIGURATION.md).

## Listening port

Default **8491** (`server.port` in the packaged `application.properties`). Override with `server.port` in `config/application.properties`, `SERVER_PORT=8491` or `--server.port=8491`. Note: this folder's deployment package (Dockerfile and compose) uses 8490.

See [LISTENING-PORTS.md](../LISTENING-PORTS.md) for precedence and all systems.
