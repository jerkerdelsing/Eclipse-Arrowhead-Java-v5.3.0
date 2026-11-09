# IP block, InterfaceContract and chiplet network-property library

## Engineer workflow

In ChipletLocalCloudManager, open **Floorplans**. The SysML **IP block library** replaces the original CPU/MEMORY/IO/ADC/NPU/CUSTOM preset palette. Choose a library block to add it to the selected chiplet; with no selection, an editable neutral chiplet container is created. Drag a library block onto an existing chiplet to add it there, or onto free interposer space to create a new chiplet. **Add empty chiplet** creates only an empty physical container. Its initial dimensions are editable layout placeholders, not dimensions from the SysML model. Existing saved floorplans remain readable. Select the block or one of its teal port markers to inspect a typed interface. The **Interface contract** selector chooses which port's contract to edit. Contract types follow the SysML block definition, so a digital input cannot be silently assigned an RF contract.

Use **Use this interface contract in the design** to include a port, or leave it unused. Filled teal markers denote included interfaces; hollow markers denote unused interfaces. The type is retained either way. Enter block parameters and interface values. Numeric fields use explicit SI units: V, A, Hz, bit/s, ohm, W and bit. The source UML supplies value types but no concrete electrical limits; no electrical defaults, physical die sizes, or verified hardware capabilities are inferred from it. Empty fields remain unspecified draft values. Payload/configuration fields are distinct from numeric electrical fields.

Open **Network properties on this chiplet** and tick the Paper 6 properties present in the design. The criterion for verifying each property is shown. This is separate from the existing floorplan-wide network model and D2D carrier selection. Individual chiplets may therefore have different property selections. No properties are automatically enabled by choosing a network model. These are engineer declarations, not evidence of implementation.

Save the floorplan, or save a copy. Reopening restores IP blocks, parameter values, selected typed contracts and network properties. Undo/redo, auto-layout, JSON import/export and normal chiplet editing retain these values. IP-block layout within a chiplet is a schematic arrangement, not a physical IP placement constraint. This editor does not deploy or enable hardware.

## System design and interface extension

FloorPlanGenerator embeds a versioned, source-derived catalog. Existing `floorplanDiscovery.query-floorplans` accepts `library=true` and returns the catalog instead of stored plans. This keeps the existing service discovery, orchestration, mutual TLS and authorization boundary. No additional management privilege is required. Manager BFF route: `GET /api/chiplet/floorplans/library`. Direct discovery request: `GET /chipletfloorplangenerator/floorplans?library=true`.

The response contains `version`, `sources` (paths and SHA-256), `unitsNote`, `ipBlocks`, `contracts`, `networkProperties`, and the three source compatibility rules. Nine contract definitions include the ElectricalContract base and eight concrete contracts. The library is compiled into the generator; UI and backend use the same catalog. `tools/import-ip-library.py` regenerates it from the two specified source files.

Each floorplan chiplet may add:

```json
{
  "networkProperties": ["IP1", "IP13", "IP22"],
  "ipBlocks": [{
    "instanceId": "ADC-1",
    "definitionId": "ADC",
    "libraryVersion": "paper9-paper6-20261029-1",
    "parameters": {"resolution": 12},
    "interfaces": [
      {"port": "analogIn", "contractId": "AnalogReceiverContract", "values": {}},
      {"port": "clockIn", "contractId": "DigitalReceiverContract", "values": {}},
      {"port": "dataOut", "contractId": "DigitalProviderContract", "values": {}},
      {"port": "complete", "contractId": "DigitalProviderContract", "values": {}}
    ]
  }]
}
```

The generator checks library version, definitions, unique instance IDs per chiplet, complete typed port lists, boolean interface usage flags, contract/port matches, known fields, enum values, numeric types, nonnegative rates/current/resistance/power/bit counts, whole bit counts, and unique property IDs IP1–IP22. Limits: 64 blocks per chiplet, 256 per floorplan; existing request-size limits still apply. Older floorplans with no library fields remain supported. Missing numeric values are allowed for design drafts. This is schema/type validation, not a proof of electrical compatibility between two instantiated ports. Existing network binding admission still reads authoritative ResourceRegistry electrical contracts and remains unchanged.

GraphDB stores the complete floorplan JSON and an RDF projection of each block, its typed interfaces/contract values, library revision and each chiplet's declared network properties. Generator auto-placement preserves these nested selections. Catalog discovery does not require a GraphDB query, but floorplan saves/reopens use the existing GraphDB repository. Existing hardware delivery and signed-recipe behavior has not been extended to instantiate IP blocks; the additions describe the design.

## Source block inventory

