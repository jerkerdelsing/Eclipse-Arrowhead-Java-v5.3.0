# All Java support systems - verification, 25 October 2026

All **15 Java support systems** in the copied Eclipse-Arrowhead distribution now have their own source, build, executable JAR, application image, configuration and deployment definition. All 15 passed individual-folder offline builds and isolated deployment checks. No active support build or deployment depends on JavaSupportRuntime or a sibling support folder.

Canonical folder: `/Users/jerkerdelsing/Work/lule/EISLAB/Project/Eclipse/Eclipse-Arrowhead`. This is the existing sibling of Arrowhead; no nested Arrowhead/Eclipse-Arrowhead folder was created. The copied core systems and historical combined runtime are excluded from the support-system count. LocalCloudManager is the separate Node.js/nginx exception described below.

## Results

| System | Source | Build | JAR | Image | Config | Compose | Isolation | Tests passed |
|---|---|---|---|---|---|---|---|---:|
| [AIGym](ah5-AIGym-java-spring/DEPLOYMENT.md) | yes | yes | yes | yes | yes | yes | pass | 1 |
| [Blacklist](ah5-Blacklist-java-spring/DEPLOYMENT.md) | yes | yes | yes | yes | yes | yes | pass | 139 |
| [CertificateAuthority](ah5-CertificateAuthority-java-spring/DEPLOYMENT.md) | yes | yes | yes | yes | yes | yes | pass | 12 |
| [Choreographer](ah5-Choreographer-java-spring/DEPLOYMENT.md) | yes | yes | yes | yes | yes | yes | pass | 18 |
| [Configuration](ah5-Configuration-java-spring/DEPLOYMENT.md) | yes | yes | yes | yes | yes | yes | pass | 11 |
| [DataManager](ah5-DataManager-java-spring/DEPLOYMENT.md) | yes | yes | yes | yes | yes | yes | pass | 15 |
| [DeviceQoSEvaluator](ah5-DeviceQoSEvaluator-java-spring/DEPLOYMENT.md) | yes | yes | yes | yes | yes | yes | pass | 301 |
| [EventHandler](ah5-EventHandler-java-spring/DEPLOYMENT.md) | yes | yes | yes | yes | yes | yes | pass | 14 |
| [GateTunnel](ah5-GateTunnel-java-spring/DEPLOYMENT.md) | yes | yes | yes | yes | yes | yes | pass | 15 |
| [Gatekeeper](ah5-Gatekeeper-java-spring/DEPLOYMENT.md) | yes | yes | yes | yes | yes | yes | pass | 9 |
| [Onboarding](ah5-Onboarding-java-spring/DEPLOYMENT.md) | yes | yes | yes | yes | yes | yes | pass | 25 |
| [PlantDescriptionEngine](ah5-PlantDescriptionEngine-java-spring/DEPLOYMENT.md) | yes | yes | yes | yes | yes | yes | pass | 20 |
| [SysMonitor](ah5-SysMonitor-java-spring/DEPLOYMENT.md) | yes | yes | yes | yes | yes | yes | pass | 11 |
| [TimeManager](ah5-TimeManager-java-spring/DEPLOYMENT.md) | yes | yes | yes | yes | yes | yes | pass | 17 |
| [TranslationManager](ah5-TranslationManager-java-spring/DEPLOYMENT.md) | yes | yes | yes | yes | yes | yes | pass | 490 |

**1098 Java tests passed; zero failures, errors or skips.** All fifteen project-local verification scripts ran in a Java 21 Maven container with `--network none`. Only the selected project folder and a populated Maven cache were mounted. Initial third-party cache population used Maven repositories. No external Arrowhead common/core parent or Arrowhead common library JAR was needed for the ten newly independent Spring builds.

The original audit found that only four native Java systems had complete independent packaging. The ten other Spring systems now own private Arrowhead common/DTO source under vendor/, standalone POMs, app and private-DB Dockerfiles, secure file-based configurations, environment templates, local verification scripts, per-project networks/volumes and Compose definitions. AIGym retains its standalone Java build and gained the missing secure deployment definition. Original POMs/licenses and private-source hashes are retained for provenance.

Every current JAR was inspected for the correct system implementation/main class and absence of another support-system application or JavaSupportRuntime. Each of the fifteen application images exists locally and was deployed in the checks below. The ten database-backed projects also built their own schema image. Image IDs, artifact hashes, build/config/source hashes and per-check logs are in [the machine-readable results](verification/all-support-2026-10-25/inventory-results.json).

## What isolation verified

For each of the eleven Spring applications, Compose started only that support system and, where needed, its own fresh private MySQL database. The selected test network supplied disposable mTLS protocol fixtures for registry, identity, DSO and authorization; it contained no other support-system implementation. The actual application completed startup and an authenticated HTTPS operation returned 200. Requests without an identity token returned 401; requests without a client certificate failed the TLS handshake. Application containers ran as UID 10001 with read-only roots, mounted CRLs and per-system credentials. DB TLS verification and Hibernate schema validation passed against private database volumes with no published DB ports. No production authority, credential or database was reused. The Onboarding Compose database-user interpolation was corrected. Test endpoints follow each system's actual published API path.

The four native Java systems were separately redeployed using their own Compose files, private temporary SQLite volumes and generated test PKI. Their JAR/image storage checks, non-executable temporary-storage behavior, fixed system packaging and process startup passed. SysMonitor correctly returns degraded health without its AMQP/MQTT connections; the other three return process liveness. AH-GT-1 vector/security tests are included in the GateTunnel unit suite. Native federation startup used explicit legacy-mTLS test compatibility and does not claim a new inter-cloud acceptance run.

