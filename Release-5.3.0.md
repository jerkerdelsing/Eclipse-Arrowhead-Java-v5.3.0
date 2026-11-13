# Eclipse Arrowhead 5.3.0 — draft release notes

**Status:** Draft for the Java reference implementation and its deployable test cloud.  
**Comparison baseline:** Official Java-Spring 5.2.2 release.  
**Prepared:** 12 November 2026.  
**Primary source inspected:** `Eclipse-Arrowhead-java`, HEAD `370196734ce2a07708fb0e42c4eb995c7407f006`.

## Principal changes

The proposed 5.3.0 reference extends the official 5.2.2 core and three released support systems into a more complete engineering and operations environment. The principal additions are a broader Java-Spring support-system suite, enforced secure deployment settings, native monitoring, plant-driven orchestration, graphical cloud design and administration, and policy-constrained federation and deployment packaging.

The reference also supplies a reproducible test-cloud build and provisioning path. The current Java cloud definition contains **50 Compose services**: 24 framework microsystems, one demonstration producer, 22 private store containers, two brokers and a constrained Docker proxy. These are configured services, not a claim that 50 containers were checked live while preparing these notes. [Cloud definition](../Arrowhead-test-clouds/Arrowhead-test-cloud-java/compose.yaml).

This document describes local reference changes and candidate release content. It does not announce an official upstream 5.3.0 publication. Component version identifiers are still mixed, as explained below.

## 1. What is inherited from official 5.2.2?

