# CertificateAuthority 5.2.2

This support system migrates the Arrowhead 4.6 Certificate Authority functions to the
5.2.2 service model. It signs X.509 certificate requests, validates and revokes issued
certificates, manages time-bounded trusted public keys, registers its services in the
ServiceRegistry, and produces the standard `sysMonitor` service over AMQP 0-9-1 with mutual
TLS (local-cloud AMQP broker, exchange `arrowhead`).

All HTTPS endpoints require a client certificate issued by the local-cloud CA. Every operation except
the `/health` liveness check also requires an Arrowhead identity token of the same system as the client
certificate (`Authorization: Bearer IDENTITY-TOKEN//<token>`): the token is verified with Authentication
and its system must equal the certificate CN. Management operations are permitted to `LocalCloudManager`,
`Sysop` and `CertificateAuthority`; signing additionally to `Onboarding`. The system logs in itself as
`CertificateAuthority` (`AUTHENTICATION_URL`, `AUTHENTICATOR_PASSWORD`) and sends its token to the
ServiceRegistry. Persistent state is kept in a private SQLite database.

The default endpoint is `https://certificate-authority:8448/certificate-authority`.

[SysD, SysDD and an SD and IDD for each produced service](documentation/v5.2.2/README.md) (`certificateSigning`, `certificateValidation`, `certificateAuthorityManagement`, `sysMonitor`), generated from the implementation with the official Arrowhead LaTeX templates by [`documentation-tools`](../documentation-tools/README.md); the PDFs are beside the sources. The earlier hand-written `documentation/v5.2.2` set is in `../../_superseded/2026-10-22/`. They describe the deployed JavaSupportRuntime implementation (role `certificate-authority`). The earlier Markdown interface summary and superseded builds are in `../../_superseded/2026-10-23/`.

## Signing policy (onboarding procedure)

`sign` takes `encodedCSR`, optional `profile` (`on`, `de`, `sy`, `op`) and optional `addresses` (DNS names and IP addresses for
the SAN). The CA builds subject and extensions itself; nothing else is copied from the CSR. The common name must be
`<Name>.<Cloud>.<Organization>.arrowhead.eu` of this cloud. Profiles per caller: Onboarding `on`/`de`/`sy`, Sysop
`on`/`de`/`sy`/`op`, LocalCloudManager and CertificateAuthority `sy`. Onboarding certificates live at most 24 hours
(`ONBOARDING_CERT_VALIDITY_HOURS`), others at most 365 days (`CERT_VALIDITY_DAYS`). Onboarding may revoke the certificates
it requested (off-boarding). In the Arrowhead 5.2.2 onboarding procedure every certificate of a device and its systems is
signed here (see `../Onboarding`).

## Certificate revocation list (CRL)

The CA publishes its revocation register as a CRL (security finding H3 of the test-cloud security evaluation):
- **Operations.** `GET /crl` (`certificateValidation get-crl`) returns the current CRL. With `CRL_DIR` set, it is also
  written atomically to `CRL_DIR/ca.crl`.
- **Refresh.** The CRL is rewritten at start, after every revocation and every `CRL_REFRESH_SECONDS` (3600).
- **Validity.** Each CRL is valid `CRL_VALIDITY_HOURS` (168). Consumers fail closed if the CA stops refreshing it.
- **Revocation by content.** `revoke-certificate-by-content` (`POST /mgmt/revocations`, operators only) revokes any
  certificate of this CA by content, also one it did not issue through `sign`, such as a deployment certificate.

In the test clouds the MQTT and AMQP brokers and the inter-cloud relay check client certificates against this CRL.
