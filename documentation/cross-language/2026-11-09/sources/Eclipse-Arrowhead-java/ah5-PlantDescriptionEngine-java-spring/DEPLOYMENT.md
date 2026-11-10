# PlantDescriptionEngine - independent deployment

This folder owns the complete source, build, executable JAR, application image, configuration and Compose deployment for PlantDescriptionEngine 5.2.2. No JavaSupportRuntime, sibling source folder or shared support-system data is used.

## Build

With Java 21 and Maven 3.9, run `mvn -B clean verify` here. `scripts/verify.sh` also checks artifact contents. Maven Central dependencies require network access on the first build; `-o` works with a populated dependency cache. The output is `target/arrowhead-plantdescriptionengine-5.2.2.jar`. Alternatively `docker compose build` builds from this folder, runs the tests and creates `arrowhead-plantdescriptionengine-standalone:5.2.2` and its private database image.

The Arrowhead common utilities and DTOs are private source copies under `vendor/`, compiled into this application by the local POM. There is no Arrowhead common/core Maven parent or separately installed Arrowhead library JAR. Upstream POMs, licenses and source hashes are retained in `vendor/PROVENANCE.json`. Security fixes to private copies must be propagated to every affected system.

## Provision and select this system

1. Copy `.env.example` to `.env`. Select an existing cloud network and registry endpoint. Use a unique Compose project name, host port and private subnet for each deployment on a Docker host. The project does not create or start core systems or other support systems.
2. Provision this system's own identity and password in Authentication, its certificate and required authorization/orchestration rules. The certificate must identify `PlantDescriptionEngine.<LOCAL_CLOUD>.<ORGANIZATION>.arrowhead.eu` with the system profile `sy`. Keep credential files outside version control and ensure they are readable only by their intended container user.
3. Provision `security/PlantDescriptionEngine.p12` and `security/truststore.p12`. The system key alias is `PlantDescriptionEngine.<LOCAL_CLOUD>.<ORGANIZATION>.arrowhead.eu`. Put the store password in `security/store-password`, identity password in `security/identity-password`, and a valid cloud-issuer CRL in `security/crl/ca.crl`. Paths can be changed through `.env`. The application runs as UID 10001. The secure deployment file is `config/application.properties`; it overrides the inherited development sample. No bundled development keystore is used.
4. Provision independent random database user/root password files and database TLS material. The DB certificate must cover `plantdescriptionengine-db` as a DNS SAN. Set `DB_CA_FILE`, `DB_CERT_FILE`, `DB_KEY_FILE`, `DB_PASSWORD_FILE`, and `DB_ROOT_PASSWORD_FILE` in `.env`; the database key must be readable by the MySQL container. The app receives only the public DB CA. The DB has a private internal network, no published host port, and a project-owned volume. `DB_SUBNET` must not overlap another deployment. Its init SQL comes from this system's own schema and runs only on an empty volume; no sample password is used by the initializer. After the schema, `deployment/database/least-privilege.sh` narrows the application account created from `MYSQL_USER` to `SELECT, INSERT, UPDATE, DELETE` on its own schema (the MySQL image would otherwise grant it `ALL`); schema changes are made by the root account only. A volume initialised before this change keeps the wider grant until an operator runs the same `REVOKE`/`GRANT` as root (see `DEPLOYMENT-GUIDE.md`). After the schema, `deployment/database/least-privilege.sh` reduces the application account from the `ALL` grant the MySQL image gives `MYSQL_USER` to `SELECT, INSERT, UPDATE, DELETE` on its own schema; schema changes need the root account. A volume initialised before this change keeps the old grant until an operator applies the same `REVOKE`/`GRANT` as root.

Run from this folder:

```sh
docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose logs
```

For direct JAR execution, point Spring at the local secure configuration and supply the corresponding secret files, certificate paths and network/database settings. The image uses `--spring.config.additional-location=file:/app/config/application.properties`; host paths must be adapted for a direct host launch.

## Operational dependencies and verification

The HTTPS API uses port 8432 by default. Packaging and deployment are independent; normal Arrowhead protocol dependencies remain. ServiceRegistry and Authentication are required for registration and identity. DSO and ConsumerAuthorization are required for consumed/authorized services; AMQP/MQTT, federation peers, NTP and other providers are needed according to this system's functions. Missing providers may cause startup failure or degraded features. Independence does not mean replacing core services with local bypasses.

The image health check tests the local listener only. Confirm application-ready and successful registration in the logs, then make an authorized HTTPS request and check required provider/broker status. A healthy listener does not certify monitoring, orchestration or full application readiness.

The distribution verification deploys one support system at a time with its own data and generated test credentials. Spring startup and authenticated HTTPS access are checked against isolated mTLS core-service protocol fixtures, not production authorities. Broker/NTP/federation functions require separate operational acceptance checks. See [the distribution verification report](../ALL-JAVA-SUPPORT-VERIFICATION-2026-10-25.md).

`docker compose down` stops only this project and preserves its state. Back up its database before upgrades. `down -v` deliberately erases this project's private state and must not be used for ordinary stops. No live cloud was changed by the packaging work.

The SysDD is in `documentation/v5.2.2`; the support GSoSDD is in the enclosing distribution's `roadmap/5.2/Arrowhead SoS architecture/WhiteBox_5.2.2`. The [distribution deployment guide](../DEPLOYMENT-GUIDE.md) covers selecting multiple systems.
