# EventHandler 5.2.2

> Current standalone packaging (25 October 2026): build and deploy from this folder. See [DEPLOYMENT.md](DEPLOYMENT.md). Any combined-tree build or live-cloud examples below are historical integration context.

EventHandler is an Eclipse Arrowhead support system for local-cloud publish-subscribe messaging. This module is the v5.2.2 successor to the v4.6 `core-java-spring/eventhandler` implementation.

The migration keeps event publication, subscriptions, unsubscription, metadata filtering, validity windows, management operations, and HTTP callback delivery. It adopts the v5 system lifecycle, authenticated requester identity, Java 21, Spring Boot 3, OpenAPI 3, independent persistence, and the Arrowhead naming convention.

## Services

- `eventPublish`: `publish`
- `eventSubscription`: `subscribe`, `unsubscribe`
- `eventHandlerManagement`: `query-subscriptions`, `update-subscription`, `remove-subscription`
- `generalManagement`: `get-log`, `get-config`
- `sysMonitor` (AMQP, native, via the common library when `sysmonitor.enabled=true`): `echo`, `power`, `status`

System names use PascalCase, service and event type names use camelCase, service operation names use kebab-case, and JSON fields use camelCase.

## Security

- Authentication: any v5 policy; secured deployments (and the Arrowhead test clouds) use `outsourced` with mutual TLS
  (`server.ssl.client-auth=need`). The common library accepts an identity token only together with the client
  certificate of the same system. All services are registered as `generic_https` with `CERT_AUTH`.
- Authorization: with `service.authorization.enabled=true` (`EventHandlerAuthorizationFilter`), ConsumerAuthorization
  decides every `eventPublish.publish`, `eventSubscription.subscribe` and `eventSubscription.unsubscribe`
  (`authorization.verify` with EventHandler as provider; decisions cached for 30 s, fail closed). Denial gives 403.
  Management uses the common management filter (`management.policy`).
- Delivery: notifications are POSTed with the EventHandler client certificate and its identity token. With TLS enabled,
  `notifyUri` must use HTTPS (400 otherwise), so the token never travels in clear text. The notification `timestamp` is
  an RFC 3339 string.

## Database

Create the system-owned database and tables with the scripts under `src/main/resources/database`. Update credentials in `application.properties` before deployment.

## Build

Install `ah5-common-java-spring` and the `arrowhead-core` 5.2.2 parent pom (from `ah5-core-java-spring`) into the local Maven repository, then run from this directory:

```sh
mvn clean test
```

`docker/Dockerfile-EventHandler` builds a self-contained image from the workspace root (common library, parent pom,
EventHandler).

## Deployment in the Arrowhead test clouds

`Arrowhead-test-cloud` and `Arrowhead-test-cloud-go` deploy EventHandler with `make up` (or `make up-eventhandler`):
the jar is built by `scripts/build-java-jars.sh` and packaged by `eventhandler/Dockerfile`; the service `eventhandler`
listens on 8455 (9455 in the Go cloud) with its own TLS MySQL `eventhandler-db`, X.509 stores in
`runtime/EventHandler/config`, the Authentication identity `EventHandler`, the grants of
`scripts/configure-eventhandler-policy.sh`, and a broker ACL entry for its `sysMonitor` topic.

## Documentation

[SysD, SysDD and an SD and IDD for each produced service](documentation/v5.2.2/README.md) (`eventPublish`, `eventSubscription`, `eventHandlerManagement`, `generalManagement`, `sysMonitor`), generated from the implementation with the official Arrowhead LaTeX templates by [`documentation-tools`](../documentation-tools/README.md); the PDFs are beside the sources. The earlier `Document/LaTeX` set is in `../../_superseded/2026-10-22/`.
