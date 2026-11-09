# Libraries for the Arrowhead Framework 5 (ah5-common-java-spring)

Visit the official documentation site:

https://aitia-iiot.github.io/ah5-docs-java-spring/home/welcome/

## Source of this library in the primary tree

This folder is the source of `arrowhead-common`, `arrowhead-common-utils` and `arrowhead-data-transfer-objects` for the Core systems. The support systems hold private copies under `vendor/`; `verification/tools/verify_reference_conformance.py` fails if a copy differs. Change a file here first, then copy it to each `vendor/` folder. The test PKI of the upstream repository (`certificates/`) is not included.

Build and install this library before building `ah5-core-java-spring`:

```sh
mvn -B clean install
```

## Secure profile check (local extension)

`eu.arrowhead.common.init.SecurityProfileGuard` stops a system at startup when its effective configuration falls below the default secure profile. See [SECURITY-PROFILE.md](../SECURITY-PROFILE.md).

## Native sysMonitor publisher (local extension)

Package `eu.arrowhead.common.sysmonitor` lets every system built on this library publish the
Arrowhead 5.2.2 `sysMonitor` service (contract SysMonitor-AMQP-IDD-5.2.2) itself:

- `SysMonitorPublisher` is created only when `sysmonitor.enabled=true`. Every
  `sysmonitor.publish.interval` seconds (default 30) it publishes `echo`, `power` and `status`
  observations to the AMQP 0-9-1 topic exchange `sysmonitor.exchange` (default `arrowhead`) with
  routing key `<sysmonitor.local.cloud>.sysmonitor.<system>.<sysmonitor.service.instance>.<type>`,
  using the RabbitMQ Java client over mutual TLS (SASL `EXTERNAL`: the broker user is the
  certificate CN). Messages are persistent and confirmed by the broker.
- `ApplicationInitListener` (and the ServiceRegistry init listener) registers the offer with
  interface `generic_amqps` (`generic_amqp` without SSL; model `AmqpInterfaceModel`), policy
  `CERT_AUTH` and metadata
  `publisherMode=native`, so no delegated publisher is needed.
- A system may contribute a `SysMonitorHealthIndicator` bean; when it reports `degraded`, `echo`
  becomes `degraded` with quality `uncertain`. Without one the system reports `alive`.

| Property | Default |
|---|---|
| `sysmonitor.enabled` | `false` |
| `sysmonitor.broker.address` / `sysmonitor.broker.port` | `amqp-broker` / `5671` |
| `sysmonitor.exchange` | `arrowhead` |
| `sysmonitor.local.cloud` | `TestCloud` |
| `sysmonitor.service.instance` | system name |
| `sysmonitor.publish.interval` | `30` |
| `sysmonitor.ssl.enabled` | `true` |
| `sysmonitor.ssl.key-store(-password)`, `sysmonitor.ssl.trust-store(-password)` | the `server.ssl` stores |

The broker must authenticate the client certificate and allow the system to write its own topic.

## Failed start-ups exit (local extension)

`eu.arrowhead.common.init.StartupFailureExit` (registered in `META-INF/spring.factories`) watches for a failed Spring
Boot start-up. If the JVM is still running 10 seconds later, because a library left a non-daemon thread behind, it exits
with status 1, so a container restart policy retries the start. It is disabled with
`-Darrowhead.startup.failure.exit=false` and inside Maven Surefire test runs. `SysMonitorPublisher.stop()` always
disconnects and closes its MQTT client, which also cancels a running automatic reconnect cycle.

## Identity token bound to the client certificate (local extension)

Over HTTPS the `outsourced` authentication filter (`OutsourcedFilter`) accepts an identity token (or an authenticator
key) only together with the TLS client certificate of the same system: the system named by the token must be the
system named by the certificate CN (`eu.arrowhead.common.security.TokenCertificateBinding`). A stolen token replayed
from another system's certificate is refused with 401, as is a token sent over TLS without a client certificate. Plain
HTTP requests carry no TLS identity and are left to the token alone. Authentication applies the same check to its own
token-protected operations and binds `login`, `logout` and `change` to the certificate, so that a system can only log in
as the system named by its certificate. Operators therefore need an operator certificate (`dnQualifier=op`) whose CN
names their identity, for example `Sysop.<cloud>.<organization>.arrowhead.eu`.

Related start-up behaviour:

- The outsourced login job (`OutsourcedLoginJob`) keeps retrying after an authentication failure instead of
  unscheduling itself, because the refusal can be transient (Authentication restarting, or an identity that is
  provisioned after the system started, such as an outsourced ServiceRegistry).
- Before an outsourced system registers itself, it logs in directly (`loginIfNeeded`) when the scheduled login job has not
  produced a token yet, for example because the system starts its Quartz scheduler later
  (`spring.quartz.auto-startup=false`). Registration needs the token when the ServiceRegistry is outsourced.
- A system registers its own interfaces with policy `CERT_AUTH` when it verifies its requesters behind TLS (certificate,
  outsourced or internal authentication) and `NONE` for declared identities or without TLS.
  `service.interface.policy` (for example `NONE`) overrides this default, and `service.extra.interfaces` adds further
  ingresses (`ServiceInterfaceExtensions`).
