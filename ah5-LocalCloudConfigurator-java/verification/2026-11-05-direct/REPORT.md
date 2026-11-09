# Direct Java LocalCloudConfigurator default — 5 November 2026

Corrected the earlier deployment: the November 5 image had still used ColonyOS. The primary source at `Eclipse-Arrowhead-java/ah5-LocalCloudConfigurator-java/` now implements direct local execution and makes it the default.

## Persistent startup

- The standard Java cloud `OLD/Arrowhead/Arrowhead-test-cloud-java/compose.yaml` and this chat's `redeploy-work/java-compose.json` both select `eclipse-arrowhead/localcloudconfigurator-java:direct`, explicit `-backend direct`, and `restart: unless-stopped`.
- The configurator receives no ColonyOS key, variables or network attachment. It uses a dedicated worker network and the existing isolated manager connection.
- A macOS login service, `eu.arrowhead.java-localcloud-direct`, starts the worker automatically at login, with KeepAlive enabled for crash recovery. Docker Desktop must also be running; the worker can start before Docker and serve real status once Docker is available.
- The worker listens only on host loopback port 8098. The container API remains on loopback port 8097. The editor is **http://testcloud.localhost:8490/ → LC / Local cloud configuration** with the existing login.
- Source `make deploy`, `make image` and executor start/restart targets now use direct mode. The former standalone ColonyOS executor was stopped. Shared ColonyOS servers/other colonies were not altered.

## Safeguards and persistence

The worker independently recomputes the reviewed plan against current cloud state, then uses the existing catalog-checked Runner. Operator credentials, stale-plan rejection, protected component/ownership checks, command argument lists and output redaction remain in force. The local process has only the host operator's existing deployment authority.

Workflow results are saved atomically. A file lock rejects duplicate workers. On recovery, unfinished workflows become failed and are not replayed. Completed actions are not rolled back automatically; inspect real state and review a fresh plan. Reinstallation refuses to interrupt an active deployment. Historical ColonyOS records remain retained and legacy active markers are archived on migration.

Saved cloud designs remain in the existing Docker volume. Worker releases are immutable JAR/catalog copies under the existing private credential directory, and persistent workflow state is separate from source/build output. Resolved startup files and backups are mode 0600. No credentials appear in this report.

## Verification

- **25 source tests passed** (including five new direct-mode tests).
- **17 live checks passed**: direct backend identity, authenticated status/catalog/design listing, missing-token rejection, verification-only plan review, stale-plan rejection, successful local workflow, absence of ColonyOS container dependencies, and persistent startup settings.
- **7 restart checks passed**: worker restart, standard cloud container restart, post-restart runtime status, saved design persistence, workflow result persistence, temporary design cleanup, and automatic worker recovery following process termination without manual start or ColonyOS.
- Manager UI serves the editor and its backend reaches the configurator's catalog and status (HTTP 200).
- All **40 other Java containers retained their IDs**. Only LocalCloudConfigurator was replaced. Its final container is healthy.
- No provision/start/policy/stop plan steps were submitted; the executed plan was verification-only.
- A full machine reboot was not performed. Login configuration, worker/container restarts and automatic worker recovery were tested.

See `build.log`, `live-checks.json`, `restart-checks.json` and `deployment-result.json` for evidence. Primary current operational instructions are `documentation/DIRECT-EXECUTION.md` and `DEPLOYMENT.md`.
