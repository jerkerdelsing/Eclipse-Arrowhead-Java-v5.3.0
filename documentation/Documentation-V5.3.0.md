# Documentation V5.3.0

Reviewed and consolidated on 10 November 2026.

## Which documentation is current?

The individual microsystem `documentation/v5.3.0/` folders supersede the shared folder's older system/interface descriptions and the earlier cross-language compilations. Each system's LaTeX SysD, SysDD, SD and IDD is the maintained specification source; its PDF is the compiled reading copy. Maintain each edition with its own README, SOURCES and build-docs.sh.

The shared `documentation/` folder still serves a purpose: this overview holds the edition-wide index, migration record and shared maintenance follow-up. `v5.3.0-validation/` preserves build and integrity evidence. `cross-language/` preserves superseded historical compilations and their provenance. These shared records do not replace the individual system specifications.

The original per-system `documentation/v5.2.2/` editions remain retained baselines. Their files have not been edited as part of this consolidation.

## Edition and implementation versions

5.3.0 is the documentation edition. It did not change application source, dependency versions, JAR names, image tags or deployed systems. The primary Java framework constant and core Maven revision still identify 5.2.2; published service versions and wire identifiers must continue to match their implementations. In particular, the native telemetry identifier remains `SysMonitor-AMQP-IDD-5.2.2`.

Moving executable implementations to 5.3.0 is a separate release activity involving version constants, POMs, artifacts, images, deployment references and verification. A 5.3.0 document cover alone does not perform that release. Go/Rust sections in the per-system SysDDs and IDDs record implementation-specific boundaries; they do not establish interchangeability.

## System editions

The installed set comprises 295 LaTeX documents and matching PDFs in 26 edition folders: five Java core systems, nineteen Java support systems, and the separately versioned Go and Rust LocalCloudManager sets. Other native catalogues without a v5.2.2 baseline were outside that conversion.

| Microsystem | Implementation | Documents/PDFs | Maintained edition |
|---|---|---:|---|
| Authentication | Java | 13 | [documentation/v5.3.0](../ah5-core-java-spring/authentication/documentation/v5.3.0/) |
| ConsumerAuthorization | Java | 19 | [documentation/v5.3.0](../ah5-core-java-spring/consumer-authorization/documentation/v5.3.0/) |
| DynamicServiceOrchestration | Java | 21 | [documentation/v5.3.0](../ah5-core-java-spring/serviceorchestration-dynamic/documentation/v5.3.0/) |
| SimpleStoreServiceOrchestration | Java | 19 | [documentation/v5.3.0](../ah5-core-java-spring/serviceorchestration-simple/documentation/v5.3.0/) |
| ServiceRegistry | Java | 19 | [documentation/v5.3.0](../ah5-core-java-spring/serviceregistry/documentation/v5.3.0/) |
| Gatekeeper | Java | 10 | [documentation/v5.3.0](../ah5-Gatekeeper-java-spring/documentation/v5.3.0/) |
| Configuration | Java | 12 | [documentation/v5.3.0](../ah5-Configuration-java-spring/documentation/v5.3.0/) |
| CertificateAuthority | Java | 10 | [documentation/v5.3.0](../ah5-CertificateAuthority-java-spring/documentation/v5.3.0/) |
| DataManager | Java | 8 | [documentation/v5.3.0](../ah5-DataManager-java-spring/documentation/v5.3.0/) |
| TranslationManager | Java | 16 | [documentation/v5.3.0](../ah5-TranslationManager-java-spring/documentation/v5.3.0/) |
| TimeManager | Java | 8 | [documentation/v5.3.0](../ah5-TimeManager-java-spring/documentation/v5.3.0/) |
| LocalCloudConfigurator | Java | 6 | [documentation/v5.3.0](../ah5-LocalCloudConfigurator-java/documentation/v5.3.0/) |
| GateTunnel | Java | 10 | [documentation/v5.3.0](../ah5-GateTunnel-java-spring/documentation/v5.3.0/) |
| DeviceQoSEvaluator | Java | 13 | [documentation/v5.3.0](../ah5-DeviceQoSEvaluator-java-spring/documentation/v5.3.0/) |
| Onboarding | Java | 10 | [documentation/v5.3.0](../ah5-Onboarding-java-spring/documentation/v5.3.0/) |
| LocalCloudManager | Java | 6 | [documentation/v5.3.0](../ah5-LocalCloudManager-java-spring/documentation/v5.3.0/) |
| GraphDBManager | Java | 8 | [documentation/v5.3.0](../ah5-GraphDBManager-java-spring/documentation/v5.3.0/) |
| SysMonitor | Java | 10 | [documentation/v5.3.0](../ah5-SysMonitor-java-spring/documentation/v5.3.0/) |
| EventHandler | Java | 12 | [documentation/v5.3.0](../ah5-EventHandler-java-spring/documentation/v5.3.0/) |
| PlantDescriptionEngine | Java | 10 | [documentation/v5.3.0](../ah5-PlantDescriptionEngine-java-spring/documentation/v5.3.0/) |
| Choreographer | Java | 12 | [documentation/v5.3.0](../ah5-Choreographer-java-spring/documentation/v5.3.0/) |
| AIGym | Java | 6 | [documentation/v5.3.0](../ah5-AIGym-java-spring/documentation/v5.3.0/) |
| Blacklist | Java | 13 | [documentation/v5.3.0](../ah5-Blacklist-java-spring/documentation/v5.3.0/) |
| LocalCloudManager | Go | 9 | [documentation/v5.3.0](../../Eclipse-Arrowhead-go/ah5-LocalCloudManager-go/documentation/v5.3.0/) |
| LocalCloudManager | Rust | 7 | [documentation/v5.3.0](../../Eclipse-Arrowhead-rust/ah5-LocalCloudManager-rust/documentation/v5.3.0/) |
| SoSConfigurator | Java | 8 | [documentation/v5.3.0](../ah5-SoSConfigurator-java/documentation/v5.3.0/) |

