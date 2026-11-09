# General management registration

The system advertises `generalManagement` version `1.0.0` during its normal authenticated startup registration. Its `generic_https` interface uses `CERT_AUTH` and the system's advertised address and listening port.

| Operation | Method | Path |
|---|---|---|
| `status` | GET | `/datamanager/mgmt/general/status` |

The operation returns the system's current status. Mutual TLS and a valid certificate-bound identity token are required. Management access is limited to authenticated system operators (the existing sysop-only policy). Existing system-specific services and management routes remain available.

This registration advertises the implemented status operation; it does not advertise unimplemented log or configuration operations. Registration is performed by the system itself, so recreating the service restores the offer without an external registry patch.

The source tests cover permitted access, denial of ordinary consumers, and missing/mismatched identity credentials. Deployment verification also checks the actual registry offer, interface, and endpoint against the live Java test cloud.
