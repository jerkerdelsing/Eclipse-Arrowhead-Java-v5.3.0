# Java support distribution - complete independent packaging

All 19 support systems are Java-Spring and have their own source, build, JAR, image, configuration and deployment definition. They do not depend on JavaSupportRuntime.

See [deployment guide](../DEPLOYMENT-GUIDE.md) and [full verification report](ALL-JAVA-SUPPORT-VERIFICATION-2026-10-25.md).

| System | Own-folder deployment | Tests passed |
|---|---|---:|
| AIGym | [ah5-AIGym-java-spring](../ah5-AIGym-java-spring/DEPLOYMENT.md) | 1 |
| Blacklist | [ah5-Blacklist-java-spring](../ah5-Blacklist-java-spring/DEPLOYMENT.md) | 139 |
| CertificateAuthority | [ah5-CertificateAuthority-java-spring](../ah5-CertificateAuthority-java-spring/DEPLOYMENT.md) | 45 |
| Choreographer | [ah5-Choreographer-java-spring](../ah5-Choreographer-java-spring/DEPLOYMENT.md) | 18 |
| Configuration | [ah5-Configuration-java-spring](../ah5-Configuration-java-spring/DEPLOYMENT.md) | 11 |
| DataManager | [ah5-DataManager-java-spring](../ah5-DataManager-java-spring/DEPLOYMENT.md) | 15 |
| DeviceQoSEvaluator | [ah5-DeviceQoSEvaluator-java-spring](../ah5-DeviceQoSEvaluator-java-spring/DEPLOYMENT.md) | 301 |
| EventHandler | [ah5-EventHandler-java-spring](../ah5-EventHandler-java-spring/DEPLOYMENT.md) | 14 |
| GateTunnel | [ah5-GateTunnel-java-spring](../ah5-GateTunnel-java-spring/DEPLOYMENT.md) | 155 |
| Gatekeeper | [ah5-Gatekeeper-java-spring](../ah5-Gatekeeper-java-spring/DEPLOYMENT.md) | 43 |
| Onboarding | [ah5-Onboarding-java-spring](../ah5-Onboarding-java-spring/DEPLOYMENT.md) | 25 |
| PlantDescriptionEngine | [ah5-PlantDescriptionEngine-java-spring](../ah5-PlantDescriptionEngine-java-spring/DEPLOYMENT.md) | 20 |
| SysMonitor | [ah5-SysMonitor-java-spring](../ah5-SysMonitor-java-spring/DEPLOYMENT.md) | 63 |
| TimeManager | [ah5-TimeManager-java-spring](../ah5-TimeManager-java-spring/DEPLOYMENT.md) | 17 |
| TranslationManager | [ah5-TranslationManager-java-spring](../ah5-TranslationManager-java-spring/DEPLOYMENT.md) | 490 |
| GraphDBManager | [ah5-GraphDBManager-java-spring](../ah5-GraphDBManager-java-spring/README.md#standalone-deployment) | — |
| LocalCloudManager | [ah5-LocalCloudManager-java-spring](../ah5-LocalCloudManager-java-spring/DEPLOYMENT.md) | 53 (manager 46, Docker proxy 7) |
| LocalCloudConfigurator | [ah5-LocalCloudConfigurator-java](../ah5-LocalCloudConfigurator-java/DEPLOYMENT.md) | 42 |
| SoSConfigurator | [ah5-SoSConfigurator-java](../ah5-SoSConfigurator-java/DEPLOYMENT.md) | 29 |

Test counts of the seven systems converted on 8 November 2026 are from their latest offline builds; the others are from the 25 October report.

The copied core is a separate core distribution. ah5-support-runtime-java is historical reference, not a selectable support system. No upstream repository was created or published. Original Arrowhead folders and live clouds remain unchanged. Production core/provider dependencies remain as documented; isolated startup is not full broker/NTP/federation acceptance.
