> **Historical archive — superseded for ongoing documentation, 10 November 2026.** Use [Documentation-V5.3.0.md](../../Documentation-V5.3.0.md) and the individual microsystem editions. This notice covers the whole compilation, including its generated Markdown and embedded source snapshots. The original text below describes this earlier archive only.

# Eclipse Arrowhead SysDD and IDD document set

Revision: 2026-11-09. Review draft. Five core and nineteen support systems; two documents per system. Target baseline 5.2.2, including local extensions. TestTranslation and random data producers are test/example applications and shared libraries are not independently deployed systems.

Each PDF contains newly authored Java/Go/Rust sections plus full available native design appendices. Markdown is the editable source for the front sections; original LaTeX sources and dependencies are retained in sources/. Baseline appendices preserve their original release labels and dates. This compilation does not certify implementation parity or revalidate all historical contract statements.

Go and Rust are documented as available implementations, development profiles or absent ports as appropriate. GraphDBManager and SoSConfigurator have no corresponding Go/Rust system folders. LocalCloudConfigurator has no Rust folder.

SoSConfigurator was newly documented from current Java API/security/planner sources; its older README contains an obsolete unregistered-tool statement. The current source establishes its support-system identity and registered services.

| System | Group | SysDD | IDD |
|---|---|---|---|
| Authentication | core | [PDF](core/Authentication/Authentication-SysDD.pdf) | [PDF](core/Authentication/Authentication-IDD.pdf) |
| ConsumerAuthorization | core | [PDF](core/ConsumerAuthorization/ConsumerAuthorization-SysDD.pdf) | [PDF](core/ConsumerAuthorization/ConsumerAuthorization-IDD.pdf) |
| DynamicServiceOrchestration | core | [PDF](core/DynamicServiceOrchestration/DynamicServiceOrchestration-SysDD.pdf) | [PDF](core/DynamicServiceOrchestration/DynamicServiceOrchestration-IDD.pdf) |
| SimpleStoreServiceOrchestration | core | [PDF](core/SimpleStoreServiceOrchestration/SimpleStoreServiceOrchestration-SysDD.pdf) | [PDF](core/SimpleStoreServiceOrchestration/SimpleStoreServiceOrchestration-IDD.pdf) |
| ServiceRegistry | core | [PDF](core/ServiceRegistry/ServiceRegistry-SysDD.pdf) | [PDF](core/ServiceRegistry/ServiceRegistry-IDD.pdf) |
| AIGym | support | [PDF](support/AIGym/AIGym-SysDD.pdf) | [PDF](support/AIGym/AIGym-IDD.pdf) |
| Blacklist | support | [PDF](support/Blacklist/Blacklist-SysDD.pdf) | [PDF](support/Blacklist/Blacklist-IDD.pdf) |
| CertificateAuthority | support | [PDF](support/CertificateAuthority/CertificateAuthority-SysDD.pdf) | [PDF](support/CertificateAuthority/CertificateAuthority-IDD.pdf) |
| Choreographer | support | [PDF](support/Choreographer/Choreographer-SysDD.pdf) | [PDF](support/Choreographer/Choreographer-IDD.pdf) |
| Configuration | support | [PDF](support/Configuration/Configuration-SysDD.pdf) | [PDF](support/Configuration/Configuration-IDD.pdf) |
| DataManager | support | [PDF](support/DataManager/DataManager-SysDD.pdf) | [PDF](support/DataManager/DataManager-IDD.pdf) |
| DeviceQoSEvaluator | support | [PDF](support/DeviceQoSEvaluator/DeviceQoSEvaluator-SysDD.pdf) | [PDF](support/DeviceQoSEvaluator/DeviceQoSEvaluator-IDD.pdf) |
| EventHandler | support | [PDF](support/EventHandler/EventHandler-SysDD.pdf) | [PDF](support/EventHandler/EventHandler-IDD.pdf) |
| GateTunnel | support | [PDF](support/GateTunnel/GateTunnel-SysDD.pdf) | [PDF](support/GateTunnel/GateTunnel-IDD.pdf) |
| Gatekeeper | support | [PDF](support/Gatekeeper/Gatekeeper-SysDD.pdf) | [PDF](support/Gatekeeper/Gatekeeper-IDD.pdf) |
| GraphDBManager | support | [PDF](support/GraphDBManager/GraphDBManager-SysDD.pdf) | [PDF](support/GraphDBManager/GraphDBManager-IDD.pdf) |
| LocalCloudConfigurator | support | [PDF](support/LocalCloudConfigurator/LocalCloudConfigurator-SysDD.pdf) | [PDF](support/LocalCloudConfigurator/LocalCloudConfigurator-IDD.pdf) |
| LocalCloudManager | support | [PDF](support/LocalCloudManager/LocalCloudManager-SysDD.pdf) | [PDF](support/LocalCloudManager/LocalCloudManager-IDD.pdf) |
| Onboarding | support | [PDF](support/Onboarding/Onboarding-SysDD.pdf) | [PDF](support/Onboarding/Onboarding-IDD.pdf) |
| PlantDescriptionEngine | support | [PDF](support/PlantDescriptionEngine/PlantDescriptionEngine-SysDD.pdf) | [PDF](support/PlantDescriptionEngine/PlantDescriptionEngine-IDD.pdf) |
| SoSConfigurator | support | [PDF](support/SoSConfigurator/SoSConfigurator-SysDD.pdf) | [PDF](support/SoSConfigurator/SoSConfigurator-IDD.pdf) |
| SysMonitor | support | [PDF](support/SysMonitor/SysMonitor-SysDD.pdf) | [PDF](support/SysMonitor/SysMonitor-IDD.pdf) |
| TimeManager | support | [PDF](support/TimeManager/TimeManager-SysDD.pdf) | [PDF](support/TimeManager/TimeManager-IDD.pdf) |
| TranslationManager | support | [PDF](support/TranslationManager/TranslationManager-SysDD.pdf) | [PDF](support/TranslationManager/TranslationManager-IDD.pdf) |

Validation: all PDF files reopened and page counts checked; each requires both Go and Rust sections. Native appendix compile status is recorded in build-report.json. Source hashes are recorded in source-manifest.json. Documentation-only work; no implementations or deployments changed.
