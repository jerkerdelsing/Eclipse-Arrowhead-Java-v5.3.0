# LocalCloudConfigurator 5.2.2 documentation

Arrowhead LaTeX documentation of the LocalCloudConfigurator support system (implementation `ah5-LocalCloudConfigurator-java`
of the primary Java source tree `Eclipse-Arrowhead-java`), written with the Eclipse Arrowhead templates (`templates/`).

## System documents

- `LocalCloudConfigurator-v5.2.2-SysD.tex` — System Description (black box).
- `LocalCloudConfigurator-v5.2.2-SysDD.tex` — System Design Description (implementation, host worker, database, deployment).

## Service descriptions and interface design descriptions

| Service | SD | IDD |
|---|---|---|
| `localCloudConfiguration` | `LocalCloudConfigurator-v5.2.2-localCloudConfiguration-SD.tex` | `LocalCloudConfigurator-v5.2.2-localCloudConfiguration-HTTP-IDD.tex` |
| `sysMonitor` | `LocalCloudConfigurator-v5.2.2-sysMonitor-SD.tex` | `LocalCloudConfigurator-v5.2.2-sysMonitor-AMQP-IDD.tex` |

PDFs are built beside the sources with `./build-docs.sh` (latexmk, pdfLaTeX and BibTeX). See `SOURCES.md`.
