# General management registration

The system registers `generalManagement` version `1.0.0` (metadata `systemVersion` 5.2.2) through the common `ApplicationInitListener`. Its `generic_https` interface uses `CERT_AUTH` and the advertised address and port.

| Operation | Method | Path |
|---|---|---|
| `status` | GET | `/gatetunnel/mgmt/general/status` |
| `get-log` | POST | `/gatetunnel/mgmt/general/logs` |
| `get-config` | GET | `/gatetunnel/mgmt/general/get-config` |

`status` returns the same document as 5.2.2 (registration, relay link, inter-cloud and control sessions). `get-log` reads the `logs` table; `get-config` returns the public configuration keys. Mutual TLS and a certificate-bound identity token are required. Access is limited to LocalCloudManager and Sysop by the management filter (whitelist policy) and the GateTunnel caller filter. `api.GateTunnelRouteContractTest` and `it.MySqlEndToEndTest` cover the access rules.
