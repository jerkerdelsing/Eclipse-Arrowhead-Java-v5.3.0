# TimeManager 5.2.2

> Current standalone packaging (25 October 2026): build and deploy from this folder. See [DEPLOYMENT.md](DEPLOYMENT.md). Any combined-tree build or live-cloud examples below are historical integration context.

TimeManager is an Eclipse Arrowhead 5.2.2 support system that gives the local cloud the current time, the configured time zone and an assessment whether the time can be trusted. It is the v5.2.2 successor to the v4.6 `core-java-spring/timemanager` core system.

It periodically compares its clock with NTP servers (SNTP, RFC 4330) and reports the time as *trusted* while the absolute clock offset is below `time.offset.threshold`. It publishes the Arrowhead 5.2.2 `sysMonitor` service natively; `echo` is `degraded` while the time is not trusted.

## Services

| Service | Version | Operation | Interface |
| --- | --- | --- | --- |
| `time` | 1.0.0 | `get-time`: `GET /timemanager/time` | `generic_https` (`generic_http` without SSL) |
| `generalManagement` | 1.0.0 | `get-log`: `POST /timemanager/general/mgmt/logs`; `get-config`: `GET /timemanager/general/mgmt/get-config` | `generic_https` |
| `sysMonitor` | 1.0.0 | `echo`, `power`, `status` on `<localCloud>.sysmonitor.TimeManager.TimeManager.<type>` (exchange `arrowhead`) | `generic_amqps`, `CERT_AUTH`, `publisherMode=native` |

`get-time` returns:

```json
{"epoch":1791779052,"epochMs":1791779052879,"tz":"Europe/Stockholm","dst":true,"trusted":true,
 "offsetMs":111,"timeSource":"pool.ntp.org","lastCheckTime":"2026-10-12T04:24:05.559Z"}
```

`epoch`, `epochMs`, `tz`, `dst` and `trusted` are the v4.6 fields; `offsetMs`, `timeSource` and `lastCheckTime` are additive and `null` until the first NTP check.

## Trust rule

| Situation | `trusted` | `sysMonitor` `echo` |
| --- | --- | --- |
| No NTP check completed since start-up | `false` | `degraded` |
| \|offset\| < `time.offset.threshold` | `true` | `alive` |
| \|offset\| ≥ `time.offset.threshold` | `false` | `degraded` |
| No configured NTP server answers | `false` | `degraded` |

The servers of `ntp.server.list` are tried in order; the first valid answer decides. Replies are accepted only in server mode, with stratum 1–15, and when their originate timestamp echoes the request.

## Configuration

| Property | Default | Meaning |
| --- | --- | --- |
| `server.port` | `8463` | HTTP(S) port (as in v4.6) |
| `time.timezone` | `UTC` | IANA time zone reported as `tz`/`dst`; an invalid zone stops start-up |
| `ntp.server.list` | `pool.ntp.org` | Comma-separated NTP servers |
| `time.offset.threshold` | `5000` | Milliseconds |
| `ntp.check.interval` | `60000` | Milliseconds between checks |
| `ntp.timeout` | `10000` | Milliseconds per NTP query |
| `service.authorization.enabled` | `false` | When `true`, every `get-time` call is checked with ConsumerAuthorization (`authorization.verify`, target `time`, scope `get-time`; decisions cached 30 s, fail closed). The test clouds enable it. |
| `sysmonitor.enabled`, `sysmonitor.broker.*`, `sysmonitor.ssl.*` | `true`, `amqp-broker:5671`, `server.ssl` stores | Native `sysMonitor` publisher of the common library |

The host must reach the NTP servers on UDP port 123.

## Database

TimeManager owns the `ah_timemanager` schema, which holds only its operational log (`generalManagement get-log`). Create it with `src/main/resources/database/create_empty_db.sql` and `grant_privileges.sql`, and set the credentials in `application.properties`.

## Build

Install `ah5-common-java-spring` and the `arrowhead-core` 5.2.2 parent pom (from `ah5-core-java-spring`) into the local Maven repository, then run from this directory:

```sh
mvn clean test
```

`docker/Dockerfile-TimeManager` builds an image from the workspace root (it needs `ah5-common-java-spring`, `ah5-core-java-spring/pom.xml` and this folder).

## Test-cloud deployment

Both 5.2.2 test clouds (`Arrowhead-test-cloud`, `Arrowhead-test-cloud-go`) deploy TimeManager as service `timemanager` with its own `timemanager-db` (`make up`, or `make up-timemanager` on a running cloud). It runs with mTLS (`server.ssl.client-auth=need`), outsourced authentication with Authentication identity tokens and `management.policy=sysop-only`. When TLS requires a client certificate, `TimeManagerApplicationInitListener` re-registers the HTTP offers with interface policy `CERT_AUTH` (the framework would register `NONE` for non-certificate authentication). The `time` and `sysMonitor` grants are set by the cloud's `scripts/configure-timemanager-policy.sh`.

## Migration from v4.6

- Core system `TIMEMANAGER` becomes support system `TimeManager`; service `time` keeps its name and URL and gains the operation name `get-time`.
- `GET /timemanager/echo` is removed; liveness and trust are published through `sysMonitor`.
- The WebSocket push (`/timemanager/time/ws`) is removed: Arrowhead 5.2 defines no WebSocket interface template. Consumers call `get-time`.
- Trust fixes: the absolute offset is compared (v4.6 trusted any clock that ran ahead), the time is untrusted until the first check (v4.6 started trusted), and a server list with fallback is supported (v4.6 resolved the whole list as one host name).
- Own database schema instead of the shared v4 database; OpenAPI 3, Jakarta and Java 21; a built-in SNTP client replaces Apache commons-net.

## Documentation

[SysD, SysDD and an SD and IDD for each produced service](documentation/v5.2.2/README.md) (`time`, `generalManagement`, `sysMonitor`), generated from the implementation with the official Arrowhead LaTeX templates by [`documentation-tools`](../documentation-tools/README.md); the PDFs are beside the sources. The earlier `documentation/v5.2` set is in `../../_superseded/2026-10-22/`.

## Listening port

Default **8463** (`server.port` in the packaged `application.properties`). Override with `server.port` in `config/application.properties`, `SERVER_PORT=8463` or `--server.port=8463`.

See [LISTENING-PORTS.md](../LISTENING-PORTS.md) for precedence and all systems.