Isolation means independent packaging, configuration and state. Production still needs the ordinary Arrowhead core services and the selected function's grants, orchestration, brokers/providers, NTP sources or federation peers. A liveness check alone does not prove monitoring delivery, inter-cloud exchange, NTP accuracy or complete domain workflows. Those were not newly certified by this audit. Tests were on Linux ARM64; another architecture was not tested. Private helper/library copies require security fixes to be propagated to every affected project.

## Non-Java exception

LocalCloudManager is Node.js/nginx, despite the Java-style folder name; a Java JAR is not applicable. It now has its own root Compose definition, environment template, guide and verification script, using its own API/UI images. All 23 API tests passed offline. An isolated console deployment passed health/UI checks, anonymous API rejection (401), operator login (204) and authenticated session retrieval. It uses generated credentials and unprivileged, read-only containers. The selected console-only deployment has no Docker host access; container inventory/log/lifecycle features require a separate authorized integration and are unavailable here. Automatic approval review rejected adding a host Docker-socket proxy because of its broad host-control risk; it was excluded. The existing proxy source was left untouched.

## Documentation and operation

The [deployment guide](DEPLOYMENT-GUIDE.md) and each system's DEPLOYMENT.md give independent build/deployment commands, required credentials, cloud prerequisites, ports, network/subnet selection and state lifecycle. Eleven expanded Spring SysDDs and the support GSoSDD were updated and rebuilt from local LaTeX assets. The four native SysDDs from the prior separation remain valid and their project guides were refreshed. The new PDFs were rendered and visually reviewed, with no overfull boxes or unresolved references/citations in final logs. The GSoSDD now describes all fifteen Java systems, private library ownership and the non-Java console exception. Documents remain release candidates/drafts pending quality assurance.

| Design document | PDF |
|---|---|
| AIGym SysDD | [PDF](ah5-AIGym-java-spring/documentation/v5.2.2/AIGym-v5.2.2-SysDD.pdf) |
| Blacklist SysDD | [PDF](ah5-Blacklist-java-spring/documentation/v5.2.2/Blacklist-v5.2.2-SysDD.pdf) |
| CertificateAuthority SysDD | [PDF](ah5-CertificateAuthority-java-spring/documentation/v5.2.2/CertificateAuthority-v5.2.2-SysDD.pdf) |
| Choreographer SysDD | [PDF](ah5-Choreographer-java-spring/documentation/v5.2.2/Choreographer-v5.2.2-SysDD.pdf) |
| Configuration SysDD | [PDF](ah5-Configuration-java-spring/documentation/v5.2.2/Configuration-v5.2.2-SysDD.pdf) |
| DataManager SysDD | [PDF](ah5-DataManager-java-spring/documentation/v5.2.2/DataManager-v5.2.2-SysDD.pdf) |
| DeviceQoSEvaluator SysDD | [PDF](ah5-DeviceQoSEvaluator-java-spring/documentation/v5.2.2/DeviceQoSEvaluator-v5.2.2-SysDD.pdf) |
| EventHandler SysDD | [PDF](ah5-EventHandler-java-spring/documentation/v5.2.2/EventHandler-v5.2.2-SysDD.pdf) |
| GateTunnel SysDD | [PDF](ah5-GateTunnel-java-spring/documentation/v5.2.2/GateTunnel-v5.2.2-SysDD.pdf) |
| Gatekeeper SysDD | [PDF](ah5-Gatekeeper-java-spring/documentation/v5.2.2/Gatekeeper-v5.2.2-SysDD.pdf) |
| Onboarding SysDD | [PDF](ah5-Onboarding-java-spring/documentation/v5.2.2/Onboarding-v5.2.2-SysDD.pdf) |
| PlantDescriptionEngine SysDD | [PDF](ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2/PlantDescriptionEngine-v5.2.2-SysDD.pdf) |
| SysMonitor SysDD | [PDF](ah5-SysMonitor-java-spring/documentation/v5.2.2/SysMonitor-v5.2.2-SysDD.pdf) |
| TimeManager SysDD | [PDF](ah5-TimeManager-java-spring/documentation/v5.2.2/TimeManager-v5.2.2-SysDD.pdf) |
| TranslationManager SysDD | [PDF](ah5-TranslationManager-java-spring/documentation/v5.2.2/TranslationManager-v5.2.2-SysDD.pdf) |
| Support GSoSDD | [PDF](<roadmap/5.2/Arrowhead SoS architecture/WhiteBox_5.2.2/GSoSDD_Arrowhead_Support_Systems_5.2.pdf>) |

SHA-256 checks confirmed 2020 original support/common/roadmap files unchanged. Original source checksum inputs are in verification/all-support-original-checksums-2026-10-25.json. No original Arrowhead source folder or live cloud was updated. All disposable test containers, volumes, networks and generated credentials were removed after verification; built artifacts/images are retained.

Earlier copy manifests and the four-system report are historical records. This all-system report is the current result of the full support inventory audit. Details and commands are retained under verification/all-support-2026-10-25 and verification/tools.

## Restoration to the requested distribution

The independent projects and their SysDDs were restored from the retained `EA-fork` copy to `Eclipse-Arrowhead` on 25 October 2026. The support GSoSDD is maintained in `roadmap/5.2/Arrowhead SoS architecture/WhiteBox_5.2.2`. Rechecked 4160 source, build, configuration and JAR files against the prior verified inventory, excluding macOS folder metadata. All fifteen SysDD source/PDF pairs are present and the twelve previously rendered updated PDF hashes match. The earlier 1,098 Java tests and fifteen isolated deployments remain the test evidence; no new deployment run was performed during restoration. See [restoration verification](verification/support-restoration-2026-10-25.json).
