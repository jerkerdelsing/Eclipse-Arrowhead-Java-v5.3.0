# General management registration

The system registers `generalManagement` version `1.0.0` at startup. Its `generic_https` interface uses `CERT_AUTH`, base path `/certificate-authority/mgmt/general`, and the system's advertised address and port.

| Operation | Method | Path |
|---|---|---|
| `status` | GET | `/certificate-authority/mgmt/general/status` |
| `get-log` | POST | `/certificate-authority/mgmt/general/logs` |
| `get-config` | GET | `/certificate-authority/mgmt/general/get-config?keys=...` |

`status` keeps the path of the native 5.2.2 implementation. `get-log` and `get-config` are the common Arrowhead operations; `get-config` returns only the public configuration keys of `CertificateAuthoritySystemInfo`.

Mutual TLS and a certificate-bound identity token are required. The common management filter admits Sysop, CertificateAuthority and Onboarding to management paths (`management.policy=whitelist`); the CA then allows only Sysop and CertificateAuthority for these operations.
