Provision this system's own credentials here before deployment. Never commit private keys or passwords. This directory is excluded from the Docker build context.

| File (default) | Content |
|---|---|
| `CertificateAuthority.p12` | PKCS#12 keystore with the system certificate (CN `CertificateAuthority.<LOCAL_CLOUD>.<ORGANIZATION>.arrowhead.eu`, DN qualifier `sy`) and its key, alias = the CN |
| `truststore.p12` | PKCS#12 truststore with the cloud issuer certificate |
| `store-password` | password of both stores |
| `identity-password` | the system's password in Authentication |
| `ca.crt`, `ca.key` | PEM issuer certificate and signing key of the local cloud. Only this system mounts the key |
| `crl/` | directory the CA publishes `ca.crl` into (writable by UID 10001); it must hold a valid CRL before the first start |
| `db-password`, `db-root-password` | passwords of the private database |
| `db-ca.crt`, `db.crt`, `db.key` | TLS material of the private database |
