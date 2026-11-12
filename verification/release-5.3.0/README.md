# Release 5.3.0 qualification record

Evidence for items 1 and 2 of section 8 of [Release-5.3.0.md](../../Release-5.3.0.md), produced on 12 November 2026
for commit **`02b4b4e229b0fc0e5645dd22f7290a30a86d3a52`** of this repository. Every result below was produced from
that commit exported with `git archive`, never from a working tree, so uncommitted changes and `target/` leftovers
cannot have entered it. The commands are in [verification/tools](../tools/); each one rewrites its JSON file here.

| File | Content | Tool |
|---|---|---|
| [manifest.json](manifest.json) | source-to-image manifest: 49 images, runtime dependencies, common and core test runs | `release_manifest.py` |
| [db-integration-tests.json](db-integration-tests.json) | MySQL integration tests against the release's own database images | `db_integration_tests.py` |
| [relay-integration-tests.json](relay-integration-tests.json) | Gatekeeper and GateTunnel tests over the relay of `Arrowhead-test-clouds/Arrowhead-relay` | `relay_integration_tests.py` |
| [sysmonitor-broker-test.json](sysmonitor-broker-test.json) | SysMonitor's AMQP → store → MQTT round trip against throw-away brokers | `sysmonitor_broker_test.py` |
| [live-security.json](live-security.json) | smoke and security regression of the Java test cloud running these images | the test cloud's scripts |
| [dependency-review.json](dependency-review.json) | known vulnerabilities of the runtime Maven dependencies (OSV) | `dependency_review.py` |
| [api-dto-review.json](api-dto-review.json) | DTOs, constants and request mappings against the upstream 5.2.2 tags | `api_dto_review.py` |
| [negative-access-review.json](negative-access-review.json) | tests that assert a refusal (401/403), per project | `negative_access_review.py` |

Build logs are written to `build-logs/` and not committed (about 50 MB); the manifest records each log's SHA-256.

## Item 1: version identifiers

Commit `11ddb4a9` moved the framework, artifact and image identifiers from 5.2.2 to 5.3.0: the common library version
and `Constants.AH_FRAMEWORK_VERSION` (with the vendored copies and their `PROVENANCE.json` checksums), the core reactor
revision, every support system's POM version and JAR name, system version constants and metadata, banners, and the JAR
names and image tags in Dockerfiles, compose files, scripts and deployment instructions.

Deliberately unchanged:

- wire-contract identifiers such as `SysMonitor-AMQP-IDD-5.2.2`;
- the independent service versions of Gatekeeper and GateTunnel (`VERSION_SERVICES`, `VERSION_CONNECTION`, ...);
- documentation editions (`documentation/v5.2.2/`), `legacy/` folders, vendored upstream POM copies and historical reports;
- comments that describe the behaviour of the native 5.2.2 implementations.

`verify_reference_conformance.py`: 3214 of 3214 checks pass, including the library version against the constant in
the library and in every vendored copy. `verify_listening_ports.py`: pass (25 systems; it now also knows
SoLocalCloudConfigurator on 8496).

## Item 2: pinned and qualified artifacts

### Source-to-image manifest

`release_manifest.py` built every image with `--no-cache`: 20 support application images, their database images (one
of them GraphDB), LocalCloudManager's Docker proxy, and the five core systems with their databases. 49 of 49 built.
For each image the manifest records the source folder and its git tree hash, the Dockerfile, build arguments, the base
images (all pinned by digest), image ID, size, architecture, user, the SHA-256 of the packaged JAR, the test summary and
the build log's SHA-256. Each image carries the label `org.opencontainers.image.revision` with the commit.

Images are not bit-for-bit reproducible: the runtime layers apply the distribution's security updates at build time,
and timestamps differ. The manifest pins which source produced which image, so a rebuild can be compared JAR by JAR.

Built on Docker 29.7.2, linux/arm64. An amd64 build was not part of this run.

### Tests

| Run | Result |
|---|---|
| Support images (tests run inside each image build) | 1553 run, 0 failures, 16 skipped |
| `ah5-common-java-spring` (`mvn clean install`) | 868 run, 0 failures |
| `ah5-core-java-spring` (`mvn clean verify`; the core image builds skip tests) | 2607 run, 0 failures |
| MySQL integration tests (`db_integration_tests.py`) | 14 run, 0 failures |
| Relay integration tests (`relay_integration_tests.py`) | 4 run, 0 failures |
| SysMonitor broker round trip (`sysmonitor_broker_test.py`) | 1 run, 0 failures |

The 16 tests the image builds skip need external fixtures; all of them now run and pass, as does Gatekeeper's
`RelayRoundTripIT`, an `*IT` class that the unit-test phase never selects.

- **MySQL (14):** the end-to-end tests run against each system's own 5.3.0 database image with throw-away
  credentials: CertificateAuthority 1, Gatekeeper 2, GateTunnel 1, LocalCloudConfigurator 7, SoSConfigurator 1,
  SysMonitor 2. GateTunnel's had stopped working unnoticed: its federation settings require the organisation CA's CRL
  path since SOS-6 and the test did not supply one. It was fixed in `02b4b4e2`.
