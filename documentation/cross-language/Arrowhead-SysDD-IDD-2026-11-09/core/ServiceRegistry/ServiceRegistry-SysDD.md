# ServiceRegistry
## System Design Description
Eclipse Arrowhead 5.2.2 | core system | Revision 2026-11-09 | REVIEW DRAFT
## Purpose and document structure
This document describes implementation ownership, architecture, persistence, security and operational boundaries.
The current cross-language sections below precede the detailed implementation appendices. Appendix dates and release labels belong to the retained baseline, not to a newly qualified release. Existing descriptions have been assembled from the primary trees; no live interoperability or semantic-parity claim is made.
## Java implementation
Primary folder: Eclipse-Arrowhead-java/ah5-core-java-spring/serviceregistry.
1 detailed Java SysDD source document(s) are included in the appendices. Their architecture, operation parameters, models, transport bindings, access rules and errors remain associated with Java. They must not be read as automatically implemented by the ports.
## Go implementation
Primary folder: Eclipse-Arrowhead-go/ah5-core-go/serviceregistry.
### ServiceRegistry — Go 5.2.2
Standalone core system, reorganized on 2026-10-28 following the Eclipse-Arrowhead-java distribution layout.
Build and operate from this folder; no original Arrowhead checkout or sibling source module is required.
The executable is `bin/ServiceRegistry`. Entry point: `.`. Module: `arrowhead/serviceregistry`. See [DEPLOYMENT.md](DEPLOYMENT.md) for configuration and standalone container use. Existing compatibility limits remain in the copied upstream documentation and [UPSTREAM-README.md](UPSTREAM-README.md).
Existing role selection and API behavior are retained.
Documentation and license notices are included in this folder. Source provenance and import-only rewrites are listed in the distribution's SOURCE-MANIFEST.json. No running cloud or upstream source was changed.
### Listening port
Default **8443** (`server.port` in `application.properties`). Override with `server.port`, `SERVER_PORT=N` or `-server.port N`; the host is `server.address`.
See [LISTENING-PORTS.md](../../LISTENING-PORTS.md) for precedence and all systems.
### Native Go registry design and protocol contract
### Arrowhead 5.2.2 ServiceRegistry (Go)
A Go implementation of the Arrowhead 5.2.2 **ServiceRegistry** core system, modelled on the
Java Spring reference implementation (`eclipse-arrowhead/ah5-core-java-spring`). HTTP/TLS handling uses
the standard library; the optional native monitor uses `github.com/rabbitmq/amqp091-go`. State is
held in memory with an atomic JSON snapshot file. See the parent repository's compatibility report
for limitations of the complete Go core-system bundle.
### Build, test, run
    go build -o serviceregistry .
    go test ./...
    ./serviceregistry -config application.properties
