# General management registration

The system registers `generalManagement` version `1.0.0` (metadata `systemVersion=5.2.2`) during its normal authenticated startup registration. Its `generic_https` interface uses `CERT_AUTH` and the system's address and port.

| Operation | Method | Path |
|---|---|---|
| `status` | GET | `/gatekeeper/mgmt/general/status` |
| `get-log` | POST | `/gatekeeper/mgmt/general/logs` |
| `get-config` | GET | `/gatekeeper/mgmt/general/get-config` |

`status` returns the same document as `/gatekeeper/mgmt/status`: status, system, version, implementation, registration and relay link. `get-log` and `get-config` are the common Arrowhead operations: log entries from the `logs` table, and the public configuration keys.

Mutual TLS and a certificate-bound identity token are required. Access is limited to LocalCloudManager and the system operator, by the common management filter (whitelist policy) and Gatekeeper's caller rules. The route contract test covers permitted access, the refusal of other systems, and missing or mismatched credentials.
