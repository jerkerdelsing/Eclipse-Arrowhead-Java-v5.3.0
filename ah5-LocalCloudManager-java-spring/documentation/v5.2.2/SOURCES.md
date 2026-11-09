# Sources

Structure, cover metadata and macros follow the Eclipse Arrowhead LaTeX templates
(`roadmap/Documentation_templates/LaTeX/template_{sysd,sysdd,sd,idd}.tex`, copied to `templates/`); `arrowhead.cls`
and the logo are copied from there. The SD and IDD `\osubsection` macros take their input and output arguments
already formatted, so that primitive types can be named as well as structs.

The service, operation, path and parameter content was extracted from the implementation
(`ah5-LocalCloudManager-java-spring`): for the Spring systems from `*SystemInfo.java`, the `@RestController`
classes, the MQTT topic handlers and the Java records of the module and of `ah5-common-java-spring`; for AIGym from its ServiceRegistry registration code. Deployment facts
came from `Arrowhead-test-cloud-java/compose.yaml`.

Descriptions of fields and operations were written for these documents from the implementation; they do not copy the
upstream documentation site. The documents describe the implementation of this workspace, including its local
changes, and are marked RELEASE CANDIDATE until quality assurance.

On 2026-11-09 the documents were updated by hand for the Java-Spring implementation (`src/main/java`,
`docker-proxy/`, `config/application.properties`, `compose.yaml`): routes, status codes and JSON shapes from
`ConsoleApiController`, `ConsoleFilter` and `ConsoleSecurityConfig`; registration from `LocalCloudManagerSystemInfo`;
the sysMonitor wire format from the vendored common `SysMonitorPublisher`. The earlier generator no longer applies;
edit the `.tex` files.
