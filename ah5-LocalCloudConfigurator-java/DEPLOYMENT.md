# Deployment — Java-Spring LocalCloudConfigurator

LocalCloudConfigurator is an Arrowhead Java-Spring support system (Blacklist layout, vendored common library):

| Part | Where | Port | Security |
|---|---|---|---|
| `localCloudConfiguration`, generalManagement, sysMonitor | container `localcloudconfigurator` | 8492 | mutual TLS (`client-auth=need`), outsourced authentication, CERT_AUTH, ConsumerAuthorization, secure profile |
| Private database `ah_localcloudconfigurator` | container `localcloudconfigurator-db` (MySQL 8.4.11) | none published | TLS (`verify-full`), DML-only account (`least-privilege.sh`), internal network |
| Host worker | per-user launchd service on the host | 127.0.0.1:8493 | mutual TLS, loopback only, does not register |

The ports moved from 8097/8098 (inside the GateTunnel control pool) to the operator-tools range 8492/8493.

## Build

```sh
make build   # docker run maven:3.9-eclipse-temurin-21 mvn -B clean verify, with ~/.m2 mounted
make image   # system image (UID 10001) and database image
```

The packaged `application.properties` is a development sample and is refused in the default secure profile. `config/application.properties` is the deployment configuration (copied to `/app/config`).

## Standalone stack of this folder

Copy `.env.example` to `.env` and provide under `./security/` (or the paths named in `.env`):

- `LocalCloudConfigurator.p12` (system certificate `LocalCloudConfigurator.<cloud>.<organization>.arrowhead.eu`) and `truststore.p12`, `store-password`;
- `identity-password` (the Authentication identity of LocalCloudConfigurator);
- `db-password`, `db-root-password`, `db-ca.crt`, `db.crt`, `db.key`;
- `crl/ca.crl` (the cloud CRL).

`docker compose up -d --build` starts the database and the system with a read-only root filesystem, no capabilities and `no-new-privileges`. Port 8492 is published on the host's loopback interface by default (`HOST_BIND_ADDRESS`).

Register the identity `LocalCloudConfigurator` in Authentication. Two ConsumerAuthorization grants are needed, and `scripts/deploy.sh` applies both through the cloud's policy scripts (`make up` does too):

- **LocalCloudManager** gets the ten operations of `localCloudConfiguration` (provider LocalCloudConfigurator): `configure-localcloudconfigurator-policy.sh`. LocalCloudManager reaches the service through orchestration with its own certificate and identity token. Without the grant, the LC view reports "Orchestration found no authorized provider of localCloudConfiguration".
- **LocalCloudConfigurator** gets ConsumerAuthorization's `authorization` `verify` (it is listed in `VERIFIERS` of `configure-orchestration-dependency-policies.sh`). It checks every call with it and fails closed without it.

`LCC_API_WHITELIST` can name further systems that skip the grant (empty by default).

## Host worker

The worker is the same JAR started as `worker` (a non-web Spring application). It needs a system certificate **LocalCloudConfiguratorWorker** of the cloud CA (PKCS12), the cloud truststore and the store password:

```
~/.local/share/JavaLocalCloudConfigurator/certs/LocalCloudConfiguratorWorker.p12
~/.local/share/JavaLocalCloudConfigurator/certs/truststore.p12
~/.local/share/JavaLocalCloudConfigurator/secrets/store-password
```

`python3 scripts/install-direct.py --cloud-dir <cloud> --compose-file <cloud>/compose.yaml [--crl-file <cloud CRL>]` (or `make worker-start`) copies an immutable JAR/catalog release, writes the store passwords into a 0600 config tree (`worker-secrets/`, never on the command line) and installs `~/Library/LaunchAgents/eu.arrowhead.java-localcloud-direct.plist` (RunAtLoad, KeepAlive). It refuses to reinstall while a deployment of the worker is still running. The worker accepts only clients presenting a valid **LocalCloudConfigurator** certificate of the cloud (checked against the CRL when given); the API accepts only a worker presenting **LocalCloudConfiguratorWorker**. There is no shared token any more.

The worker keeps its file state under `~/.local/share/JavaLocalCloudConfigurator/direct-state/`: per-step results in `workflows/`, written after every step; after a restart, interrupted deployments are marked failed and are never replayed. `active-deployment.json` and `deployments/` are its own records.

## Into a cloud

`make deploy` (`scripts/deploy.sh`) builds and tests, puts both services of `compose.yaml` into the cloud's Compose file (`scripts/configure-direct-cloud.py --env-file <cloud env>`, 0600 backup), installs the host worker and recreates only the configurator and its database. It changes a deployed cloud: run it only when an operator decides so. It was not run as part of the Spring conversion.

Interruption is not transactional rollback: completed actions remain applied. Inspect the real state and review a new plan. Legacy ColonyOS files and commands (`keygen`, `register`, `executor`, `lcc.backend=colony`) remain available for explicit historical compatibility.
