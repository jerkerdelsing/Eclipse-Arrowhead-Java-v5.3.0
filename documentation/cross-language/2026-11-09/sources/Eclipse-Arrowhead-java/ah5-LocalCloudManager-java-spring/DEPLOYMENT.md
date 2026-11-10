# LocalCloudManager deployment (Java-Spring)

Two images, built from this folder: `arrowhead-localcloudmanager-standalone:5.2.2` (console and Arrowhead consumer system, UID
10001, read-only root filesystem, no capabilities) and `arrowhead-localcloudmanager-docker-proxy-standalone:5.2.2` (Docker proxy).
nginx is no longer used.

Copy `.env.example` to `.env`; select an existing cloud network, a unique Compose project and host port and a non-overlapping
`CONTROL_SUBNET`. Provide in the named files:

- `LocalCloudManager.p12` (system certificate `LocalCloudManager.<Cloud>.<Organization>.arrowhead.eu`) and `truststore.p12`, with `store-password`;
- `identity-password` (the Authentication identity of LocalCloudManager);
- `console-password-hash`: `scrypt:<salt base64>:<hash base64>` (N=16384, r=8, p=1; the format of the native console, so existing hashes keep working);
- `crl/ca.crl`, the cloud CRL (provider certificates are refused when revoked; the overview reports its remaining validity).

`docker compose config --quiet`, `docker compose build`, `docker compose up -d`. The console is published on the host's loopback
interface only (`HOST_BIND_ADDRESS`, port 8490); the Arrowhead connector 8491 (mutual TLS, `client-auth=need`) is reachable only in
the cloud network. The console answers only clients at the loopback address, at the Docker gateway of one of the container's
networks (where the published port arrives) or in `CONSOLE_ALLOWED_CLIENTS` (comma-separated addresses or CIDR ranges, e.g. a
reverse proxy); every other client, such as another system on the cloud network, gets 403 before any console processing
(J6). This is an application-level check: such a client can still open a TCP connection to port 8490. The packaged `application.properties` is a development sample and is refused in the default secure profile.

## Console security

- Every `/api` request on the console needs a session obtained with the console password (`POST /api/session`, JSON only); the
  session cookie `ah_console` is HttpOnly and SameSite=Strict. Spring Security checks it (stateless, no HTTP session).
- Host and Origin must be allowed local names (`CONSOLE_ALLOWED_HOSTS`): DNS rebinding is refused. Origin `null` is refused.
- Wrong passwords lock the login per client, doubling up to 15 minutes.
- Optional `CONSOLE_PROXY_SECRET`: when the console is put behind a reverse proxy, only requests carrying `X-Console-Proxy` are
  served and `X-Real-IP` is the client address (not needed without such a proxy).
- Writes need `X-Arrowhead-Action: confirm` (sent by the UI after its confirmation dialog).

## Arrowhead rights

Register the identity `LocalCloudManager` in Authentication. LocalCloudManager needs ConsumerAuthorization grants for the
operations it consumes (the SysDD lists them), now including the operations of `localCloudConfiguration` (provider
LocalCloudConfigurator). `localCloudManagement` on 8491 is open to Sysop, the systems of `LCM_API_WHITELIST`, and systems with a
grant of the operation.

## Docker proxy allowlist

The console never holds the Docker socket. The proxy is a separate process on the internal `control` network and forwards only:

| Request | Condition |
|---|---|
| `GET /containers/json` | always filtered to `com.docker.compose.project=<COMPOSE_PROJECT_NAME>` (the client's filter is ignored) |
| `GET /containers/<id>/logs` | container of the project; only `stdout`, `stderr`, `timestamps` (0/1) and `tail` (≤4 digits) are passed |
| `POST /containers/<id>/start\|stop\|restart` | container of the project whose Compose service is in `MANAGED_SERVICES`; only `t` (≤3 digits) |

`<id>` must be a 12–64 hex container id. Everything else (exec, create, images, volumes, networks, other projects, other methods)
is answered 403. The proxy runs as root only to open `/var/run/docker.sock`, with a read-only root filesystem and no capabilities.

## Verification

`make build` runs both test suites (MockMvc over every `/api` route and the login flow; a real-Tomcat test that the console listens
only on loopback and serves `/api` only after login; the proxy allowlist). No live cloud was changed by the conversion.
