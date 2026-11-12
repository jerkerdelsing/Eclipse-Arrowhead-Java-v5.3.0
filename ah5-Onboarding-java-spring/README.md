# Onboarding 5.2.2

> Current standalone packaging (25 October 2026): build and deploy from this folder. See [DEPLOYMENT.md](DEPLOYMENT.md). Any combined-tree build or live-cloud examples below are historical integration context.

Onboarding is the v5.2.2 migration of the supplied v4.6.2 onboarding controller. The module is integrated into the workspace v5 core reactor. Enrollment is certificate-only (mutual TLS with an approved bootstrap certificate), with generated-key or client-CSR variants, and uses Java 21, Spring Boot 3.4.9, Jakarta APIs and Arrowhead common 5.2.2.

**Scope:** the Arrowhead 5.2.2 onboarding procedure, after Bicaku et al., *Interacting with the Arrowhead Local Cloud: On-boarding Procedure* (2018). A device that holds only a manufacturer-issued bootstrap certificate, and that an operator approved, obtains an onboarding certificate (`on`), then its device certificate (`de`) and ServiceRegistry device entry, then for each approved hosted system a system certificate (`sy`) and an Authentication identity. The systems then register themselves and their services and use orchestration like every other system. Off-boarding withdraws all of it. The local-cloud CertificateAuthority signs every certificate; Onboarding holds no CA key.

| Step | Credential presented | Operation | Result |
|---|---|---|---|
| 0 | Operator (Sysop) | `onboardingManagement` `approve-device` `PUT /onboarding/mgmt/enrollments/{device}` | Bootstrap fingerprint and approved systems stored |
| 1 | Bootstrap certificate | `onboard-with-csr` `POST /onboarding/certificate/csr` (or `-with-name`) | Onboarding certificate (`on`, at most 24 h), registry descriptors |
| 2 | Onboarding certificate | `onboard-device` `POST /onboarding/device` | Device certificate (`de`); device registered in the ServiceRegistry |
| 3 | Device certificate | `onboard-system` `POST /onboarding/system` (per system) | System certificate (`sy`) and identity password |
| 4 | System certificate + identity token | Authentication `identity-login`, ServiceRegistry `systemDiscovery`/`serviceDiscovery` `register`, orchestration | Ordinary Arrowhead system |
| 5 | Device certificate, or operator | `offboard-device` `POST /onboarding/device/offboard` (or `POST /onboarding/mgmt/enrollments/{device}/offboard`) | System and device removed, identities removed, certificates revoked, systems blacklisted |

Only the onboarding and device certificate issued last are accepted, and only while the CertificateAuthority reports them GOOD.

## Build and test

From the v5 core repository:

```sh
mvn -pl onboarding -am clean verify
```

Use Java 21 or newer with Maven 3.9. If the Arrowhead common 5.2.2 artifacts are not already installed, build the accompanying `ah5-common-reference` repository first:

```sh
mvn -f ../ah5-common-reference/pom.xml install -DskipTests
```

The parent enables remote Checkstyle configuration. For a disconnected build with all dependencies cached, use `-o -Dcheckstyle.skip`; this skips style checks, not compilation or tests. See [VALIDATION.md](VALIDATION.md) for actual verification and deployment limits.

## Deployment

