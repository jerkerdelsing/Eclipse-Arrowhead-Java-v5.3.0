# Independent Java support systems - verification, 25 October 2026

> Superseded for the complete inventory by [the all-system verification report](ALL-JAVA-SUPPORT-VERIFICATION-2026-10-25.md). This earlier report covered only the four native runtime systems.

The copied projects in Eclipse-Arrowhead now build and deploy independently from their own folders. The original Arrowhead support folders and roadmap documents were not changed. This refactors the four systems formerly implemented by JavaSupportRuntime; other copied support projects and the copied core are outside this separation.

| System | Tests passed | Own build/JAR/image | Isolated Compose startup |
|---|---:|---|---|
| CertificateAuthority | 12 | Yes | Yes; liveness check passed |
| Gatekeeper | 9 | Yes | Yes; liveness check passed |
| GateTunnel | 15 | Yes | Yes; liveness check passed |
| SysMonitor | 11 | Yes | Yes; degraded without brokers |

All 47 tests passed with zero failures/errors/skips. Each project was built with only its own folder and a populated Maven dependency cache mounted into the Java 21 Maven build container. The final project-local `scripts/verify.sh -o` runs used `--network none`; no sibling sources or parent build were available. Initial cache population used Maven Central. The Maven dependencies are normal library dependencies, not a common support runtime.

Each shaded JAR was inspected: it contains its own system implementation and fixed main class, and excludes the other three systems. The private `Messaging` copies in CA and SysMonitor exclude federation relay code. Unused MQTT client dependencies were removed per system. Shared helper code remains privately copied; security fixes must be propagated to all affected copies.

All four Dockerfiles were built from their own folder, including their JUnit tests and packaged SQLite checks. The resulting Linux ARM64 images were tested under UID 10001 with a read-only root and a non-executable temporary filesystem. SQLite native libraries are installed at image build time in `/app/lib`; this avoids runtime extraction into `/tmp`. Multi-platform images were not tested.

Each actual project-local Compose definition was validated and started with its own temporary database volume, individually mounted generated test identity/key and password, a valid generated CRL, and an isolated external bridge network. Only CA received the issuer key. No production credentials were copied or used. The test network used an explicit unused subnet because Docker's default address pools were exhausted. All disposable containers, volumes, network and generated credentials were removed after testing. Built images are retained for operator use.

The isolation test deliberately supplied no registry/authentication/DSO/brokers. CA, Gatekeeper and GateTunnel return process liveness; SysMonitor starts and correctly reports HTTP 503/degraded without its AMQP intake and MQTT output. This is a packaging/startup verification, not a fully provisioned cloud or inter-cloud acceptance test. Federation startup used explicit legacy-mTLS compatibility; AH-GT-1 cryptographic/vector tests passed in the GateTunnel suite. No new live deployment or live-cloud changes were made.

Configuration remains local to each selected project. The external cloud network, registry, service identities, grants, DSO orchestration, brokers, federation relay/peer identities and CRL distribution must be provisioned by the operator. Use a distinct Compose project name and host port for each cloud deployment. No common database or credentials bundle is mounted.

## Documentation

Updated and compiled four SysDDs, four SysDs, four AMQP publisher IDDs and the support GSoSDD (13 documents). The GSoSDD was copied with its source and assets from the roadmap into `roadmap/5.2/Arrowhead SoS architecture/WhiteBox_5.2.2`, with an independent packaging/state diagram. The per-system source, build, configuration, deployment and verification descriptions now refer to the individual projects. Inherited inaccurate insecure-mode and remote-database statements were corrected for the four native Java systems. Documents remain release candidates/drafts pending quality assurance. Final LaTeX logs contain no overfull boxes or unresolved citations/references. Rendered pages were reviewed for layout.

| Design document | Current PDF |
|---|---|
| CertificateAuthority SysDD | [PDF](../ah5-CertificateAuthority-java-spring/documentation/v5.2.2/CertificateAuthority-v5.2.2-SysDD.pdf) |
| Gatekeeper SysDD | [PDF](../ah5-Gatekeeper-java-spring/documentation/v5.2.2/Gatekeeper-v5.2.2-SysDD.pdf) |
| GateTunnel SysDD | [PDF](../ah5-GateTunnel-java-spring/documentation/v5.2.2/GateTunnel-v5.2.2-SysDD.pdf) |
| SysMonitor SysDD | [PDF](../ah5-SysMonitor-java-spring/documentation/v5.2.2/SysMonitor-v5.2.2-SysDD.pdf) |
| Support GSoSDD | [PDF](<../roadmap/5.2/Arrowhead SoS architecture/WhiteBox_5.2.2/GSoSDD_Arrowhead_Support_Systems_5.2.pdf>) |

SHA-256 checks confirmed 349 original files unchanged. See `original-source-checksums-2026-10-25.json`. Historical implementations are retained under `legacy/` in each changed project; the copied combined runtime is marked historical and is not used by the standalone projects.

Detailed machine-readable results, artifact/image identifiers, source hashes and build/container logs are in [verification/independent-support-2026-10-25](../verification/independent-support-2026-10-25/). The historical copy manifest remains an account of the initial copy and is not a checksum manifest of the newly refactored distribution.
