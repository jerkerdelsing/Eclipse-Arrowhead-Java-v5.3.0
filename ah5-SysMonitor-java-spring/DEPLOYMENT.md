# SysMonitor - independent deployment

This folder owns the complete source, build, JAR, image, configuration and Compose deployment of SysMonitor 5.2.2. No sibling folder, shared runtime or shared database is used.

## Build

Run `mvn -B clean verify` here with Java 21 and Maven 3.9. The output is `target/arrowhead-sysmonitor-5.3.0.jar`. `docker compose build` builds the application image `arrowhead-sysmonitor-standalone:5.3.0` and the database image `arrowhead-sysmonitor-standalone-db:5.3.0` from this folder.

## Provision and start

1. Copy `.env.example` to `.env`. Select an existing cloud network, the ServiceRegistry address and the AMQP and MQTT brokers. Use a unique Compose project name, host port and database subnet (`DB_SUBNET`) per deployment on one Docker host.
2. Provision the files listed in [security/README.md](security/README.md):
   - the system keystore (`SysMonitor.p12`, CN `SysMonitor.<LOCAL_CLOUD>.<ORGANIZATION>.arrowhead.eu`, DN qualifier `sy`), the truststore and their password. The same identity authenticates to the AMQP broker (SASL EXTERNAL) and the MQTT broker;
   - the identity password, registered in Authentication;
   - the CRL directory with the cloud's valid `ca.crl`;
   - database passwords and TLS material.

   Files must be readable by UID 10001.
3. Provision the cloud's rules: ConsumerAuthorization grants for `sysMonitorHistory` consumers and for SysMonitor to consume the providers' `sysMonitor` services, broker permissions for the intake queue and the output topics.
4. Run `docker compose config --quiet`, then `docker compose up -d --build`.

The database container initialises only its own schema on an empty volume. Its init script then restricts the application account to `SELECT, INSERT, UPDATE, DELETE` (`deployment/database/least-privilege.sh`); schema changes are made as root. The database has no host port and is reachable only on the project's internal network, with TLS required.

The application starts only with the secure profile: TLS with client certificates, outsourced authentication, service authorization, TLS to the brokers and a CRL. See `SECURITY-PROFILE.md` in the distribution root.

## Moving from the native implementation

The native SysMonitor kept subscriptions and history in a SQLite file on its `state` volume. The history is retention-bounded and is not migrated. Re-create subscriptions that differ from the default with `PUT /sysmonitor/management/subscriptions/{providerSystem}`. The native health port (8482) and PEM identity files are no longer used; provision the PKCS#12 keystore instead.

## Operational dependencies

The HTTPS API uses port 8475. ServiceRegistry and Authentication are required for registration and identity; DynamicServiceOrchestration and ConsumerAuthorization for orchestration and history authorization. The AMQP broker (`AMQP_HOST`, `AMQP_PORT`) and the MQTT v5 broker (`MQTT_HOST`, `MQTT_PORT`) are needed for operational readiness: without them `/health` and `generalManagement` status report degraded (HTTP 503). `sysMonitorManagement` status shows the broker, binding, storage, registration and orchestration details.

`docker compose down` stops only this project and keeps its database volume. `down -v` erases this project's state and must not be used for ordinary stops.

The SysDD is in `documentation/v5.2.2`. The [distribution deployment guide](../DEPLOYMENT-GUIDE.md) covers selecting several systems.
