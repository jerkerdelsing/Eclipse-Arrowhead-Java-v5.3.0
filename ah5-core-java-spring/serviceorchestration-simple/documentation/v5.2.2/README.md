# SimpleStoreServiceOrchestration 5.2.2 documentation

Arrowhead LaTeX documentation of the SimpleStoreServiceOrchestration core system (Java Spring, `ah5-core-java-spring/serviceorchestration-simple`),
written with the Eclipse Arrowhead templates (`templates/`, from `roadmap/Documentation_templates/LaTeX`).

## System documents

- `SimpleStoreServiceOrchestration-v5.2.2-SysD.tex` — System Description (black box).
- `SimpleStoreServiceOrchestration-v5.2.2-SysDD.tex` — System Design Description (implementation, deployment).

## Service descriptions and interface design descriptions

| Service | SD | IDD |
|---|---|---|
| `serviceOrchestration` | `SimpleStoreServiceOrchestration-v5.2.2-serviceOrchestration-SD.tex` | `SimpleStoreServiceOrchestration-v5.2.2-serviceOrchestration-HTTP-IDD.tex`, `SimpleStoreServiceOrchestration-v5.2.2-serviceOrchestration-MQTT-IDD.tex` |
| `serviceOrchestrationSimpleStoreManagement` | `SimpleStoreServiceOrchestration-v5.2.2-serviceOrchestrationSimpleStoreManagement-SD.tex` | `SimpleStoreServiceOrchestration-v5.2.2-serviceOrchestrationSimpleStoreManagement-HTTP-IDD.tex`, `SimpleStoreServiceOrchestration-v5.2.2-serviceOrchestrationSimpleStoreManagement-MQTT-IDD.tex` |
| `serviceOrchestrationPushManagement` | `SimpleStoreServiceOrchestration-v5.2.2-serviceOrchestrationPushManagement-SD.tex` | `SimpleStoreServiceOrchestration-v5.2.2-serviceOrchestrationPushManagement-HTTP-IDD.tex`, `SimpleStoreServiceOrchestration-v5.2.2-serviceOrchestrationPushManagement-MQTT-IDD.tex` |
| `serviceOrchestrationHistoryManagement` | `SimpleStoreServiceOrchestration-v5.2.2-serviceOrchestrationHistoryManagement-SD.tex` | `SimpleStoreServiceOrchestration-v5.2.2-serviceOrchestrationHistoryManagement-HTTP-IDD.tex`, `SimpleStoreServiceOrchestration-v5.2.2-serviceOrchestrationHistoryManagement-MQTT-IDD.tex` |
| `generalManagement` | `SimpleStoreServiceOrchestration-v5.2.2-generalManagement-SD.tex` | `SimpleStoreServiceOrchestration-v5.2.2-generalManagement-HTTP-IDD.tex`, `SimpleStoreServiceOrchestration-v5.2.2-generalManagement-MQTT-IDD.tex` |
| `sysMonitor` | `SimpleStoreServiceOrchestration-v5.2.2-sysMonitor-SD.tex` | `SimpleStoreServiceOrchestration-v5.2.2-sysMonitor-AMQP-IDD.tex` |

PDFs are built beside the sources with `./build-docs.sh` (latexmk, pdfLaTeX and BibTeX). See `SOURCES.md` for
what the content was checked against.
