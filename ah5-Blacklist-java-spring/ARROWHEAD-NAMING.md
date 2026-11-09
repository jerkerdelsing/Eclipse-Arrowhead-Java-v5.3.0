# Arrowhead 5.2.2 naming convention

The `Blacklist` microsystem uses PascalCase for its system name, camelCase for service definitions, lowercase kebab-case for operations, and lowercase snake_case for interface templates.

Its liveness interface is the `echo.echo` service. The standard operational telemetry contract is `sysMonitor.observe` over AMQP 0-9-1 and is supplied by the local cloud's SysMonitorPublisher. The legacy `monitor` service-definition name is not used.