1. Create the module-owned MySQL database using `src/main/resources/database/create_empty_db.sql` and `create_tables.sql`. Provision a database account with SELECT/INSERT on `ah_onboarding.logs`; the application uses `ddl-auto=validate` and does not manage the schema.
2. Supply a v5 **system-profile** HTTPS server certificate for `Onboarding.<cloud>.<organization>.arrowhead.eu`, with its private key/chain in a PKCS#12 keystore. Use a separate trust store containing the applicable cloud/manufacturer roots. Client authentication must be `need`: every caller, including an enrolling device, presents a trusted client certificate.
3. Deploy the local-cloud CertificateAuthority and grant Onboarding its operations: ConsumerAuthorization policies for `certificateSigning`, `certificateValidation`, `certificateAuthorityManagement` `revoke-certificate`, Authentication `identityManagement` `identity-mgmt-create`/`-remove` and Blacklist `blacklistManagement` `create`/`remove`; ServiceRegistry management whitelist and direct access for `device-create`, `device-update`, `device-remove`, `system-remove` (the test clouds' `scripts/configure-onboarding-policy.sh` and `compose.yaml`).
4. Configure the required environment variables listed below and the registry address/port.
5. Start the executable JAR:

```sh
java -jar onboarding/target/arrowhead-onboarding-5.3.0.jar \
  --spring.config.additional-location=file:/secure/onboarding.properties
```

Required environment variables in the shipped configuration:

- `ONBOARDING_DB_PASSWORD` (optional username override: `ONBOARDING_DB_USER`).
- `ONBOARDING_TLS_KEYSTORE`, `ONBOARDING_TLS_PASSWORD`, `ONBOARDING_TLS_ALIAS`: the Onboarding system certificate; its issuer is taken as the local-cloud CA.
- `ONBOARDING_TRUSTSTORE`, `ONBOARDING_TRUSTSTORE_PASSWORD`: the cloud CA and the approved manufacturer CAs.
- `ONBOARDING_HOST` for the advertised host; defaults to `localhost`.

Devices are approved at run time, by an operator, with `onboardingManagement` (in the test clouds `scripts/onboarding-approve-device.sh <Device> <bootstrap.pem> <System>...`). The approval is the SHA-256 fingerprint of the bootstrap certificate; its subject may differ from the device name. Use direct TLS termination: certificates and peer IP are taken from the servlet TLS connection; forwarded identity headers are not accepted. Prefer CSRs: without one, Onboarding generates the key and returns it once.

## API and compatibility

| Method | Path | Input |
|---|---|---|
| POST | `/onboarding/certificate/name` | `{"commonName":"Device1.TestCloud.Company.arrowhead.eu"}` |
| POST | `/onboarding/certificate/csr` | `{"certificateSigningRequest":"<Base64 DER PKCS#10>"}` |
| POST | `/onboarding/device` | `{"certificateSigningRequest":"...","addresses":["172.19.0.9"],"metadata":{}}` |
| POST | `/onboarding/system` | `{"systemName":"DemoSensor","certificateSigningRequest":"..."}` |
| POST | `/onboarding/device/offboard` | none |
| GET, PUT, POST, DELETE | `/onboarding/mgmt/enrollments[/{device}[/offboard]]` | `{"bootstrapFingerprint":"<sha256>","systems":["DemoSensor"]}` for PUT |
| GET | `/onboarding/echo` | Liveness (any trusted client certificate), returns `OK` |
| POST | `/onboarding/general/mgmt/logs` | Standard v5 LogRequest |
| GET | `/onboarding/general/mgmt/get-config` | Standard v5 optional `keys` query |

The registered `onboarding` service has version `1.0.0` and operations `onboard-with-name`, `onboard-with-csr`, `onboard-device`, `onboard-system` and `offboard-device`; `onboardingManagement` has `query-enrollments`, `get-enrollment`, `approve-device`, `offboard-device` and `remove-enrollment`. Both use `generic_https` with `CERT_AUTH`; `onboardingManagement` additionally needs an operator identity token bound to the operator certificate. `generalManagement` uses v5 authentication (`outsourced`: an identity token of the same system as the client certificate) and management filtering (system operators by default, i.e. the `Sysop` token presented with the `Sysop` operator certificate). The system logs in as `Onboarding` with `authenticator.credentials` and sends its token to the ServiceRegistry.

`sysMonitor` 1.0.0 is published natively by the Arrowhead 5.2.2 common-library publisher when `sysmonitor.enabled=true`: `echo`, `power` and `status` observations every 30 seconds to the AMQP exchange `arrowhead` with routing key `<local.cloud>.sysmonitor.Onboarding.Onboarding.<type>`, over mutual TLS (`generic_amqps`, `CERT_AUTH`, `publisherMode=native`). Broker and stores are set with `sysmonitor.broker.address`, `sysmonitor.broker.port` and `sysmonitor.ssl.*` (defaulting to `server.ssl`). The framework MQTT API stays disabled. See the [sysMonitor SD](documentation/v5.2.2/Onboarding-v5.2.2-sysMonitor-SD.tex) and [IDD](documentation/v5.2.2/Onboarding-v5.2.2-sysMonitor-AMQP-IDD.tex).

OpenAPI is available at `/v3/api-docs` and `/swagger-ui/index.html` over HTTPS. The [onboarding IDD](documentation/v5.2.2/Onboarding-v5.2.2-onboarding-HTTP-IDD.tex) and the [onboardingManagement IDD](documentation/v5.2.2/Onboarding-v5.2.2-onboardingManagement-HTTP-IDD.tex) define the response fields and encodings. Success responses are not cacheable. The CSR variant never returns a private key. Required ServiceRegistry discovery is completed before signing.

Breaking changes from v4:

- `creationRequestDTO.commonName` becomes top-level `commonName`; arbitrary imported private keys are no longer accepted. Use a CSR for an existing key.
- The v4 shared-secret enrollment is removed; a device enrolls only with an approved, trusted bootstrap certificate.
- `rootCertificate`, `intermediateCertificate` and fixed legacy endpoint fields become `certificateChain` (complete leaf-first chain), `onboardingCertificate`, `services` (v5 service instances) and `nextStep`.
- `certificateType` is `ON_BOARDING`, and the issued X.509 DN qualifier is `on`. The certificate must not be treated as a system credential.
- Separate DeviceRegistry/SystemRegistry and legacy Orchestrator discovery are replaced by `deviceDiscovery`, `systemDiscovery` and `serviceDiscovery` from v5 ServiceRegistry. No orchestration service is required to enroll.
- Management moves to the standard v5 endpoints and schema. No shared v4 database is reused.

Clients must migrate their payloads and credential handoff explicitly; preserving four path names does not provide v4 wire compatibility. Keep the old deployment available for rollback until migrated clients pass site acceptance.

## Documentation

[SysD, SysDD and an SD and IDD for each produced service](documentation/v5.2.2/README.md) (`onboarding`, `onboardingManagement`, `generalManagement`, `sysMonitor`), generated from the implementation with the official Arrowhead LaTeX templates by [`documentation-tools`](../documentation-tools/README.md); the PDFs are beside the sources. The earlier hand-written `documentation/v5.2.2` set is in `../../_superseded/2026-10-22/`. [Sources](documentation/v5.2.2/SOURCES.md) lists what the content was taken from.

## Listening port

Default **8435** (`server.port` in the packaged `application.properties`). Override with `server.port` in `config/application.properties`, `SERVER_PORT=8435` or `--server.port=8435`.

See [LISTENING-PORTS.md](../LISTENING-PORTS.md) for precedence and all systems.
