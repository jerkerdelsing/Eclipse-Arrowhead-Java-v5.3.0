# Release 5.3.0 qualification record

Evidence for items 1 and 2 of section 8 of [Release-5.3.0.md](../../Release-5.3.0.md), produced on 12 November 2026
for commit **`02b4b4e229b0fc0e5645dd22f7290a30a86d3a52`** of this repository. Every result below was produced from
that commit exported with `git archive`, never from a working tree, so uncommitted changes and `target/` leftovers
cannot have entered it. The commands are in [verification/tools](../tools/); each one rewrites its JSON file here.

| File | Content | Tool |
|---|---|---|
| [manifest.json](manifest.json) | source-to-image manifest: 49 images, runtime dependencies, common and core test runs | `release_manifest.py` |
| [db-integration-tests.json](db-integration-tests.json) | MySQL integration tests against the release's own database images | `db_integration_tests.py` |
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

The 16 skipped tests need external fixtures. 14 of them are MySQL end-to-end tests, which now run against each
system's own 5.3.0 database image with throw-away credentials: CertificateAuthority 1, Gatekeeper 2, GateTunnel 1,
LocalCloudConfigurator 7, SoSConfigurator 1, SysMonitor 2. GateTunnel's had stopped working unnoticed: its federation
settings require the organisation CA's CRL path since SOS-6 and the test did not supply one. It was fixed in
`02b4b4e2`.

Still not run (they need an MQTT relay or the brokers with a test PKI): GateTunnel `RelayRoundTripTest`, SysMonitor
`BrokerRoundTripTest`, and Gatekeeper `RelayRoundTripIT` (an `*IT` class, which the unit-test phase does not select).
They belong to the federation qualification that section 8 item 4 defers.

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

Not covered here: a live negative-access run against a cloud deployed from these 5.3.0 images. The test clouds'
`smoke-security` scripts do that; their recorded runs used the earlier `20261111-v530` images.
