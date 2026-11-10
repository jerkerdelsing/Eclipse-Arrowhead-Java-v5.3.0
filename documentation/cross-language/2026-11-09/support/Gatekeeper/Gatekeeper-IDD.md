# Gatekeeper
## Interface Design Description
Eclipse Arrowhead 5.2.2 | support system | Revision 2026-11-09 | REVIEW DRAFT
## Purpose and document structure
This document consolidates the system interface designs. Detailed Java service/transport IDDs are retained as separate appendices, followed by available Go and Rust interface descriptions.
The current cross-language sections below precede the detailed implementation appendices. Appendix dates and release labels belong to the retained baseline, not to a newly qualified release. Existing descriptions have been assembled from the primary trees; no live interoperability or semantic-parity claim is made.
## Java implementation
Primary folder: Eclipse-Arrowhead-java/ah5-Gatekeeper-java-spring.
4 detailed Java IDD source document(s) are included in the appendices. Their architecture, operation parameters, models, transport bindings, access rules and errors remain associated with Java. They must not be read as automatically implemented by the ports.
## Go implementation
Primary folder: Eclipse-Arrowhead-go/ah5-Gatekeeper-go.
### Gatekeeper — Go 5.2.2
Standalone support system, reorganized on 2026-10-28 following the Eclipse-Arrowhead-java distribution layout.
Build and operate from this folder; no original Arrowhead checkout or sibling source module is required.
The executable is `bin/Gatekeeper`. Entry point: `./cmd/gatekeeper`. Module: `arrowhead/ah5-support-go/gatekeeper`. See [DEPLOYMENT.md](DEPLOYMENT.md) for configuration and standalone container use. Existing compatibility limits remain in the copied upstream documentation and [UPSTREAM-README.md](UPSTREAM-README.md).
The dedicated launcher selects only this support-system role. The formerly shared internal runtime and regression tests are copied locally, not linked from another tree. Files for common helpers and other route implementations remain in that private runtime; this packaging does not claim new functionality or Java parity.
Documentation and license notices are included in this folder. Source provenance and import-only rewrites are listed in the distribution's SOURCE-MANIFEST.json. No running cloud or upstream source was changed.
### Listening port
Built-in default **127.0.0.1:8449** (Arrowhead default). The shipped `config.json`, `compose.yaml` and Dockerfile use the same port on all interfaces (`"listen": "0.0.0.0:8449"`; 8540–8555 layout before 2026-11-09). Override with `"listen"` in the configuration file, `SERVER_PORT=N` or `-server.port N`; the port overrides keep the configured host.
See [LISTENING-PORTS.md](../LISTENING-PORTS.md) for precedence and all systems.
### Documented behavior and remaining work
| Gatekeeper | Native Go legacy-mTLS cloud discovery/negotiation engine adapted to this runtime | AH-GT-1 encryption, complete current Java edge-case parity, real peer/broker/ACL qualification |
### Detailed implementation evidence
1 native Go IDD document(s) are included in the appendices. Their protocol and storage descriptions apply to that implementation only. Current primary-folder ownership and current listening-port notes above supersede historical paths or example ports in those retained documents.
## Rust implementation
Primary folder: Eclipse-Arrowhead-rust/ah5-Gatekeeper-rust.
### Gatekeeper (Rust)
Arrowhead 5.2.2 Gatekeeper support system. Entry point `src/main.rs` is built as binary `gatekeeper` by the shared crate in [`../ah5-support-runtime-rust`](../ah5-support-runtime-rust) (`arrowhead-support-rust`).
- `examples/gatekeeper.json` — example configuration (master copy: `../ah5-support-runtime-rust/examples/`)
- `documentation/` — SysD/SysDD/IDD/SD sources (symlink into `../ah5-support-runtime-rust/documentation/Gatekeeper`, where the contract generator writes)
Run: `cargo run -p arrowhead-support-rust --bin gatekeeper -- <config.json>`
### Listening port
Default **127.0.0.1:8449** (Arrowhead default, role `gatekeeper`). Override with `"listen": "host:port"` in the configuration, `SERVER_PORT=N` or `--server.port N`; the port overrides keep the configured host.
See [LISTENING-PORTS.md](../LISTENING-PORTS.md) for precedence and all systems.
### Documented behavior and remaining work
| Gatekeeper | Neighbour/relay administration, actual MQTT global discovery and negotiation with explicit cloud/organisation bindings | Exhaustive DTO/filter/failure parity and independent review |
### Detailed implementation evidence
1 native Rust IDD document(s) are included in the appendices. Their protocol and storage descriptions apply to that implementation only. Current primary-folder ownership and current listening-port notes above supersede historical paths or example ports in those retained documents.
### Build and state ownership
Core entry points are built by arrowhead-core-rust; the ordinary support entry points are built by arrowhead-support-rust using the shared support runtime. LocalCloudManager has its own crate. Per-system documentation links may point into the shared documentation catalog. Independent identity and state configuration is still required. Read the native appendix for persistence and restart semantics.
## Cross-language qualification
Identical system names or HTTP paths do not establish interoperability. For the selected implementation, verify registered service versions and metadata, method/path and payload models, null and optional-field handling, success/error bodies, authentication and per-operation authorization, timeout/retry behavior and persistence across restart. Java MQTT/AMQP descriptions apply to a port only where its own implementation evidence explicitly supports them. Missing transports must not be advertised.
State conversion from Java SQL to native snapshots, secret provisioning, backup/restore and rollback require implementation-specific procedures. No data migration, deployment or cloud reconfiguration was performed for this document set.
## Detailed appendices and provenance
- Java: Eclipse-Arrowhead-java/ah5-Gatekeeper-java-spring/documentation/v5.2.2/Gatekeeper-v5.2.2-gatekeeperManagement-HTTP-IDD.tex
- Java: Eclipse-Arrowhead-java/ah5-Gatekeeper-java-spring/documentation/v5.2.2/Gatekeeper-v5.2.2-globalServiceDiscovery-HTTP-IDD.tex
- Java: Eclipse-Arrowhead-java/ah5-Gatekeeper-java-spring/documentation/v5.2.2/Gatekeeper-v5.2.2-interCloudNegotiation-HTTP-IDD.tex
- Java: Eclipse-Arrowhead-java/ah5-Gatekeeper-java-spring/documentation/v5.2.2/Gatekeeper-v5.2.2-sysMonitor-AMQP-IDD.tex
- Go: Eclipse-Arrowhead-go/ah5-Gatekeeper-go/documentation/Gatekeeper/Gatekeeper-HTTPS-IDD.tex
- Rust: Eclipse-Arrowhead-rust/ah5-support-runtime-rust/documentation/Gatekeeper/Gatekeeper-HTTPS-IDD.tex
The package sources/ folder retains the selected LaTeX, figures, bibliography, language README/build evidence and source snapshots for newly described contracts. source-manifest.json records SHA-256 hashes of original inputs. build-report.json records whether an appendix used an existing newer PDF or was compiled from source.
