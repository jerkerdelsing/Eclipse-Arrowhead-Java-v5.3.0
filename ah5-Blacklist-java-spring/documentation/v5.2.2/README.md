# Blacklist 5.2.2 documentation

Arrowhead LaTeX documentation of the Blacklist support system (implementation `ah5-Blacklist-java-spring` of the primary Java source tree `Eclipse-Arrowhead-java`),
written with the Eclipse Arrowhead templates (`templates/`, from `roadmap/Documentation_templates/LaTeX`).

## System documents

- `Blacklist-v5.2.2-SysD.tex` — System Description (black box).
- `Blacklist-v5.2.2-SysDD.tex` — System Design Description (implementation, deployment).

## Service descriptions and interface design descriptions

| Service | SD | IDD |
|---|---|---|
| `blacklistManagement` | `Blacklist-v5.2.2-blacklistManagement-SD.tex` | `Blacklist-v5.2.2-blacklistManagement-HTTP-IDD.tex`, `Blacklist-v5.2.2-blacklistManagement-MQTT-IDD.tex` |
| `blacklistDiscovery` | `Blacklist-v5.2.2-blacklistDiscovery-SD.tex` | `Blacklist-v5.2.2-blacklistDiscovery-HTTP-IDD.tex`, `Blacklist-v5.2.2-blacklistDiscovery-MQTT-IDD.tex` |
| `generalManagement` | `Blacklist-v5.2.2-generalManagement-SD.tex` | `Blacklist-v5.2.2-generalManagement-HTTP-IDD.tex`, `Blacklist-v5.2.2-generalManagement-MQTT-IDD.tex` |
| `sysMonitor` | `Blacklist-v5.2.2-sysMonitor-SD.tex` | `Blacklist-v5.2.2-sysMonitor-AMQP-IDD.tex` |

PDFs are built beside the sources with `./build-docs.sh` (latexmk, pdfLaTeX and BibTeX). See `SOURCES.md` for
what the content was checked against.
