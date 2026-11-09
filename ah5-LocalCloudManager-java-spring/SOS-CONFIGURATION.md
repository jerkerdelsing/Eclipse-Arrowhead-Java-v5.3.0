# Graphical System of Systems configuration

Open **SoS** in Java LocalCloudManager: http://testcloud.localhost:8490/ . The view edits PlantDescriptionEngine configurations, so compositions are shared with the existing Plant descriptions view and use its established runtime behavior.

## Compose and edit

Choose **New SoS**, name it, and select a registered system to add. Its advertised provided services become service ports. **Add design system** creates an editable name/metadata placeholder. Select a system header to edit its identifier, registered name or matching metadata. Add service ports, choose required/consumer or provided/producer, and set service definition and interface template. Provider ports can have matching service metadata; consumer ports cannot.

Click a required port and a compatible provided port to connect them (either selection order works). Matching service definitions and interface templates are checked. Select the drawn connection to edit priority or remove it. Removing a system or a port removes its incident draft connections. Renaming either updates endpoint references. Drag system headers to arrange the canvas, or choose Auto-layout. Undo/redo applies to composition and layout changes. Port and connection selection supports keyboard Enter/Space; Escape cancels a pending connection.

Use **Include other SoS configurations** to compose nested configurations. Their systems and connections are projected read-only with dashed outlines. The inspector links to the owning configuration. Included ports may be referenced by local connections. Missing included records, cycles and ambiguous system IDs are rejected. Removing an include may leave invalid local references; fix those before saving.

## Save, activate and delete

**Save SoS** writes the composition to PlantDescriptionEngine. New configurations are inactive by default. **Active orchestration configuration** selects the engine's existing active behavior; saving any active configuration requires explicit confirmation because it can change orchestration constraints. Connections do not independently grant security authorization. **Save a copy** and imports create inactive designs. The classic Plant descriptions view remains available.

Save and delete compare the originally opened record and its timestamp against a fresh authority read. Stale edits are rejected and require Reload. This is a best-effort check, not an atomic transaction across independent clients. PlantDescriptionEngine remains the authoritative validator and synchronizer; runtime synchronization and service availability can still affect actual behavior. Delete asks for confirmation and removes the stored configuration, including any resulting active constraints according to engine semantics.

Node positions are intentionally separate from runtime name/metadata filters. They are remembered in browser local storage by cloud origin and configuration ID, and are included in JSON exports. Another browser can import the export to restore those positions; otherwise it automatically arranges the saved composition. Browser storage is not the composition's authoritative store. The SVG depicts configured service relationships, not observed traffic or guaranteed authorization.

## Import and export

Export creates `arrowhead-sos-1` JSON with `configuration` and `layout`. Import accepts that format or a plain PlantDescriptionEngine configuration, validates it, and opens a new inactive draft. Existing saved SoS records are never overwritten merely by importing. Existing 64 KiB request/import limits apply.

## Interface and implementation

The manager adds authenticated routes:

| Method | Route | Behavior |
|---|---|---|
| GET | `/api/sos` | Stored configurations and registered system/service catalog |
| GET | `/api/sos/{id}` | Current authoritative configuration |
| POST | `/api/sos` | Create from `{configuration, activationConfirmed?}` |
| PUT | `/api/sos/{id}` | Replace from `{configuration, expected, activationConfirmed?}` |
| DELETE | `/api/sos/{id}` | Delete using `{expected}` |

Every mutation requires the existing `X-Arrowhead-Action: confirm` header as well as the existing console session and host/origin guards. Active writes additionally require `activationConfirmed: true`. Requests use the existing authenticated/orchestrated `plantDescriptionManagement` query/create/replace/remove operations. No new authority grants, alternate store or security bypass is introduced. The catalog reads pages of 200 and supports up to 2,000 stored configurations. A configuration supports up to 200 directly declared systems, 200 ports per system, 1,000 direct connections and 200 included IDs, subject to existing request-size and engine limits.

Composition shape:

```json
{
  "plantDescription": "Example SoS",
  "active": false,
  "include": [],
  "systems": [
    {"systemId":"consumer", "systemName":"LocalCloudManager", "metadata":{},
     "ports":[{"portName":"needsTime", "serviceDefinition":"time", "serviceInterface":"generic_https", "consumer":true, "metadata":{}}]},
    {"systemId":"clock", "systemName":"TimeManager", "metadata":{},
     "ports":[{"portName":"timeOut", "serviceDefinition":"time", "serviceInterface":"generic_https", "consumer":false, "metadata":{}}]}
  ],
  "connections":[{"consumer":{"systemId":"consumer","portName":"needsTime"}, "producer":{"systemId":"clock","portName":"timeOut"}, "priority":1}]
}
```

The shared SoS model handles validation, included-record expansion and endpoint maintenance. Legacy nullable producer flags and metadata are normalized with their existing engine semantics. The UI uses existing Arrowhead category colors with white panels and turquoise service ports. PlantDescriptionEngine may apply further validation beyond client checks. Reversible live verification uses only inactive fixtures; activation's actual orchestration effect is intentionally not exercised against live application routes.
