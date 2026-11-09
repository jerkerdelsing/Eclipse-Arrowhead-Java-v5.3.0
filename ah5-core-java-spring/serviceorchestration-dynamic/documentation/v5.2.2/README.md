# DynamicServiceOrchestration 5.2.2 documentation

Arrowhead LaTeX documentation of the DynamicServiceOrchestration core system (Java Spring, `ah5-core-java-spring/serviceorchestration-dynamic`),
written with the Eclipse Arrowhead templates (`templates/`, from `roadmap/Documentation_templates/LaTeX`).

## System documents

- `DynamicServiceOrchestration-v5.2.2-SysD.tex` — System Description (black box).
- `DynamicServiceOrchestration-v5.2.2-SysDD.tex` — System Design Description (implementation, deployment).

## Service descriptions and interface design descriptions

| Service | SD | IDD |
|---|---|---|
| `serviceOrchestration` | `DynamicServiceOrchestration-v5.2.2-serviceOrchestration-SD.tex` | `DynamicServiceOrchestration-v5.2.2-serviceOrchestration-HTTP-IDD.tex`, `DynamicServiceOrchestration-v5.2.2-serviceOrchestration-MQTT-IDD.tex` |
| `serviceOrchestrationPushManagement` | `DynamicServiceOrchestration-v5.2.2-serviceOrchestrationPushManagement-SD.tex` | `DynamicServiceOrchestration-v5.2.2-serviceOrchestrationPushManagement-HTTP-IDD.tex`, `DynamicServiceOrchestration-v5.2.2-serviceOrchestrationPushManagement-MQTT-IDD.tex` |
| `serviceOrchestrationLockManagement` | `DynamicServiceOrchestration-v5.2.2-serviceOrchestrationLockManagement-SD.tex` | `DynamicServiceOrchestration-v5.2.2-serviceOrchestrationLockManagement-HTTP-IDD.tex`, `DynamicServiceOrchestration-v5.2.2-serviceOrchestrationLockManagement-MQTT-IDD.tex` |
| `serviceOrchestrationHistoryManagement` | `DynamicServiceOrchestration-v5.2.2-serviceOrchestrationHistoryManagement-SD.tex` | `DynamicServiceOrchestration-v5.2.2-serviceOrchestrationHistoryManagement-HTTP-IDD.tex`, `DynamicServiceOrchestration-v5.2.2-serviceOrchestrationHistoryManagement-MQTT-IDD.tex` |
| `serviceOrchestrationPlantManagement` | `DynamicServiceOrchestration-v5.2.2-serviceOrchestrationPlantManagement-SD.tex` | `DynamicServiceOrchestration-v5.2.2-serviceOrchestrationPlantManagement-HTTP-IDD.tex` |
| `generalManagement` | `DynamicServiceOrchestration-v5.2.2-generalManagement-SD.tex` | `DynamicServiceOrchestration-v5.2.2-generalManagement-HTTP-IDD.tex`, `DynamicServiceOrchestration-v5.2.2-generalManagement-MQTT-IDD.tex` |
| `sysMonitor` | `DynamicServiceOrchestration-v5.2.2-sysMonitor-SD.tex` | `DynamicServiceOrchestration-v5.2.2-sysMonitor-AMQP-IDD.tex` |

PDFs are built beside the sources with `./build-docs.sh` (latexmk, pdfLaTeX and BibTeX). See `SOURCES.md` for
what the content was checked against.
