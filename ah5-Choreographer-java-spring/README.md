# Choreographer 5.2.2

> Current standalone packaging (25 October 2026): build and deploy from this folder. See [DEPLOYMENT.md](DEPLOYMENT.md). Any combined-tree build or live-cloud examples below are historical integration context.

Choreographer is an Eclipse Arrowhead 5.2.2 support system. It executes pre-defined workflows (*plans*) by selecting *executor* systems for the plan steps, having DynamicServiceOrchestration orchestrate the required providers **for the executors**, and starting the executors step by step. It is the v5.2.2 successor to the v4.6 `core-java-spring/choreographer` core system, and it publishes the Arrowhead 5.2.2 `sysMonitor` service natively.

## Workflow model

| Element | Structure | Execution |
| --- | --- | --- |
| Plan | Unique name; a chain of actions starting at `firstActionName` | A session runs the plan `quantity` times |
| Action | `nextActionName` links to the following action | Starts when the previous action has finished |
| Step | `serviceRequirement` (5.2.2 orchestration requirement), `quantity`, `staticParameters`, `localCloudOnly`; `nextStepNames` form a DAG from `firstStepNames` | Starts when all of its predecessors are done; independent steps run in parallel |

Plans are validated with the v4.6 rules, plus two more: a first step has no predecessor, and every other step is reachable. Step graphs are checked for cycles and reduced transitively (the v4.6 `EdgeDestroyer`).

Executor results: `SUCCESS` continues the plan. `ERROR` excludes the executor for the session and retries the step with another one; the session is aborted if no other executor exists. `FATAL_ERROR` aborts the session. `ABORTED` acknowledges an abort.

## Services

| Service | Version | Operations (HTTP) |
| --- | --- | --- |
| `choreographerSession` | 1.0.0 | `start-session` `POST /choreographer/session/start`; `abort-session` `DELETE /choreographer/session/abort/{id}` (owner only) |
| `choreographerExecutor` | 1.0.0 | `register` `POST /choreographer/executor/register`; `unregister` `DELETE /choreographer/executor/unregister`; `notify-step-done` `POST /choreographer/executor/notify-step-done` (the step's executor only) |
| `choreographerManagement` | 1.0.0 | under `/choreographer/mgmt`: `query-plans`, `get-plan`, `create-plan`, `remove-plan`, `check-plan`, `start-sessions`, `abort-session`, `query-sessions`, `query-session-steps`, `query-worklog`, `query-executors`, `get-executor`, `create-executor`, `remove-executor` |
| `generalManagement` | 1.0.0 | `get-log` `POST /choreographer/general/mgmt/logs`; `get-config` `GET /choreographer/general/mgmt/get-config` |
| `sysMonitor` | 1.0.0 | `echo`, `power`, `status` on `<localCloud>.sysmonitor.Choreographer.Choreographer.<type>` (exchange `arrowhead`). `echo` is `alive` with plan and running-session counts while the database answers, and `degraded` otherwise |

Executor-side interface, which the Choreographer consumes: `POST <basePath>/service-info`, `/start` and `/abort` on the executor's registered address and port.

Orchestration for an executor uses DynamicServiceOrchestration `serviceOrchestrationPushManagement`: `subscribe` with the executor as target system, then `trigger`. The subscription is not removed explicitly, because DSO authorizes management operations by exact path, so `unsubscribe/{id}` is sysop-only. It expires `orchestration.timeout` + 30 s after it was made. DSO keeps one subscription per (owner, target, service definition), so orchestrations for the same executor and service definition are serialised. DSO pushes the result to `POST /choreographer/orchestration-result/{correlationId}`. That endpoint is not a registered service. With TLS it accepts only DSO's local-cloud certificate (identified by its profile type, or by its CN when the cloud's certificates carry no `dnQualifier`), and it accepts only pending random correlation ids (otherwise `404`).

## Configuration

| Property | Default | Meaning |
| --- | --- | --- |
| `server.port` | `8457` | HTTP(S) port (as in v4.6) |
| `max.plan.iteration` | `1000` | Largest session `quantity` |
| `orchestration.timeout` | `30` | Seconds to wait for a pushed orchestration result |
| `session.worker.threads` | `8` | Engine worker threads (replace the v4.6 JMS queues) |
| `sysmonitor.enabled`, `sysmonitor.broker.*`, `sysmonitor.ssl.*` | `true`, `amqp-broker:5671`, `server.ssl` stores | Native `sysMonitor` publisher of the common library |

## Local-cloud prerequisites

- ServiceRegistry: add `Choreographer` to `service.discovery.direct.access` when `service.discovery.policy=restricted`. Otherwise `check-plan` and `start-session` cannot see providers.
- ConsumerAuthorization:
  - Choreographer → DynamicServiceOrchestration `serviceOrchestrationPushManagement`;
  - executors → the step services;
  - clients → `choreographerSession`;
  - executors → `choreographerExecutor`.
- Executors register themselves in ServiceRegistry and then with `choreographerExecutor.register`. The executor address must be one of their ServiceRegistry addresses.

## Database

