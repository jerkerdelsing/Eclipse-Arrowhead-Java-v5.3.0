# DataManager 5.2.2 documentation

Arrowhead LaTeX documentation of the DataManager support system (implementation `ah5-DataManager-java-spring` of the primary Java source tree `Eclipse-Arrowhead-java`),
written with the Eclipse Arrowhead templates (`templates/`, from `roadmap/Documentation_templates/LaTeX`).

## System documents

- `DataManager-v5.2.2-SysD.tex` — System Description (black box).
- `DataManager-v5.2.2-SysDD.tex` — System Design Description (implementation, deployment).

## Service descriptions and interface design descriptions

| Service | SD | IDD |
|---|---|---|
| `dataProxy` | `DataManager-v5.2.2-dataProxy-SD.tex` | `DataManager-v5.2.2-dataProxy-HTTP-IDD.tex` |
| `dataHistorian` | `DataManager-v5.2.2-dataHistorian-SD.tex` | `DataManager-v5.2.2-dataHistorian-HTTP-IDD.tex` |
| `sysMonitor` | `DataManager-v5.2.2-sysMonitor-SD.tex` | `DataManager-v5.2.2-sysMonitor-AMQP-IDD.tex` |

PDFs are built beside the sources with `./build-docs.sh` (latexmk, pdfLaTeX and BibTeX). See `SOURCES.md` for
what the content was checked against.
