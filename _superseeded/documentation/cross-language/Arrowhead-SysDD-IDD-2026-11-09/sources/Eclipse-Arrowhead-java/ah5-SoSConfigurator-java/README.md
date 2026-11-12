# SoSConfigurator

> Since 8 November 2026 SoSConfigurator is an Arrowhead Java-Spring support system: it registers the `sosConfiguration` service in a cloud's ServiceRegistry, serves it over mutual TLS with certificate-bound identity tokens, keeps its records in its own MySQL database, and has a separate password-protected operator console. Ports 8494 (Arrowhead) and 8495 (console) replace 8096, which lay inside the GateTunnel port pool.

SoSConfigurator builds a System of Systems (SoS) from Arrowhead 5.2.2 local clouds. The operator picks which of the available local clouds take part and which inter-cloud service exchanges they need: a consumer system in one cloud uses a service of a provider in another cloud. The configurator checks the request against the organization's security and authorization policies. If the request passes, it derives the exact Gatekeeper neighbours and ConsumerAuthorization policies that the exchanges need and submits them to [ColonyOS](https://colonyos.io) as one workflow. One ColonyOS executor per local cloud, plus one for the inter-cloud relay, carries out the steps.

It follows the approach of the LocalCloudConfigurator (`Eclipse-Arrowhead-go/ah5-LocalCloudConfigurator-go`):

- a catalog;
- a planner;
- an API and UI that submit ColonyOS workflows;
- executors on the hosts that check every step against their own catalog.

The difference is the level. LocalCloudConfigurator fills one local cloud with systems. SoSConfigurator connects several local clouds.

## Parts

| Part | Where | Role |
|---|---|---|
| Catalog | `catalogs/sos.json` | The organization, the relay, the available local clouds (organization, implementation, inter-cloud support, ports), and per cloud the systems it may let consume services of other clouds and the services it may export |
| Policy engine | `policy/PolicyEngine` | Security (S) and authorization (A) rules. It derives the least-privilege configuration per member cloud |
| Planner | `planner/Planner` | All checks come first (relay, then each cloud), then `neighbours` → `grants` → `verify` → `discover` |
| Support system | `sosconfigurator serve` (default; container in the ColonyOS stack) | Spring Boot application on the Arrowhead common library (`vendor/`). Service `sosConfiguration` on port 8494 (mutual TLS, CERT_AUTH): `catalog` (GET `/api/catalog`), `status` (GET `/api/status`), `plan` (POST `/api/plan`), `submit-deployment` (POST `/api/deployments`), `list-deployments` (GET `/api/deployments`), `get-deployment` (GET `/api/deployments/{id}`); `generalManagement` (`get-log`, `get-config`) for Sysop; sysMonitor |
| Access | `security/SosAccessFilter` | Sysop, the systems of `sos.api.whitelist`, and systems with a ConsumerAuthorization grant of the operation |
| Operator console | port 8495, published on `127.0.0.1` only | The UI at `/` and the same `/api` operations after a password login (`POST /api/session`, HttpOnly SameSite cookie, scrypt hash, lockout per client, Host/Origin check). Browsers need no client certificate |
| Records | MySQL schema `ah_sosconfigurator` (`sosconfigurator-db`) | Who submitted which SoS request and the plan the policies allowed; operational logs. The workflow state stays in ColonyOS |
| Executors | `sosconfigurator executor --sos.role=relay` and `--sos.role=cloud:<Name>` (host processes, non-web Spring applications) | Serve `sos.status` and `sos.step` for their own role only |
| Step scripts | `scripts/sos-cloud.sh`, `scripts/sos-relay.sh` | Run by the executors. A cloud step uses the cloud's own `lib-policies.sh` as Sysop |
| ColonyOS client | `colony/` | ColonyOS v1.9.10 RPC in Java: secp256k1 + SHA3-256 signatures, workflows, assignment and registration. There is no Java ColonyOS SDK |
| ColonyOS stack | `deploy/colonyos/compose.yaml` | TimescaleDB, Colonies server v1.9.10 (`127.0.0.1:50081`), the configurator and its database; colony `arrowhead-sos`; subnet `10.240.91.0/24`; the configurator also joins the selected cloud network |

```
operator ──UI/API──▶ SoSConfigurator ──workflow──▶ Colonies server ◀──assign── relay executor  ──▶ Arrowhead-relay (check)
                     (policy check)                                  ◀──assign── TestCloud executor ─▶ Gatekeeper / ConsumerAuthorization
                                                                     ◀──assign── GoCloud executor   ─▶ Gatekeeper / ConsumerAuthorization
```

## Policies

The configurator applies an SoS only when every rule holds. Each rule has a short id, which appears in refusals.

**Security (S)**

