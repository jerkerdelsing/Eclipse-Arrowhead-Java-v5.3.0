# General management registration

The system registers `generalManagement` version `1.0.0` through the common `ApplicationInitListener` during its authenticated startup registration. Its `generic_https` interface uses `CERT_AUTH` and the system's advertised address and port.

| Operation | Method | Path |
|---|---|---|
| `status` | GET | `/sysmonitor/mgmt/general/status` |
| `get-log` | POST | `/sysmonitor/mgmt/general/logs` |
| `get-config` | GET | `/sysmonitor/mgmt/general/config` |

`status` returns the system's status document (HTTP 200 running, 503 degraded), as in the native 5.2.2 implementation. `get-log` and `get-config` are the common-library operations (log entries from the `logs` table, public configuration keys). Mutual TLS and a valid certificate-bound identity token are required. The common management filter (`management.policy=whitelist`) admits only Sysop and LocalCloudManager.

`SysMonitorRouteContractTest` covers permitted access, denial of ordinary consumers, and missing or mismatched identity credentials.
