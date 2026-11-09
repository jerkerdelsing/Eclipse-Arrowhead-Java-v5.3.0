# DeviceQoSEvaluator 5.2.2 documentation

Arrowhead LaTeX documentation of the DeviceQoSEvaluator support system (implementation `ah5-DeviceQoSEvaluator-java-spring` of the primary Java source tree `Eclipse-Arrowhead-java`),
written with the Eclipse Arrowhead templates (`templates/`, from `roadmap/Documentation_templates/LaTeX`).

## System documents

- `DeviceQoSEvaluator-v5.2.2-SysD.tex` — System Description (black box).
- `DeviceQoSEvaluator-v5.2.2-SysDD.tex` — System Design Description (implementation, deployment).

## Service descriptions and interface design descriptions

| Service | SD | IDD |
|---|---|---|
| `qualityEvaluation` | `DeviceQoSEvaluator-v5.2.2-qualityEvaluation-SD.tex` | `DeviceQoSEvaluator-v5.2.2-qualityEvaluation-HTTP-IDD.tex`, `DeviceQoSEvaluator-v5.2.2-qualityEvaluation-MQTT-IDD.tex` |
| `deviceQualityDataManagement` | `DeviceQoSEvaluator-v5.2.2-deviceQualityDataManagement-SD.tex` | `DeviceQoSEvaluator-v5.2.2-deviceQualityDataManagement-HTTP-IDD.tex`, `DeviceQoSEvaluator-v5.2.2-deviceQualityDataManagement-MQTT-IDD.tex` |
| `generalManagement` | `DeviceQoSEvaluator-v5.2.2-generalManagement-SD.tex` | `DeviceQoSEvaluator-v5.2.2-generalManagement-HTTP-IDD.tex`, `DeviceQoSEvaluator-v5.2.2-generalManagement-MQTT-IDD.tex` |
| `sysMonitor` | `DeviceQoSEvaluator-v5.2.2-sysMonitor-SD.tex` | `DeviceQoSEvaluator-v5.2.2-sysMonitor-AMQP-IDD.tex` |

PDFs are built beside the sources with `./build-docs.sh` (latexmk, pdfLaTeX and BibTeX). See `SOURCES.md` for
what the content was checked against.