Choreographer owns the `ah_choreographer` schema with these tables: `plan`, `plan_action`, `plan_step`, `plan_step_connection`, `executor`, `executor_service_definition`, `plan_session`, `session_step`, `worklog` and `logs`. Create the schema with the scripts in `src/main/resources/database/`: `create_empty_db.sql`, then `create_tables.sql`, then `grant_privileges.sql`. Hibernate validates the schema at start-up.

## Build

Install `ah5-common-java-spring` and the `arrowhead-core` 5.2.2 parent pom (from `ah5-core-java-spring`) into the local Maven repository, then run from this directory:

```sh
mvn clean package
```

`docker/Dockerfile-Choreographer` builds an image from the workspace root. It needs `ah5-common-java-spring`, `ah5-core-java-spring/pom.xml` and this folder.

## Verification

- **Unit tests (18):**
  - step graph tests against the v4.6 examples;
  - plan validation;
  - version windows;
  - notify URI;
  - orchestration-result broker.
- **Runtime smoke test** against MySQL 8.4, the Go ServiceRegistry, Mosquitto, a fake DynamicServiceOrchestration and a fake executor:
  - schema validated and the five services registered;
  - a two-action plan run twice, 8 session steps all `DONE`;
  - a forced `ERROR` that aborts the session (no other executor available), with notifications sent;
  - cyclic and duplicate plans rejected;
  - `403` for a non-owner abort and for a step result from the wrong system;
  - `404` for an unknown push id and `401` for other paths without authentication;
  - sessions active at restart aborted;
  - `sysMonitor` echo, power and status observed.

## Test-cloud deployment

Both 5.2.2 test clouds (`Arrowhead-test-cloud`, `Arrowhead-test-cloud-go`) deploy Choreographer as service `choreographer` with its own `choreographer-db` (`make up`, or `make up-choreographer` on a running cloud). Host port 8457 (Java cloud) or 9457 (Go cloud).
- **Security:** mTLS with `server.ssl.client-auth=need`, outsourced authentication with Authentication identity tokens (identity `Choreographer`), and `management.policy=sysop-only`.
- **Registration:** through the bootstrap proxy on port 8513. The HTTP offers are re-registered as `CERT_AUTH`. Choreographer is added to ServiceRegistry `service.discovery.direct.access`.
- **Grants (`scripts/configure-choreographer-policy.sh`):**
  - DSO `serviceOrchestrationPushManagement` → Choreographer;
  - `choreographerSession` → `CHOREOGRAPHER_SESSION_CLIENTS`;
  - `choreographerExecutor` → `CHOREOGRAPHER_EXECUTORS` (both default `["LocalCloudManager"]`);
  - `sysMonitor` → SysMonitor.
- **Database:** the clouds run many MySQL servers, so `choreographer-db` runs with `--innodb-use-native-aio=0` to stay within the kernel AIO limit.
- **Executors:** none is deployed. An executor needs a certificate, an Authentication identity, a ServiceRegistry system entry, the `choreographerExecutor` grant and grants for its step services.

Verified in both clouds with a temporary mTLS executor (since removed):
- LocalCloudManager ran a two-action plan twice: 6 steps `DONE`.
- For each step, DSO orchestrated TimeManager `time` for the executor and pushed the result. The executor called TimeManager with its own identity token and reported back.
- A push from a non-DSO certificate was refused (`403`).
- The cloud smoke test and the native-sysMonitor verification passed (63 checks per cloud).

## Migration from v4.6

- Core system `CHOREOGRAPHER` becomes support system `Choreographer`.
- The services become `choreographerSession` (new: clients start their own sessions by plan name), `choreographerExecutor` and `choreographerManagement`.
- A step names a 5.2.2 orchestration service requirement instead of `serviceDefinition`/`minVersion`/`maxVersion`/`srTemplate`. The version window is derived from `versions`.
- "Orchestration by proxy" is replaced by DSO push orchestration with the executor as target system. Authorization and tokens are evaluated for the executor.
- `notifyStepDone` (`runningStepId`) becomes `notify-step-done` (`sessionStepId`) and is accepted only from the step's executor.
- `/echo` is removed; liveness is published through `sysMonitor`.
- Not continued:
  - the Gatekeeper/Gateway path, i.e. inter-cloud executor discovery through GSD and relayed executor tunnels. Inter-cloud is limited to the orchestration flag `ALLOW_INTERCLOUD`, and `chooseOptimalExecutor` only ranks local executors;
  - JMS/ActiveMQ.
- Fixes:
  - parallel step completions are serialised per session, and each step is claimed exactly once;
  - sessions left active by a restart are aborted instead of hanging;
  - a missing orchestration result times out (`orchestration.timeout`).
- Own database schema instead of the shared v4 database; OpenAPI 3, Jakarta and Java 21.

## Documentation

[SysD, SysDD and an SD and IDD for each produced service](documentation/v5.2.2/README.md) (`choreographerSession`, `choreographerExecutor`, `choreographerManagement`, `generalManagement`, `sysMonitor`), generated from the implementation with the official Arrowhead LaTeX templates by [`documentation-tools`](../documentation-tools/README.md); the PDFs are beside the sources. The earlier `documentation/v5.2` set is in `../../_superseded/2026-10-22/`.
