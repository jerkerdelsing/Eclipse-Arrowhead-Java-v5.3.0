# Choreographer 5.2.2 documentation

Arrowhead LaTeX documentation of the Choreographer support system (implementation `ah5-Choreographer-java-spring` of the primary Java source tree `Eclipse-Arrowhead-java`),
written with the Eclipse Arrowhead templates (`templates/`, from `roadmap/Documentation_templates/LaTeX`).

## System documents

- `Choreographer-v5.2.2-SysD.tex` — System Description (black box).
- `Choreographer-v5.2.2-SysDD.tex` — System Design Description (implementation, deployment).

## Service descriptions and interface design descriptions

| Service | SD | IDD |
|---|---|---|
| `choreographerSession` | `Choreographer-v5.2.2-choreographerSession-SD.tex` | `Choreographer-v5.2.2-choreographerSession-HTTP-IDD.tex` |
| `choreographerExecutor` | `Choreographer-v5.2.2-choreographerExecutor-SD.tex` | `Choreographer-v5.2.2-choreographerExecutor-HTTP-IDD.tex` |
| `choreographerManagement` | `Choreographer-v5.2.2-choreographerManagement-SD.tex` | `Choreographer-v5.2.2-choreographerManagement-HTTP-IDD.tex` |
| `generalManagement` | `Choreographer-v5.2.2-generalManagement-SD.tex` | `Choreographer-v5.2.2-generalManagement-HTTP-IDD.tex` |
| `sysMonitor` | `Choreographer-v5.2.2-sysMonitor-SD.tex` | `Choreographer-v5.2.2-sysMonitor-AMQP-IDD.tex` |

PDFs are built beside the sources with `./build-docs.sh` (latexmk, pdfLaTeX and BibTeX). See `SOURCES.md` for
what the content was checked against.