The Go LocalCloudManager 5.2.2 baseline is nested under `documentation/LocalCloudManager/v5.2.2/`; its new edition is directly under `documentation/v5.3.0/`. Rust LocalCloudManager keeps its existing documentation link into the primary shared native catalogue. Those folder arrangements do not change ownership of the primary implementations.

## Edition-wide migration and validation record

On 9 November 2026, 287 existing versioned LaTeX documents, including two native catalogues, were carried into the new edition. Eight missing SoSConfigurator documents were authored from its primary source, bringing the total to 295. The conversion updated document metadata, edition references, bibliography edition numbers and revision histories. Historical/runtime version literals retained their original meanings.

The recorded publication checks covered 295 compiled PDFs and 2,729 pages, found no unresolved references or citations, and verified 845 retained v5.2.2 files unchanged. Inherited missing Array anchors and larger path/property overflows were corrected in the new copies. Small inherited width warnings, about 1.9–3.3 pt, remain recorded. Selected covers and implementation/interface pages were rendered and inspected. These are the results of that publication run, not evidence that application tests or live deployments have been rerun since.

The current folder inventory was checked again during this consolidation: all 295 LaTeX documents still have matching PDFs. The original evidence remains available:

- [Final document/hash validation](v5.3.0-validation/final-validation.json).
- [Retained v5.2.2 file hashes](v5.3.0-validation/v5.2.2-before-sha256.json).
- [Post-installation preservation result](v5.3.0-validation/preservation-after.json).
- [Primary implementation input hashes](v5.3.0-validation/input-source-sha256.json).
- [Final LaTeX logs](v5.3.0-validation/final-latex-logs.zip).

Use the build-docs.sh inside the selected v5.3.0 folder. Older extraction/generation scripts may still write v5.2.2 and must be made version-aware before being used to regenerate the new edition. A source snapshot inside an old compilation is historical evidence, not a development tree.

## Shared follow-up retained from the earlier notes

### Recorded upstream alignment requests

The following publication/integration requests were recorded on 7 November 2026. They describe follow-up beyond the local per-system specifications. Upstream release and submission status has not been rechecked during this consolidation; the earlier note's “not released/not submitted” wording must not be treated as a current upstream status report.

