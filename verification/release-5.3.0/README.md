# Release 5.3.0 qualification record

Evidence for items 1 and 2 of section 8 of [Release-5.3.0.md](../../Release-5.3.0.md) for commit
**`b7c685e7865204f4334b53f4971ce08ba9511011`** of this repository (see `manifest.json` for the full hash), produced on 13 November 2026. It repeats the
qualification of the same day for `4ac53157` (core application images as non-root, see *Core application images run
as non-root*), `f1103ac9` (core database images on MySQL 8.4.13, see *Finding: the core database images*) and
`d25225ae` (start-up login fix, below), and of 12 November for `02b4b4e2`, after limiting how long the common
library's HTTP client reuses a resolved host name (see *Finding: stale host-name cache*). The results are the same
apart from those changes, the new core database test and one more common-library test (870). Every result below was produced from
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
| [manifest-amd64.json](manifest-amd64.json) | the same 49 images built for linux/amd64 (tags `…:5.3.0-amd64`), common and core tests on amd64 | `release_manifest.py --platform linux/amd64` |
| [platform-comparison-amd64.json](platform-comparison-amd64.json) | amd64 against arm64 images: architecture, user, JAR content, Java start | `compare_platforms.py` |
| [os-vulnerability-scan.json](os-vulnerability-scan.json), [os-vulnerability-scan-amd64.json](os-vulnerability-scan-amd64.json) | OS-package vulnerabilities of the base and release images (Trivy) | `os_vulnerability_scan.py` |
| [core-db-images-test.json](core-db-images-test.json), [core-db-images-test-amd64.json](core-db-images-test-amd64.json) | each core database image initialised on an empty volume: server version, schema, deployment password | `core_db_images_test.py` |

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

Built on Docker 29.7.2, linux/arm64 (the host).

**amd64.** The same 49 images were built for linux/amd64 from the same commit with `--platform linux/amd64` (Docker's
emulation; every pinned base image is a multi-architecture index that includes amd64), tagged `<image>:5.3.0-amd64`.
All 49 built, with the same test results inside the image builds (1553 run, 0 failures, 16 skipped), and the common
(870) and core (2607) test suites also pass in an amd64 Maven container. `compare_platforms.py` finds every amd64 image
recorded as amd64 with the same user as its arm64 counterpart, every application image starts Java 21.0.13 under
amd64, and the packaged JARs are identical entry by entry: about 1.2 million entries, none different. A live
deployment on amd64 hardware was not part of this run.

### Tests

| Run | Result |
|---|---|
| Support images (tests run inside each image build) | 1553 run, 0 failures, 16 skipped |
| `ah5-common-java-spring` (`mvn clean install`) | 870 run, 0 failures |
| `ah5-core-java-spring` (`mvn clean verify`; the core image builds skip tests) | 2607 run, 0 failures |
| MySQL integration tests (`db_integration_tests.py`) | 14 run, 0 failures |
| Relay integration tests (`relay_integration_tests.py`) | 4 run, 0 failures |
| SysMonitor broker round trip (`sysmonitor_broker_test.py`) | 1 run, 0 failures |
| Core database images (`core_db_images_test.py`, arm64 and amd64) | 5 of 5 pass on each |

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

**Operating-system packages.** `os_vulnerability_scan.py` runs Trivy 0.58.1 (`aquasec/trivy`, pinned) on every base
image as published and on every release image, for both architectures, with the vulnerability database of
13 November 2026. Each image is exported with `docker save` and Trivy reads only that file; it gets no Docker socket.
Every image's OS was recognised.

| Images | OS | Critical | High | Medium | Low | With a fix |
|---|---|---|---|---|---|---|
| 26 application images (support and core, Docker proxy) | Ubuntu 26.04 | 0 | 0 | 29 | 4 | none |
| 22 database images: 17 support, 5 core (MySQL 8.4.13) | Oracle Linux 9.8 | 0 | 0 | 0 | 0 | – |
| GraphDB image | Ubuntu 24.04 | 0 | 0 | 81 (amd64: 25) | 20 (amd64: 4) | none |
| RabbitMQ, Mosquitto, RandomDataProducer of the test cloud | Alpine 3.23/3.24 | 0 | 0 | 0–1 | 0 | – |

Every release image applies the distribution's updates at build time: their base images carry 2 (eclipse-temurin)
and 0 (`mysql:8.4.13`) high findings, the built images none, and no remaining finding has a fixed version. (On
`d25225ae` the five core database images, then on `mysql:8.1` / Oracle Linux 8.8, had 120 high findings each, all
with a fixed version: see below.)

**Finding: the core database images (fixed in `f1103ac9`).** In `d25225ae`,
`ah5-core-java-spring/*/docker/Dockerfile-*-DB` were still the upstream 5.2.2 Dockerfiles on `mysql:8.1` (an
innovation release past its support) on Oracle Linux 8.8: 120 high findings each (python39, libxml2, gnutls, expat,
glibc, ...), every one with a fixed version. Their scripts also granted to an `ah-operator` user that is never
created, which fails on an empty MySQL 8.4 volume, and the system user kept the password published in the scripts.

