# CertificateAuthority - independent deployment

This folder owns the complete source, build, JAR, image, configuration and Compose deployment of CertificateAuthority 5.2.2. No sibling folder, shared runtime or shared database is used.

## Build

Run `mvn -B clean verify` here with Java 21 and Maven 3.9. The output is `target/arrowhead-certificate-authority-5.3.0.jar`. `docker compose build` builds the application image `arrowhead-certificate-authority-standalone:5.3.0` and the database image `arrowhead-certificate-authority-standalone-db:5.3.0` from this folder.

## Provision and start

1. Copy `.env.example` to `.env`. Select an existing cloud network and ServiceRegistry address. Use a unique Compose project name, host port and database subnet (`DB_SUBNET`) per deployment on one Docker host.
2. Provision the files listed in [security/README.md](security/README.md):
   - the system keystore (`CertificateAuthority.p12`, CN `CertificateAuthority.<LOCAL_CLOUD>.<ORGANIZATION>.arrowhead.eu`, DN qualifier `sy`), the truststore and their password;
   - the identity password, registered in Authentication;
   - the PEM issuer certificate and signing key of the cloud (`ca.crt`, `ca.key`). Only this system mounts the key;
   - the CRL directory with a valid initial `ca.crl`. The CA replaces it atomically after every revocation and every `CRL_REFRESH_SECONDS`;
   - database passwords and TLS material.

   Files must be readable by UID 10001; the CRL directory must be writable by it.
3. Run `docker compose config --quiet`, then `docker compose up -d --build`.

The database container initialises only its own schema on an empty volume. Its init script then restricts the application account to `SELECT, INSERT, UPDATE, DELETE` (`deployment/database/least-privilege.sh`); schema changes are made as root. The database has no host port and is reachable only on the project's internal network, with TLS required.

The application starts only with the secure profile: TLS with client certificates, outsourced authentication and a CRL. See `SECURITY-PROFILE.md` in the distribution root.

## Moving from the native implementation

The native 5.2.2 CertificateAuthority kept its state in a SQLite file on the `state` volume. To keep issued certificates, revocations and trusted keys:

1. Stop the native CA and copy `/data/certificate-authority.db` out of its volume.
2. Start only the new database: `docker compose up -d certificate-authority-db`.
3. Convert and import as root, as described in `scripts/migrate-sqlite-to-mysql.py`.
4. Start the CA: `docker compose up -d --build`.

The native health port (8483) and PEM system identity are no longer used; provision the PKCS#12 keystore instead.

## Operational dependencies

The HTTPS API uses port 8448. ServiceRegistry and Authentication are required for registration and identity; DynamicServiceOrchestration and ConsumerAuthorization for consumed services. The sysMonitor observations need the AMQP broker (`AMQP_HOST`, `AMQP_PORT`). The CA does not use Blacklist, so it keeps working while Blacklist is unavailable.

`docker compose down` stops only this project and keeps its database volume. Back up the database before upgrades. `down -v` erases this project's state and must not be used for ordinary stops.

The SysDD is in `documentation/v5.2.2`. The [distribution deployment guide](../DEPLOYMENT-GUIDE.md) covers selecting several systems.
