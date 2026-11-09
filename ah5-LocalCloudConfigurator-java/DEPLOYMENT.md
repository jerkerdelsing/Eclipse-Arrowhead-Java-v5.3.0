# Deployment in the Java cloud

Primary implementation: `Eclipse-Arrowhead-java/ah5-LocalCloudConfigurator-java`. Primary manager: `Eclipse-Arrowhead-java/ah5-LocalCloudManager-java-spring`. The existing `Arrowhead/Arrowhead-test-cloud-java/` remains the deployment consumer. Its manager and configurator build contexts now point to the primary Java trees.

## Prerequisites

Java 21 or newer on the cloud host; Docker/Compose; Python 3 plus PyYAML for the deployment-file installer. The supplied deployment reuses the already-running SoS ColonyOS server on loopback port 50081 and external Docker network `sos-colonyos_default`. It creates a separate colony, not a new TimescaleDB/server pair. It does not alter existing server settings, credentials, colonies or Arrowhead authorization policies.

Default private credential directory: `~/.local/share/JavaLocalCloudConfigurator`, mode 0700. `keygen` creates missing API/executor/colony keys and an operator token with mode 0600, without printing them. Registration uses the existing server-owner key from the primary Java SoSConfigurator deployment; this bootstrap key is never mounted in the new API or passed in argv as a value. Registration verifies colony ownership and existing executor identities, and uses the owner's official approval operation for matching retired identities. Mismatches fail closed. Server re-registration and stale-member policies are not relaxed.

## Deploy

```sh
make build
make deploy
```

`deploy.sh` creates missing private credentials, installs the narrowly scoped Java cloud Compose additions, registers the separate colony, builds the configurator and manager from primary sources, recreates only those three services and starts the host executor. It preserves a one-time pre-change Compose backup. The cloud installer is idempotent. The Java cloud's existing core/support/application services are not rebuilt or recreated by these commands.

The API runs non-root with a read-only root filesystem, dropped capabilities, no privilege gain and bounded memory/processes. Its only writable persistent area is the designs/workflow-marker volume; `/tmp` is ephemeral. Its operator port is loopback-only, 8097. The manager-to-configurator network is internal (`10.240.92.0/28`), with only those two services attached. Existing authenticated Arrowhead calls retain their original TLS and authorization boundary.

The executor is a host process, as in the Go reference: it needs access to the existing cloud's Compose files and provisioning/policy scripts. `scripts/executor.py start|stop|restart|status` manages only its own PID after checking the command. It copies the built JAR to a private, content-addressed runtime path; future Maven builds cannot invalidate the running JVM's class loader. Use `make executor-restart` after a code/catalog change. Restart checks the active workflow marker and refuses to replace an executor while a deployment is still active. The helper uses Java 21+ and the user's Docker CLI. It does not install a boot-time service; restart it after a host reboot.

API keys use a 60-second authenticated assignment heartbeat. The host executor's normal assignment loop provides its heartbeat. An API identity whose membership was retired during a long outage must be restored through the authorized registration command before restarting, rather than bypassing server checks.

## Status and testing

```sh
make status
make test
```

Use the manager **LC** view to save/reopen a design and review/apply a plan. Verification-only plans use `smoke: true` while all selected components are already running. Their real signed ColonyOS workflow checks baseline runtime state without restarting/stopping services. This is the live deployment test used here; mutation paths are unit-tested with a controlled command executor. Retained workflow records are operational audit data; temporary saved test designs are deleted.

Stopping/reapplying components is operationally significant: inspect the plan and dependents first. Applying is not an atomic transaction and does not automatically restore earlier steps on failure. Saved design revisions and plan hashes prevent common stale/concurrent edits; they do not lock independent external Docker operators.
