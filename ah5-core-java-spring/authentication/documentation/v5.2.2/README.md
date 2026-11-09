# Authentication 5.2.2 documentation

Arrowhead LaTeX documentation of the Authentication core system (Java Spring, `ah5-core-java-spring/authentication`),
written with the Eclipse Arrowhead templates (`templates/`, from `roadmap/Documentation_templates/LaTeX`).

## System documents

- `Authentication-v5.2.2-SysD.tex` — System Description (black box).
- `Authentication-v5.2.2-SysDD.tex` — System Design Description (implementation, deployment).

## Service descriptions and interface design descriptions

| Service | SD | IDD |
|---|---|---|
| `identity` | `Authentication-v5.2.2-identity-SD.tex` | `Authentication-v5.2.2-identity-HTTP-IDD.tex`, `Authentication-v5.2.2-identity-MQTT-IDD.tex` |
| `identityManagement` | `Authentication-v5.2.2-identityManagement-SD.tex` | `Authentication-v5.2.2-identityManagement-HTTP-IDD.tex`, `Authentication-v5.2.2-identityManagement-MQTT-IDD.tex` |
| `generalManagement` | `Authentication-v5.2.2-generalManagement-SD.tex` | `Authentication-v5.2.2-generalManagement-HTTP-IDD.tex`, `Authentication-v5.2.2-generalManagement-MQTT-IDD.tex` |
| `sysMonitor` | `Authentication-v5.2.2-sysMonitor-SD.tex` | `Authentication-v5.2.2-sysMonitor-AMQP-IDD.tex` |

PDFs are built beside the sources with `./build-docs.sh` (latexmk, pdfLaTeX and BibTeX). See `SOURCES.md` for
what the content was checked against.
