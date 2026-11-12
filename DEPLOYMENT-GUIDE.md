# Selectable support-system deployments

All 19 support systems are Java-Spring and packaged in their own ah5-<System>-java-spring folders. Each owns its source, build, executable JAR, image, configuration and Compose definition. No selected system builds or mounts JavaSupportRuntime or a sibling support folder. The copied combined runtime remains historical reference only; core systems are a separate distribution.

Every system except AIGym compiles private Arrowhead common/DTO source under vendor/ and follows the common Spring pattern (SystemInfo, ApplicationInitListener registration, filter chain, security profile guard, generalManagement, sysMonitor). AIGym has a standalone Spring build. Since 8 November 2026 CertificateAuthority, Gatekeeper, GateTunnel and SysMonitor (formerly native JDK code with SQLite), LocalCloudManager (formerly Node.js/nginx), LocalCloudConfigurator and SoSConfigurator (formerly plain JDK HTTP servers) are Spring systems too. Third-party Maven libraries and published base images are normal external dependencies. Private source copies require coordinated security maintenance; provenance is recorded locally.

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

Repeat only for the additional support systems you select. No root combined runtime is required. The external cloud network and core systems must already exist and be configured. Every system that stores data starts only its own private MySQL database alongside the app. MySQL volumes and internal subnets are per project; DB TLS certificates must include the database service's DNS name. Set a unique DB_SUBNET when deploying multiple clouds or when Docker address pools are exhausted. No database port is published. AIGym and LocalCloudManager keep no persistent state. The operator consoles of LocalCloudManager (8490) and SoSConfigurator (8495) are plain-HTTP connectors with password login, published on the host loopback interface only; their Arrowhead services use mutual TLS. CertificateAuthority (`scripts/migrate-sqlite-to-mysql.py`) and Gatekeeper (`migrate-sqlite` start mode) import a native SQLite store into the new schema.

Independence concerns source, packaging and state ownership. It does not remove normal service dependencies: registration/identity require ServiceRegistry and Authentication; consumption uses DSO and ConsumerAuthorization; monitoring, federation, NTP and domain operations require their normal brokers/providers. The selected deployments do not bypass or start those core services. Configure the appropriate grants and service offers. A successful listener health check is not full application readiness: require successful startup, registration, an authorized operation and required dependency status.

`docker compose down` stops that project and preserves its volume. Back up state before upgrades and perform explicit schema migrations for existing databases. `down -v` deliberately erases private state. The generated database initializer is only for a fresh volume and does not set a sample user password. It then restricts the application account to `SELECT, INSERT, UPDATE, DELETE` on its own schema (`deployment/database/least-privilege.sh`); run schema migrations as the database root account. For a volume created before this change, apply the same restriction as root, writing each `_` of the schema name as `\_` (the form the MySQL image used for its grant), for example ``REVOKE ALL ON `ah\_eventhandler`.* FROM 'eventhandler'@'%'; GRANT SELECT, INSERT, UPDATE, DELETE ON `ah\_eventhandler`.* TO 'eventhandler'@'%';``.

## Inventory

