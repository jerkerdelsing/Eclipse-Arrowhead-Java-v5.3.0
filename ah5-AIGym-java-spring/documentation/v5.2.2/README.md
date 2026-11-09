# AIGym 5.2.2 documentation

Arrowhead LaTeX documentation of the AIGym support system (implementation `ah5-AIGym-java-spring` of the primary Java source tree `Eclipse-Arrowhead-java`),
written with the Eclipse Arrowhead templates (`templates/`, from `roadmap/Documentation_templates/LaTeX`).

## System documents

- `AIGym-v5.2.2-SysD.tex` — System Description (black box).
- `AIGym-v5.2.2-SysDD.tex` — System Design Description (implementation, deployment).

## Service descriptions and interface design descriptions

| Service | SD | IDD |
|---|---|---|
| `aiGym` | `AIGym-v5.2.2-aiGym-SD.tex` | `AIGym-v5.2.2-aiGym-HTTP-IDD.tex` |
| `sysMonitor` | `AIGym-v5.2.2-sysMonitor-SD.tex` | `AIGym-v5.2.2-sysMonitor-AMQP-IDD.tex` |

PDFs are built beside the sources with `./build-docs.sh` (latexmk, pdfLaTeX and BibTeX). See `SOURCES.md` for
what the content was checked against.