| IP block | Typed ports |
|---|---|
| DigitalFilter | dataIn: DigitalReceiverContract, clockIn: DigitalReceiverContract, dataOut: DigitalProviderContract, config: ConfigurationContract |
| ADC | analogIn: AnalogReceiverContract, clockIn: DigitalReceiverContract, dataOut: DigitalProviderContract, complete: DigitalProviderContract |
| DAC | dataIn: DigitalReceiverContract, clockIn: DigitalReceiverContract, analogOut: AnalogProviderContract |
| Timer | clockIn: DigitalReceiverContract, triggerIn: DigitalReceiverContract, pulseOut: DigitalProviderContract, interruptOut: DigitalProviderContract |
| Register | dataIn: DigitalReceiverContract, writeEnable: DigitalReceiverContract, clockIn: DigitalReceiverContract, dataOut: DigitalProviderContract |
| Memory | addressIn: DigitalReceiverContract, dataIn: DigitalReceiverContract, controlIn: DigitalReceiverContract, dataOut: DigitalProviderContract |
| ShiftRegister | serialIn: DigitalReceiverContract, parallelIn: DigitalReceiverContract, serialOut: DigitalProviderContract, parallelOut: DigitalProviderContract |
| ProcessingUnit | instructionBus: DigitalReceiverContract, dataBus: DigitalReceiverContract, statusOut: DigitalProviderContract |
| DigitalIO | padIn: DigitalReceiverContract, padOut: DigitalProviderContract, config: ConfigurationContract |
| AnalogIO | analogIn: AnalogReceiverContract, analogOut: AnalogProviderContract, config: ConfigurationContract |
| EthernetInterface | mdi: DifferentialContract, hostDataIn: DigitalReceiverContract, hostDataOut: DigitalProviderContract |
| WLANInterface | rf: RFContract, hostDataIn: DigitalReceiverContract, hostDataOut: DigitalProviderContract |
| BluetoothInterface | rf: RFContract, hostDataIn: DigitalReceiverContract, hostDataOut: DigitalProviderContract |
| RS232Interface | rx: DigitalReceiverContract, tx: DigitalProviderContract |
| I2CInterface | sda: OpenDrainBusContract, scl: OpenDrainBusContract |

## Paper 6 network-property inventory

| ID | Property | Verification criterion |
|---|---|---|
| IP1 | Location-independent endpoint identity: services are addressed by logical endpoint identifiers (LEIDs) that do not encode physical position | Migrating an endpoint requires no change in any peer's state |
| IP2 | Programmable indirection from LEID to physical port at every fabric attach point | Translation entries can be rewritten at runtime; lookup latency is bounded |
| IP3 | Fabric-level discoverability: each attach point exposes a readable identity and capability descriptor | Control plane enumerates all endpoints and links without prior knowledge beyond the root |
| IP4 | Switched topology with programmable routing, not fixed point-to-point wiring or a single shared medium | Any admissible source--destination pair is reachable by table update alone |
| IP5 | Path and lane redundancy | Single-link or lane failure leaves every admitted flow reachable |
| IP6 | Protocol-agnostic packet transport with separated message classes (request, response, control, migration, telemetry) | Each class maps to its own virtual channel; no class can block another |
| IP7 | Deadlock freedom for every admissible configuration and every transition between configurations | Channel dependency graph check passes before commit (Section sec:proto) |
| IP8 | Ordering domains defined per microsystem rather than globally | Order preserved within a domain across migration; other domains unaffected |
| IP9 | Scoped drain and quiesce primitives with completion detection | Control plane can detect when no packets of a flow or version remain in flight |
| IP10 | Versioned, atomic configuration with per-packet consistency | No packet is routed, translated, or filtered by a mixture of old and new configuration |
| IP11 | Bounded, predictable reconfiguration time | Measured reconfiguration time stays within the bound of Eq. eq:trc |
| IP12 | Reservable bandwidth and latency per flow | Admitted flows meet their reservation while other flows are reconfigured |
| IP13 | Protected control channel (out-of-band or reserved in-band) that survives data-plane congestion or failure | Configuration and telemetry remain deliverable under saturating data traffic and single data-link failure |
| IP14 | Unforgeable source identity and endpoint firewalls | A packet with a forged source or disallowed destination is dropped and reported |
| IP15 | Authenticated configuration writes and scrub on reassignment | Unsigned or stale-version configuration is rejected; buffers of a reassigned endpoint are cleared |
| IP16 | Per-link and per-flow telemetry (errors, retries, latency, occupancy) | Counters readable at runtime without halting traffic |
| IP17 | Error containment and repair (retry, lane repair, link isolation, poisoned-packet handling) | A failing link is isolated and flows rerouted without corrupting other flows |
| IP18 | Trace and fault injection for V&V | Controlled faults can be injected on any link and their effect observed |
| IP19 | Version and capability negotiation with protocol bridging | Chiplets of different interface revisions interoperate or are refused explicitly |
| IP20 | Clock- and power-domain independence of attach points | An endpoint or region can be clock- or power-gated without stalling unrelated traffic |
| IP21 | Always-reachable lifecycle proxy: identity, health, wake request, and isolation state remain accessible while the functional endpoint is gated | A gated endpoint is discoverable and can be authenticated, awakened, tested, or refused without accessing its inactive functional registers |
| IP22 | Typed electrical contract for every bindable physical port, checked before admission and after link-mode change | Incompatible signal type, voltage thresholds, drive/load current, data rate, or required standard-specific parameters prevent binding and are reported |

## Source provenance

- `Papers/Paper-9/SysMLModelling/SysMLModelling.uml` — SHA-256 `9dbaabd04572c2b6d156d522a8d6f278413cbbfb9519d385df3bb7ae2e5dbe3a`
- `Papers/Paper-6/chiplet interconnect reorg.tex` — SHA-256 `364dd1c7f286df2512d1bcf034dbbce5e65f03d3248c7d9bf004f574717a6603`
