# Device QoS Evaluator System (ah5-device-qos-evaluator-java-spring)

> Current standalone packaging (25 October 2026): build and deploy from this folder. See [DEPLOYMENT.md](DEPLOYMENT.md). Any combined-tree build or live-cloud examples below are historical integration context.

Support system for enabling device-related Quality of Service measurement collection and evaluation in an Arrowhead Local Cloud.

Visit the official documentation site:

https://aitia-iiot.github.io/ah5-docs-java-spring/home/welcome/

[SysD, SysDD and an SD and IDD for each produced service](documentation/v5.2.2/README.md) (`generalManagement`, `qualityEvaluation`, `deviceQualityDataManagement`, `sysMonitor`), generated from the implementation with the official Arrowhead LaTeX templates by [`documentation-tools`](../documentation-tools/README.md); the PDFs are beside the sources. The earlier `documentation/v5.2` set is in `../../_superseded/2026-10-22/`.

## Listening port

Default **8472** (`server.port` in the packaged `application.properties`). Override with `server.port` in `config/application.properties`, `SERVER_PORT=8472` or `--server.port=8472`.

See [LISTENING-PORTS.md](../LISTENING-PORTS.md) for precedence and all systems.

## Device agents

Augmented measurements (CPU load, memory, network load) come from a device agent (`device_agent/python`). The agent
is an optional program on the measured device, not part of this system: it is not registered and offers no Arrowhead
service.

The channel uses mutual TLS by default:

| Setting | Default | Meaning |
|---|---|---|
| `device.agent.ssl.enabled` | `true` | HTTPS with this system's certificate and truststore; the agent's certificate must be issued by the cloud CA for the device address (hostname verification). No identity token is sent. `false` (plain HTTP) is refused by the secure profile. |
| `device.agent.port` | `59473` | Listening port of the agents (`DEVICE_AGENT_PORT` in the deployment configuration). |

The agent answers only the certificate common names in its `tls.allowed_clients`, normally
`DeviceQoSEvaluator.<cloud>.<organization>.arrowhead.eu`. It checks the client certificate against the cloud CA's CRL
and refuses every client if the CRL is missing or expired. See `device_agent/python/ReadMe.md`.

Limit: like every outbound call of the Java reference systems, the evaluator does not check the agent's certificate
against a CRL. It relies on the CA chain and the device address in the certificate.
