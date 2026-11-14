# Eclipse Arrowhead 5.3.0 — Java reference implementation

The Java-Spring implementation of the Eclipse Arrowhead 5 framework: the five core systems, twenty support systems
(one of them legacy), the shared common library, the system and interface documentation, the SysML profile and library, and the tools that
verify them. It extends the official Java-Spring 5.2.2 release (five core systems, three support systems) into a
complete local-cloud and System-of-LocalClouds environment.

> **Primary source:** this is the primary Java implementation tree of its Eclipse workspace, designated on
> 2026-10-30 (workspace policy `PRIMARY-SOURCES.md`, one level above this repository). The Go and Rust
> implementations live in the sibling trees `Eclipse-Arrowhead-go` and `Eclipse-Arrowhead-rust`.

**Versions.** The primary Java framework constant, common/Core Maven versions and support application versions
are aligned to the 5.3.0 release candidate. Independent service versions and wire-contract identifiers retain their
own values; for example, `SysMonitor-AMQP-IDD-5.2.2` is unchanged. Older build/deployment records do not by themselves
qualify the aligned artifacts (see [release notes](releases/5.3.0.md)).

## Core systems

[`ah5-core-java-spring/`](ah5-core-java-spring/README.md) is one Maven reactor with a module per system.

| System | Module | Default port |
|---|---|---|
| ServiceRegistry | `serviceregistry` | 8443 |
| Authentication | `authentication` | 8444 |
| ConsumerAuthorization | `consumer-authorization` | 8445 |
| DynamicServiceOrchestration | `serviceorchestration-dynamic` | 8441 |
| SimpleStoreServiceOrchestration | `serviceorchestration-simple` | 8442 |

[`ah5-common-java-spring/`](ah5-common-java-spring/README.md) holds the shared library (`common-utils`,
`data-transfer-objects`): system lifecycle and registration, the security filter chain, the secure-profile guard,
`generalManagement` and the native `sysMonitor` publisher.

## Support systems

Each support system is a self-contained folder with its own source, Maven build, Dockerfile, configuration,
`compose.yaml` and documentation; deployment instructions are in `DEPLOYMENT.md` or, for GraphDBManager, its README. All but AIGym compile a private copy of the common library under
`vendor/`, so no folder depends on a sibling at build time.

| System | Folder | Default port | Purpose |
|---|---|---|---|
| AIGym | [ah5-AIGym-java-spring](ah5-AIGym-java-spring/README.md) | 8478 | model sandbox for AI clients |
| Blacklist | [ah5-Blacklist-java-spring](ah5-Blacklist-java-spring/README.md) | 8464 | blocks systems from using the cloud |
| CertificateAuthority | [ah5-CertificateAuthority-java-spring](ah5-CertificateAuthority-java-spring/README.md) | 8448 | issues and revokes the cloud's X.509 certificates, publishes the CRL |
| Choreographer | [ah5-Choreographer-java-spring](ah5-Choreographer-java-spring/README.md) | 8457 | executes workflow plans step by step through executor systems |
| Configuration | [ah5-Configuration-java-spring](ah5-Configuration-java-spring/README.md) | 8459 | central configuration files, fetched by each system at run time |
| DataManager | [ah5-DataManager-java-spring](ah5-DataManager-java-spring/README.md) | 8461 | caches and stores SenML sensor and actuator data |
| DeviceQoSEvaluator | [ah5-DeviceQoSEvaluator-java-spring](ah5-DeviceQoSEvaluator-java-spring/README.md) | 8472 | collects and evaluates device quality-of-service measurements |
| EventHandler | [ah5-EventHandler-java-spring](ah5-EventHandler-java-spring/README.md) | 8455 | publish-subscribe messaging within the cloud |
| Gatekeeper | [ah5-Gatekeeper-java-spring](ah5-Gatekeeper-java-spring/README.md) | 8449 | inter-cloud service discovery and negotiation over the relay |
| GateTunnel | [ah5-GateTunnel-java-spring](ah5-GateTunnel-java-spring/README.md) | 8453 | relay sessions that carry inter-cloud service traffic |
| GraphDBManager | [ah5-GraphDBManager-java-spring](ah5-GraphDBManager-java-spring/README.md) | 8467 | DataManager variant with its history in an RDF triple store |
| LocalCloudConfigurator | [ah5-LocalCloudConfigurator-java](ah5-LocalCloudConfigurator-java/README.md) | 8492 | catalog-based design and deployment of one local cloud |
| LocalCloudManager | [ah5-LocalCloudManager-java-spring](ah5-LocalCloudManager-java-spring/README.md) | 8491 | operator console of a local cloud |
| Onboarding | [ah5-Onboarding-java-spring](ah5-Onboarding-java-spring/README.md) | 8435 | certificate-based onboarding of devices and their systems |
| PlantDescriptionEngine | [ah5-PlantDescriptionEngine-java-spring](ah5-PlantDescriptionEngine-java-spring/README.md) | 8432 | keeps orchestration consistent with an engineered plant description |
| SoLocalCloudConfigurator | [ah5-SoLocalCloudConfigurator-java](ah5-SoLocalCloudConfigurator-java/README.md) | 8496 | designs Systems of LocalClouds and builds their deployment packages |
| SoSConfigurator (legacy) | [ah5-SoSConfigurator-java](ah5-SoSConfigurator-java/README.md) | 8494 | earlier System-of-Systems configurator, superseded by SoLocalCloudConfigurator |
| SysMonitor | [ah5-SysMonitor-java-spring](ah5-SysMonitor-java-spring/README.md) | 8475 | collects, stores and republishes the systems' `sysMonitor` observations |
| TimeManager | [ah5-TimeManager-java-spring](ah5-TimeManager-java-spring/README.md) | 8463 | cloud time, time zone and whether the time can be trusted |
| TranslationManager | [ah5-TranslationManager-java-spring](ah5-TranslationManager-java-spring/README.md) | 8465 | automatic interface and data-model translation |

