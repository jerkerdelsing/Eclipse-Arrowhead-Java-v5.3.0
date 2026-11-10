# Blacklist Support System (ah5-blacklist-java-spring)

> Current standalone packaging (25 October 2026): build and deploy from this folder. See [DEPLOYMENT.md](DEPLOYMENT.md). Any combined-tree build or live-cloud examples below are historical integration context.

Visit the official documentation site:

https://aitia-iiot.github.io/ah5-docs-java-spring/home/welcome/

[SysD, SysDD and an SD and IDD for each produced service](documentation/v5.2.2/README.md) (`generalManagement`, `blacklistManagement`, `blacklistDiscovery`, `sysMonitor`), generated from the implementation with the official Arrowhead LaTeX templates by [`documentation-tools`](../documentation-tools/README.md); the PDFs are beside the sources. The earlier `documentation/v5.2` set is in `../../_superseded/2026-10-22/`.

## Listening port

Default **8464** (`server.port` in the packaged `application.properties`). Override with `server.port` in `config/application.properties`, `SERVER_PORT=8464` or `--server.port=8464`.

See [LISTENING-PORTS.md](../LISTENING-PORTS.md) for precedence and all systems.
