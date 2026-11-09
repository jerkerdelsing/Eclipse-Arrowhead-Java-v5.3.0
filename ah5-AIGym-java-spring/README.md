# Arrowhead AIGym 5.2.2

> Current standalone packaging (25 October 2026): build and deploy from this folder. See [DEPLOYMENT.md](DEPLOYMENT.md). Any combined-tree build or live-cloud examples below are historical integration context.

AIGym is an Arrowhead 5.2.2 support system migrated from the v4.6 `rsafg-ai-gym-client` helper library. It provides a small, testable model sandbox and replaces v4 numeric IDs and management APIs with v5 system names, service operations, DynamicServiceOrchestration, Authentication identity tokens, ConsumerAuthorization policies, and certificate-authenticated HTTPS/MQTT.

## Services

The `aiGym` service uses `generic_https` and `CERT_AUTH` at `/aigym`:

| Operation | Method and path | Purpose |
|---|---|---|
| `status` | `GET /aigym/status` | Runtime, model, registration, MQTT (`sysMonitorData`), AMQP (`sysMonitor`), and orchestration state |
| `add-sample` | `POST /aigym/samples` | Add a labelled numeric feature vector |
| `predict` | `POST /aigym/predict` | Predict with the model's nearest labelled samples |
| `observations` | `GET /aigym/observations` | Read the latest authorized `sysMonitorData` observations |

The system also produces the baseline `sysMonitor.echo` and `sysMonitor.power` operations plus advertised status and AI extensions over AMQPS to the local-cloud AMQP broker (exchange `arrowhead`, routing key `TestCloud.sysmonitor.AIGym.aiGym.<type>`). SysMonitor discovers this service through DynamicServiceOrchestration. AIGym separately orchestrates and subscribes to the `sysMonitorData.subscribe` service, making normalized monitoring data available to experiments; `sysMonitorData` stays on the MQTT broker.

Every `aiGym` request requires:

1. a client certificate signed by the local-cloud CA;
2. `Authorization: Bearer IDENTITY-TOKEN//<token>`;
3. a token system name matching the certificate CN; and
4. an allow decision from ConsumerAuthorization for the requested operation.

## Local build

```sh
docker build -t arrowhead-aigym-v5:5.2.2 .
```

The test-cloud integration provisions the `AIGym` identity, certificate, trust store, MQTT ACL (`sysMonitorData`), AMQP access list (`sysMonitor`), and policies. From `Arrowhead-test-cloud`, run `make up-aigym` and then `make smoke-aigym`.

Example authorized calls use the LocalCloudManager identity:

```sh
TOKEN=$(curl -fsS --cacert security/x509/ca.crt --cert security/x509/local-cloud-manager.crt \
  --key security/x509/local-cloud-manager.key -H 'Content-Type: application/json' \
  --data '{"systemName":"LocalCloudManager","credentials":{"password":"LCM-test-52"}}' \
  https://127.0.0.1:8444/authentication/identity/login | jq -r .token)

curl --cacert security/x509/ca.crt --cert security/x509/local-cloud-manager.crt \
  --key security/x509/local-cloud-manager.key -H "Authorization: Bearer IDENTITY-TOKEN//$TOKEN" \
  -H 'Content-Type: application/json' --data '{"model":"demo","features":{"x":1},"target":10}' \
  https://127.0.0.1:8478/aigym/samples
```

## Documentation

[SysD, SysDD and an SD and IDD for each produced service](documentation/v5.2.2/README.md) (`aiGym`, `sysMonitor`), generated from the implementation with the official Arrowhead LaTeX templates by [`documentation-tools`](../documentation-tools/README.md); the PDFs are beside the sources. The earlier Markdown documents are in `../../_superseded/2026-10-23/`.

## Listening port

Default **8478** (`server.port` in the packaged `application.properties`). Override with `server.port` in `config/application.properties`, `SERVER_PORT=8478` or `--server.port=8478`.

See [LISTENING-PORTS.md](../LISTENING-PORTS.md) for precedence and all systems.
