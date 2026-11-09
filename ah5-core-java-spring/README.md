# Arrowhead Framework 5 Core Systems (ah5-core-java-spring)

Visit the official documentation site:

https://aitia-iiot.github.io/ah5-docs-java-spring/home/welcome/

## Build

Core uses the shared library in the sibling folder `ah5-common-java-spring`. Install it first, then build Core:

```sh
(cd ../ah5-common-java-spring && mvn -B clean install)
mvn -B clean verify
```

## Local extensions in this workspace

- **Secure profile at startup.** A system stops before it accepts a connection when its effective configuration is below the default secure profile. The packaged `application.properties` files are development samples and are refused too. See [SECURITY-PROFILE.md](../SECURITY-PROFILE.md).
- **SimpleStoreServiceOrchestration with restricted discovery.** Under the default `service.discovery.policy=restricted`, a consumer that resolves pull results with `lookup` must be in `service.discovery.direct.access`. Otherwise the instance is missing from the result without an error. See the SysDD of the system for the three intended combinations.
- **Inter-cloud orchestration (DynamicServiceOrchestration).** Upstream 5.2.2 returns no inter-cloud results. Here, with
  `enable.intercloud=true`, `InterCloudServiceOrchestration` asks the local Gatekeeper's `globalServiceDiscovery.query`
  for authorized providers in the neighbour clouds, then negotiates one with `interCloudNegotiation.negotiate`. The
  result carries the provider cloud as cloud identifier and the local GateTunnel session endpoint as its interface.
  `ONLY_INTERCLOUD` goes straight to the Gatekeeper; `ALLOW_INTERCLOUD` falls back to it when local orchestration finds
  no provider. Warnings: `interCloud`, `interCloudDisabled`, `interCloudFailed`. See `../Arrowhead-relay/README.md`.
- **ServiceRegistry** honours `service.interface.policy` for its own services, and supports the outsourced policy with
  token-to-certificate binding (common library).

## Listening port

Each core system sets its default in the packaged `application.properties` (`server.port`); override it with `server.port` in `config/application.properties`, `SERVER_PORT=N` or `--server.port=N`.

| System | Module | Default |
|---|---|---|
| DynamicServiceOrchestration | `serviceorchestration-dynamic` | 8441 |
| SimpleStoreServiceOrchestration | `serviceorchestration-simple` | 8442 |
| ServiceRegistry | `serviceregistry` | 8443 |
| Authentication | `authentication` | 8444 |
| ConsumerAuthorization | `consumer-authorization` | 8445 |

See [LISTENING-PORTS.md](../LISTENING-PORTS.md) for precedence and all systems.
