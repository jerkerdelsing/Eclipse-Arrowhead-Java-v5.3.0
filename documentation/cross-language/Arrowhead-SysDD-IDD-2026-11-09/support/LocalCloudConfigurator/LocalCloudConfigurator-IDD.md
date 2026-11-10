# LocalCloudConfigurator
## Interface Design Description
Eclipse Arrowhead 5.2.2 | support system | Revision 2026-11-09 | REVIEW DRAFT
## Purpose and document structure
This document consolidates the system interface designs. Detailed Java service/transport IDDs are retained as separate appendices, followed by available Go and Rust interface descriptions.
The current cross-language sections below precede the detailed implementation appendices. Appendix dates and release labels belong to the retained baseline, not to a newly qualified release. Existing descriptions have been assembled from the primary trees; no live interoperability or semantic-parity claim is made.
## Java implementation
Primary folder: Eclipse-Arrowhead-java/ah5-LocalCloudConfigurator-java.
2 detailed Java IDD source document(s) are included in the appendices. Their architecture, operation parameters, models, transport bindings, access rules and errors remain associated with Java. They must not be read as automatically implemented by the ports.
## Go implementation
Primary folder: Eclipse-Arrowhead-go/ah5-LocalCloudConfigurator-go.
### LocalCloudConfigurator
LocalCloudConfigurator fills an Arrowhead 5.2.2 local cloud with the support systems and application systems an operator picks. The operator picks the systems in a web UI or through the API. The configurator turns the selection into an ordered deployment plan and submits that plan to [ColonyOS](https://colonyos.io) as a workflow. A ColonyOS executor on the cloud host carries out each step.
The first target is the GoCloud (`Arrowhead-test-cloud-go`). The executor runs the cloud's own scripts, so the configurator follows the same steps as the cloud's Makefile.
### Parts
| Part | Where | Role |
|---|---|---|
| Catalog | `catalogs/gocloud.json` | The cloud's selectable systems, their Compose services, alternates (Rust/Java swaps), dependencies, identity provisioning and policy scripts |
| Planner | `internal/planner` | Turns a selection into steps (`provision` → `up` → `policy`, then `stop`, then `smoke`) in dependency order |
| API + UI | `localcloudconfigurator serve` (container in the ColonyOS stack) | `GET /api/catalog`, `GET /api/status`, `POST /api/plan`, `POST /api/deployments`, `GET /api/deployments/{id}`; UI at `/` |
| Cloud executor | `localcloudconfigurator executor` (on the cloud host) | ColonyOS executor of type `arrowhead-cloud-executor`; serves `lcc.status` and `lcc.step` |
| ColonyOS stack | `deploy/colonyos/compose.yaml` | TimescaleDB, Colonies server v1.9.10, the API |
### Rules the executor enforces
- The executor checks every step against its own catalog. It does not trust the arguments of a process.
  - Only catalogued Compose services can be started or stopped. Core systems are never touched.
  - Only catalogued scripts in the cloud's `scripts/` directory can run.
- Services start with `docker compose up -d --wait --no-deps <service>`. Dependencies are explicit plan steps. Compose therefore never starts a Go service while its Rust or Java alternate runs (see the GoCloud swap targets).
- When an alternate runs, the system counts as present. The configurator neither starts nor stops it; the cloud's `swap-*`/`restore-*` targets manage it.
- Step output goes to the ColonyOS process logs with credential-like values masked. Secrets are never printed.
- No secret rotation and no PKI changes. Identities are provisioned through the cloud's `provision-*-identity.sh` scripts, which log in as Sysop with `sysop.crt`.
LocalCloudConfigurator is an operations tool. It is not an Arrowhead system: it does not register in the ServiceRegistry and has no Arrowhead identity. The new-system checklist (AMQP ACL, sysMonitor policy, Orchestration grants) therefore does not apply.
### Build and test
Deploying and the smoke test are described in [DEPLOYMENT.md](DEPLOYMENT.md).
### Adding a cloud or a system
Add a system as an entry in the catalog: `id`, `kind` (`infrastructure`/`support`/`application`), `services`, and optionally `alternates`, `requires`, `provision` and `policies`. To support another cloud, write a catalog with its own `composeProject` and `executorType`. Then run one executor per cloud host with `-catalog` and `-cloud-dir`.
### Listening port
`serve` listens on **127.0.0.1:8095** by default. Override with `-listen host:port`, `SERVER_PORT=N` or `-server.port N`; the port overrides keep the `-listen` host.
See [LISTENING-PORTS.md](../LISTENING-PORTS.md) for precedence and all systems.
### Go interface and design differences
The native Go LocalCloudConfigurator is an operator API and embedded UI using net/http and ColonyOS. It is not assumed equivalent to the Java Arrowhead support-system wrapper. internal/api/api.go registers GET /, GET /healthz, GET /api/catalog, GET /api/status, POST /api/plan, POST /api/deployments and GET /api/deployments/{id}. There is no GET /api/deployments collection route in this handler.
API calls require Authorization: Bearer <operator-token>, checked in constant time; an empty configured token or mismatch gives 401 with {error:"operator token required"}. This is an operator bearer token, not an Arrowhead IDENTITY-TOKEN. The health endpoint returns text ok. Security headers include X-Content-Type-Options, X-Frame-Options and Content-Security-Policy. TLS termination and network exposure must follow this implementation's deployment configuration; no Java mutual-TLS parity is inferred.
Planning rejects unknown JSON fields, limits request input to 64 KiB, returns 400 on decoding errors, 502 on status lookup failure and 422 on planner rejection. A successful plan gives 200. A deployment with no steps gives 200 and an empty id; otherwise successful workflow submission gives 202. Source snapshots of the API, planner, catalog and ColonyOS adapter are included for exact data types and failure behavior.
### Detailed implementation evidence
0 native Go IDD document(s) are included in the appendices. Their protocol and storage descriptions apply to that implementation only. Current primary-folder ownership and current listening-port notes above supersede historical paths or example ports in those retained documents.
## Rust implementation
No Rust implementation folder for LocalCloudConfigurator was found in the designated primary Rust tree on 2026-11-09. No executable, build command, endpoint or feature parity is asserted. A future port must define architecture, state migration, service registration, DTO validation, authorization and transport coverage before it can replace this system.
## Cross-language qualification
Identical system names or HTTP paths do not establish interoperability. For the selected implementation, verify registered service versions and metadata, method/path and payload models, null and optional-field handling, success/error bodies, authentication and per-operation authorization, timeout/retry behavior and persistence across restart. Java MQTT/AMQP descriptions apply to a port only where its own implementation evidence explicitly supports them. Missing transports must not be advertised.
State conversion from Java SQL to native snapshots, secret provisioning, backup/restore and rollback require implementation-specific procedures. No data migration, deployment or cloud reconfiguration was performed for this document set.
## Detailed appendices and provenance
- Java: Eclipse-Arrowhead-java/ah5-LocalCloudConfigurator-java/documentation/v5.2.2/LocalCloudConfigurator-v5.2.2-localCloudConfiguration-HTTP-IDD.tex
- Java: Eclipse-Arrowhead-java/ah5-LocalCloudConfigurator-java/documentation/v5.2.2/LocalCloudConfigurator-v5.2.2-sysMonitor-AMQP-IDD.tex
The package sources/ folder retains the selected LaTeX, figures, bibliography, language README/build evidence and source snapshots for newly described contracts. source-manifest.json records SHA-256 hashes of original inputs. build-report.json records whether an appendix used an existing newer PDF or was compiled from source.