`f1103ac9` moves all five to `mysql:8.4.13` (pinned by digest, Oracle Linux 9.8), removes `mysql-shell` and applies
the distribution's updates at build time, rebuilds `gosu` with the current Go toolchain, drops the `ah-operator`
grants, and adds `docker-db/zz-arrowhead-system-user-password.sh`, which sets the system user's password from
`AH_DB_USER` and `AH_DB_USER_PASSWORD` (or `_FILE`) at initialisation. `core_db_images_test.py` starts each image on an
empty volume with throw-away passwords and checks MySQL 8.4, the schema tables (ServiceRegistry 11, Authentication 4,
ConsumerAuthorization 10, DynamicServiceOrchestration 6, SimpleStoreServiceOrchestration 4), that the deployment
password works and the published default is refused, and that the system user cannot create tables. All five pass on
both architectures; the images of `d25225ae` fail it (no schema: the `ah-operator` grant aborts the initialisation).
An existing volume initialised by MySQL 8.1 is upgraded in place on first start (see the live run).

**Core application images run as non-root (since `4ac53157`).** Up to `f1103ac9` the five core application images
followed the upstream 5.2.2 Dockerfiles and ran as root (the test cloud overrode the user to 10001 in Compose).
`4ac53157` gives `/app` to UID/GID 10001 and switches to it, as the support images do; the manifests now record
`10001:10001` for all 26 application images (the Docker proxy: `10001:0`). The entrypoint still moves the default
configuration into `/app/config` on first start, so a volume or host folder mounted there must be writable by UID
10001 (the docker `ReadMe.md` files say so). Checked before the build: ServiceRegistry started without a mount and with
an empty named volume at `/app/config`, wrote its configuration there as 10001, and stopped only at the secure-profile
guard (expected with the packaged development configuration). The 22 database images set no `USER`: the MySQL
entrypoint starts as root and runs `mysqld` as `mysql`.

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
was redeployed on these images on 13 November 2026 (before: on the images of `4ac53157`, `f1103ac9` and `d25225ae` the same day and
of `02b4b4e2` on 12 November). Its
`v530/use-release-images.py` tags the images of the manifest under the cloud's names (`20261113-r530e`; before `20261113-r530d`, `20261113-r530c`), including the five
core database images, and builds the cloud's read-only core adapter on the release core images. (Before `f1103ac9`
the cloud built its own core database images, because the release's were on MySQL 8.1.) The existing core database
volumes, initialised by MySQL 8.1, were upgraded in place by MySQL 8.4.13 on first start. After deployment all 47 containers of these images ran the
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
it. Intermittent (2 of 21 systems on that start) and recovered by a restart, but a system without a restart policy
stays down.

**Fixed in `d25225ae`:** `loginIfNeeded()` synchronizes on a lock shared by all instances, and the renewal check
inside it lets the second caller skip. The new test `testConcurrentLoginFromTwoJobInstancesLogsInOnce` fails on the
previous code (two logins) and passes with the fix; the change is in the common library and its 19 vendored copies.
On the redeployment with the fixed images `deploy.sh up` completed on the first attempt, each of the 22 systems
logged in exactly once at start-up (before: twice), and no container logged the error.

**Finding: stale host-name cache after a peer is recreated.** On the deployment of `4ac53157` (tag `20261113-r530d`)
DataManager exited at start-up twice with `Blacklist system is not available`: ServiceRegistry's blacklist filter could
not reach Blacklist, although Blacklist was running, registered and reachable from the ServiceRegistry container.
ServiceRegistry's first failed call came about a second before the recreated Blacklist started, and from then on its
calls to `blacklist:8464` failed without opening a connection; they stopped by themselves about ten minutes later
(14:45 UTC, Docker's DNS TTL is 600 s), after which `deploy.sh up` completed and every check passed. The cause is
consistent with the HTTP client of the common library (`HttpService`, Reactor Netty with its default resolver) caching
the failed or old address for the record's TTL. It is not caused by the non-root change (the containers ran as 10001
before too) and depends on start-up order, but a system that starts during that window fails.

Reproduced on that deployment by recreating Blacklist with a new address (a placeholder container held the old one):
ServiceRegistry's checks kept failing from 15:38:12 to 15:42:37 UTC, about four and a half minutes after Blacklist
was ready, without a connection attempt. Reactor Netty's resolver (1.3.8 / Netty 4.2.19) caches each answer for the
record's TTL, Docker's DNS returns 600 s, and the client kept the old address.

**Fixed in `b7c685e7`:** `HttpService` builds its client with
`resolver(spec -> spec.cacheMaxTimeToLive(10 s).cacheNegativeTimeToLive(0))`, so a resolved name is reused for at
most ten seconds and a failed lookup is not cached. The new test `testHttpClientsReuseResolvedHostNamesBriefly`
checks the client's resolver settings (the previous code has none); the change is in the common library and its 19
vendored copies, whose `PROVENANCE.json` record the new checksum. (Six other entries per copy, 114 in all, still
recorded checksums from before the changes of `ef1bb18d` and `a64e73f2`, and two files of those commits,
`Jackson2Defaults.java` and `SecurityProfileGuard.java`, had no entry. The commit after this qualification updates
and adds them: every one of the 6,497 entries now matches its file, and every vendored file is identical to
`ah5-common-java-spring`. `PROVENANCE.json` is only in the Maven build stage, not in the JARs or runtime images, so the
qualified images are unaffected.) On the deployment of the fixed
images (tag `20261113-r530e`) the same reproduction (Blacklist 172.19.0.7 → 172.19.0.29, recreated at 19:29:33 UTC,
ready at 19:29:44) gave one failed check at 19:29:36, while Blacklist was still starting, and none in the eight
minutes after; the blacklist enforcement checks of the security run then passed against the new address.
