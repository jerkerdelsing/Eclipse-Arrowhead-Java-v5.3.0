# AIGym - independent deployment

This folder owns the complete source, build, executable JAR, application image, configuration and Compose deployment for AIGym 5.2.2. No JavaSupportRuntime, sibling source folder or shared support-system data is used.

## Build

With Java 21 and Maven 3.9, run `mvn -B clean verify` here. `scripts/verify.sh` also checks artifact contents. Maven Central dependencies require network access on the first build; `-o` works with a populated dependency cache. The output is `target/arrowhead-ai-gym-5.2.2.jar`. Alternatively `docker compose build` builds from this folder, runs the tests and creates `arrowhead-aigym-standalone:5.2.2`.

## Provision and select this system

1. Copy `.env.example` to `.env`. Select an existing cloud network and registry endpoint. Use a unique Compose project name, host port and private subnet for each deployment on a Docker host. The project does not create or start core systems or other support systems.
2. Provision this system's own identity and password in Authentication, its certificate and required authorization/orchestration rules. The certificate must identify `AIGym.<LOCAL_CLOUD>.<ORGANIZATION>.arrowhead.eu` with the system profile `sy`. Keep credential files outside version control and ensure they are readable only by their intended container user.
3. Provision `security/AIGym.p12` and `security/truststore.p12`. The system key alias is `AIGym.<LOCAL_CLOUD>.<ORGANIZATION>.arrowhead.eu`. Put the store password in `security/store-password`, identity password in `security/identity-password`, and a valid cloud-issuer CRL in `security/crl/ca.crl`. Paths can be changed through `.env`. The application runs as UID 10001. The secure deployment file is `config/application.properties`; it overrides the inherited development sample. No bundled development keystore is used.

Run from this folder:

```sh
docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose logs
```

For direct JAR execution, point Spring at the local secure configuration and supply the corresponding secret files, certificate paths and network/database settings. The image uses `--spring.config.additional-location=file:/app/config/application.properties`; host paths must be adapted for a direct host launch.

## Operational dependencies and verification

The HTTPS API uses port 8478 by default. Packaging and deployment are independent; normal Arrowhead protocol dependencies remain. ServiceRegistry and Authentication are required for registration and identity. DSO and ConsumerAuthorization are required for consumed/authorized services; AMQP/MQTT, federation peers, NTP and other providers are needed according to this system's functions. Missing providers may cause startup failure or degraded features. Independence does not mean replacing core services with local bypasses.

The image health check tests the local listener only. Confirm application-ready and successful registration in the logs, then make an authorized HTTPS request and check required provider/broker status. A healthy listener does not certify monitoring, orchestration or full application readiness.

The distribution verification deploys one support system at a time with its own data and generated test credentials. Spring startup and authenticated HTTPS access are checked against isolated mTLS core-service protocol fixtures, not production authorities. Broker/NTP/federation functions require separate operational acceptance checks. See [the distribution verification report](../ALL-JAVA-SUPPORT-VERIFICATION-2026-10-25.md).

`docker compose down` stops only this project and preserves its state. Back up its database before upgrades. `down -v` deliberately erases this project's private state and must not be used for ordinary stops. No live cloud was changed by the packaging work.

The SysDD is in `documentation/v5.2.2`; the support GSoSDD is in the enclosing distribution's `roadmap/5.2/Arrowhead SoS architecture/WhiteBox_5.2.2`. The [distribution deployment guide](../DEPLOYMENT-GUIDE.md) covers selecting multiple systems.
