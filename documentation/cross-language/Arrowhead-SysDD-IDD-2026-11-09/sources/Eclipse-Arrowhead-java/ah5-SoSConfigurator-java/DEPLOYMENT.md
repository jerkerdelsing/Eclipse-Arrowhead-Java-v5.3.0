# Deploying SoSConfigurator with ColonyOS

## Prerequisites

You need the following:

- Docker Desktop with Compose v2.
- Java 21 or newer on the host, for the executors and the CLI.
- `curl`, `jq` and `openssl` on the host. The step scripts and the clouds' `lib-policies.sh` use them.
- The local clouds and the relay running:
  - `../../Arrowhead/Arrowhead-test-cloud-java` (TestCloud);
  - `../../Arrowhead/Arrowhead-test-cloud-go` (GoCloud);
  - `../../Arrowhead/Arrowhead-relay`.

  Start order: the clouds first, then `make -C Arrowhead-relay up`.
- Maven is not needed on the host. `make build` runs it in `maven:3.9-eclipse-temurin-21`.
- The configurator's Arrowhead identity in the cloud it joins (default TestCloud on the network `arrowhead-local-cloud`), in `deploy/colonyos/secrets/` or at the paths set in the environment:
  - `SoSConfigurator.p12` (CN `SoSConfigurator.<cloud>.<organization>.arrowhead.eu`, DN qualifier `sy`), `truststore.p12`, `store-password`;
  - `identity-password`, registered for `SoSConfigurator` in that cloud's Authentication;
  - `crl/ca.crl` of that cloud;
  - TLS material of its database: `db-ca.crt`, `db.crt`, `db.key`.

  Give the systems that should use `sosConfiguration` (besides Sysop) a ConsumerAuthorization grant, or list them in `SOS_API_WHITELIST`.

## 1. ColonyOS stack and API

```sh
make build up
```

`make up` does the following:

1. `make keygen` writes fresh secrets to `deploy/colonyos/secrets/`. The directory has mode 0700 and the files mode 0600; both are git-ignored. The secrets are:
   - the Colonies server key and id;
   - the colony key;
   - the API key;
   - one executor key per role (`relay`, `testcloud`, `gocloud`, `rustdev`);
   - the Colonies database password and the configurator's database passwords;
   - the console password (`sos_console_password`) and its scrypt hash (`sos_console_password_hash`). Only the hash reaches the container.

   Existing secrets are kept unless you pass `-force`.
2. It builds the image `eclipse-arrowhead/sosconfigurator-java:5.2.2` (JRE, non-root, read-only root filesystem).
3. It starts TimescaleDB and the Colonies server. The server is published on `127.0.0.1:50081` only. The LocalCloudConfigurator's own stack keeps `50080`.
4. `make register` creates the colony `arrowhead-sos`. It registers and approves `sos-api` and the executors `sos-relay-executor`, `sos-testcloud-executor`, `sos-gocloud-executor` and `sos-rustdev-executor`. Each executor has its own executor type, so a step can only reach the executor of its cloud.
5. It starts the configurator and its database. `sosConfiguration` is published on `127.0.0.1:8494` and inside the cloud network; the console on `127.0.0.1:8495` only. The configurator refuses to start without TLS, outsourced authentication and service authorization (secure profile).

The stack network uses the explicit subnet `10.240.91.0/24`. Override it with `SOS_SUBNET`.

## 2. Executors

```sh
make executors-start      # all roles on this host; logs in state/<role>.log
ROLE=cloud:GoCloud make executor     # one role in the foreground (java -jar ... executor --sos.role=cloud:GoCloud)
make executors-stop
```

An executor drives its host's `docker compose` and the cloud's management APIs, so it runs as a host process. In a distributed SoS, run each cloud's executor on that cloud's host with only its own key file.

## 3. Use

- **UI.** Open http://127.0.0.1:8495 and log in with the password from `deploy/colonyos/secrets/sos_console_password`.
  1. Tick the member clouds.
  2. Add the exchanges.
  3. Choose *Check policies and plan*. Refusals show their rule ids.
  4. Apply. The workflow's steps update live.
- **CLI.**

```sh
java -jar target/sosconfigurator.jar apply -request requests/testcloud-gocloud.json   # logs in to the console with the password file
make plan REQUEST=requests/refused-rustdev.json
```

## 4. Smoke test

```sh
make smoke
```

The smoke test checks five things:

1. The console is healthy and requires a login.
2. Status makes the round trip through every executor. The relay, TestCloud and GoCloud are `ready`; RustDev reports no inter-cloud support.
3. A request that violates the policies gets HTTP 422 and nothing is submitted.
4. A one-way SoS (GoCloud → TestCloud) is applied. GoCloud's grant to TestCloud is revoked and verified as denied.
5. The bidirectional SoS is applied again and verified, with discovery over the relay in both directions.

The test ends in the clouds' normal inter-cloud setup, the same as `make intercloud` in both clouds. It ends with `SMOKE PASSED`.

## Operations

- `make status` shows the stack containers.
- `make down` stops the executors and the stack. The volumes are kept.
- A failed step is a failed ColonyOS process. The UI and the CLI show its masked error, and the rest of the workflow does not run. A failed check stops the workflow before anything is changed.
- After a cloud swaps its native core (see the GoCloud swap targets), run the cloud's `configure-*-policy.sh` scripts first. Otherwise `cloud-check` reports the missing Gatekeeper or GateTunnel grants.
- Gatekeeper neighbours of another organization are never touched, for example `RustDev|Lab` from the Rust candidate's federation trial.
