# Gatekeeper 5.2.2

Arrowhead v5.2.2-compatible Gatekeeper preserving the v4.6 GSD/ICN paths with mandatory mTLS, Authentication identity tokens, CN binding, ConsumerAuthorization policy, persistent cloud/relay configuration, and v5 ServiceRegistry registration (directly at `https://serviceregistry:8443` over mutual TLS with the Gatekeeper certificate and its identity token; the registry accepts a token only with the client certificate of the same system). Remote GSD/ICN execution requires a separately configured peer transport.

## sysMonitor

Gatekeeper publishes the Arrowhead 5.2.2 `sysMonitor` service natively
(`src/sysmonitor_publisher.py` with `src/amqp_client.py`, pika). Every 30 seconds it
publishes `echo`, `power` and `status` observations to the AMQP exchange `arrowhead` with
routing key `<LOCAL_CLOUD>.sysmonitor.Gatekeeper.Gatekeeper.<type>` on `AMQP_HOST:AMQP_PORT`
(default `amqp-broker:5671`) using its own certificate and key (SASL `EXTERNAL`); every
message is persistent and broker-confirmed. The offer is registered with `generic_amqps`,
`CERT_AUTH` and `publisherMode=native`; `echo` reports `degraded` when the SQLite store is
unavailable. The broker access list (`amqp/acl`) must allow the system to publish below its
own routing key, and SysMonitor needs a
ConsumerAuthorization grant for `Gatekeeper|sysMonitor`. Tests:
`python -m unittest discover -s test`.

## Inter-cloud exchange

GSD (`/gatekeeper/init_gsd`) and ICN (`/gatekeeper/init_icn`) are executed with the Gatekeepers of the neighbour clouds
(`gatekeeperManagement` clouds of the own organization) over the inter-cloud relay: MQTT over mutual TLS
(`RELAY_HOST`, `RELAY_PORT`, `RELAY_CA_CERT=/certs/org-ca.crt`, `RELAY_CHAIN_CERT=/certs/gatekeeper-relay-chain.crt`, `RELAY_KEY=/certs/gatekeeper-relay.key`). As
provider, the Gatekeeper answers only with providers that its ConsumerAuthorization grants to the remote consumer and
cloud (`authorizationManagement.check-policies`). ICN opens a GateTunnel session on both sides. The DynamicServiceOrchestration
result points to the local GateTunnel. Shared relay client: `src/relay_client.py`.
On the relay the Gatekeeper uses a separate relay identity, not its cloud certificate:
`/C=SE/O=Company/OU=<Cloud>/CN=Gatekeeper.<Cloud>.Company.arrowhead.eu/dnQualifier=sy` with its own key. The chain to
the organization CA runs through the cloud CA's cross-certificate, which is name-constrained to `OU=<Cloud>`. The
relay takes the full subject DN as user name, so no other cloud can pose as this Gatekeeper.
See `../../Arrowhead-relay/README.md`.

## Documentation

[SysD, SysDD and an SD and IDD for each produced service](documentation/v5.2.2/README.md) (`globalServiceDiscovery`, `interCloudNegotiation`, `gatekeeperManagement`, `sysMonitor`), generated from the implementation with the official Arrowhead LaTeX templates by [`documentation-tools`](../documentation-tools/README.md); the PDFs are beside the sources. The earlier `Document/LaTeX` set is in `../../_superseded/2026-10-22/`. They describe the deployed JavaSupportRuntime implementation (role `gatekeeper`).
