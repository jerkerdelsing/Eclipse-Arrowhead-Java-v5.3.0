# Arrowhead 5.2.2 model coverage

Snapshot: 2026-10-27. Counts include the same operation separately for each protocol interface.

| System | Role | Services | Interfaces | Operations |
|---|---|---:|---:|---:|
| ServiceRegistry | Core | 6 | 11 | 61 |
| Authentication | Core | 4 | 7 | 27 |
| ConsumerAuthorization | Core | 6 | 11 | 43 |
| DynamicServiceOrchestration | Core | 7 | 12 | 31 |
| SimpleStoreServiceOrchestration | Core | 6 | 11 | 31 |
| AIGym | Support | 2 | 2 | 9 |
| Blacklist | Support | 4 | 7 | 17 |
| CertificateAuthority | Support | 4 | 4 | 13 |
| Choreographer | Support | 5 | 5 | 24 |
| Configuration | Support | 5 | 5 | 12 |
| DataManager | Support | 3 | 3 | 11 |
| DeviceQoSEvaluator | Support | 4 | 7 | 15 |
| EventHandler | Support | 5 | 5 | 11 |
| GateTunnel | Support | 4 | 4 | 12 |
| Gatekeeper | Support | 4 | 4 | 9 |
| LocalCloudManager | Support | 2 | 2 | 25 |
| Onboarding | Support | 4 | 4 | 15 |
| PlantDescriptionEngine | Support | 4 | 4 | 16 |
| SysMonitor | Support | 4 | 4 | 13 |
| TimeManager | Support | 3 | 3 | 6 |
| TranslationManager | Support | 5 | 9 | 23 |

Total: 21 system definitions (five core variants, sixteen support systems), 91 services, 124 protocol interfaces, 424 operations.

`sources.json` contains the complete operation inventory and input SHA-256 hashes. The unregistered HTTP endpoints from the core extraction are recorded there separately; they are deliberately not presented as registered Arrowhead services.
