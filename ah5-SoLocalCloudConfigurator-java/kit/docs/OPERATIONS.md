# Operating a System of LocalClouds

## Revocation lists (fail closed)

Every CRL has a nextUpdate; an expired CRL makes its checkers refuse **all** certificates of that CA.

| CRL | Issued by | Validity | Consumers | Renew |
|---|---|---|---|---|
| cloud CA (`work/security/crl/ca.crl`) | the cloud's CertificateAuthority | renewed automatically while it runs | the cloud's systems; the relay; peer GateTunnels | member: `clouds/<Cloud>/export-crl.sh`, then on the organisation host `org/relay.sh crl <Cloud> <file>` and `org/sign-cloud.sh --bundles`; peers import their new bundle |
| organisation CA (`org/state/public/org-ca.crl`) | `org/crl.sh` | 180 days (`ORG_CRL_DAYS`) | the relay; every Gatekeeper and GateTunnel | `org/crl.sh`, `org/sign-cloud.sh --bundles`, members import the bundle |

Schedule the cross-host steps well before nextUpdate, e.g. weekly with cron on each host, and monitor them:
`openssl crl -in <file> -noout -nextupdate`.

## Backups

Per member host, back up `clouds/<Cloud>/work/` (keys, passwords, `.env`, generated configuration; it is the cloud's
identity) and the Docker volumes of the project (`docker volume ls --filter label=com.docker.compose.project=<project>`),
for example with `docker run --rm -v <volume>:/v -v "$PWD":/b alpine tar -czf /b/<volume>.tgz -C /v .` while the cloud is
stopped. On the organisation host, back up `org/state/`; the organisation CA key in `org/state/ca/` is its most
sensitive file (an offline copy, encrypted).

Backups contain private keys and passwords: store them encrypted, never in the package folder you hand out.

## Secrets

- Rotate a cloud's passwords: `cd clouds/<Cloud>/work && ./scripts/rotate-secrets.sh rotate && ./scripts/rotate-secrets.sh apply && ./scripts/rotate-secrets.sh restart`.
- Console password: macOS keychain (service `arrowhead-console-<project>`) or `work/operator/console-password` (Linux).
- Revoke a system certificate of a cloud: `cd clouds/<Cloud>/work && ./scripts/revoke-certificate.sh <file>`; then export
  the CRL as above.

## Upgrades

A new package revision of the same SoLC: unpack it **next to** the old one, copy each member's `clouds/<Cloud>/work/` from
the old package folder into the new one (same host), then `clouds/<Cloud>/install.sh`. Keys, passwords and data are kept;
images and configuration are replaced. On the organisation host copy `org/state/` likewise.

## Removal

`clouds/<Cloud>/uninstall.sh` stops the cloud and keeps its data; `--purge` deletes containers, volumes and the work folder
(irreversible). On the organisation host: `org/relay.sh down`.
