# SysDD — LocalCloudConfigurator

## White-box components

| Component | Responsibility |
|---|---|
| Catalog | Validates names/ownership/scripts/dependencies; defines the four default core systems and protected closure; computes a revision hash |
| Planner | Adds defaults and transitive infrastructure, orders starts/setup and reverse-order removals, blocks protected/selected/dependency-conflicting removals |
| Runner | Validates each step independently, reads compact runtime names, refuses arbitrary service/script claims, prevents recreating running services, rechecks stops and bounds/redacts command output |
| CloudColony / ColonyBackend | Retained legacy backend (`lcc.backend=colony`): signed status requests and chained `lcc.step` workflows |
| LocalCloudConfigurationAPI | `/api/*` controller: bounded strict JSON (64 KiB), native paths, JSON shapes and status codes |
| ConfiguratorAccessFilter | After the common authentication filter: Sysop, whitelist or ConsumerAuthorization grant per operation (cached 30 s, fail closed) |
| DeploymentCoordinator | Fresh plans, plan-hash checks, single active deployment; used by the API (JPA store) and by the host worker (file store) |
| ConfigurationService / JPA | Saved designs with revisions; deployment records with the last reported step results; active-deployment record (MySQL `ah_localcloudconfigurator`, DML-only account) |
| WorkerClient / MutualTls | API → host worker over mutual TLS; peer certificate system names pinned on both sides; cloud CRL |
| WorkerApplication / WorkerServer | Host worker: non-web Spring application, loopback 8493, DirectBackend with per-step file results; interrupted deployments marked failed and never replayed |
| LocalCloudConfiguratorMain / ConfiguratorCli | `serve` (default), `worker`, and the retained tools keygen, register, executor and offline plan |
| Manager proxy | LocalCloudManager routes `/api/localcloud/*`, consuming `localCloudConfiguration` through orchestration with its own certificate and token |
| Manager editor | Graphical selection/layout, derived infrastructure, distinct live state, undo/redo, import/export and real plan/progress views |

Designs, deployment records and the active-deployment record are rows of the private MySQL schema (JPA, `ddl-auto=validate`). Each update/delete checks an expected revision under a server lock. A new process reads the active-deployment record and checks workflow state before accepting another deployment. API workflow lookups are restricted to IDs submitted by that API and retained in its state.

A plan is derived afresh from a live executor status response before apply. Its hash includes catalog revision, expanded selection, ordered steps and notes. The executor also compares its own catalog hash and target ownership; a stale queued start becomes a no-op if the service is already running. Stops are rechecked against current dependents. These checks are not a distributed transaction with independent host administrators.

The host executor loads an immutable JAR copy. API and host identities are approved separately in a dedicated colony; authenticated heartbeats keep them live without owner keys in service processes. Registration uses the server-owner list operation because owner keys are not ordinary colony members; matching retired executor identities can be approved explicitly by their colony owner.

### Database ownership

A defined database is a private integrated asset of its host microsystem. `internalAssets` identifies the owned database Compose services; `services` orders database startup before host startup (reverse for stopping). It is not a catalog component or external dependency. Other microsystems access data only through the host’s produced or consumed services. Dedicated internal database networks admit only the host and its database, with no published database port. The default design has four microsystems plus one AMQP infrastructure node.
