# Arrowhead 5.2 local test cloud

A Docker Compose deployment of an Eclipse Arrowhead 5.2 local cloud, pinned by default to the 5.2.2 security-fix release.

The default stack contains the mandatory operational core (ServiceRegistry, DynamicServiceOrchestration, ConsumerAuthorization, and Authentication), every support system released for 5.2 (Blacklist, TranslationManager, and DeviceQoSEvaluator), a v5-native test DataManager, and a v5.2.2 Gatekeeper compatibility microsystem. SimpleStoreServiceOrchestration remains an opt-in profile.

It also includes a LocalCloudManager microsystem with a management API/collector and a separately served graphical dashboard. The API registers `localCloudManagement` in ServiceRegistry and exposes a constrained container lifecycle interface; the browser-facing service never receives the Docker socket.

An X.509-secured `RandomDataProducer` publishes a cryptographically generated integer from 0 through 100 to DataManager every 30 seconds. It first obtains an identity token from Authentication, then asks DynamicServiceOrchestration for an authorized `historian` provider and builds the destination from the returned `generic_https`/`CERT_AUTH` interface. DataManager verifies both the token and the client-certificate identity before enforcing the operation allowlist. The generated CA private key stays on the host and is never mounted into a container.

> Snapshot of the test-cloud integration; the maintained deployment is in `Arrowhead-test-cloud`. `datamanager/` is the superseded Python DataManager.

`SysMonitor` implements the accompanying SysDD and IDDs. Two X.509-only brokers carry the monitoring traffic: the AMQP 0-9-1 broker `amqp-broker` carries provider `sysMonitor` observations (SysMonitor receives only the observation types selected in `sysMonitorManagement`), and the MQTT 5 broker carries the normalized `sysMonitorData`. SysMonitor stores the observations of all monitored microsystems itself: every validated observation is written to its own observation history in SQLite (volume `sysmonitor-data`, retention `SYSMONITOR_RETENTION_DAYS`, default 30 days) and is readable through the registered `sysMonitorHistory` service (`list-systems`, `query`, `latest`), which ConsumerAuthorization grants to LocalCloudManager (`scripts/configure-sysmonitor-policy.sh`). DataManager stores no `sysMonitor` data. SysMonitor also keeps subscription policies and an outbox for the `sysMonitorData` republication, advertises its mTLS management API in ServiceRegistry, obtains Authentication identity tokens, and orchestrates the authorized `sysMonitor` services. Every core and support system publishes `sysMonitor` natively (see below); DataManager's `sysMonitor` replaces its v4.6 `echo` service; SysMonitor refreshes its authorized orchestration result every 30 seconds and subscribes to the returned provider topic.

## Requirements

- Docker Engine or Docker Desktop with Compose v2
- About 8 GB of free memory for the default stack
- `curl` for the smoke test

The upstream 5.2.2 images are published for `linux/amd64`. The Compose file pins that platform, allowing Docker Desktop to run them through emulation on Apple Silicon. Native amd64 Linux does not incur that overhead.

## Start the cloud

```sh
make init
```

Edit `.env` and replace both example passwords, then run:

```sh
make pull
make up
make smoke
```

