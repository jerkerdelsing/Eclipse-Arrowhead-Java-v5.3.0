# Network architecture and deployment update

Updated 10 November 2026. LaTeX remains the maintained source; the corresponding PDFs were rebuilt and checked.

## Updated current documents

| Document | New section | PDF pages | Added section pages |
|---|---|---:|---|
| [GSoSD 5.3.0](BlackBox_5.3.0/GSoSD-Core-and-Support-Systems-5.3.0.pdf) | Network Architecture | 42 | 9–11 |
| [Core GSoSDD 5.3.0](WhiteBox_5.3.0/GSoSDD_Arrowhead_Core_Systems_5.3.pdf) | Network Deployment | 39 | 12–14 |
| [Support GSoSDD 5.3.1](WhiteBox_5.3.0/GSoSDD_Arrowhead_Support_Systems_5.3.1.pdf) | Network Deployment | 38 | 14–16 |

The support 5.3.1 source is the current edited support document. Older sources/PDFs, including the support 5.3 filename whose document header remains 5.2.2, were left unchanged. Document version identifiers are retained; dates and amendment records identify this update.

## Model basis and traceability

The requested Papyrus project is used directly: [Arrowhead-5.2.uml](../../../profile-library-sysml/distribution/Papyrus/ArrowheadDSL_2025_v5/EclipseArrowhead-5.0-JD/Arrowhead-5.2.uml) and [Arrowhead-5.2.notation](../../../profile-library-sysml/distribution/Papyrus/ArrowheadDSL_2025_v5/EclipseArrowhead-5.0-JD/Arrowhead-5.2.notation). The Network diagram and UML model are authoritative for this update, rather than the numbered textual SysML exports. The generic Device and local-cloud templates were also inspected.

The model defines generic intra-cloud, inter-cloud and DMZ full-port contracts and their stereotyped design-description specialisations; typed ports subset the generic interfaces. Ethernet and WiFi are bearer examples; ProtocolKind is a separate application-protocol catalogue. The exact source spellings GenericIntracloudNetworkDMX and IntercloudNetworkDMZ_802.3 are documented rather than silently renamed. The GSoSD contains a classifier/XMI traceability table.

The source does not provide an as-deployed address plan, VLANs, firewalls, network equipment, host allocations or availability guarantees. The new diagrams are explicitly labelled model-derived interpretations. Concrete placement, routing, address/listening-versus-advertised port mapping, access rules and commissioning checks are deployment refinements, not facts extracted from a running cloud. The HTTP defaults in the model do not override the existing Arrowhead protected-communication baseline. The model's protocol catalogue does not establish support in every implementation or remove the existing AMQP telemetry contract.

## Maintained LaTeX assets

- [Architecture section](BlackBox_5.3.0/sections/network-architecture.tex) and [architecture figure](BlackBox_5.3.0/figures/network-architecture-model.tex).
- [Shared deployment section](WhiteBox_5.3.0/sections/network-deployment.tex) and [deployment figure](WhiteBox_5.3.0/figures/network-deployment-model.tex). Core/support-specific introductory obligations are selected by the Core document's local macro.
- The bibliography used by each edited document includes the local Papyrus model reference. Existing missing local prior-art references in the Core bibliography were resolved to the retained foundational-principles and standards drafts.
- Small existing compilation repairs: the GSoSD's references to a commented-out interoperability section now cite the Core/Support GSoSDDs; the support abstract's reference to a commented-out baseline points to its existing independent-support section. Two long database identifiers use breakable formatting. No existing technical implementation claims were rewritten as part of those reference/layout repairs.

## Verification

All three PDFs were built with latexmk/pdfLaTeX and BibTeX. The final logs have no unresolved references/citations, multiply defined labels, missing characters or overfull boxes. Every new section page was rendered for inspection, and the two network figures were checked at larger scale. Model identifiers used in the GSoSD trace table were matched against the UML file before authoring.

No model, source implementation, network configuration, running cloud or historical document was modified. This is documentation work; the commissioning checks are requirements for a future deployment assessment, not results of network tests performed here. Source/model hashes and PDF validation details are in the adjacent JSON record.
