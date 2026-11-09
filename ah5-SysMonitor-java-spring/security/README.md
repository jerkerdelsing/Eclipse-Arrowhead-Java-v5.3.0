Provision this system's own credentials here before deployment. Never commit private keys or passwords. This directory is excluded from the Docker build context.

| File (default) | Content |
|---|---|
| `SysMonitor.p12` | PKCS#12 keystore with the system certificate (CN `SysMonitor.<LOCAL_CLOUD>.<ORGANIZATION>.arrowhead.eu`, DN qualifier `sy`) and its key, alias = the CN. Also the client identity at the AMQP and MQTT brokers |
| `truststore.p12` | PKCS#12 truststore with the cloud issuer certificate |
| `store-password` | password of both stores |
| `identity-password` | the system's password in Authentication |
| `crl/ca.crl` | the cloud CA's current CRL (client and broker certificates are checked against it) |
| `db-password`, `db-root-password` | passwords of the private database |
| `db-ca.crt`, `db.crt`, `db.key` | TLS material of the private database |
