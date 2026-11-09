# IDD — Implementation and dependency interfaces

## Host worker (default)

The API calls the host worker at `https://host.docker.internal:8493` (`lcc.worker.url`): `GET /api/status`, `POST /api/deployments` (the reviewed selection plus `expectedPlanHash`; the worker re-plans and compares), `GET /api/deployments/{id}` (ids `local-<uuid>`). Mutual TLS, TLS 1.3/1.2: the API presents `LocalCloudConfigurator.<cloud>.<organization>.arrowhead.eu`, the worker `LocalCloudConfiguratorWorker.<cloud>.<organization>.arrowhead.eu`; both validate the chain against the cloud truststore (and the CRL) and accept only that peer system name. The worker binds to a loopback address only and refuses to start otherwise.

## ColonyOS (legacy backend)

Pinned existing server: Colonies v1.9.10. The Java client is adapted from primary Java SoSConfigurator, with secp256k1/SHA3-256 signed RPC, workflow chaining, process state/output and approved executor identities. The Java cloud has its own `arrowhead-java-localcloud` colony and `arrowhead-java-cloud-executor` executor type.

`lcc.status` returns one JSON string containing running Compose service names. The API sends a bounded request; the executor reads `docker compose -p <catalog project> -f compose.yaml ps --status running --format '{{json .Service}}'`. Restricting output to service names avoids truncating verbose container metadata and does not collect environment values.

`lcc.step` carries a JSON `step` (`kind`, `target`, `system`) plus `catalogHash`; extra top-level kind/target fields are for readable process summaries, not execution authority. The executor trusts its own catalog only:

- `provision` and `policy`: exact scripts of that claimed component from the cloud's real `scripts/` directory; path escapes rejected.
- `up`: exact owned Compose service; if already running, return unchanged; otherwise `up -d --wait --no-deps --no-build --pull never`.
- `stop`: exact owned service; protected closure and current dependent guards must pass.
- `smoke`, target `verify-localcloud`, no system: verify the default baseline is running; no mutation.

There is no raw shell/command function. Status commands have a 30-second deadline, operational commands 540 seconds; workflow steps have bounded execution time and dependent ordering. Output is capped and credential-like values/private-key blocks are redacted. No Docker socket is mounted in the API or browser-facing manager.

## Existing Java cloud

The catalog is `catalogs/javacloud.json`, derived from the supplied Java Compose deployment with setup mappings from the primary Go reference. It includes Java databases/brokers, core/support/application components and protected manager access. Existing provisioning/policy scripts remain deployment consumers; no new implementation code is developed under the older tree. Manager/configurator Compose build contexts point at the primary Java source.

## Persistence and UI

The API stores designs, deployment records and the active-deployment record in its private MySQL schema `ah_localcloudconfigurator` (tables `configuration`, `deployment`, `active_deployment`, `logs`); graphical positions therefore survive container restarts and another browser session. The manager proxy carries only fixed API paths and validated request bodies. Browser exports omit credentials; imports become new designs and clear removal/reapply actions. There is no operator token any more. Certificates, store passwords and ColonyOS keys are private files outside the repository (Compose secrets, the worker's config tree), never stored in designs or passed as argument values.

### Database ownership

A defined database is a private integrated asset of its host microsystem. `internalAssets` identifies the owned database Compose services; `services` orders database startup before host startup (reverse for stopping). It is not a catalog component or external dependency. Other microsystems access data only through the host’s produced or consumed services. Dedicated internal database networks admit only the host and its database, with no published database port. The default design has four microsystems plus one AMQP infrastructure node.
