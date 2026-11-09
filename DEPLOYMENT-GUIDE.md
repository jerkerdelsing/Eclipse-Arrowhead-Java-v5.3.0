# Selectable support-system deployments

All 15 Java support systems are packaged in their own ah5-<System>-java-spring folders. Each owns its source, build, executable JAR, image, configuration and Compose definition. No selected system builds or mounts JavaSupportRuntime or a sibling support folder. The copied combined runtime remains historical reference only; core systems are a separate distribution.

The ten Spring systems that previously inherited an Arrowhead parent now compile private Arrowhead common/DTO source under vendor/. AIGym already has a standalone Spring build. CertificateAuthority, Gatekeeper, GateTunnel and SysMonitor have independent native Java builds. Third-party Maven libraries and published base images are normal external dependencies. Private source copies require coordinated security maintenance; provenance is recorded locally.

## Select, provision and deploy

Start in the selected system's own folder. Copy .env.example to .env, choose the cloud network/registry and a unique Compose project name and host port. Provision its own registered identity/password, certificate/key or PKCS12 stores, public trust and valid CRL, plus its database TLS/secrets or federation relay identity where required. The project-local DEPLOYMENT.md lists exact files and permissions. No production credentials or signing keys are supplied. Only CertificateAuthority receives an issuer signing key.

```sh
mvn -B clean verify             # Java 21 + Maven, or use the Docker build below
docker compose config --quiet
docker compose build
docker compose up -d
docker compose ps
docker compose logs
```

Repeat only for the additional support systems you select. No root combined runtime is required. The external cloud network and core systems must already exist and be configured. The ten database-backed systems start only their own private database alongside the app. MySQL volumes and internal subnets are per project; DB TLS certificates must include the database service's DNS name. Set a unique DB_SUBNET when deploying multiple clouds or when Docker address pools are exhausted. No database port is published. Native Java systems own SQLite volumes. AIGym is volatile. LocalCloudManager is a non-Java console exception with its own Node/nginx deployment and no JAR.

Independence concerns source, packaging and state ownership. It does not remove normal service dependencies: registration/identity require ServiceRegistry and Authentication; consumption uses DSO and ConsumerAuthorization; monitoring, federation, NTP and domain operations require their normal brokers/providers. The selected deployments do not bypass or start those core services. Configure the appropriate grants and service offers. A successful listener health check is not full application readiness: require successful startup, registration, an authorized operation and required dependency status.

`docker compose down` stops that project and preserves its volume. Back up state before upgrades and perform explicit schema migrations for existing databases. `down -v` deliberately erases private state. The generated database initializer is only for a fresh volume and does not set a sample user password.

## Inventory

| System | Folder / deployment instructions | Artifact | HTTPS port |
|---|---|---|---:|
| AIGym | [ah5-AIGym-java-spring](ah5-AIGym-java-spring/DEPLOYMENT.md) | `arrowhead-ai-gym-5.2.2.jar` | 8478 |
| Blacklist | [ah5-Blacklist-java-spring](ah5-Blacklist-java-spring/DEPLOYMENT.md) | `arrowhead-blacklist-5.2.2.jar` | 8464 |
| CertificateAuthority | [ah5-CertificateAuthority-java-spring](ah5-CertificateAuthority-java-spring/DEPLOYMENT.md) | `arrowhead-certificate-authority-5.2.2.jar` | 8448 |
| Choreographer | [ah5-Choreographer-java-spring](ah5-Choreographer-java-spring/DEPLOYMENT.md) | `arrowhead-choreographer-5.2.2.jar` | 8454 |
| Configuration | [ah5-Configuration-java-spring](ah5-Configuration-java-spring/DEPLOYMENT.md) | `arrowhead-configuration-5.2.2.jar` | 8450 |
| DataManager | [ah5-DataManager-java-spring](ah5-DataManager-java-spring/DEPLOYMENT.md) | `arrowhead-datamanager-5.2.2.jar` | 8461 |
| DeviceQoSEvaluator | [ah5-DeviceQoSEvaluator-java-spring](ah5-DeviceQoSEvaluator-java-spring/DEPLOYMENT.md) | `arrowhead-device-qos-evaluator-5.2.2.jar` | 8472 |
| EventHandler | [ah5-EventHandler-java-spring](ah5-EventHandler-java-spring/DEPLOYMENT.md) | `arrowhead-eventhandler-5.2.2.jar` | 8455 |
| GateTunnel | [ah5-GateTunnel-java-spring](ah5-GateTunnel-java-spring/DEPLOYMENT.md) | `arrowhead-gatetunnel-5.2.2.jar` | 8453 |
| Gatekeeper | [ah5-Gatekeeper-java-spring](ah5-Gatekeeper-java-spring/DEPLOYMENT.md) | `arrowhead-gatekeeper-5.2.2.jar` | 8449 |
| Onboarding | [ah5-Onboarding-java-spring](ah5-Onboarding-java-spring/DEPLOYMENT.md) | `arrowhead-onboarding-5.2.2.jar` | 8435 |
| PlantDescriptionEngine | [ah5-PlantDescriptionEngine-java-spring](ah5-PlantDescriptionEngine-java-spring/DEPLOYMENT.md) | `arrowhead-plantdescriptionengine-5.2.2.jar` | 8432 |
| SysMonitor | [ah5-SysMonitor-java-spring](ah5-SysMonitor-java-spring/DEPLOYMENT.md) | `arrowhead-sysmonitor-5.2.2.jar` | 8475 |
| TimeManager | [ah5-TimeManager-java-spring](ah5-TimeManager-java-spring/DEPLOYMENT.md) | `arrowhead-timemanager-5.2.2.jar` | 8463 |
| TranslationManager | [ah5-TranslationManager-java-spring](ah5-TranslationManager-java-spring/DEPLOYMENT.md) | `arrowhead-translation-manager-5.2.2.jar` | 8465 |
| LocalCloudManager | [Node/nginx console](ah5-LocalCloudManager-java-spring/DEPLOYMENT.md) | npm/API + static UI; JAR not applicable | 8490 (loopback HTTP console) |

## Verification boundaries

See [the complete verification report](ALL-JAVA-SUPPORT-VERIFICATION-2026-10-25.md) and verification/all-support-2026-10-25 for logs, hashes and machine-readable results. Verification uses individual folder mounts for compilation, local image contexts and one support system at a time with generated credentials/private state. Spring registry/authentication/DSO dependencies use disposable mTLS protocol fixtures. Production security filters are exercised; no startup bypass is introduced. Monitoring/NTP/federation application acceptance is not inferred from startup. The original Arrowhead sources and live clouds remain unchanged.

The documentation source/PDF for each system is in its own documentation/v5.2.2 folder. The support GSoSDD is in roadmap/5.2/Arrowhead SoS architecture/WhiteBox_5.2.2. The original copy manifest and earlier four-system report are retained as historical evidence; the new all-system report is authoritative for this audit.