Open the management dashboard at [http://127.0.0.1:8490](http://127.0.0.1:8490). To build or update only the management microsystem, run `make up-monitor`.
The HTML source is not a standalone application; if opened as a local file it redirects to the deployed dashboard so API telemetry remains available.

The dashboard's **DM** view graphically plots numeric DataManager historian streams and lists recent SenML records. DataManager content is fetched server-side using the LocalCloudManager X.509 client certificate, so private keys are never exposed to the browser. The overview retains a live five-minute runtime activity graph and shows the secured DataManager-to-broker monitoring publication plus SysMonitor's orchestrated subscription.

`make up` waits for all database health checks. The Arrowhead containers populate their default configuration and test certificates into `runtime/<System>/config` on their first start. API ports are exposed only on `127.0.0.1` by default; databases are not exposed to the host.

## Deployment variants

| Command | Systems added to the default stack |
| --- | --- |
| `make up` | None; dynamic orchestration plus all released support systems |
| `make up-simple` | SimpleStoreServiceOrchestration (port 8442) |
| `make up-auth` | Alias for the fully authenticated default stack |
| `make up-all` | Default stack plus SimpleStoreServiceOrchestration |

Dynamic and SimpleStore orchestration intentionally use separate databases and ports, so they can be tested side by side.

Authentication is enabled by default. `make up` creates local-CA identities, enables TLS 1.2/1.3 with mandatory client certificates, provisions system identities, and configures ConsumerAuthorization policies. DynamicServiceOrchestration, SimpleStoreServiceOrchestration, and ConsumerAuthorization run with `outsourced` authentication. The deployment also publishes ServiceRegistry's bootstrap management dependency through a private `generic_https`/`CERT_AUTH` interface and grants only DynamicServiceOrchestration access to it.

Arrowhead 5.2.2 has a pre-login discovery dependency: a core system must discover Authentication before it can obtain its first token. The deployment therefore keeps ServiceRegistry as a narrowly scoped `declared` bootstrap anchor, with a management whitelist and internal fixed-identity registry adapters. These adapters are private to the Compose network and expose only the bootstrap operations. Normal service consumption uses Authentication tokens, ConsumerAuthorization, mTLS, and provider-side token/certificate binding.

For a host-side mTLS check, use the generated LocalCloudManager identity:

```sh
curl --cacert security/x509/ca.crt \
  --cert security/x509/local-cloud-manager.crt \
  --key security/x509/local-cloud-manager.key \
  https://127.0.0.1:8444/authentication/monitor/echo
```

## Endpoints

| System | Default URL |
| --- | --- |
| ServiceRegistry | `http://127.0.0.1:8443` |
| DynamicServiceOrchestration mTLS API | `https://127.0.0.1:8441` |
| ConsumerAuthorization mTLS API | `https://127.0.0.1:8445` |
| Blacklist | `http://127.0.0.1:8464` |
| TranslationManager | `http://127.0.0.1:8465` |
| DeviceQoSEvaluator | `http://127.0.0.1:8472` |
| DataManager | `http://127.0.0.1:8461` |
| DataManager mTLS API | `https://127.0.0.1:8461` |
| RandomDataProducer mTLS status | `https://127.0.0.1:8500` |
| SysMonitor mTLS management API | `https://127.0.0.1:8475` |
| sysMonitor AMQP 0-9-1 over mTLS | `amqps://127.0.0.1:5671` |
| sysMonitorData MQTT 5 over mTLS | `mqtts://127.0.0.1:8883` |
| Gatekeeper 5.2.2 mTLS API | `https://127.0.0.1:8449` |
| GateTunnel 5.2.2 mTLS API | `https://127.0.0.1:8453` |
| SimpleStoreServiceOrchestration mTLS API (profile) | `https://127.0.0.1:8442` |
| Authentication mTLS API | `https://127.0.0.1:8444` |
| LocalCloudManager dashboard | `http://127.0.0.1:8490` |

ServiceRegistry and the support systems use HTTP only inside the private Compose network, with application calls protected by Arrowhead identity tokens. Authentication, both orchestration implementations, ConsumerAuthorization, DataManager, and RandomDataProducer use HTTPS with mandatory local-CA client certificates. The orchestration APIs additionally require outsourced identity tokens and ConsumerAuthorization decisions.

### Gatekeeper 4.6 compatibility

The `gatekeeper` microsystem preserves the v4.6 resource paths used by existing
GSD/ICN clients (`/gatekeeper/init_gsd`, `/gatekeeper/init_multi_gsd`, and
`/gatekeeper/init_icn`) while adopting the Arrowhead 5.2.2 system and
service-operation model. It has an independent SQLite store for neighbor clouds
and relays, registers `globalServiceDiscovery`, `interCloudNegotiation`,
`gatekeeperManagement`, and `echo` as `generic_https`/`CERT_AUTH` services,
and binds every identity token to the caller's X.509 common name. Management is
restricted to `LocalCloudManager`; orchestration operations are restricted to
the registered orchestration systems and their ConsumerAuthorization policies.

Arrowhead 5.2.2 does not define an official Gatekeeper wire protocol. The local
compatibility API and secure control plane are deployable now; actual cross-cloud
GSD/ICN results require a separately configured remote Gatekeeper transport and
trust federation. With no remote peers configured, GSD returns an empty result
and ICN reports that negotiation is unavailable instead of fabricating a route.

### GateTunnel (Gateway 4.6 successor)

The v5.2.2 replacement for the v4.6 `Gateway` is named `GateTunnel`. It retains
compatibility aliases for `/gateway/connect_provider`,
`/gateway/connect_consumer`, `/gateway/close_sessions`, and
`/gateway/publickey`, while its native v5 base path is `/gatetunnel`. GateTunnel
uses an independent persistent session store and publishes
`gateTunnelConnection`, `gateTunnelKey`, `gateTunnelManagement`, and `echo`
as `generic_https`/`CERT_AUTH` services. Every control request requires both a
trusted client certificate and a matching Arrowhead identity token, with
ConsumerAuthorization policies limiting connection control to Gatekeeper and
the orchestration systems and management to LocalCloudManager.

The session-control lifecycle and port allocation are implemented. A production
cross-cloud data plane still requires an external relay/transport deployment;
GateTunnel does not expose an unauthenticated raw TCP bridge when no relay is
configured.

## Operations

```sh
make status       # container state
make logs         # follow logs for default and profiled services
make smoke        # check the three mandatory core monitor endpoints
make smoke-all    # also check support and enabled optional systems
make configure-orchestration-security-policies # restore the least-privilege DSO bootstrap grant
make up-monitor   # build/deploy the management API and graphical UI
make up-datamanager # build/deploy DataManager and refresh the monitor
make up-random-producer # generate X.509 material and deploy the 30-second producer
make up-sysmonitor # build/deploy SysMonitor, the AMQP and MQTT brokers, and secured telemetry path
make down         # stop/remove containers; preserve DB volumes and config
make clean        # also delete all local-cloud database volumes
```

Configuration is bind-mounted from `runtime/` and survives `make down`. Database state is held in named Docker volumes. `make clean` is destructive and resets every database, but leaves generated configuration files in `runtime/`.

For access from another machine, set `BIND_ADDRESS=0.0.0.0` (or a specific host address). The systems advertise their Compose DNS names to one another; external clients should use the published host address and ports. Expose them only on a trusted test network and review the generated security configuration first.

## Version scope

The Compose model follows the official Arrowhead v5 aggregated deployment layout and image names. Version 5.2.2 is used because it is the latest 5.2 service release and contains security fixes. To reproduce 5.2.0 exactly, set `AH_VERSION=5.2.0` in `.env`.

Official references:

- [Arrowhead 5 Docker deployment documentation](https://aitia-iiot.github.io/ah5-docs-java-spring/home/getting_started/docker/docker_aggregated/)
- [Arrowhead 5.2.2 release](https://projects.eclipse.org/projects/iot.arrowhead/releases/5.2.2)
- [Arrowhead 5 Java/Spring core source](https://github.com/eclipse-arrowhead/ah5-core-java-spring/tree/v5.2.2)
