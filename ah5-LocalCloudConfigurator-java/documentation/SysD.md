# SysD — LocalCloudConfigurator

## Black-box purpose

An operator composes a local cloud from permitted microsystems and infrastructure, saves its design, reviews ordered changes, and applies them through the host worker (ColonyOS only as the retained legacy backend). The default consists of the Arrowhead core minus SimpleOrchestration, plus necessary infrastructure. The target is an existing provisioned Java cloud, not bare-host PKI/database bootstrap.

Inputs: named designs, selected component IDs, explicit removals, reapply/verification flags and node positions. Outputs: persisted versioned designs, observed runtime states, dependency-expanded plans, reviewed plan hashes and workflow progress/results.

Interfaces: the `localCloudConfiguration` Arrowhead service (consumed by LocalCloudManager through orchestration), generalManagement and sysMonitor; the host worker over mutual TLS → existing Docker Compose and catalogued scripts; signed ColonyOS RPC only for the legacy backend. LocalCloudConfigurator is a registered Arrowhead support system (Java-Spring); its host worker is its local agent and does not register. The browser cannot supply arbitrary commands or images. Saving/editing a design does not modify the runtime.

Protected assets: core control-plane availability, database volumes, manager reachability, host command execution, identity material and operator credentials. Existing services remain unchanged unless an explicitly reviewed permitted action targets them. Authorization: ConsumerAuthorization grants of the `localCloudConfiguration` operations (for LocalCloudManager), Sysop for generalManagement, and certificate-pinned mutual TLS between API and host worker.

### Database ownership

A defined database is a private integrated asset of its host microsystem. `internalAssets` identifies the owned database Compose services; `services` orders database startup before host startup (reverse for stopping). It is not a catalog component or external dependency. Other microsystems access data only through the host’s produced or consumed services. Dedicated internal database networks admit only the host and its database, with no published database port. The default design has four microsystems plus one AMQP infrastructure node.
