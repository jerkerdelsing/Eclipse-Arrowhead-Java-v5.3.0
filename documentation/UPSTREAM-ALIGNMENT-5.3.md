# Upstream alignment for the 5.3 reference

This list records what the primary Java tree changes compared with the released Eclipse Arrowhead 5.2.2 components. Each item is fixed here. None is released upstream, and nothing has been submitted upstream from this list.

| Item | State of the primary tree | What upstream needs |
|---|---|---|
| Framework version | `Constants.AH_FRAMEWORK_VERSION` is `5.2.2`. A unit test ties it to the library version. | Set the constant in `arrowhead-common`; release 5.2.2 carries `5.2.1`. |
| SimpleStoreServiceOrchestration port | Source default and image use 8442. | Change the default and rebuild the published image, which still carries 8465. |
| Onboarding port | 8435. | Adopt the port specification. |
| Monitoring | Systems publish the native `sysMonitor` service over AMQPS (SysMonitor-AMQP-IDD-5.2.2). The two stale `monitor` TODO comments are gone. | Decide on `sysMonitor` as the standard service, then release the contract and `eu.arrowhead.common.sysmonitor`. |
| Secure profile | `SecurityProfileGuard` stops a system below the profile. | Adopt the guard, or an equivalent check, in `arrowhead-common`. |
| Restricted discovery with SimpleStore | The three intended combinations are documented. | Add them to the upstream documentation. |
| Inter-cloud | Gatekeeper and GateTunnel exist as project implementations. The provider side uses `authorizationManagement.check-policies`. | Release Gatekeeper and GateTunnel, or a replacement. DynamicServiceOrchestration needs the inter-cloud path as well. |

## Versions

The architecture documents are at 5.3.0 and this tree builds as 5.2.2. Moving the code to 5.3.0 is a separate release step: it changes every POM, JAR name, image tag and deployment reference.
