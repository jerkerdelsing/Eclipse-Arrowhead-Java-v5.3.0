# Translation and compatibility notes

This update extends the existing SysML v2 translation of the SysML v1.6 Arrowhead DSL. It retains `AFPVNDatatypes`, `ArrowheadProfile`, all translated metadata definition names and all enum literals, including historical spelling mistakes. It also restores the profile's protocol stereotype names and public datatype spellings.

| Previous translation / v1.6 detail | Updated library decision |
|---|---|
| Local untyped `String`/`Int` declarations | Standard `ScalarValues::String` and `ScalarValues::Integer` |
| `@SysML::PartUsage` / `@SysML::PortUsage` applied as annotations | Removed: metaclasses are not metadata definitions |
| Tags inheriting `SemanticMetadata` without a defined `baseType` | Plain metadata definitions; actual part/port semantics are supplied by the structural library |
| Device and network description tags without design-stage inheritance | Description tags specialize their design tags |
| `IDD::Port` modeled as a nested port | Numeric attribute, consistent with the v1.6 profile's port-number property |
| `IDD::Proyocol` typo | Canonical `Protocol`; optional old property retained for migration, with no automatic synchronization |
| `Certificate_level`, `Crypto_algorithm` | v1.6 `CertificateLevel` and `CryptoAlgorithm` aliases added |
| `SoLCName`, `IOports`, `InterNetworkDomain`, `LC_Workflow`, `LC_Orchestration` | v1.6 aliases `SoLC_Name`, `'I/Oports'`, `InterNetworkDomainName`, `LC_workflow`, `LC_orchestration` added |
| `HTTP11MethodKind`, `HTTP2FrameKind`, `MQTT311MessageTypeKind`, `MQTT50MessageTypeKind` | v1.6 datatype spelling aliases added, including quoted names with punctuation |
| Missing HTTP/2 `PRIORITY` literal | Restored from the v1.6 profile |
| `ServiceProtocol_HTTP`, `ServiceProtocol_MQTT`, `ServiceProtocol_COAP`, `ServiceIProtocol_Websocket` | Restored as specializations of the previous v2 protocol tags; HTTP/MQTT/CoAP status-code properties and MQTT `Topic` alias restored |
| `System_of_LocacloudsDesignDescription` spelling | Retained; corrected `System_of_LocalcloudsDesignDescription` also provided |
| Connections whose ends were typed by metadata definitions | Original nine association names retained in `ArrowheadFoundation`, with structural part/port endpoint types |

HTTP `PATCH`, HTTPS/MQTTS/AMQP/AMQPS protocols and Onboarding/Operator/System certificate categories are additive extensions. Existing literals such as `ComprressionKind`, `ElipticCurve` and `StatusKodKind` are intentionally retained for compatibility. `FullPort` and `ProxyPort` remain metadata tags; SysML v2 service contracts use native port definitions.

The previous translation's textual interpretation of properties such as `FunctionalProperties` is retained rather than reverting to apparent `UnlimitedNatural` properties in the v1.6 profile. This is an update of the translation, not a lossless UML XMI conversion. UML association back-reference properties are represented by native structure/connections rather than copied as metadata object graphs. Structural associations preserve relationship kinds; design-stage classification remains in metadata and does not itself enforce endpoint stage matching.

No old library file is overwritten. Models that imported the previous translation should replace it with this version and update association imports to `ArrowheadFoundation`. Enum additions preserve old names, but exhaustive consumers should accommodate the new literals.
