# DeviceQoSEvaluator device agent

A lightweight monitoring program for a device. It measures CPU load, memory and network load, and serves the figures
to the Eclipse Arrowhead DeviceQoSEvaluator (`GET /device-qos`). The DeviceQoSEvaluator scores providers with these
figures, so the channel is protected.

The agent is not an Arrowhead system. It is not registered and offers no Arrowhead service; the DeviceQoSEvaluator
reads it directly at the device address.

## Security

Default since 2026-11-09:

- **Mutual TLS (TLS 1.2 or later).** The agent presents a certificate issued by the cloud CA. The DeviceQoSEvaluator
  verifies it against the device address, so the certificate's subject alternative names must contain the address
  the DeviceQoSEvaluator uses for the device (IP or DNS name). The client must present a certificate from the same CA.
- **Allowed clients.** Only the certificate common names in `tls.allowed_clients` get data, normally
  `DeviceQoSEvaluator.<cloud>.<organization>.arrowhead.eu`. Others get HTTP 403.
- **Revocation.** The client certificate is checked against the cloud CA's CRL (`tls.crl`). A missing, unreadable or
  expired CRL refuses every client. The agent re-reads the CRL, CA and its own certificate within 30 s of a change.
- **Plain HTTP** is possible only for isolated development: with `tls.enabled: false` and a loopback
  `server.address` (`127.0.0.1`, `::1` or `localhost`). The agent refuses to start with plain HTTP on any other
  address.
- Only `GET /device-qos` is served. The agent receives no identity token.

## Requirements

- Python 3.9 or later
- `pip install -r requirements.txt` (psutil, PyYAML)

## Certificates

Issue the agent a certificate from the cloud CA (for example with the CertificateAuthority) with:

- extended key usage `serverAuth`;
- subject alternative names that cover the device address registered in the ServiceRegistry.

Copy the cloud CA certificate and its CRL next to it, and keep the CRL current (the cloud publishes a new one
regularly).

## Configure

Edit `config.yaml`. Relative paths are relative to the configuration file.

| Key | Default | Meaning |
|---|---|---|
| `server.address` | `0.0.0.0` | Listening address |
| `server.port` | `59473` | Listening port; overridden by `SERVER_PORT`, then by `--server.port` |
| `tls.enabled` | `true` | `false` only with a loopback `server.address` |
| `tls.certificate`, `tls.private_key` | required | The agent's certificate and key |
| `tls.ca` | required | Cloud CA certificate (verifies clients) |
| `tls.crl` | required | Cloud CA CRL |
| `tls.allowed_clients` | required | Certificate common names that may read the measurements |
| `measurements.*.enabled` | | Which measurements run |
| `measurements.network_load.link_address` | | IP address of the measured network interface |

The configuration file is `config.yaml` next to `run.py`. Select another with `--config <file>` or
`DEVICE_AGENT_CONFIG`.

## Run

```
python run.py [--config config.yaml] [--server.port 59473]
```

If the agent uses a port other than 59473, set `device.agent.port` in the DeviceQoSEvaluator to the same value.

## Test

```
python -m unittest discover -s tests
```

The tests build a throwaway PKI with `openssl`. They cover mutual TLS, the allow-list, a revoked and an expired CRL
case, plain HTTP refusal and the port overrides.