| Recorded follow-up | Current local documentation to consult |
|---|---|
| Reconcile the upstream common-library framework constant with the released library version. The local constant remains 5.2.2. | [Primary Constants.java](../ah5-common-java-spring/common-utils/src/main/java/eu/arrowhead/common/Constants.java) and [core POM](../ah5-core-java-spring/pom.xml). |
| Propagate the agreed SimpleStoreServiceOrchestration default port 8442 into upstream defaults/images, and adopt Onboarding port 8435. | [Listening ports](../LISTENING-PORTS.md) and the respective microsystem editions above. |
| Standardize/release the native sysMonitor service, its contract and common publisher implementation. | [SysMonitor edition](../ah5-SysMonitor-java-spring/documentation/v5.3.0/) and each producer's SD/AMQP IDD. |
| Adopt SecurityProfileGuard or an equivalent startup conformance check in the upstream common library. | [Secure profile](../SECURITY-PROFILE.md). |
| Carry the documented restricted-discovery/SimpleStore combinations into upstream documentation. | [SimpleStore SysD](../ah5-core-java-spring/serviceorchestration-simple/documentation/v5.3.0/SimpleStoreServiceOrchestration-v5.3.0-SysD.tex). |
| Qualify/release Gatekeeper/GateTunnel or their replacements together with the DynamicServiceOrchestration inter-cloud integration and provider-side authorization checks. | [Gatekeeper edition](../ah5-Gatekeeper-java-spring/documentation/v5.3.0/), [GateTunnel edition](../ah5-GateTunnel-java-spring/documentation/v5.3.0/) and [DynamicServiceOrchestration edition](../ah5-core-java-spring/serviceorchestration-dynamic/documentation/v5.3.0/). |

### Cross-system name-reservation issue

The 30 October report noted that GraphDBManager is missing from Onboarding's built-in reserved-system-name set. That issue remains visible in the current [EnrollmentService.java](../ah5-Onboarding-java-spring/src/main/java/eu/arrowhead/onboarding/service/EnrollmentService.java): the default list omits GraphDBManager and is selected through `ONBOARDING_RESERVED_NAMES`. This is retained here as a cross-system maintenance issue; this documentation consolidation does not change the list or a deployment override.

Other still-relevant system-specific caveats from the October report are already documented in their own editions: AIGym development fallbacks, source-versus-deployment ports, LocalCloudManager operation registration and the LocalCloudManager/PDE management-policy requirements. Their details are maintained in those system documents and [LISTENING-PORTS.md](../LISTENING-PORTS.md), rather than duplicated here.

## Disposition of earlier shared documents

| Earlier material | Disposition and replacement |
|---|---|
| [DOCUMENTATION-UPDATE-2026-10-30.md](DOCUMENTATION-UPDATE-2026-10-30.md) | Superseded as current guidance; retained as the October change/build record. Current system content lives in the microsystem editions. The still-valid cross-system reservation issue is retained above. |
| [UPSTREAM-ALIGNMENT-5.3.md](UPSTREAM-ALIGNMENT-5.3.md) | Superseded by the recorded upstream follow-up section above and the linked current system/security/port documents. Original text retained for provenance. |
| [V5.3.0-DOCUMENTATION.md](V5.3.0-DOCUMENTATION.md) | Superseded by this consolidated overview, including its edition index and validation record. Original publication report retained. |
| [cross-language/](cross-language/) | Both earlier 48-document compilations, generated Markdown front sections, packaged ZIP and embedded source snapshots are superseded as maintained documentation. Archive notices apply to the complete compilations; their source snapshots and validation/provenance files remain intact. |
| [v5.3.0-validation/](v5.3.0-validation/) | Retained validation evidence, not superseded by prose and not a system specification. |

The October report's “core source/documents missing” statement is obsolete: primary core sources and the five per-system editions now exist. Its statement that the native sysMonitor publisher is not registered must not be used as current guidance: the current common publisher supplies a service model for registration when enabled. The earlier Node.js LocalCloudManager architecture is also historical; the current [LocalCloudManager README](../ah5-LocalCloudManager-java-spring/README.md) and its edition describe the Java-Spring replacement. External scratchpad/backup references in the old report are historical notes, not a verified recovery procedure.

Per-system v5.2.2/v5.3.0 LaTeX/PDFs, embedded source snapshots, ZIP contents and machine-readable validation records remain unchanged. Notices were added to the old shared reports and archive entry points; their original text is preserved below each notice.