| System | Folder / deployment instructions | Artifact | HTTPS port |
|---|---|---|---:|
| AIGym | [ah5-AIGym-java-spring](ah5-AIGym-java-spring/DEPLOYMENT.md) | `arrowhead-ai-gym-5.3.0.jar` | 8478 |
| Blacklist | [ah5-Blacklist-java-spring](ah5-Blacklist-java-spring/DEPLOYMENT.md) | `arrowhead-blacklist-5.3.0.jar` | 8464 |
| CertificateAuthority | [ah5-CertificateAuthority-java-spring](ah5-CertificateAuthority-java-spring/DEPLOYMENT.md) | `arrowhead-certificate-authority-5.3.0.jar` | 8448 |
| Choreographer | [ah5-Choreographer-java-spring](ah5-Choreographer-java-spring/DEPLOYMENT.md) | `arrowhead-choreographer-5.3.0.jar` | 8454 |
| Configuration | [ah5-Configuration-java-spring](ah5-Configuration-java-spring/DEPLOYMENT.md) | `arrowhead-configuration-5.3.0.jar` | 8450 |
| DataManager | [ah5-DataManager-java-spring](ah5-DataManager-java-spring/DEPLOYMENT.md) | `arrowhead-datamanager-5.3.0.jar` | 8461 |
| DeviceQoSEvaluator | [ah5-DeviceQoSEvaluator-java-spring](ah5-DeviceQoSEvaluator-java-spring/DEPLOYMENT.md) | `arrowhead-device-qos-evaluator-5.3.0.jar` | 8472 |
| EventHandler | [ah5-EventHandler-java-spring](ah5-EventHandler-java-spring/DEPLOYMENT.md) | `arrowhead-eventhandler-5.3.0.jar` | 8455 |
| GateTunnel | [ah5-GateTunnel-java-spring](ah5-GateTunnel-java-spring/DEPLOYMENT.md) | `arrowhead-gatetunnel-5.3.0.jar` | 8453 |
| Gatekeeper | [ah5-Gatekeeper-java-spring](ah5-Gatekeeper-java-spring/DEPLOYMENT.md) | `arrowhead-gatekeeper-5.3.0.jar` | 8449 |
| Onboarding | [ah5-Onboarding-java-spring](ah5-Onboarding-java-spring/DEPLOYMENT.md) | `arrowhead-onboarding-5.3.0.jar` | 8435 |
| PlantDescriptionEngine | [ah5-PlantDescriptionEngine-java-spring](ah5-PlantDescriptionEngine-java-spring/DEPLOYMENT.md) | `arrowhead-plantdescriptionengine-5.3.0.jar` | 8432 |
| SysMonitor | [ah5-SysMonitor-java-spring](ah5-SysMonitor-java-spring/DEPLOYMENT.md) | `arrowhead-sysmonitor-5.3.0.jar` | 8475 |
| TimeManager | [ah5-TimeManager-java-spring](ah5-TimeManager-java-spring/DEPLOYMENT.md) | `arrowhead-timemanager-5.3.0.jar` | 8463 |
| TranslationManager | [ah5-TranslationManager-java-spring](ah5-TranslationManager-java-spring/DEPLOYMENT.md) | `arrowhead-translation-manager-5.3.0.jar` | 8465 |
| GraphDBManager | [ah5-GraphDBManager-java-spring](ah5-GraphDBManager-java-spring/DEPLOYMENT.md) | `arrowhead-graphdbmanager-5.3.0.jar` | 8467 |
| LocalCloudManager | [ah5-LocalCloudManager-java-spring](ah5-LocalCloudManager-java-spring/DEPLOYMENT.md) | `localcloudmanager.jar`, `localcloudmanager-docker-proxy.jar` | 8491 (console 8490, loopback) |
| LocalCloudConfigurator | [ah5-LocalCloudConfigurator-java](ah5-LocalCloudConfigurator-java/DEPLOYMENT.md) | `localcloudconfigurator.jar` | 8492 (host worker 8493, loopback) |
| SoSConfigurator (legacy; superseded by SoLocalCloudConfigurator) | [ah5-SoSConfigurator-java](ah5-SoSConfigurator-java/DEPLOYMENT.md) | `sosconfigurator.jar` | 8494 (console 8495, loopback) |
| SoLocalCloudConfigurator | [ah5-SoLocalCloudConfigurator-java](ah5-SoLocalCloudConfigurator-java/DEPLOYMENT.md) | `solocalcloudconfigurator.jar` | 8496 (no console; LocalCloudManager SoLC view) |

## Secure profile

A Spring system stops at startup when its effective configuration disables TLS, uses `authentication.policy=declared` or disables authorization enforcement. The `config/application.properties` of each folder meets the profile. Do not set `arrowhead.security.profile=development` in a deployment; see [SECURITY-PROFILE.md](SECURITY-PROFILE.md).

## Database account privileges

At run time a system only reads and changes rows: JPA uses `ddl-auto=validate`, no system migrates its schema, Quartz keeps jobs in memory and Log4j2 only inserts into `logs`. The application database account therefore holds only `SELECT, INSERT, UPDATE, DELETE` on its own schema. Tables are created and changed by root through the initialisation scripts.

- The `grant_privileges.sql` source scripts of the Core and support systems grant exactly these four privileges.
- The ten MySQL-backed Compose deployments add `deployment/database/least-privilege.sh` after the schema script. It revokes the `ALL` grant that the MySQL image gives the `MYSQL_USER` account and grants the four privileges instead.
- Init scripts run only on an empty volume. For a database initialised earlier, run as root (schema and user from the folder's `compose.yaml`; the underscores are escaped as in the image's own grant):

```sql
REVOKE ALL ON `ah\_eventhandler`.* FROM 'eventhandler'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah\_eventhandler`.* TO 'eventhandler'@'%';
```

This applies to every system with a database, including CertificateAuthority, Gatekeeper, GateTunnel, SysMonitor, LocalCloudConfigurator and SoSConfigurator, which moved from SQLite or files to MySQL on 8 November 2026.

## Verification boundaries

See [the complete verification report](ALL-JAVA-SUPPORT-VERIFICATION-2026-10-25.md) and verification/all-support-2026-10-25 for logs, hashes and machine-readable results. Verification uses individual folder mounts for compilation, local image contexts and one support system at a time with generated credentials/private state. Spring registry/authentication/DSO dependencies use disposable mTLS protocol fixtures. Production security filters are exercised; no startup bypass is introduced. Monitoring/NTP/federation application acceptance is not inferred from startup. The original Arrowhead sources and live clouds remain unchanged.

The documentation source/PDF for each system is in its own documentation/v5.2.2 folder. The support GSoSDD is in roadmap/5.2/Arrowhead SoS architecture/WhiteBox_5.2.2. The original copy manifest and earlier four-system report are retained as historical evidence; the new all-system report is authoritative for this audit.
