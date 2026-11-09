# LocalCloudManager 5.2.2 documentation

Arrowhead LaTeX documentation of the LocalCloudManager support system (implementation `ah5-LocalCloudManager-java-spring` of the primary Java source tree `Eclipse-Arrowhead-java`),
written with the Eclipse Arrowhead templates (`templates/`, from `roadmap/Documentation_templates/LaTeX`).

## System documents

- `LocalCloudManager-v5.2.2-SysD.tex` — System Description (black box).
- `LocalCloudManager-v5.2.2-SysDD.tex` — System Design Description (implementation, deployment).

## Service descriptions and interface design descriptions

| Service | SD | IDD |
|---|---|---|
| `localCloudManagement` | `LocalCloudManager-v5.2.2-localCloudManagement-SD.tex` | `LocalCloudManager-v5.2.2-localCloudManagement-HTTP-IDD.tex` |
| `sysMonitor` | `LocalCloudManager-v5.2.2-sysMonitor-SD.tex` | `LocalCloudManager-v5.2.2-sysMonitor-AMQP-IDD.tex` |

PDFs are built beside the sources with `./build-docs.sh` (latexmk, pdfLaTeX and BibTeX). See `SOURCES.md` for
what the content was checked against.
