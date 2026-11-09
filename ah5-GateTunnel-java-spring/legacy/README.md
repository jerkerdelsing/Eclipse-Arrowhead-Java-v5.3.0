# GateTunnel 5.2.2

GateTunnel is the Arrowhead 5.2.2-compatible successor to the Arrowhead 4.6
Gateway microsystem. Its native API base path is `/gatetunnel`; compatibility
aliases remain under `/gateway` for the v4 connection-control operations.

## Security

- TLS 1.2 or TLS 1.3 with mandatory client certificates
- Arrowhead Authentication identity tokens
- X.509 common-name and token-identity binding
- ConsumerAuthorization policies for connection, key, and management services
- `generic_https` / `CERT_AUTH` ServiceRegistry interfaces

The expected system certificate common name is:

`GateTunnel.TestCloud.Company.arrowhead.eu`

## Registered services

- `gateTunnelConnection`
- `gateTunnelKey`
- `gateTunnelManagement`
- `echo`
- `sysMonitor` (AMQP, native)

## Runtime configuration

See `compose.example.yaml`. The service requires the local-cloud CA, GateTunnel
certificate and private key mounted under `/certs`. Run the included identity
and policy scripts after Authentication and ConsumerAuthorization are ready.
GateTunnel calls the ServiceRegistry directly (`SERVICE_REGISTRY_URL`, default
`https://serviceregistry:8443`) over mutual TLS with its own certificate and its
identity token: the registry runs the outsourced policy and accepts a token only
together with the client certificate of the same system.

The service implements the secured connection/session control lifecycle and
persistent port allocation. Cross-cloud payload forwarding requires a separately
deployed relay transport; no unauthenticated raw TCP bridge is exposed.

## sysMonitor

GateTunnel publishes the Arrowhead 5.2.2 `sysMonitor` service natively
(`src/sysmonitor_publisher.py` with `src/amqp_client.py`, pika). Every 30 seconds it
publishes `echo`, `power` and `status` observations to the AMQP exchange `arrowhead` with
routing key `<LOCAL_CLOUD>.sysmonitor.GateTunnel.GateTunnel.<type>` on `AMQP_HOST:AMQP_PORT`
(default `amqp-broker:5671`) using its own certificate and key (SASL `EXTERNAL`); every
message is persistent and broker-confirmed. The offer is registered with `generic_amqps`,
`CERT_AUTH` and `publisherMode=native`; `echo` reports `degraded` when the SQLite store is
unavailable. The broker access list (`amqp/acl`) must allow the system to publish below its
own routing key, and SysMonitor needs a
ConsumerAuthorization grant for `GateTunnel|sysMonitor`. Tests:
`python -m unittest discover -s test`.

## Inter-cloud exchange

Sessions negotiated by the Gatekeepers (`/gatetunnel/intercloud/provider`, `/gatetunnel/intercloud/consumer`, local Gatekeeper
only) carry HTTPS requests over the inter-cloud relay. The consumer side is an HTTPS listener on 8200–8299 for the
session consumer only. The provider side accepts only the negotiated peer GateTunnel and paths below the provider's base
path, and calls the provider with the GateTunnel certificate and identity token (`X-Arrowhead-Remote-Consumer`). Relay
settings: `RELAY_HOST`, `RELAY_PORT`, `RELAY_CA_CERT`, `RELAY_CHAIN_CERT=/certs/gatetunnel-relay-chain.crt`, `RELAY_KEY=/certs/gatetunnel-relay.key`.
On the relay the GateTunnel uses a separate relay identity, not its cloud certificate:
`/C=SE/O=Company/OU=<Cloud>/CN=GateTunnel.<Cloud>.Company.arrowhead.eu/dnQualifier=sy` with its own key. The chain to
the organization CA runs through the cloud CA's cross-certificate, which is name-constrained to `OU=<Cloud>`. The
relay takes the full subject DN as user name, so no other cloud can pose as this GateTunnel.
See `../../Arrowhead-relay/README.md`.

## Documentation

[SysD, SysDD and an SD and IDD for each produced service](documentation/v5.2.2/README.md) (`gateTunnelConnection`, `gateTunnelKey`, `gateTunnelManagement`, `sysMonitor`), generated from the implementation with the official Arrowhead LaTeX templates by [`documentation-tools`](../documentation-tools/README.md); the PDFs are beside the sources. The earlier `Document/LaTeX` set is in `../../_superseded/2026-10-22/`. They describe the deployed JavaSupportRuntime implementation (role `gatetunnel`).
