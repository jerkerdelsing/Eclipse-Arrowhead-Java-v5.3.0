# TranslationManager 5.2.2 documentation

Arrowhead LaTeX documentation of the TranslationManager support system (implementation `ah5-TranslationManager-java-spring` of the primary Java source tree `Eclipse-Arrowhead-java`),
written with the Eclipse Arrowhead templates (`templates/`, from `roadmap/Documentation_templates/LaTeX`).

## System documents

- `TranslationManager-v5.2.2-SysD.tex` — System Description (black box).
- `TranslationManager-v5.2.2-SysDD.tex` — System Design Description (implementation, deployment).

## Service descriptions and interface design descriptions

| Service | SD | IDD |
|---|---|---|
| `translationReport` | `TranslationManager-v5.2.2-translationReport-SD.tex` | `TranslationManager-v5.2.2-translationReport-HTTP-IDD.tex`, `TranslationManager-v5.2.2-translationReport-MQTT-IDD.tex` |
| `translationBridge` | `TranslationManager-v5.2.2-translationBridge-SD.tex` | `TranslationManager-v5.2.2-translationBridge-HTTP-IDD.tex`, `TranslationManager-v5.2.2-translationBridge-MQTT-IDD.tex` |
| `translationBridgeManagement` | `TranslationManager-v5.2.2-translationBridgeManagement-SD.tex` | `TranslationManager-v5.2.2-translationBridgeManagement-HTTP-IDD.tex`, `TranslationManager-v5.2.2-translationBridgeManagement-MQTT-IDD.tex` |
| `generalManagement` | `TranslationManager-v5.2.2-generalManagement-SD.tex` | `TranslationManager-v5.2.2-generalManagement-HTTP-IDD.tex`, `TranslationManager-v5.2.2-generalManagement-MQTT-IDD.tex` |
| `sysMonitor` | `TranslationManager-v5.2.2-sysMonitor-SD.tex` | `TranslationManager-v5.2.2-sysMonitor-AMQP-IDD.tex` |

PDFs are built beside the sources with `./build-docs.sh` (latexmk, pdfLaTeX and BibTeX). See `SOURCES.md` for
what the content was checked against.
