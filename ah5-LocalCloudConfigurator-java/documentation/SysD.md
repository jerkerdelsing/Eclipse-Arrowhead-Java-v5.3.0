# SysD — LocalCloudConfigurator

## Black-box purpose

An operator composes a local cloud from permitted microsystems and infrastructure, saves its design, reviews ordered changes, and applies them through ColonyOS. The default consists of the Arrowhead core minus SimpleOrchestration, plus necessary infrastructure. The target is an existing provisioned Java cloud, not bare-host PKI/database bootstrap.

Inputs: named designs, selected component IDs, explicit removals, reapply/verification flags and node positions. Outputs: persisted versioned designs, observed runtime states, dependency-expanded plans, reviewed plan hashes and workflow progress/results.

Interfaces: authenticated manager proxy/operator API; signed ColonyOS RPC; host executor → existing Docker Compose and catalogued scripts. This is an operations tool rather than a registered Arrowhead system. The browser cannot supply arbitrary commands or images. Saving/editing a design does not modify the runtime.

Protected assets: core control-plane availability, database volumes, manager reachability, host command execution, identity material and operator credentials. Existing services remain unchanged unless an explicitly reviewed permitted action targets them. Authorization is inherited from manager sessions/operator-token access and ColonyOS approved identities; no new Arrowhead grants are needed.

### Database ownership

A defined database is a private integrated asset of its host microsystem. `internalAssets` identifies the owned database Compose services; `services` orders database startup before host startup (reverse for stopping). It is not a catalog component or external dependency. Other microsystems access data only through the host’s produced or consumed services. Dedicated internal database networks admit only the host and its database, with no published database port. The default design has four microsystems plus one AMQP infrastructure node.
