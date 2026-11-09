# PlantDescriptionEngine 5.2.2 documentation

Arrowhead LaTeX documentation of the PlantDescriptionEngine support system (implementation `ah5-PlantDescriptionEngine-java-spring` of the primary Java source tree `Eclipse-Arrowhead-java`),
written with the Eclipse Arrowhead templates (`templates/`, from `roadmap/Documentation_templates/LaTeX`).

## System documents

- `PlantDescriptionEngine-v5.2.2-SysD.tex` — System Description (black box).
- `PlantDescriptionEngine-v5.2.2-SysDD.tex` — System Design Description (implementation, deployment).

## Service descriptions and interface design descriptions

| Service | SD | IDD |
|---|---|---|
| `plantDescriptionManagement` | `PlantDescriptionEngine-v5.2.2-plantDescriptionManagement-SD.tex` | `PlantDescriptionEngine-v5.2.2-plantDescriptionManagement-HTTP-IDD.tex` |
| `plantDescriptionMonitor` | `PlantDescriptionEngine-v5.2.2-plantDescriptionMonitor-SD.tex` | `PlantDescriptionEngine-v5.2.2-plantDescriptionMonitor-HTTP-IDD.tex` |
| `generalManagement` | `PlantDescriptionEngine-v5.2.2-generalManagement-SD.tex` | `PlantDescriptionEngine-v5.2.2-generalManagement-HTTP-IDD.tex` |
| `sysMonitor` | `PlantDescriptionEngine-v5.2.2-sysMonitor-SD.tex` | `PlantDescriptionEngine-v5.2.2-sysMonitor-AMQP-IDD.tex` |

PDFs are built beside the sources with `./build-docs.sh` (latexmk, pdfLaTeX and BibTeX). See `SOURCES.md` for
what the content was checked against.
