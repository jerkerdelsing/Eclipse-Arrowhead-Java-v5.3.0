> **Java-Spring support system (8 November 2026).** LocalCloudConfigurator is now an Arrowhead Java-Spring support system: `localCloudConfiguration` on port 8492 with mutual TLS, outsourced authentication, CERT_AUTH, ConsumerAuthorization, generalManagement and sysMonitor, with a private MySQL schema. Execution stays direct (no ColonyOS) through the host worker, which now listens on loopback 8493 with mutual TLS instead of a shared token. See [DEPLOYMENT.md](DEPLOYMENT.md).

# Java LocalCloudConfigurator

LocalCloudConfigurator designs and fills one existing, provisioned Java Arrowhead local cloud. It ports the primary Go LocalCloudConfigurator approach: a trusted component catalog, dependency planner, the `localCloudConfiguration` Arrowhead service, durable direct workflows and a login-managed host worker that independently validates every step.

The integrated graphical editor is in **Java LocalCloudManager → LC / Local cloud configuration** at http://testcloud.localhost:8490/ . Existing manager credentials apply. LocalCloudManager consumes `localCloudConfiguration` through orchestration with its own certificate and identity token; it needs a ConsumerAuthorization grant for the service's operations. There is no separate public UI and no shared operator token.

## Default cloud

New designs contain exactly these four microsystems:

- ServiceRegistry
- Authentication
- ConsumerAuthorization
- DynamicServiceOrchestration

SimpleOrchestration is optional and is not selected by default. Each database is an integrated private asset of its owning microsystem, not independent infrastructure. The default canvas has four microsystems (each containing its database) and one AMQP broker infrastructure component. Existing running services—including SimpleOrchestration—are retained unless an operator explicitly marks an allowed component for stopping and applies the reviewed plan. A default design is not an instruction to prune an existing cloud.

## Graphical workflow

Open **LC**, choose a component from the library, inspect it and **Add to design**. Dependencies appear automatically. Select components on the canvas and drag them to arrange the cloud. The inspector distinguishes explicit selections, automatic dependencies, live state and protected components. Microsystem category colors match Arrowhead; infrastructure is lavender. Undo/redo, automatic layout, JSON import/export, saved copies and named saved designs are available.

**Save design** persists selections and graphical positions in the configurator's private MySQL schema (`ah_localcloudconfigurator`). It does not change the runtime. Versioned writes reject stale edits. Deleting a saved design also leaves services unchanged. Imported designs clear stop/reapply actions and require fresh review before deployment.

**Review deployment plan** asks the real cloud executor for running service names and derives provision → up → policy → explicit stop → verification actions in dependency order. Apply requires that reviewed plan's hash to still match the current plan. A confirmation names the runtime effect. Existing selected services are not recreated; explicit reapply can rerun catalogued setup scripts. No unselected component is implicitly stopped. The API serializes deployments; its active-deployment record (MySQL) survives restart. The UI displays the real local workflow and redacted step results. Failure can leave earlier successful steps applied; there is no transactional rollback of arbitrary deployment scripts.

## Scope and safeguards

The shipped catalog describes 27 components of the existing Java test-cloud Compose project. Components are limited to its known images, services, dependencies, existing provisioning scripts and existing policy scripts. New images/ports/credentials require deliberate catalog/cloud setup work; arbitrary images, shell commands and secret changes are not accepted from the browser. Starts use `--no-deps --no-build --pull never`; missing images or unprovisioned credentials must be prepared separately. No legacy source tree is built by the executor.

The four default core systems, their required infrastructure and manager access are protected from stops. Remaining running dependents prevent a stop. The executor checks claimed component ownership, step kind, exact target/script, catalog hash and current runtime again. It runs argument lists, not interpolated shell commands. Output capture is bounded, credential-like values and private-key blocks are redacted. The catalog/scripts themselves remain trusted host-admin inputs.

The API is a registered Arrowhead system: callers present a client certificate of the cloud and an identity token bound to it, and each `localCloudConfiguration` operation needs Sysop, `lcc.api.whitelist` or a ConsumerAuthorization grant. LocalCloudManager keeps its own console session, host/origin and action-confirmation protections in front of it. The default backend is the host worker on loopback 8493, reached over mutual TLS: the API presents its LocalCloudConfigurator certificate, the worker presents LocalCloudConfiguratorWorker; each side accepts only that peer name from the cloud CA (and the CRL). No ColonyOS server, membership or keys are required. The legacy ColonyOS backend (`lcc.backend=colony`) is retained only for explicit historical use.

## Build and operate

See [DEPLOYMENT.md](DEPLOYMENT.md), [system design](documentation/SysD.md), [detailed design](documentation/SysDD.md), [service descriptions](documentation/SD.md) and [interface design](documentation/IDD.md).

```sh
make build    # mvn clean verify in the Maven container
make image    # docker compose build (system and database images)
make deploy   # changes a deployed cloud: only when an operator decides so
```

Java 21, Spring Boot 3.4 with the vendored Arrowhead common library (`vendor/`), Docker Compose and a macOS login-managed host worker. Package layout: `LocalCloudConfiguratorMain`/`SystemInfo`/`Constants`/`Defaults`, `api/http` (controllers), `security` (access filter), `service` (coordinator, configurations, backends), `jpa`, `worker` (host worker), `tls` (mutual TLS), and the unchanged `Catalog`, `Planner`, `Runner`, `DirectBackend`, `CloudColony` and `colony/`. Host worker state stays at `~/.local/share/JavaLocalCloudConfigurator/`; designs, deployment records and the active-deployment record are in MySQL.

Deployment verification: [2026-10-30 report](verification/2026-10-30/REPORT.md).

## Internal database assets

All 16 defined databases, including GraphDBManager’s graph database, belong exclusively to their host microsystem. They have no standalone palette, graph node or stop action. The host inspector lists its integrated assets. Database containers start before the host and stop after it; volumes are retained. Access is limited to host internal actions or host-produced/consumed services. Each database uses a dedicated internal network shared only with its host, with no published database ports. Legacy design database selections become host selections; standalone database stop flags and positions are discarded by the editor.

Database ownership update: [verification report](verification/2026-10-30/database-assets/REPORT.md).

## Listening port

Default **8492** (`server.port` in the packaged `application.properties`). Override with `server.port` in `config/application.properties`, `SERVER_PORT=8492` or `--server.port=8492`.

See [LISTENING-PORTS.md](../LISTENING-PORTS.md) for precedence and all systems.
