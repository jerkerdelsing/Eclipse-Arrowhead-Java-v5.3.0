# CertificateAuthority 5.2.2 (Java-Spring)

> Since 8 November 2026 this system follows the Arrowhead Java-Spring pattern, like the other support systems. The previous native implementation (JDK HttpsServer and SQLite) is no longer built; it is described in `legacy/` and in the history of this folder. See [DEPLOYMENT.md](DEPLOYMENT.md) for deployment.

CertificateAuthority issues the X.509 certificates of one local cloud, keeps track of them, revokes them and publishes the cloud's certificate revocation list (CRL). It is the only system that holds the cloud's issuer signing key.

## Structure

This folder is a complete, independent build and deployment unit. It has the same structure as `ah5-Blacklist-java-spring` and `ah5-TimeManager-java-spring`:

| Part | Where |
|---|---|
| Spring Boot application | `eu.arrowhead.certificateauthority.CertificateAuthorityMain` |
| System and service models | `CertificateAuthoritySystemInfo` |
| ServiceRegistry registration, CERT_AUTH interfaces, sysMonitor | common `ApplicationInitListener` and `SysMonitorPublisher` |
| Authentication (identity token bound to the client certificate) | common `OutsourcedFilter` |
| Caller profile check (only `sy` and `op` certificates) | `security/CallerProfileFilter` |
| Signing, validation, management, CRL | `service/` |
| Storage | JPA entities `IssuedCertificate`, `TrustedKey`, `Logs` in the private MySQL schema `ah_certificate_authority` |
| Arrowhead common library | private source copy under `vendor/` (see `vendor/PROVENANCE.json`) |

## Services

The service definitions, operations and HTTP paths are unchanged from 5.2.2. Every path is served both with and without the `/certificate-authority` base path.

| Service | Operation | Method and path | Who may call |
|---|---|---|---|
| `certificateSigning` | `sign` | POST `/certificate-authority/sign` | Onboarding (profiles on, de, sy), Sysop (on, de, sy, op), CertificateAuthority (sy) |
| `certificateValidation` | `check-certificate` | POST `/certificate-authority/checkCertificate` | any authenticated system |
| | `check-trusted-key` | POST `/certificate-authority/checkTrustedKey` | any authenticated system |
| | `get-crl` | GET `/certificate-authority/crl` | any authenticated system |
| `certificateAuthorityManagement` | `list-certificates`, `list-keys`, `add-key` (PUT `/mgmt/keys`), `delete-key`, `revoke-certificate-by-content` (POST `/mgmt/revocations`) | `/certificate-authority/mgmt/...` | Sysop, CertificateAuthority |
| | `revoke-certificate` | DELETE `/certificate-authority/mgmt/certificates/{id}` | Sysop, CertificateAuthority; Onboarding for the certificates it requested |
| `generalManagement` | `status`, `get-log`, `get-config` | `/certificate-authority/mgmt/general/...` | Sysop, CertificateAuthority |
| `sysMonitor` | echo, power, status over AMQP | routing key `<cloud>.sysmonitor.CertificateAuthority.certificateAuthority` | — |

Issued certificates always have `ca=false`. Reserved system names (the Core and support systems, Sysop) are issued only to Sysop. A name that has a valid certificate requested by another identity cannot be taken over.

### Differences from the native implementation

- Every path, including `check-certificate`, `check-trusted-key`, `get-crl` and `/health`, now needs an identity token bound to the client certificate. The known consumers (Onboarding, LocalCloudManager) already send one.
- Error responses use the Arrowhead `ErrorMessageDTO` (`errorMessage`, `errorCode`, `exceptionType`, `origin`) instead of `{"error": ...}`. Status codes are unchanged, except that a duplicate trusted key or an invalid time now returns 400 instead of 500.
- `generalManagement` adds `get-log` and `get-config` next to `status`.
- The separate plain-HTTP health listener (port 8483) is gone. The container health check is a TCP check of port 8448.
- The system identity is a PKCS#12 keystore, like the other Spring systems. The issuer certificate and key stay PEM files.
- The status document reports `"implementation": "java-spring"` and the certificate counts.

## Build and test

```sh
mvn -B clean verify
```

`scripts/verify.sh` also checks the contents of the JAR, and `scripts/verify-image.sh` builds the image and checks that it runs with a read-only root and refuses an insecure configuration. `docker compose build` builds from this folder only.

The tests cover signing rules, revocation, the CRL, validation, the caller profile check, and every 5.2.2 route with and without the base path. `MySqlEndToEndTest` runs the whole application against a real MySQL schema when started with `-Dca.it.db.url=... -Dca.it.db.password=...`; it is skipped otherwise.

## Moving from the native SQLite store

`scripts/migrate-sqlite-to-mysql.py` converts the native `/data/certificate-authority.db` into SQL for the new schema. Issued certificates, revocations and trusted keys keep their ids. Run it once, as the database root user, before the new CA starts; it refuses a schema that already holds data. See the script header.

## Documentation

The SysD, SysDD and service interface documents are in `documentation/v5.2.2`. `make docs` rebuilds the SysDD. The support GSoSDD is in the roadmap's `WhiteBox_5.3.0` folder.

## Listening port

Default **8448** (`server.port` in the packaged `application.properties`). Override with `server.port` in `config/application.properties`, `SERVER_PORT=8448` or `--server.port=8448`.

See [LISTENING-PORTS.md](../LISTENING-PORTS.md) for precedence and all systems.
