# Configuration 5.2.2 documentation

Arrowhead LaTeX documentation of the Configuration support system (implementation `ah5-Configuration-java-spring` of the primary Java source tree `Eclipse-Arrowhead-java`),
written with the Eclipse Arrowhead templates (`templates/`, from `roadmap/Documentation_templates/LaTeX`).

## System documents

- `Configuration-v5.2.2-SysD.tex` — System Description (black box).
- `Configuration-v5.2.2-SysDD.tex` — System Design Description (implementation, deployment).

## Service descriptions and interface design descriptions

| Service | SD | IDD |
|---|---|---|
| `configuration` | `Configuration-v5.2.2-configuration-SD.tex` | `Configuration-v5.2.2-configuration-HTTP-IDD.tex` |
| `configurationProxy` | `Configuration-v5.2.2-configurationProxy-SD.tex` | `Configuration-v5.2.2-configurationProxy-HTTP-IDD.tex` |
| `configurationManagement` | `Configuration-v5.2.2-configurationManagement-SD.tex` | `Configuration-v5.2.2-configurationManagement-HTTP-IDD.tex` |
| `generalManagement` | `Configuration-v5.2.2-generalManagement-SD.tex` | `Configuration-v5.2.2-generalManagement-HTTP-IDD.tex` |
| `sysMonitor` | `Configuration-v5.2.2-sysMonitor-SD.tex` | `Configuration-v5.2.2-sysMonitor-AMQP-IDD.tex` |

PDFs are built beside the sources with `./build-docs.sh` (latexmk, pdfLaTeX and BibTeX). See `SOURCES.md` for
what the content was checked against.