The official [release-version table](https://aitia-iiot.github.io/ah5-docs-java-spring/home/release-versions/#522), dated 3 September 2026, lists five core systems—ServiceRegistry, Authentication, ConsumerAuthorization, DynamicServiceOrchestration and SimpleStoreServiceOrchestration—and three support systems: Blacklist, TranslationManager and DeviceQoSEvaluator. That version-specific inventory is the baseline here; newer items in the website navigation are not automatically attributed to 5.2.2.

Java 21, service discovery, authentication and authorization services, dynamic and stored orchestration, protected HTTP/MQTT bindings, databases and Docker Compose deployment already existed. The additions below concern their implementation, integration and operational coverage; they do not introduce these underlying concepts. See the official [implementation overview](https://aitia-iiot.github.io/ah5-docs-java-spring/home/implementation/), [authentication policies](https://aitia-iiot.github.io/ah5-docs-java-spring/api/authentication_policy/), [communication profiles](https://aitia-iiot.github.io/ah5-docs-java-spring/api/communication-profiles/communication-profiles-overview/) and [Compose guide](https://aitia-iiot.github.io/ah5-docs-java-spring/home/getting_started/docker/docker_aggregated/).

This is a documentation- and source-based comparison, not an exhaustive diff against every official release-tag file or a blanket API-compatibility certification. Some linked upstream 5.2.2 changelog pages were unavailable during review.

## 2. Secure operation is enforced at startup

The reference makes its secure profile an enforced startup condition. `SecurityProfileGuard` rejects effective configurations that disable required TLS, use declared identities or disable service authorization. Insecure development operation requires an explicit development-profile selection. Packaged development samples must therefore be replaced or overridden by the secure deployment configuration before normal startup.

The secured cloud combines mutual TLS, certificate-bound identity tokens, operation-specific ConsumerAuthorization policies, configured CRL enforcement and broker certificate identities/ACLs. Credential bundles expose a system's own credentials and required public trust material; the cloud CA signing key is confined to the CertificateAuthority role. Database access is private, with TLS and restricted application accounts rather than public database ports. These are concrete deployment controls, not a claim that TLS or authorization was absent from 5.2.2.

The reference's management and browser interfaces have distinct identities and access rules. A console session does not grant unrestricted Core management, Docker or inter-cloud rights. Revocation coverage must be assessed at the actual inbound/outbound enforcement points of the selected component; no universal instantaneous revocation guarantee is implied.

Evidence: [startup guard](ah5-common-java-spring/common-utils/src/main/java/eu/arrowhead/common/init/SecurityProfileGuard.java), [token/certificate binding](ah5-common-java-spring/common-utils/src/main/java/eu/arrowhead/common/security/TokenCertificateBinding.java), [deployment guidance](DEPLOYMENT-GUIDE.md), [cloud security regression script](../Arrowhead-test-clouds/Arrowhead-test-cloud-java/scripts/smoke-security.sh).

## 3. Broader support-system coverage

Beyond the three support systems listed for official 5.2.2, the active Java reference cloud adds the following capabilities. Several restore or adapt earlier Arrowhead 4.6 functionality to the current identities, services and deployment model; they should not all be described as newly invented Arrowhead functions.

| Capability | Added reference systems | Principal contribution |
|---|---|---|
| Engineering and workflow | [PlantDescriptionEngine](ah5-PlantDescriptionEngine-java-spring/README.md), [Choreographer](ah5-Choreographer-java-spring/README.md) | Maintain engineered system/service connections, synchronize orchestration constraints, report deviations and execute plans through selected executors. |
| Trust and admission | [CertificateAuthority](ah5-CertificateAuthority-java-spring/README.md), [Onboarding](ah5-Onboarding-java-spring/README.md) | Cloud certificate issuance/revocation and staged manufacturer → onboarding → device → system enrollment, with off-boarding of associated identities and registrations. |
| Application information | [DataManager](ah5-DataManager-java-spring/README.md), [Configuration](ah5-Configuration-java-spring/README.md), [EventHandler](ah5-EventHandler-java-spring/README.md), [TimeManager](ah5-TimeManager-java-spring/README.md) | SenML cache/history, configuration distribution, event subscriptions/publication and time/trust information. |
| Observation and analytics | [SysMonitor](ah5-SysMonitor-java-spring/README.md), [AIGym](ah5-AIGym-java-spring/README.md) | Native system observations, retained monitoring history, subscriptions/republication and an application-oriented analytics extension. |
| Federation | [Gatekeeper](ah5-Gatekeeper-java-spring/README.md), [GateTunnel](ah5-GateTunnel-java-spring/README.md) | Cross-cloud discovery/negotiation and scoped relayed service sessions. |
| Operator and deployment engineering | [LocalCloudManager](ah5-LocalCloudManager-java-spring/README.md), [LocalCloudConfigurator](ah5-LocalCloudConfigurator-java/README.md), [SoLocalCloudConfigurator](ah5-SoLocalCloudConfigurator-java/README.md) | Integrated administration, revisioned local-cloud design/planning and portable System of LocalClouds packages. |
| Alternative historian | [GraphDBManager](ah5-GraphDBManager-java-spring/README.md) | RDF/GraphDB-backed history, exposed through distinct `graphDataProxy` and `graphDataHistorian` service definitions. |

Together with Blacklist, TranslationManager and DeviceQoSEvaluator, these constitute **19 support systems in the current cloud definition**. The source also retains **SoSConfigurator as a legacy implementation**; it is superseded by SoLocalCloudConfigurator for the current SoLC workflow and is not an additional service in that cloud definition.

Support applications now follow Java-Spring packaging, including components that previously used native JDK/SQLite or Node.js implementations. Systems own their build, configuration and state; many include a private copy of the Arrowhead common/DTO sources. Core continues to build against the shared common library. Database ownership was already a 5.x principle: the reference strengthens its packaging and access isolation rather than introducing it. Vendored common code requires coordinated maintenance across the copies. [Packaging and inventory](DEPLOYMENT-GUIDE.md).

## 4. Engineering decisions reach orchestration and administration

### Plant-driven dynamic orchestration

PlantDescriptionEngine translates the active plant's consumer/provider connections into an atomic DSO plant-rule snapshot. The local `serviceOrchestrationPlantManagement` extension supports querying and replacing these rules. Unresolved or unavailable engineered providers remain managed constraints; they are not silently replaced by unrelated providers. Monitoring history supports deviation alarms. This extends the existing orchestrator with engineering-controlled selection rather than replacing the upstream orchestration services.

The reference also integrates dependency consumption through the common service collector, with explicit bootstrap exceptions for discovery, identity and the authorities needed by orchestration itself. It is not an absolute “no configured endpoints” rule: brokers, bootstrap authorities and deployment infrastructure still require configuration.

Evidence: [PDE design and synchronization](ah5-PlantDescriptionEngine-java-spring/README.md), [DSO plant-rule API](ah5-core-java-spring/serviceorchestration-dynamic/src/main/java/eu/arrowhead/serviceorchestration/api/http/OrchestrationPlantManagementAPI.java), [service collector](ah5-common-java-spring/common-utils/src/main/java/eu/arrowhead/common/collector/ServiceCollector.java).

### Integrated operator console and local-cloud engineering

LocalCloudManager supplies operational views and actions for registry content, monitoring, application data, plant descriptions, orchestration rules, authorization policies, logs and permitted lifecycle operations. Graphical orchestration/security editing exposes the underlying services through the manager's own identity and grants.

LocalCloudConfigurator adds revisioned saved designs, component/dependency planning, reviewed deployment plans and an independently validating host worker. Saving a design does not deploy it. Apply requires a matching reviewed-plan hash; stops are explicit and constrained by dependency/protection rules. The normal Java backend is now **direct execution through a loopback mutual-TLS host worker**, not a mandatory ColonyOS service. Deployment steps can have partial effects; arbitrary scripts do not acquire transactional rollback.

Evidence: [graphical orchestration and security](ah5-LocalCloudManager-java-spring/GRAPHICAL-ORCHESTRATION.md), [LocalCloudConfigurator](ah5-LocalCloudConfigurator-java/README.md).

### Systems of LocalClouds and portable packages

SoLocalCloudConfigurator adds a policy-constrained SoLC design and package service. It pins member clouds to stored LocalCloudConfigurator design revisions, validates permitted consumers/exports/operations and derives neighbour relationships, grants/revocations and relay ACL subjects. Revision checks reject stale designs and package requests.

Its downloadable packages contain plans, configuration, scripts, manifests, checksums and installation/operation documentation. Credentials are generated on the hosts that use them; the package generator has no Docker socket. A separate host-side assembly step adds selected source/image archives, including architecture-specific options. This replaces the former console SoS workflow; the earlier SoSConfigurator/ColonyOS path remains legacy material.

Evidence: [SoLC service](ah5-SoLocalCloudConfigurator-java/README.md), [deployment/package requirements](ah5-SoLocalCloudConfigurator-java/DEPLOYMENT.md), [SoLC console workflow](ah5-LocalCloudManager-java-spring/SOLC-CONFIGURATION.md).

## 5. Native monitoring and implemented federation

Each participating framework system publishes its own `sysMonitor` observations, including echo, power and status, under its own identity. SysMonitor validates and stores selected observations, provides HTTPS history/subscription management and republishes normalized `sysMonitorData` through MQTT. This is a native observation path, not a delegated monitor sidecar. Application SenML history remains DataManager's concern; monitoring history belongs to SysMonitor. The existing wire identifier `SysMonitor-AMQP-IDD-5.2.2` is retained. [SysMonitor implementation](ah5-SysMonitor-java-spring/README.md).

The local DSO inter-cloud path uses Gatekeeper discovery/negotiation and returns a local GateTunnel session endpoint. `ONLY_INTERCLOUD` selects the cross-cloud path; `ALLOW_INTERCLOUD` permits fallback when no local provider is found. Provider-side authorization is checked for the foreign cloud and requested operations. AH-GT-1 adds end-to-end protection of relayed payloads; its legacy-mTLS alternative is a separate, weaker profile, not an automatic fallback. [Core extensions](ah5-core-java-spring/README.md), [Gatekeeper](ah5-Gatekeeper-java-spring/README.md), [GateTunnel](ah5-GateTunnel-java-spring/README.md).

**Deployment boundary:** the standalone v5.3 Java test cloud deliberately has no configured federation peers or organization cross-certificate, and its Gatekeeper relay is disabled. Federation capability in the source, portable package support and older multi-cloud test results are not evidence that this standalone cloud has exercised a federated deployment. The sibling Go/Rust clouds and relay are separate deployment consumers with their own evidence.

## 6. Deployable Java reference cloud and build refresh

The maintained deployment path is [Arrowhead-test-cloud-java/v530](../Arrowhead-test-clouds/Arrowhead-test-cloud-java/v530/README.md). Its generator combines primary system definitions, provisioning, policy setup, credential preparation, image builds and ordered startup. The root Compose file now describes project `arrowhead-v53-test-cloud`; older migration prose about combined JavaSupportRuntime, Node.js managers or SQLite histories must not be read as its current implementation inventory.

| Inspected item, 12 November 2026 | Recorded/configured value |
|---|---|
| Compose services | 50: five Core + nineteen support + one demo producer + twenty-two stores + two brokers + one Docker proxy. |
| Named volumes | 24 declared. No database service publishes a host port. |
| Build-result entries | 52 entries marked PASS, including database and intermediate Core adapter images; these are not 52 microsystems. |
| Image naming | `arrowhead-java-v530/*:20261111-v530`. |
| Source pin used for SoLC packaging | `370196734ce2a07708fb0e42c4eb995c7407f006`, the source pin recorded at the earlier inspection. Primary HEAD has since advanced; this does not attest the images to current source. |
| Local-cloud backend | Direct host worker; no default ColonyOS dependency. |
| Federation | Disabled/unconfigured in this standalone cloud. |

The primary common-library POM now selects Spring Boot **4.1.2**, retains Java **21**, and the Core build pins the RabbitMQ client to **5.37.1**. This is a build/dependency refresh; the historical README references to Spring Boot 3.x are not the current POM baseline. A dependency update alone does not establish that an artifact is free of vulnerabilities. [Common POM](ah5-common-java-spring/pom.xml), [Core POM](ah5-core-java-spring/pom.xml).

The cloud retains both orchestration implementations. The graphical configurator's default new design selects ServiceRegistry, Authentication, ConsumerAuthorization and DSO; it does not implicitly remove an already running SimpleStore system. Existing port differences between packaged defaults and deployment overrides remain significant. Use [LISTENING-PORTS.md](LISTENING-PORTS.md) and the effective Compose settings rather than a document cover version as the endpoint specification.

## 7. Documentation and evidence

The 5.3 documentation adds maintained per-system LaTeX SysD/SysDD/SD/IDD editions, Go/Rust implementation-boundary sections and updated GSoSD/GSoSDDs. The architecture documents now cover intra-cloud, inter-cloud and DMZ network roles, deployment refinements and traceability to the Arrowhead-5.2 Papyrus model. These documentation changes do not themselves configure a network or certify implementation parity. [Documentation overview](documentation/Documentation-V5.3.0.md), [network update record](<5.3 SoS documentation/Arrowhead SoS architecture/NETWORK-MODEL-UPDATE-2026-11-10.md>).

Available execution evidence must retain its date and scope:

- The [v530 build record](../Arrowhead-test-clouds/Arrowhead-test-cloud-java/v530/build-results.json) contains 52 PASS image entries at inspection. Docker layer caching was used; these entries do not mean every Maven test ran again for every image.
- The [11 November deployment account](../Arrowhead-test-clouds/Arrowhead-test-cloud-java/v530/README.md) records the earlier 48-service deployment, 14/14 smoke checks, console/LC checks and reviewed SimpleStore stop/start trials. Its 48-container health statement predates the added SoLC system/database and must not be silently relabelled as a 50-container verification.
- The later [SoLC security regression log](../Arrowhead-test-clouds/Arrowhead-test-cloud-java/v530/smoke-security-solc.log) records a passing security run, including SoLC access restrictions. It explicitly skips relay checks because federation is absent.

No builds, application tests, live-container checks or deployments were executed to prepare this draft. The release qualification of section 8, item 2 was produced afterwards: its builds and tests are recorded in the [qualification record](verification/release-5.3.0/README.md). Earlier Go/Rust interoperability runs and skipped-fixture tests remain separate evidence, not an acceptance certificate for the current Java source revision.

## 8. Compatibility, migration and release completion

The intent is to retain the established 5.x service model while adding capabilities. It is not a blanket drop-in compatibility promise. New services, stricter startup controls, updated dependencies and explicit operator/worker interfaces require consumer and deployment review.

Before publishing this as a completed 5.3.0 implementation release:

1. **Version identifiers aligned (done 12 November 2026).** The primary framework constant, common/Core Maven versions, support application versions, JAR names and image tags identify 5.3.0. Independent service versions and wire-contract identifiers such as `SysMonitor-AMQP-IDD-5.2.2` retain their own values. `verify_reference_conformance.py` passes 3214 of 3214 checks. The aligned artifacts are qualified by item 2; historical images and test records do not qualify them.
2. **Release artifacts pinned and qualified (done 13 November 2026, for commit `b7c685e7`; before for `4ac53157`, `f1103ac9`, `d25225ae` and, on 12 November, `02b4b4e2`).** See the [qualification record](verification/release-5.3.0/README.md).
   - All 49 images were built without cache from a `git archive` export of that commit, with base images pinned by digest, and are recorded in a source-to-image manifest.
   - Tests: 5,030 unit tests pass (support images 1553, common 870, core 2607). All 16 tests the image builds skip now run and pass: 14 MySQL integration tests against each system's own 5.3.0 database image (this exposed and fixed a GateTunnel test that no longer started), the GateTunnel relay test and Gatekeeper's relay test against the relay deployed from `Arrowhead-test-clouds/Arrowhead-relay`, and SysMonitor's AMQP → MQTT round trip against throw-away brokers. Each of the five core database images initialises its schema on an empty volume and accepts only the deployment password (both architectures).
   - Dependency review (OSV, 228 runtime packages): `mariadb-java-client` 3.5.10 had four low/moderate advisories and is pinned to 3.5.11; no known vulnerability remains.
   - API/DTO review against the upstream 5.2.2 tags: no DTO removed or changed incompatibly, three added. The orchestrators' HTTP `monitor` service is removed in favour of `sysMonitor`, and the HTTP collector now defaults to orchestration.
   - Negative-access review: refusal tests were added for Choreographer's orchestration-callback filter, PlantDescriptionEngine's authorization filter and (in `28833e70`, after the qualified commit; tests only) DeviceQoSEvaluator's use of the common authentication and management filters. Every project now has some. DeviceQoSEvaluator's `qualityEvaluation` is open to every authenticated system, as upstream.
   - Live: the Java test cloud redeployed on exactly these images (47 containers including the five core databases, image IDs checked; its MySQL 8.1 core database volumes upgraded in place) passes its smoke test (14/14) and security regression (194 checks, relay checks skipped without federation). The first live start, on the images of `02b4b4e2`, exposed an intermittent start-up login race in the common library (LocalCloudManager and EventHandler exited once). It is fixed in `d25225ae`, with a test that reproduces it; on the fixed images every system logs in once at start-up and the deployment completes on the first attempt.
   - amd64: the same 49 images built for linux/amd64 from the same commit; all build, all tests pass under amd64 (5,030), and the packaged JARs are identical to the arm64 ones entry by entry. Not deployed on amd64 hardware.
   - OS packages (Trivy, base and release images, both architectures): no critical finding; no high finding in any release image, and no remaining finding has a fixed version. In `d25225ae` the five core database images were still the upstream ones on `mysql:8.1` (120 high findings each, all with a fix, and an `ah-operator` grant that fails on MySQL 8.4); `f1103ac9` moves them to MySQL 8.4.13 with the distribution's updates applied, removes that grant and sets the system user's password from the deployment.
   - Containers: since `4ac53157` the five core application images run as UID/GID 10001 like the support images (before: root, as upstream 5.2.2); a volume mounted at `/app/config` must be writable by that user.
   - Peer address changes: since `b7c685e7` the common library's HTTP client reuses a resolved host name for at most ten seconds and does not cache failed lookups. Before, it kept an address for the DNS TTL (about ten minutes in Docker), so after a peer system was recreated with a new address its calls failed until the entry expired; on one redeployment this made DataManager's start-up blacklist check fail. Reproduced before the fix (about 4.5 minutes of failures after the peer was ready) and checked after it (none once the peer was ready).
   - Not covered: a federated deployment of clouds over the relay (item 4).
3. **Verify fresh-cloud provisioning.** Release qualification targets newly created, disposable test clouds. Verify database/schema initialization, certificate and identity provisioning, and secret configuration from a clean deployment. Migration of existing SQLite/file state or previous cloud identities is outside this release’s acceptance scope; upgrades of existing installations require a separate migration plan.
4. **Defer federation deployment qualification pending the Roadmap WG decision.** End-to-end federation deployment qualification is outside the v5.3.0 release acceptance scope and is not a blocker for the code release or standalone local-cloud deployment. Organization trust provisioning, peer authorization grants, relay ACLs, revocation handling and federation deployment procedures are deferred to v5.4.0 or a later release, subject to the Roadmap WG decision. Existing federation implementation remains available, but v5.3.0 does not claim a qualified end-to-end federation deployment.
5. **Documentation cleanup completed; track remaining integration qualification.** The shared security/deployment guides, support inventory and documentation index were reconciled with the primary source on 12 November 2026; obsolete shared reports and compilations were moved to [_superseeded/](_superseeded/README.md). The [current integration follow-up](documentation/Documentation-V5.3.0.md#current-integration-follow-up) records management-policy requirements, onboarding reserved-name omissions, effective port differences, SoLocalCloudConfigurator specification coverage and verification-tool limitations. This cleanup does not claim those code/configuration or release-qualification tasks are complete.

The release draft therefore distinguishes three deliverables: implemented reference capabilities, a deployable cloud with dated execution evidence, and the remaining work required for an upstream release decision.