The ports are the packaged defaults; every system's listening port is configurable (`server.port`, `SERVER_PORT`,
`--server.port`). [LISTENING-PORTS.md](LISTENING-PORTS.md) lists the precedence, the Go and Rust defaults and the
known deployment differences.

### Local clouds and Systems of LocalClouds

- **One local cloud:** [LocalCloudConfigurator](ah5-LocalCloudConfigurator-java/README.md) and the LocalCloudManager
  **LC** view give catalog-based graphical cloud composition, saved designs and reviewed deployment workflows.
- **Several federated clouds:** [SoLocalCloudConfigurator](ah5-SoLocalCloudConfigurator-java/README.md) and the
  LocalCloudManager **SoLC** view ([guide](ah5-LocalCloudManager-java-spring/SOLC-CONFIGURATION.md)) compose member
  clouds from stored local-cloud designs, add inter-cloud infrastructure and exchanges, validate them against the
  organisation policy and build deployment packages for other hardware.

## Build and deploy

Requirements: Java 21 and Maven, or Docker (the build image is `maven:3.9-eclipse-temurin-21`).

```sh
cd ah5-<System>-java-spring
make build      # mvn -B clean verify: compiles and runs the unit tests
make image      # docker compose build
make deploy     # docker compose up -d --build
```

The core reactor, the common library and SoLocalCloudConfigurator have no Makefile: build them with
`mvn -B clean verify` in their folder. The Makefiles of LocalCloudManager, LocalCloudConfigurator and SoSConfigurator
run Maven in the build image, so they need only Docker; the support systems' Dockerfiles also build and test inside
the image.

A system needs, from the cloud it joins: its own certificate and identity, the cloud trust store and CRL, database
secrets and ConsumerAuthorization grants. Each folder's deployment guide (GraphDBManager: README) lists them, and
[DEPLOYMENT-GUIDE.md](DEPLOYMENT-GUIDE.md) describes selecting, provisioning and deploying individual systems.

Complete clouds built from this tree are in the separate repository `Arrowhead-test-clouds-v5.3.0` (Java, Go and
Rust test clouds).

## Security

Mutual TLS, certificate-bound identity tokens and operation-level ConsumerAuthorization are the default. Every system
built on the common library refuses to start with a configuration that disables them; development operation needs an
explicit exception. See [SECURITY-PROFILE.md](SECURITY-PROFILE.md).

## Documentation and models

| Path | Content |
|---|---|
| `ah5-*/documentation/v5.3.0/` | per-system SysD, SysDD, SD and IDD (LaTeX sources and PDFs); `v5.2.2/` holds the retained baseline |
| [documentation/](documentation/Documentation.md) | edition-wide index, publication record, validation evidence and links to archived compilations |
| `5.3 SoS documentation/` | Arrowhead SoS architecture (black-box and white-box models, SoS tests), foundational principles, and the standards the framework builds on |
| `profile-library-sysml/` | Arrowhead SysML profile and library: SysML v1.6 (Eclipse Papyrus: metamodel and profile, DSL update site, Eclipse Arrowhead 5.0 library) and SysML v2 models of the systems |
| [releases/5.3.0.md](releases/5.3.0.md) | draft release notes against the official 5.2.2 release |
| [releases/5.4.0.md](releases/5.4.0.md) | release notes of the version in development |
| [CHANGELOG.md](CHANGELOG.md) | index of releases and their branches and tags |

## Verification

| Path | Content |
|---|---|
| [verification/tools/](verification/tools/README.md) | current static/release checks and limitations of the retained October build/deployment/console fixtures |
| `verification/` | inventories, checksums and results of the support-system verification of 25 October 2026 |
| [JAVA-SUPPORT-FOLDERS.md](JAVA-SUPPORT-FOLDERS.md) | the current support-system inventory and links to dated test evidence |
| [ALL-JAVA-SUPPORT-VERIFICATION-2026-10-25.md](_superseeded/ALL-JAVA-SUPPORT-VERIFICATION-2026-10-25.md), [INDEPENDENT-SUPPORT-VERIFICATION-2026-10-25.md](_superseeded/INDEPENDENT-SUPPORT-VERIFICATION-2026-10-25.md) | historical verification reports, retained in `_superseeded/` |

```sh
python3 verification/tools/verify_reference_conformance.py
python3 verification/tools/verify_listening_ports.py
```

## Licence

Eclipse Public License 2.0, see [LICENSE](LICENSE).
