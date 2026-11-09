# Integrated microsystem database assets

2026-10-30 — deployed in the Java cloud from primary Eclipse-Arrowhead-java sources.

All 16 defined databases are private integrated assets of their owning microsystems. The catalog now has 27 selectable components instead of 43; databases have no independent graph node, palette entry or stop operation. The host box identifies its internal database, and the host inspector lists the exact assets and access boundary. The default cloud has four core microsystems (each including its database) and one AMQP infrastructure component.

The lifecycle planner starts database services before host services, stops them after the host, and retains stored volumes. The executor validates ownership and refuses host startup before its database or database shutdown while the host is running. Legacy design selections map databases to hosts; the editor removes obsolete independent database positions and stop flags. Database access is exclusively through host internal actions or the host’s produced/consumed services.

## Verified

- 20 Java tests passed, including all-database ownership, start/stop ordering and rejection of independent database removal.
- 10 focused manager/API/editor tests passed, including legacy design migration.
- Ten live browser check groups passed: default five-node graph, hidden standalone database entries, integrated asset inspector, selections, drag/save/reopen, stale edits, reviewed signed ColonyOS execution, desktop/mobile layout and cleanup.
- Compose configuration and Docker runtime were checked for every database: no published database port, one internal network, exactly database and host as members. Evidence: lcc-assets-isolation.json.
- Configurator and manager containers were rebuilt/deployed and healthy. The existing private database networking already met the requested boundary, so no database or microsystem container was restarted. Live workflow testing was verification-only; lifecycle mutations were unit-tested.

## Ownership

| Microsystem | Integrated database asset |
| --- | --- |
| ServiceRegistry | serviceregistry-db |
| DynamicServiceOrchestration | serviceorchestration-dynamic-db |
| SimpleOrchestration | serviceorchestration-simple-db |
| ConsumerAuthorization | consumerauthorization-db |
| Authentication | authentication-db |
| Blacklist | blacklist-db |
| TranslationManager | translation-manager-db |
| DeviceQoSEvaluator | device-qos-evaluator-db |
| Onboarding | onboarding-db |
| DataManager | datamanager-db |
| TimeManager | timemanager-db |
| EventHandler | eventhandler-db |
| PlantDescriptionEngine | plantdescriptionengine-db |
| Configuration | configuration-db |
| Choreographer | choreographer-db |
| GraphDBManager | graphdbmanager-graphdb |

Implementation: catalogs/javacloud.json, Catalog.java, Planner.java, Runner.java, ApiServer.java; primary Java LocalCloudManager cloud-config model/editor and documentation. Existing database container names and volumes are preserved.
