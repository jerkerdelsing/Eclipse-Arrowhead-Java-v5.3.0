# Eclipse Arrowhead SysML v2 library — 5.2.2

Updated 2026-10-27 from the local Eclipse-Arrowhead implementation and documentation snapshot. Status: RELEASE CANDIDATE, consistent with the source documents. This library contains 41 `.sysml` files describing five core system variants and sixteen support systems: 91 services, 124 HTTP/MQTT/AMQP interfaces and 424 protocol-specific operations.

## Import and use

Import **all `.sysml` files in this directory together** into a SysML v2 project that provides the standard SysML/KerML libraries. Do not also import the old `ArrowheadDSL.sysml`: it defines the same `AFPVNDatatypes` and `ArrowheadProfile` namespaces.

- `ArrowheadDSL.sysml`: translated DSL metadata, datatype enums and compatibility names.
- `ArrowheadFoundation.sysml`: structural system/service, local-cloud, device and network definitions and associations.
- `ArrowheadCoreData.sysml` and `Arrowhead<System>Data.sysml`: core DTOs/enums and support-system data models.
- `Arrowhead<System>.sysml`: each system's SysD, SysDD and implementation definitions, service SD/IDD/implementation port definitions and operation action definitions.
- `ArrowheadLocalCloudLibrary.sysml`: an optional-membership cloud catalogue and a system-of-local-clouds definition.
- `COVERAGE.md`, `MIGRATION.md`, `sources.json` and `VALIDATION.md`: coverage, migration decisions, exact provenance and validation results.

For example:

```sysml
package MyArrowheadDesign {
    private import ArrowheadServiceRegistry::*;
    private import ArrowheadEventHandler::*;
    part registry : ServiceRegistry;
    part events : EventHandler;
    action publishEvent : 'EventHandler_eventPublish_http_publish_0';
}
```

For reusable cloud composition, specialize `ArrowheadLocalCloudLibrary::ArrowheadLocalCloud` and select the systems required by the design. Catalogue members have `[0..1]` multiplicity and subset `systems`; this is an engineering catalogue, not a claim that every member is deployed or required. Dynamic and simple-store orchestration remain separate variants. Connections, bindings, selected transports and deployment multiplicities must be supplied by the application model.

## Modeling conventions

Each system has `<System>_SysD`, `<System>_SysDD` and `<System>` definitions. Each service has an `_SD` port and one `_HTTP_IDD`, `_MQTT_IDD` or `_AMQP_IDD` port per documented transport, with corresponding `_Implementation` definitions. SysD service ports and SysDD protocol ports represent different abstraction levels and remain separately named; they are not automatically bound. The system implementation inherits its SysDD protocol ports. Operation action definitions are reusable contracts, not executable HTTP clients or workflow implementations.

Port direction is from the provider's perspective: request inputs enter the provider; responses and published observations leave it. `@WireOperation` retains method, full HTTP path or topic/routing address, source parameter types, query/path parameters and documented status codes. The core extraction does not specify MQTT response types; these remain `JSONMessage`. Support HTTP parameter details are retained in their source IDDs, rather than inferred from request samples. Registration facts are modeled when extractable and also retained in documentation comments. `TestCloud` broker names, routing keys and ports are source deployment examples, not universal Arrowhead defaults.

Data fields preserve available source types. Lists and arrays become multi-valued features. Known DTO references are typed; open maps, unknown schemas and unconstrained JSON use `JSONValue`/`JSONMessage`. Primitive request/response values remain attributes. Source types and field descriptions are retained beside each field. `[0..1]` and `[0..*]` are deliberately permissive because the extracted catalogues do not establish every field's required/nullability constraints; these models are not JSON validation schemas. JSON `Number` uses `Real`; Java integer types use `Integer`.

## Sources and scope

Primary inputs are the earlier v2 translation under `distribution/Papyrus/ArrowheadDSL_2025_v5`, its SysML v1.6 UML profile, the target core `documentation-tools/model.json`, and the sixteen support systems' `documentation/v5.2.2` SD/SysD/SysDD/IDD files. `sources.json` records their hashes and every modeled operation.

The target core catalogue omits the conditional native `sysMonitor` service from common-utils. Each of the five target JARs was checked for `SysMonitorPublisher.class`. The publisher class also matches the source build byte for byte in all five systems. Its AMQP IDD and core SysD text were taken from the matching source checkout at `../Arrowhead/ah5-core-java-spring`; that checkout's core catalogue matches the target catalogue byte for byte. These additional source paths and target JAR hashes are explicitly recorded. Native monitoring is conditional on `sysmonitor.enabled=true`; this library does not imply it is enabled in every deployment.

No legacy Java implementations, Go/Rust systems or obsolete 4.x services have been added. No external online specification was used as a substitute for these local updates. Authentication, ConsumerAuthorization and the two orchestration variants are modeled by their current names. Support systems retain their documented responsibilities, including PlantDescriptionEngine's managed DSO rules, Onboarding/CertificateAuthority certificate lifecycle, Gatekeeper/GateTunnel inter-cloud functions, and the AMQP-to-MQTT monitoring pipeline.

## Regeneration and validation

`tools/generate_library.py` regenerates the model files and operation/source inventory. It needs Python 3 and the source tree plus matching core documentation checkout; it does not modify source systems. Run it with an explicit new output folder when reviewing updates:

```sh
python3 tools/generate_library.py --source-root /path/to/Eclipse-Arrowhead --core-doc-root /path/to/Arrowhead --output /path/to/review-library
```

Use `tools/validate.sh` with `ECLIPSE_HOME` pointing to a SysML v2-enabled Eclipse installation and `SYSML_STANDARD_LIBRARY` pointing to the matching `sysml.library` directory. This loads all supplied models, resolves references and runs the installed Xtext validator. See `VALIDATION.md` for the exact versions and result used for this snapshot.