- **Relay (4):** against the relay deployed from `Arrowhead-test-clouds/Arrowhead-relay` (Mosquitto 2.0.22, mutual
  TLS, its own test PKI; see that folder's README). Gatekeeper `RelayRoundTripIT` (3): request/reply between two
  clouds, exact topics and fields, duplicate and forged-sender messages ignored. GateTunnel `RelayRoundTripTest` (1):
  an AH-GT-1 session with the committed fixture identities; the sealed request is executed once at the provider.
- **SysMonitor `BrokerRoundTripTest` (1):** against throw-away RabbitMQ (SASL EXTERNAL) and Mosquitto brokers with
  mutual TLS and a test CA, and SysMonitor's 5.3.0 database image: intake, storage, `sysMonitorData` output,
  duplicate and identity-mismatch rejection, and unbinding of a disabled subscription.

These are component tests over a real broker. They do not qualify a federated deployment of clouds, which section 8
item 4 defers.

### Dependency and security review

228 runtime Maven packages were checked against the OSV database. The first run found four advisories in
`mariadb-java-client` 3.5.10, managed by Spring Boot 4.1.2 and used by 18 projects: GHSA-4jq2-8v3c-97w8 (low),
GHSA-fhx5-2xqw-2cgm, GHSA-grfq-r75j-rw9x, GHSA-h599-m3w5-9m7x (moderate). Commit `24ce3c48` sets
`mariadb.version` to 3.5.11 in the common library and the 17 support POMs that use the driver. The final run finds no
known vulnerability.

Not covered: operating-system packages of the base images. The runtime images apply the distribution's updates at
build time; an image scanner (Trivy, Grype) was not available on the build host.

Also noted: the five core application images run as root. Their Dockerfiles follow the upstream 5.2.2 layout; the
support images run as UID 10001.

### API/DTO review

`api_dto_review.py` compared the commit with the upstream tags (`ah5-common-java-spring` v5.2.2 = `8835d65f`,
`ah5-core-java-spring` v5.2.2 = `3f96fb47`):

- **DTOs:** 167 compared, none removed, no record component removed or retyped, no enum constant removed. Three
  added: `OrchestrationPlantProviderDTO`, `OrchestrationPlantRuleDTO`, `OrchestrationPlantRulesDTO`.
- **Request mappings:** no mapping of an existing controller removed or changed.
- **Removed:** the HTTP `monitor` service of DynamicServiceOrchestration and SimpleStoreServiceOrchestration
  (`MonitorAPI` in both, with `SERVICE_DEF_MONITOR`, `HTTP_API_MONITOR_PATH`, `MQTT_API_MONITOR_BASE_TOPIC`,
  `VERSION_MONITOR`). This is deliberate: every system publishes `sysMonitor` instead. A consumer of the upstream
  orchestrators' `monitor` echo must move to `sysMonitor`.
- **Changed defaults:** `AH_FRAMEWORK_VERSION` (upstream 5.2.2 carries `5.2.1`), and
  `HTTP_COLLECTOR_MODE_DEFAULT` from `SR_AND_ORCH` to `ORCHESTRATION`: systems find their providers through
  orchestration by default, not the ServiceRegistry.
- **Added:** 33 constants.

The tool compares source shapes, not runtime behaviour or JSON serialisation settings.

### Negative-access review

`negative_access_review.py` counts, per project, the test methods that assert a refusal. Every project has some
except DeviceQoSEvaluator, which has no access rule of its own: it relies on the common library's filters, whose tests
assert 99 refusals.

The first run also flagged Choreographer and PlantDescriptionEngine, which do have their own refusal logic. Commit
`24ce3c48` adds tests for both (7 each):

- **Choreographer, `OrchestrationCallbackFilter`:** a result push without a certificate is refused (401); one from a
  foreign cloud or from another local system is forbidden (403). Other requests go to the authentication filter.
- **PlantDescriptionEngine, authorization filter:** an unauthenticated caller and a certificate of another system are
  refused (401); a caller without a ConsumerAuthorization grant is forbidden (403).

The live run below repeats the negative-access checks against a deployed cloud.

### Live security run

The Java test cloud (`Arrowhead-test-clouds/Arrowhead-test-cloud-java`, Compose project `arrowhead-v53-test-cloud`)
was redeployed on these images on 12 November 2026. Its `v530/use-release-images.py` tags the support images of the
manifest under the cloud's names (`20261112-r530`), builds the cloud's read-only core adapter on the release core
images and the core database images from the same commit. After deployment all 47 containers of these images ran the
recorded image IDs; the brokers and RandomDataProducer are third-party or unchanged.

- `scripts/smoke-test.sh`: 14 of 14 OK.
- `scripts/smoke-security.sh`: passed, 194 checks, 0 failures. Every check of the earlier run on the `20261111-v530`
  images passes again; the 8 additional checks cover SoLocalCloudConfigurator. It covers the console (H1),
  credentials (H2), revocation at the MQTT and AMQP brokers (H3), databases, container hardening, secrets, component
  versions, blacklist enforcement, mounts, the CRL and the grants. The relay checks are skipped: the cloud is not
  federated (section 8, item 4).

**Finding: startup login race.** On the first deployment LocalCloudManager and EventHandler exited at startup with
`AuthException: Invalid authorization header`; both started on the next attempt. At startup the common library logs in
from the main thread (`ApplicationInitListener`) and from the scheduled `OutsourcedLoginJob` at nearly the same time.
`loginIfNeeded()` is `synchronized`, but Quartz creates a new job instance for each run, so the lock does not exclude
the two; the second token supersedes the first while the registration call is under way, and ServiceRegistry refuses
it. Intermittent (2 of 21 systems on this start) and recovered by a restart, but a system without a restart policy
stays down. It is in `ah5-common-java-spring` and its vendored copies; not fixed in this release candidate.