| Rule | Condition |
|---|---|
| S0 | The SoS name is a plain identifier. |
| S1 | Every member cloud is in the catalog, listed once. |
| S2 | An exchange needs at least two members. |
| S3 | The cloud belongs to the relay's organization. The organization CA cross-signs only the organization's own clouds, and Gatekeeper neighbours are per organization. |
| S4 | The cloud's implementation supports inter-cloud exchanges: Gatekeeper GSD/ICN plus GateTunnel forwarding. The Rust v53 candidate does not. |
| S5 | The relay serves the cloud. |
| `cloud-check` (runtime) | Gatekeeper and GateTunnel run. The cloud CA is `<Cloud>.<Org>.arrowhead.eu`. The cross-certificate is name-constrained to `O=<Org>, OU=<Cloud>`. Both relay certificates have the catalogued subject DN, verify through the constrained chain to the organization CA, are not revoked, and do not expire within a day. The cloud CRL exists and is fresh, because the relay fails closed. |
| `relay-check` (runtime) | The broker runs with `allow_anonymous false`, `require_certificate true`, `use_subject_as_username true` and a `crlfile`. For each member, `mqtt/acl` has a block per relay subject (Gatekeeper and GateTunnel). The block reads only the cloud's own inbox of its role and writes only below its own CN. No line grants `#`. The relay watches the member's CRL and shares a network with its Gatekeeper. |

**Authorization (A)**

| Rule | Condition |
|---|---|
| A1 | Consumer and provider clouds are distinct members. |
| A2 | The consumer is a catalogued inter-cloud consumer of its cloud. It is never on the `forbiddenConsumers` list: Sysop, the console, or core, onboarding or inter-cloud systems. |
| A3 | The provider service is exported by the provider cloud. The catalog refuses to export management services or core systems. |
| A4 | The requested operations are a non-empty subset of the export's operations. |
| `cloud-check` (runtime) | The Gatekeeper may call `authorizationManagement.check-policies`. The provider-side GateTunnel holds the local grant for every requested operation. |

**What is applied**

- **Neighbours.** Each member's Gatekeeper gets exactly its SoS peers as neighbour clouds. Peers are the clouds it exchanges with, in either direction. Other catalogued clouds of the organization are removed. Clouds of other organizations are left alone.
- **Grants.** The provider cloud gets one ConsumerAuthorization policy `MGMT|<ConsumerCloud>|<Org>|<provider>|SERVICE_DEF|<service>` per consumer cloud. Each policy whitelists exactly the requested consumers. A catalogued export that has no exchange with a catalogued cloud of the organization has that policy revoked.
- **Verify.** Every granted operation is checked as granted. Every revocation is checked as denied, using a catalogued consumer of that cloud. The neighbour lists are compared with the declared ones.
- **Discover** (`verifyDiscovery`). The consumer cloud's Gatekeeper runs a global service discovery over the relay for each exchange and must find the provider.

ConsumerAuthorization (Java core) refuses an empty default whitelist. An inter-cloud grant is therefore per service definition. The operations are restricted by the export (A4), verified per operation, and the provider's GateTunnel can only call operations it is locally granted.

An SoS is declarative. Applying one sets the inter-cloud configuration of its members to exactly that SoS. To release clouds, apply them as members without exchanges. Clouds outside the request are not touched.

## Rules the executors enforce

- A ColonyOS process carries only the step kind and the operator's SoS request. Each executor evaluates the request again against its own catalog and derives its cloud's configuration itself. A forged process with a `Sysop` consumer or an extra grant is refused before any script runs.
- An executor serves only its own role. It runs only the configurator's own two scripts, from its install directory, with the spec in a private temporary file.
- Management calls use the operator Sysop and `sysop.crt` through the cloud's `lib-policies.sh` (J6). No running system's identity is used. No secret, key, certificate, ACL or relay configuration is created or changed. Adding a cloud to the relay stays an operator task: `mqtt/acl`, the relay's `compose.yaml`, `make -C Arrowhead-relay pki`, then a relay restart.
- Step output goes to the ColonyOS logs with credential-like values masked.

SoSConfigurator is an operations tool, like LocalCloudConfigurator. It is not an Arrowhead system, has no Arrowhead identity, and does not register in a ServiceRegistry.

## Build and test

```sh
make build      # Maven in the maven:3.9-eclipse-temurin-21 container (MVN=mvn for a local Maven); 20 unit tests
make plan REQUEST=requests/refused-rustdev.json   # offline policy check
```

Deployment and the smoke test against TestCloud, GoCloud and Arrowhead-relay are described in [DEPLOYMENT.md](DEPLOYMENT.md).

## Adding a cloud or an exchange

1. Add the cloud to `catalogs/sos.json` with these fields:
   - `name`, `organization`, `dir` and `composeProject`;
   - a unique `executorType`;
   - `intercloud` and `ports`;
   - `consumers` and `exports`.
2. Make it reachable for the relay (see the Arrowhead-relay README) and add it to `relay.clouds`.
3. Run `make keygen register` to create the new executor identity, then `make executors-start`.
4. On another host, run that cloud's executor there: `ROLE=cloud:<Name> make executor` with its key file.

## Listening port

Default **8494** (`server.port` in the packaged `application.properties`). Override with `server.port` in `config/application.properties`, `SERVER_PORT=8494` or `--server.port=8494`.

See [LISTENING-PORTS.md](../LISTENING-PORTS.md) for precedence and all systems.
