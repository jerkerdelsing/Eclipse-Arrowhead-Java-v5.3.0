# DynamicServiceOrchestration
## System Design Description
Eclipse Arrowhead 5.2.2 | core system | Revision 2026-11-09 | REVIEW DRAFT
## Purpose and document structure
This document describes implementation ownership, architecture, persistence, security and operational boundaries.
The current cross-language sections below precede the detailed implementation appendices. Appendix dates and release labels belong to the retained baseline, not to a newly qualified release. Existing descriptions have been assembled from the primary trees; no live interoperability or semantic-parity claim is made.
## Java implementation
Primary folder: Eclipse-Arrowhead-java/ah5-core-java-spring/serviceorchestration-dynamic.
1 detailed Java SysDD source document(s) are included in the appendices. Their architecture, operation parameters, models, transport bindings, access rules and errors remain associated with Java. They must not be read as automatically implemented by the ports.
## Go implementation
Primary folder: Eclipse-Arrowhead-go/ah5-core-go/serviceorchestration-dynamic.
### DynamicServiceOrchestration — Go 5.2.2
Standalone core system, reorganized on 2026-10-28 following the Eclipse-Arrowhead-java distribution layout.
Build and operate from this folder; no original Arrowhead checkout or sibling source module is required.
The executable is `bin/DynamicServiceOrchestration`. Entry point: `./cmd/serviceorchestration-dynamic`. Module: `arrowhead/ah5-core-go/serviceorchestration-dynamic`. See [DEPLOYMENT.md](DEPLOYMENT.md) for configuration and standalone container use. Existing compatibility limits remain in the copied upstream documentation and [UPSTREAM-README.md](UPSTREAM-README.md).
Existing role selection and API behavior are retained.
Documentation and license notices are included in this folder. Source provenance and import-only rewrites are listed in the distribution's SOURCE-MANIFEST.json. No running cloud or upstream source was changed.
### Listening port
Built-in default **127.0.0.1:8441** (Arrowhead default). The shipped `config.json` sets `"listen": "0.0.0.0:8441"`. Override with `"listen"` in the configuration file, `SERVER_PORT=N` or `-server.port N`; the port overrides keep the configured host.
See [LISTENING-PORTS.md](../../LISTENING-PORTS.md) for precedence and all systems.
### Detailed implementation evidence
1 native Go SysDD document(s) are included in the appendices. Their protocol and storage descriptions apply to that implementation only. Current primary-folder ownership and current listening-port notes above supersede historical paths or example ports in those retained documents.
## Rust implementation
Primary folder: Eclipse-Arrowhead-rust/ah5-core-rust/serviceorchestration-dynamic.
### DynamicServiceOrchestration (Rust)
Arrowhead 5.2.2 core system. `src/main.rs` is built as binary `serviceorchestration-dynamic` by the `arrowhead-core-rust` crate in `..`. `serviceorchestration-dynamic.json` and `documentation/` link to `../examples` and `../documentation/DynamicServiceOrchestration`.
### Detailed implementation evidence
1 native Rust SysDD document(s) are included in the appendices. Their protocol and storage descriptions apply to that implementation only. Current primary-folder ownership and current listening-port notes above supersede historical paths or example ports in those retained documents.
### Build and state ownership
Core entry points are built by arrowhead-core-rust; the ordinary support entry points are built by arrowhead-support-rust using the shared support runtime. LocalCloudManager has its own crate. Per-system documentation links may point into the shared documentation catalog. Independent identity and state configuration is still required. Read the native appendix for persistence and restart semantics.
## Cross-language qualification
Identical system names or HTTP paths do not establish interoperability. For the selected implementation, verify registered service versions and metadata, method/path and payload models, null and optional-field handling, success/error bodies, authentication and per-operation authorization, timeout/retry behavior and persistence across restart. Java MQTT/AMQP descriptions apply to a port only where its own implementation evidence explicitly supports them. Missing transports must not be advertised.
State conversion from Java SQL to native snapshots, secret provisioning, backup/restore and rollback require implementation-specific procedures. No data migration, deployment or cloud reconfiguration was performed for this document set.
## Detailed appendices and provenance
- Java: Eclipse-Arrowhead-java/ah5-core-java-spring/serviceorchestration-dynamic/documentation/v5.2.2/DynamicServiceOrchestration-v5.2.2-SysDD.tex
- Go: Eclipse-Arrowhead-go/ah5-core-go/serviceorchestration-dynamic/documentation/service-design/DynamicServiceOrchestration-SysDD.tex
- Rust: Eclipse-Arrowhead-rust/ah5-core-rust/documentation/DynamicServiceOrchestration/DynamicServiceOrchestration-SysDD.tex
The package sources/ folder retains the selected LaTeX, figures, bibliography, language README/build evidence and source snapshots for newly described contracts. source-manifest.json records SHA-256 hashes of original inputs. build-report.json records whether an appendix used an existing newer PDF or was compiled from source.
