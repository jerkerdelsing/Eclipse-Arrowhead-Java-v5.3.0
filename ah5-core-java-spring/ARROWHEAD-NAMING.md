# Arrowhead 5.2.2 naming convention

Public Arrowhead identifiers follow the validators in `ah5-common-java-spring`:

| Identifier | Form | Example |
|---|---|---|
| Microsystem (system name) | PascalCase ASCII letters and digits | `DynamicServiceOrchestration` |
| Microservice (service definition) | camelCase ASCII letters and digits | `serviceOrchestration` |
| Service operation | lowercase kebab-case | `modify-priorities` |
| Interface template | lowercase snake_case | `generic_mqtts` |

The standard operational telemetry service is `sysMonitor` with operation `observe` and the AMQP 0-9-1 `generic_amqps` IDD (SysMonitor-AMQP-IDD-5.2.2). HTTPS liveness endpoints use the service definition `echo`; they must not be registered as `monitor`, `monitoring`, or `sysMonitor`.

Java package names, Maven modules, URL paths, Docker Compose service keys, host names, and database identifiers are deployment identifiers. They keep their platform-specific lowercase or kebab-case forms and are not Arrowhead system or service names.
