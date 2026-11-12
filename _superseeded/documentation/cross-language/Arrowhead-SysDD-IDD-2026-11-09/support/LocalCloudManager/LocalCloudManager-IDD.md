# LocalCloudManager
## Interface Design Description
Eclipse Arrowhead 5.2.2 | support system | Revision 2026-11-09 | REVIEW DRAFT
## Purpose and document structure
This document consolidates the system interface designs. Detailed Java service/transport IDDs are retained as separate appendices, followed by available Go and Rust interface descriptions.
The current cross-language sections below precede the detailed implementation appendices. Appendix dates and release labels belong to the retained baseline, not to a newly qualified release. Existing descriptions have been assembled from the primary trees; no live interoperability or semantic-parity claim is made.
## Java implementation
Primary folder: Eclipse-Arrowhead-java/ah5-LocalCloudManager-java-spring.
2 detailed Java IDD source document(s) are included in the appendices. Their architecture, operation parameters, models, transport bindings, access rules and errors remain associated with Java. They must not be read as automatically implemented by the ports.
## Go implementation
Primary folder: Eclipse-Arrowhead-go/ah5-LocalCloudManager-go.
### LocalCloudManager — Go 5.2.2
Standalone support system, reorganized on 2026-10-28 following the Eclipse-Arrowhead-java distribution layout.
Build and operate from this folder; no original Arrowhead checkout or sibling source module is required.
The executable is `bin/LocalCloudManager`. Entry point: `./cmd/localcloudmanager`. Module: `arrowhead/ah5-support-go/localcloudmanager`. See [DEPLOYMENT.md](DEPLOYMENT.md) for configuration and standalone container use. Existing compatibility limits remain in the copied upstream documentation and [UPSTREAM-README.md](UPSTREAM-README.md).
Existing role selection and API behavior are retained.
Documentation and license notices are included in this folder. Source provenance and import-only rewrites are listed in the distribution's SOURCE-MANIFEST.json. No running cloud or upstream source was changed.
See [Graphical orchestration and security editor](GRAPHICAL-ORCHESTRATION.md) for local rule and policy editing.
### Listening port
Default **8480** on all interfaces. Override with `SERVER_PORT=N` (or `PORT=N`) or `--server.port N`.
See [LISTENING-PORTS.md](../LISTENING-PORTS.md) for precedence and all systems.
### Detailed implementation evidence
3 native Go IDD document(s) are included in the appendices. Their protocol and storage descriptions apply to that implementation only. Current primary-folder ownership and current listening-port notes above supersede historical paths or example ports in those retained documents.
## Rust implementation
Primary folder: Eclipse-Arrowhead-rust/ah5-LocalCloudManager-rust.
### Rust LocalCloudManager
### Current implementation — secure Rust candidate cloud (2026-10-17)
The `LocalCloudManager` executable now uses `src/secure_main.rs` and `src/secure.rs`, **not the historical HTTP launcher/router described below**. It requires `--config CONFIG.json`. Automatic legacy binary discovery is disabled. The manager depends on the parent Rust support library for verified TLS, expected-provider CN/profile pinning, certificate-bound identity login/token refresh and bounded HTTPS requests.
Deployed console: **https://127.0.0.1:10590/**. System API: **https://127.0.0.1:10520/localcloud/** (mandatory mTLS). Deployment: `Arrowhead-test-cloud-rust/candidate-native/`, project `arrowhead-rust-candidate`. The older Rust cloud remains separate. During final verification it was recreated by a separate deployment change and its UI remapped from8590to10490; this work did not modify that project.
The browser console uses HTTPS plus a private-password login and Secure/HttpOnly/SameSite cookie. It does not require a browser client certificate. The candidate CA is private, so browsers may show a certificate warning; no global trust store was changed. Password is in `candidate-native/secrets/localcloudmanager/console-password` (private file; never logged). The manager's system listener requires a verified local sy/op certificate and a matching verified sysop identity token for management. Echo is certificate-only.
See [docs/SECURE-DEPLOYMENT.md](docs/SECURE-DEPLOYMENT.md) for the current configuration, service contracts, permission boundaries, test evidence and limitations. The manager now makes real revision-checked SysMonitor subscription writes, forwards plant POST/PUT/PATCH/DELETE correctly, manages simple-store rules, reads diagnostics/historian data and uses a scoped Rust Docker gateway rather than receiving a Docker socket.
The Markdown below, the relocation source manifest, the earlier generated LaTeX/PDF catalogue and `docs/legacy/` record the **superseded relocation snapshot**, not the active deployed secure contract. They do not supersede the new secure deployment documentation. Full Java equivalence, telemetry broker publishing and the cloud's missing federation/onboarding capabilities are still not claimed.
### Historical relocation record (superseded runtime)
Canonical relocated source: `ah5-support-rust/LocalCloudManager`. Target system version5.2.2; associated services `localCloudManagement` and legacy `monitor`, both1.0.0. The latest located cloud/workspace manager sources matched SHA256 `c3150daa4dd91e854439e31626895c5ff86e77881c338c5281306bda49f44d6b` before relocation.
This is a source/documentation relocation, **not a Java-parity upgrade or secure deployment**. The existing `Arrowhead-test-cloud-rust/core-systems` copy remains a deployment compatibility snapshot because other roles share its runtime and build context. No live source, cloud, container, key or state was removed or restarted. Develop against this canonical package instead of editing both copies independently.
### Critical boundary
The inherited manager is a development HTTP adapter: **no native inbound TLS, no configured mTLS provider identity and no native AMQP sysMonitor publisher**. It sends/accepts legacy `Bearer SYSTEM//...` identities in its shared runtime. Those are not cryptographic authentication. Do not expose this package directly or assume it meets the newer Rust support runtime's certificate-authenticated contract. A TLS proxy alone does not correct legacy provider authentication. See COMPATIBILITY.md before use.
The dedicated launcher defaults to `http://127.0.0.1:8480/` instead of the inherited all-interface bind. This is a packaging safeguard, not a complete security fix. Historical UI8590/API8580 mappings belong to cloud deployment forwarding and are not created here.
### Build and source layout
This is an independent nested Cargo package, with its own Cargo.toml/lock and workspace boundary. Root Makefile build/test/check targets explicitly include it. Root `make localcloudmanager` builds it; `make localcloudmanager-docs` generates/compiles its documentation. The generic parent `arrowhead-support` launcher does not select this legacy manager profile.
`src/main.rs` is the dedicated launcher, rejecting roles other than localcloudmanager without mutating process environment. `src/localcloudmanager.rs` retains the existing Axum console handlers and compile-time embedded HTML/CSS/JavaScript in `src/ui`. `api.rs`, `registry.rs` and `state.rs` are the inherited shared runtime dependencies; retaining them avoids a dependency on the old checkout. Other role implementations remain source dependencies and are not selected by this executable. No Java/Go/Node process implements the manager.
Original manager business logic and UI are retained; deliberate packaging changes are the crate name/workspace boundary, library exports, role-default configuration helper, dedicated launcher and loopback default. Additional tests, documentation and source-provenance records are provided. The legacy `src/ui.html` remains an unused historical asset; active UI uses src/ui/index.html/app.js/styles.css.
### Configuration
| Environment | Actual source behavior |
|---|---|
| `ARROWHEAD_ROLE` | Omit or use localcloudmanager; other values rejected by dedicated launcher |
| `SYSTEM_NAME`, `DOMAIN_NAME` | Default LocalCloudManager/localcloudmanager; used as declared identity/advertised address, not validated certificate identities |
| `SERVER_PORT`, `LISTEN_ADDRESS` | Default8480 and loopback; explicit listen address can override it, but exposes inherited insecure transport |
| `DATA_FILE` | Independent JSON state, default /data/state.json; use a private dedicated path, never existing active cloud state |
| `SERVICE_REGISTRY_URL`, `AUTHENTICATION_URL` | Inherited HTTP defaults; HTTPS URLs alone do not provision a client certificate, expected-CN pin or identity-token login |
| `DATAMANAGER_URL`, `SIMPLE_ORCHESTRATION_URL`, `SYSMONITOR_URL`, `PLANT_DESCRIPTION_ENGINE_URL` | Provider adapter bases; shapes must be qualified against actual deployed providers |
| `MANAGEMENT_WHITELIST`, `TOKEN_SECRET` | Shared legacy runtime settings, not the console session boundary or proof of secure identity |
| `CONSOLE_PASSWORD_HASH` | scrypt:base64salt:base64hash, N16384/r8/p1; salt>=16bytes/hash>=32bytes; missing or invalid value returns503for console APIs |
| `CONSOLE_ALLOWED_HOSTS` | Host-name allowlist, default127.0.0.1,localhost,[::1]; ports are ignored |
| `CONSOLE_SESSION_SECONDS` | Default28800; positive bounded values should be provisioned; inherited implementation lacks robust range validation |
| `DOCKER_SOCKET`, `COMPOSE_PROJECT_NAME` | Unix Docker socket and project filter; static source allowlist controls lifecycle; MANAGED_SERVICES is not used |
Missing password fails closed for `/api/` but does not disable static assets, health or legacy Arrowhead routes. Sessions and history reset on restart. Keep this process in an isolated development environment with explicit trust boundaries.
### Documentation and provenance
`../documentation/LocalCloudManager/v5.2.2/` contains SysD, SysDD, two SDs and two HTTP IDDs, plus a combined catalogue. No sysMonitor AMQP SD/IDD is fabricated for an unimplemented publisher. The revised SysML variability model in docs/LocalCloudManager.sysml records Java-cloud, Go and Rust variants and the actual Rust qualification boundary. Historical Markdown/SysML is preserved unchanged under docs/legacy, not endorsed as evidence of interchangeability.
Run from this module. The original source defaults to ../../Arrowhead-test-cloud-rust/core-systems for read-only source hashing; override REFERENCE when needed. `docs/source-manifest.json` records original and relocated hashes plus the route inventory. Read VERIFICATION.md for actual test/build evidence. The repository's original Arrowhead LaTeX class/logo is reused.
### Listening port
System API default **127.0.0.1:8480**. Override with `"listen"` in CONFIG.json, `SERVER_PORT=N` or `--server.port N`. The HTTPS console listener (`"consoleListen"`) is configured separately.
See [LISTENING-PORTS.md](../LISTENING-PORTS.md) for precedence and all systems.
### Detailed implementation evidence
2 native Rust IDD document(s) are included in the appendices. Their protocol and storage descriptions apply to that implementation only. Current primary-folder ownership and current listening-port notes above supersede historical paths or example ports in those retained documents.
### Build and state ownership
Core entry points are built by arrowhead-core-rust; the ordinary support entry points are built by arrowhead-support-rust using the shared support runtime. LocalCloudManager has its own crate. Per-system documentation links may point into the shared documentation catalog. Independent identity and state configuration is still required. Read the native appendix for persistence and restart semantics.
## Cross-language qualification
Identical system names or HTTP paths do not establish interoperability. For the selected implementation, verify registered service versions and metadata, method/path and payload models, null and optional-field handling, success/error bodies, authentication and per-operation authorization, timeout/retry behavior and persistence across restart. Java MQTT/AMQP descriptions apply to a port only where its own implementation evidence explicitly supports them. Missing transports must not be advertised.
State conversion from Java SQL to native snapshots, secret provisioning, backup/restore and rollback require implementation-specific procedures. No data migration, deployment or cloud reconfiguration was performed for this document set.
## Detailed appendices and provenance
- Java: Eclipse-Arrowhead-java/ah5-LocalCloudManager-java-spring/documentation/v5.2.2/LocalCloudManager-v5.2.2-localCloudManagement-HTTP-IDD.tex
- Java: Eclipse-Arrowhead-java/ah5-LocalCloudManager-java-spring/documentation/v5.2.2/LocalCloudManager-v5.2.2-sysMonitor-AMQP-IDD.tex
- Go: Eclipse-Arrowhead-go/ah5-LocalCloudManager-go/documentation/LocalCloudManager/v5.2.2/LocalCloudManager-Go-v5.2.2-localCloudManagement-HTTPS-IDD.tex
- Go: Eclipse-Arrowhead-go/ah5-LocalCloudManager-go/documentation/LocalCloudManager/v5.2.2/LocalCloudManager-Go-v5.2.2-monitor-HTTPS-IDD.tex
- Go: Eclipse-Arrowhead-go/ah5-LocalCloudManager-go/documentation/LocalCloudManager/v5.2.2/LocalCloudManager-Go-v5.2.2-sysMonitor-AMQP-IDD.tex
- Rust: Eclipse-Arrowhead-rust/ah5-support-runtime-rust/documentation/LocalCloudManager/v5.2.2/LocalCloudManager-Rust-v5.2.2-localCloudManagement-HTTP-IDD.tex
- Rust: Eclipse-Arrowhead-rust/ah5-support-runtime-rust/documentation/LocalCloudManager/v5.2.2/LocalCloudManager-Rust-v5.2.2-monitor-HTTP-IDD.tex
The package sources/ folder retains the selected LaTeX, figures, bibliography, language README/build evidence and source snapshots for newly described contracts. source-manifest.json records SHA-256 hashes of original inputs. build-report.json records whether an appendix used an existing newer PDF or was compiled from source.
