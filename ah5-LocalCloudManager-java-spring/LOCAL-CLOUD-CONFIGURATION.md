# Java LocalCloudConfigurator

LocalCloudConfigurator designs and fills one existing, provisioned Java Arrowhead local cloud. It ports the primary Go LocalCloudConfigurator approach: a trusted component catalog, dependency planner, operator API, signed ColonyOS workflows and a host executor that independently validates every step.

The integrated graphical editor is in **Java LocalCloudManager → LC / Local cloud configuration** at http://testcloud.localhost:8490/ . Existing manager credentials apply. LocalCloudManager consumes LocalCloudConfigurator's `localCloudConfiguration` Arrowhead service (port 8492, mutual TLS) through orchestration with its own certificate and identity token; there is no private operator token any more.

## Default cloud

New designs contain exactly these four microsystems:

- ServiceRegistry
- Authentication
- ConsumerAuthorization
- DynamicServiceOrchestration

SimpleOrchestration is optional and is not selected by default. Each database is an integrated private asset of its owning microsystem, not independent infrastructure. The default canvas has four microsystems (each containing its database) and one AMQP broker infrastructure component. Existing running services—including SimpleOrchestration—are retained unless an operator explicitly marks an allowed component for stopping and applies the reviewed plan. A default design is not an instruction to prune an existing cloud.

## Graphical workflow

Open **LC**, choose a component from the library, inspect it and **Add to design**. Dependencies appear automatically. Select components on the canvas and drag them to arrange the cloud. The inspector distinguishes explicit selections, automatic dependencies, live state and protected components. Microsystem category colors match Arrowhead; infrastructure is lavender. Undo/redo, automatic layout, JSON import/export, saved copies and named saved designs are available.

**Save design** persists selections and graphical positions in the configurator's private MySQL schema. It does not change the runtime. Versioned writes reject stale edits. Deleting a saved design also leaves services unchanged. Imported designs clear stop/reapply actions and require fresh review before deployment.

**Review deployment plan** asks the real cloud executor for running service names and derives provision → up → policy → explicit stop → verification actions in dependency order. Apply requires that reviewed plan's hash to still match the current plan. A confirmation names the runtime effect. Existing selected services are not recreated; explicit reapply can rerun catalogued setup scripts. No unselected component is implicitly stopped. The API serializes deployments, with its active-workflow marker surviving restart. The UI displays the real ColonyOS workflow and redacted step results. Failure can leave earlier successful steps applied; there is no transactional rollback of arbitrary deployment scripts.

## Scope and safeguards

The shipped catalog describes 27 components of the existing Java test-cloud Compose project. Components are limited to its known images, services, dependencies, existing provisioning scripts and existing policy scripts. New images/ports/credentials require deliberate catalog/cloud setup work; arbitrary images, shell commands and secret changes are not accepted from the browser. Starts use `--no-deps --no-build --pull never`; missing images or unprovisioned credentials must be prepared separately. No legacy source tree is built by the executor.

The four default core systems, their required infrastructure and manager access are protected from stops. Remaining running dependents prevent a stop. The executor checks claimed component ownership, step kind, exact target/script, catalog hash and current runtime again. It runs argument lists, not interpolated shell commands. Output capture is bounded, credential-like values and private-key blocks are redacted. The catalog/scripts themselves remain trusted host-admin inputs.

LocalCloudManager reaches LocalCloudConfigurator through orchestration as the Arrowhead system LocalCloudManager (certificate and identity token; a ConsumerAuthorization grant of the `localCloudConfiguration` operations) and keeps its own console session, host/origin and action-confirmation protections. The configurator reaches its host worker over mutual TLS; no ColonyOS server, membership or keys are required.

## Build and operate

See [DEPLOYMENT.md](DEPLOYMENT.md), [system design](documentation/SysD.md), [detailed design](documentation/SysDD.md), [service descriptions](documentation/SD.md) and [interface design](documentation/IDD.md).

```sh
make build
make test
make image
make deploy
```

LocalCloudConfigurator is a Java-Spring support system with a host worker on loopback 8493 (mutual TLS); see `../ah5-LocalCloudConfigurator-java/DEPLOYMENT.md`.

Detailed API/system documentation is in `../ah5-LocalCloudConfigurator-java/documentation/`.

## Internal database assets

All 16 defined databases, including GraphDBManager’s graph database, belong exclusively to their host microsystem. They have no standalone palette, graph node or stop action. The host inspector lists its integrated assets. Database containers start before the host and stop after it; volumes are retained. Access is limited to host internal actions or host-produced/consumed services. Each database uses a dedicated internal network shared only with its host, with no published database ports. Legacy design database selections become host selections; standalone database stop flags and positions are discarded by the editor.
