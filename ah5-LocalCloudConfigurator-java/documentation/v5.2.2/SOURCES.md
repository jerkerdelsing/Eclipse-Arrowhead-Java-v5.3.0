# Sources

Structure, cover metadata, macros, `arrowhead.cls`, `IEEEtran.bst`, the logo and the templates are copied from the
LocalCloudManager documentation set (Eclipse Arrowhead LaTeX templates).

The content was written on 2026-11-09 from the Java-Spring implementation: service, operations and paths from
`LocalCloudConfiguratorSystemInfo` and `LocalCloudConfiguratorConstants`; JSON shapes, status codes and error texts from
`LocalCloudConfigurationAPI`, `DeploymentCoordinator`, `ConfigurationService`, `Catalog`, `Planner`, `DirectBackend` and the
DTO records; access rules from `ConfiguratorAccessFilter`; the host worker from `worker/`, `tls/MutualTls` and
`scripts/install-direct.py`; deployment facts from `config/application.properties` and `compose.yaml`; the sysMonitor
wire format from the vendored common `SysMonitorPublisher`. Catalog examples follow `catalogs/javacloud.json`.

The documents are marked RELEASE CANDIDATE until quality assurance.
