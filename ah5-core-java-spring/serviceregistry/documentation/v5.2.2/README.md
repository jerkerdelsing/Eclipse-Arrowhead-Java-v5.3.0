# ServiceRegistry 5.2.2 documentation

Arrowhead LaTeX documentation of the ServiceRegistry core system (Java Spring, `ah5-core-java-spring/serviceregistry`),
written with the Eclipse Arrowhead templates (`templates/`, from `roadmap/Documentation_templates/LaTeX`).

## System documents

- `ServiceRegistry-v5.2.2-SysD.tex` — System Description (black box).
- `ServiceRegistry-v5.2.2-SysDD.tex` — System Design Description (implementation, deployment).

## Service descriptions and interface design descriptions

| Service | SD | IDD |
|---|---|---|
| `deviceDiscovery` | `ServiceRegistry-v5.2.2-deviceDiscovery-SD.tex` | `ServiceRegistry-v5.2.2-deviceDiscovery-HTTP-IDD.tex`, `ServiceRegistry-v5.2.2-deviceDiscovery-MQTT-IDD.tex` |
| `systemDiscovery` | `ServiceRegistry-v5.2.2-systemDiscovery-SD.tex` | `ServiceRegistry-v5.2.2-systemDiscovery-HTTP-IDD.tex`, `ServiceRegistry-v5.2.2-systemDiscovery-MQTT-IDD.tex` |
| `serviceDiscovery` | `ServiceRegistry-v5.2.2-serviceDiscovery-SD.tex` | `ServiceRegistry-v5.2.2-serviceDiscovery-HTTP-IDD.tex`, `ServiceRegistry-v5.2.2-serviceDiscovery-MQTT-IDD.tex` |
| `serviceRegistryManagement` | `ServiceRegistry-v5.2.2-serviceRegistryManagement-SD.tex` | `ServiceRegistry-v5.2.2-serviceRegistryManagement-HTTP-IDD.tex`, `ServiceRegistry-v5.2.2-serviceRegistryManagement-MQTT-IDD.tex` |
| `generalManagement` | `ServiceRegistry-v5.2.2-generalManagement-SD.tex` | `ServiceRegistry-v5.2.2-generalManagement-HTTP-IDD.tex`, `ServiceRegistry-v5.2.2-generalManagement-MQTT-IDD.tex` |
| `sysMonitor` | `ServiceRegistry-v5.2.2-sysMonitor-SD.tex` | `ServiceRegistry-v5.2.2-sysMonitor-AMQP-IDD.tex` |

PDFs are built beside the sources with `./build-docs.sh` (latexmk, pdfLaTeX and BibTeX). See `SOURCES.md` for
what the content was checked against.