Docker: `docker build -t ah5-serviceregistry-go .`
Configuration uses the reference property names (see `application.properties`); each property can
be overridden by an environment variable (`SERVER_PORT`, `DOMAIN_NAME`, `AUTHENTICATION_POLICY`, ...).
### API (base path `/serviceregistry`)
| Area | Endpoints |
|---|---|
| Monitor | `GET /monitor/echo` |
| Device discovery | `POST /device-discovery/register`, `POST /device-discovery/lookup`, `DELETE /device-discovery/revoke/{name}` |
| System discovery | `POST /system-discovery/register`, `POST /system-discovery/lookup[?verbose]`, `DELETE /system-discovery/revoke` |
| Service discovery | `POST /service-discovery/register`, `POST /service-discovery/lookup[?verbose]`, `DELETE /service-discovery/revoke/{instanceId}` |
| Management | `/mgmt/{devices,systems,service-definitions,service-instances,interface-templates}` (POST create, PUT update where applicable, DELETE with `names=` / `serviceInstances=`) and `POST .../query` |
| General management | `POST /general/mgmt/logs`, `GET /general/mgmt/get-config?keys=` |
Implemented behaviour: the three authentication policies (declared, certificate, outsourced), the
management filter (sysop-only / whitelist / authorization) and the blacklist filter, restricted
service discovery (`unrestrictedDiscovery` metadata), interface policies (open / extendable /
restricted) with default templates, property validators, the metadata-requirement DSL, cascading
deletes, pagination, Arrowhead error bodies, and self-registration of the ServiceRegistry and its
five services at startup (revoked on shutdown; disabled by `standalone.mode=true`).
### Outsourced authentication with mutual TLS
With `authentication.policy=outsourced`, `server.ssl.enabled=true` and `server.ssl.client-auth=need` every request needs a
local-cloud client certificate and an identity token (or the Authentication system's authenticator key):
- The token's system must be the system named by the client certificate CN (token-to-certificate binding), so a stolen
  token cannot be used from another certificate.
- A lookup of exactly the `identity` service is allowed without a token, as in the Java framework: a system must find
  Authentication before its first login. The client certificate is still required.
- The registry logs in itself with `authenticator.credentials` (system name `ServiceRegistry`), reuses its token for the
  `identity-verify` calls to Authentication and logs in again when the token is refused. Authentication's authenticator
  key is checked against `authenticator.secret.keys`.
- Its own services are registered as `generic_https` with policy `CERT_AUTH` when client certificates are required.
### sysMonitor (native publisher)
With `sysmonitor.enabled=true` the registry registers its own `sysMonitor` 1.0.0 offer
(`generic_amqps`, `CERT_AUTH`, metadata `publisherMode=native`) and publishes `echo`, `power`
and `status` observations every `sysmonitor.publish.interval` seconds (default 30) to the AMQP topic
exchange `sysmonitor.exchange` with routing key
`<sysmonitor.local.cloud>.sysmonitor.ServiceRegistry.ServiceRegistry.<type>` (contract SysMonitor-AMQP-IDD-5.2.2).
Messages are persistent and confirmed by the broker. The client (`sysmonitor.go`, `github.com/rabbitmq/amqp091-go`)
connects with mutual TLS and SASL EXTERNAL, so the broker user is the certificate CN; `echo` reports
`degraded` when the snapshot file is not writable.
The registry also seeds the interface templates `generic_amqp` and `generic_amqps` (properties `accessAddresses`,
`accessPort`, `exchange`, `baseRoutingKey`, `operations`).
| Property | Default | Meaning |
|---|---|---|
| `sysmonitor.enabled` | `false` | Register and publish `sysMonitor`. |
| `sysmonitor.broker.address` / `.port` | `amqp-broker` / `5671` | AMQP broker (TLS). |
| `sysmonitor.exchange` | `arrowhead` | AMQP topic exchange. |
| `sysmonitor.local.cloud` | `TestCloud` | Local-cloud routing-key word. |
| `sysmonitor.publish.interval` | `30` | Seconds between publication cycles. |
| `sysmonitor.ssl.cert-file`, `.key-file`, `.ca-file` | — | PEM client certificate, key and CA. |
Environment overrides follow the usual mapping (`SYSMONITOR_ENABLED`, `SYSMONITOR_BROKER_ADDRESS`, ...).
### Additional ingresses of the registry's own services
`service.registry.extra.interfaces` (environment `SERVICE_REGISTRY_EXTRA_INTERFACES`, default `[]`) is a JSON list of extra
interfaces that the registry adds to its own service instances every time it registers itself, for example a private mTLS gateway:
Base path and operations are copied from the service's native interface; `protocol` defaults from the template name and
`policy` to `NONE`. Because the entries are part of the configuration, they survive registry restarts (the registry revokes
its own instances on shutdown). The Java ServiceRegistry of `ah5-core-java-spring` supports the same property.
### Differences from the Java reference
- No MQTT API.
- JSON-file persistence instead of MySQL.
- Empty metadata is omitted from responses; whole-number floats lose `.0` after a persistence round trip.
- Lookup filters evaluate each interface independently (the Java code leaks policy/template/property
  flags between interfaces, treated as a bug); the address-type filter is applied independently of
  property requirements; mandatory template properties are checked uniformly.
- `addressType` in management queries is case-insensitive.
- String-set properties keep input order with duplicates removed.
- Outbound calls to ConsumerAuthorization and Blacklist services are best-effort implementations of
  the documented `verify`/`check` operations and have not been tested against the real services.
  Identity login and verification are verified in the Arrowhead test cloud (`Arrowhead-test-cloud-go`).
- Tested with unit/httptest integration tests and in the Go test cloud.
- The parent Go core bundle additionally tests a temporary native registry process with the new
  Go orchestration and authorization implementations. Management authorization now sends the
  v5.2.2 `targetType` / `target` / `scope` fields and registered operation names; outsourced tokens
  grant sysop access only when Authentication returns `sysop=true`, not merely for a `Sysop` name.
### Detailed implementation evidence
0 native Go SysDD document(s) are included in the appendices. Their protocol and storage descriptions apply to that implementation only. Current primary-folder ownership and current listening-port notes above supersede historical paths or example ports in those retained documents.
## Rust implementation
Primary folder: Eclipse-Arrowhead-rust/ah5-core-rust/serviceregistry.
### ServiceRegistry (Rust)
Arrowhead 5.2.2 core system. `src/main.rs` is built as binary `serviceregistry` by the `arrowhead-core-rust` crate in `..`. `serviceregistry.json` and `documentation/` link to `../examples` and `../documentation/ServiceRegistry`.
### Detailed implementation evidence
1 native Rust SysDD document(s) are included in the appendices. Their protocol and storage descriptions apply to that implementation only. Current primary-folder ownership and current listening-port notes above supersede historical paths or example ports in those retained documents.
### Build and state ownership
Core entry points are built by arrowhead-core-rust; the ordinary support entry points are built by arrowhead-support-rust using the shared support runtime. LocalCloudManager has its own crate. Per-system documentation links may point into the shared documentation catalog. Independent identity and state configuration is still required. Read the native appendix for persistence and restart semantics.
## Cross-language qualification
Identical system names or HTTP paths do not establish interoperability. For the selected implementation, verify registered service versions and metadata, method/path and payload models, null and optional-field handling, success/error bodies, authentication and per-operation authorization, timeout/retry behavior and persistence across restart. Java MQTT/AMQP descriptions apply to a port only where its own implementation evidence explicitly supports them. Missing transports must not be advertised.
State conversion from Java SQL to native snapshots, secret provisioning, backup/restore and rollback require implementation-specific procedures. No data migration, deployment or cloud reconfiguration was performed for this document set.
## Detailed appendices and provenance
- Java: Eclipse-Arrowhead-java/ah5-core-java-spring/serviceregistry/documentation/v5.2.2/ServiceRegistry-v5.2.2-SysDD.tex
- Rust: Eclipse-Arrowhead-rust/ah5-core-rust/documentation/ServiceRegistry/ServiceRegistry-SysDD.tex
The package sources/ folder retains the selected LaTeX, figures, bibliography, language README/build evidence and source snapshots for newly described contracts. source-manifest.json records SHA-256 hashes of original inputs. build-report.json records whether an appendix used an existing newer PDF or was compiled from source.
