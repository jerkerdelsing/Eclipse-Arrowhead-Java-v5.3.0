# EventHandler 5.2.2 documentation

Arrowhead LaTeX documentation of the EventHandler support system (implementation `ah5-EventHandler-java-spring` of the primary Java source tree `Eclipse-Arrowhead-java`),
written with the Eclipse Arrowhead templates (`templates/`, from `roadmap/Documentation_templates/LaTeX`).

## System documents

- `EventHandler-v5.2.2-SysD.tex` — System Description (black box).
- `EventHandler-v5.2.2-SysDD.tex` — System Design Description (implementation, deployment).

## Service descriptions and interface design descriptions

| Service | SD | IDD |
|---|---|---|
| `eventPublish` | `EventHandler-v5.2.2-eventPublish-SD.tex` | `EventHandler-v5.2.2-eventPublish-HTTP-IDD.tex` |
| `eventSubscription` | `EventHandler-v5.2.2-eventSubscription-SD.tex` | `EventHandler-v5.2.2-eventSubscription-HTTP-IDD.tex` |
| `eventHandlerManagement` | `EventHandler-v5.2.2-eventHandlerManagement-SD.tex` | `EventHandler-v5.2.2-eventHandlerManagement-HTTP-IDD.tex` |
| `generalManagement` | `EventHandler-v5.2.2-generalManagement-SD.tex` | `EventHandler-v5.2.2-generalManagement-HTTP-IDD.tex` |
| `sysMonitor` | `EventHandler-v5.2.2-sysMonitor-SD.tex` | `EventHandler-v5.2.2-sysMonitor-AMQP-IDD.tex` |

PDFs are built beside the sources with `./build-docs.sh` (latexmk, pdfLaTeX and BibTeX). See `SOURCES.md` for
what the content was checked against.
