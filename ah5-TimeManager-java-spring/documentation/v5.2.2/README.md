# TimeManager 5.2.2 documentation

Arrowhead LaTeX documentation of the TimeManager support system (implementation `ah5-TimeManager-java-spring` of the primary Java source tree `Eclipse-Arrowhead-java`),
written with the Eclipse Arrowhead templates (`templates/`, from `roadmap/Documentation_templates/LaTeX`).

## System documents

- `TimeManager-v5.2.2-SysD.tex` — System Description (black box).
- `TimeManager-v5.2.2-SysDD.tex` — System Design Description (implementation, deployment).

## Service descriptions and interface design descriptions

| Service | SD | IDD |
|---|---|---|
| `time` | `TimeManager-v5.2.2-time-SD.tex` | `TimeManager-v5.2.2-time-HTTP-IDD.tex` |
| `generalManagement` | `TimeManager-v5.2.2-generalManagement-SD.tex` | `TimeManager-v5.2.2-generalManagement-HTTP-IDD.tex` |
| `sysMonitor` | `TimeManager-v5.2.2-sysMonitor-SD.tex` | `TimeManager-v5.2.2-sysMonitor-AMQP-IDD.tex` |

PDFs are built beside the sources with `./build-docs.sh` (latexmk, pdfLaTeX and BibTeX). See `SOURCES.md` for
what the content was checked against.
