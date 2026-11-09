# Verification record — 2026-10-14 (onboarding procedure)

## Build and unit tests

```sh
mvn -q -o -Dcheckstyle.skip package     # Maven 3.9.9, Temurin 21, Arrowhead common/core 5.2.2
```

24 tests passed, zero failures/errors/skips:

| Suite | Tests | Evidence |
|---|---:|---|
| BootstrapAuthenticationTest | 5 | TLS required, missing certificate, Basic credentials refused, approval by fingerprint in the enrollment table, off-boarded device refused |
| CloudCertificateAuthenticationTest | 4 | Good device certificate accepted (name and serial), wrong profile, foreign issuer (no CA call), revoked certificate |
| CsrSupportTest | 3 | Generated CSR passes, name mismatch, missing proof of possession and malformed input |
| DiscoveryServiceTest | 3 | Exact v5 lookup, descriptor propagation, missing/null dependency rejection |
| OnboardingProcedureTest | 9 | `on` certificate via the CertificateAuthority, device step (certificate addresses vs. registry addresses), system step preconditions, system certificate and identity, failed identity revokes the certificate, only the latest `on`/`de` certificate accepted, re-issued `on` certificate revokes the previous one, off-boarding order and continuation after a failure, approval validation |

The embedded signer (`CertificateIssuer`) and its tests (`CertificateIssuerTest`, `HttpsEnrollmentTest`) were removed on
2026-10-14 together with the embedded CA; they are kept under `_superseded/2026-10-14-onboarding-embedded-signer/`.

## End-to-end procedure in both test clouds

`Arrowhead-test-cloud/scripts/smoke-onboarding-procedure.sh` and the GoCloud copy passed on 2026-10-14 against the deployed
CertificateAuthority, ServiceRegistry (Java and Go), Authentication, ConsumerAuthorization, DynamicServiceOrchestration,
DataManager and Blacklist: the operator approves Device1; the demo device (only the manufacturer bootstrap certificate)
obtains `on`, `de` and `sy` certificates, logs in as DemoSensor, registers the system on Device1 and a service, is
orchestrated to DataManager and stores a measurement; a second run re-issues every credential and off-boards the device
(7 actions, none failed); the identity is refused afterwards and the bootstrap certificate is refused until re-approval.

## Documents

All six documents compile (2026-10-14: SysD, SD, onboardingManagement SD, SysDD and IDD rewritten for the procedure) with the official Arrowhead class 1.2.1 and local LaTeX tools. PDF pages were rendered and visually inspected. The source class/logo are unchanged from the official local roadmap template. The module's `documentation/v5.2.2/build-docs.sh` reproduces the PDFs.

## Deployment acceptance still required

- Configure actual local-cloud CA and server credentials, bootstrap identities, trust stores, database account and registry address.
- Verify startup registration/revocation and discovery against the target v5.2.2 ServiceRegistry and its actual policies.
- Verify MySQL log persistence and operator log/config access with deployment certificates.
- Complete and test the separate device/system certificate handoff. This module issues onboarding certificates only; those are intentionally insufficient for ordinary v5 registry APIs.
- Apply cloud-specific revocation, rate limits and bootstrap credential rotation. The local PKIX CA-chain check does not retrieve CRLs/OCSP responses.

No production cloud was contacted, deployed to, or modified. No original v4 source, existing DataManager/EventHandler implementation or unrelated repository files were changed.
