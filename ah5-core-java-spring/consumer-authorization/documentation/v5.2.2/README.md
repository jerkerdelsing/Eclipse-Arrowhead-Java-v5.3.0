# ConsumerAuthorization 5.2.2 documentation

Arrowhead LaTeX documentation of the ConsumerAuthorization core system (Java Spring, `ah5-core-java-spring/consumer-authorization`),
written with the Eclipse Arrowhead templates (`templates/`, from `roadmap/Documentation_templates/LaTeX`).

## System documents

- `ConsumerAuthorization-v5.2.2-SysD.tex` — System Description (black box).
- `ConsumerAuthorization-v5.2.2-SysDD.tex` — System Design Description (implementation, deployment).

## Service descriptions and interface design descriptions

| Service | SD | IDD |
|---|---|---|
| `authorization` | `ConsumerAuthorization-v5.2.2-authorization-SD.tex` | `ConsumerAuthorization-v5.2.2-authorization-HTTP-IDD.tex`, `ConsumerAuthorization-v5.2.2-authorization-MQTT-IDD.tex` |
| `authorizationToken` | `ConsumerAuthorization-v5.2.2-authorizationToken-SD.tex` | `ConsumerAuthorization-v5.2.2-authorizationToken-HTTP-IDD.tex`, `ConsumerAuthorization-v5.2.2-authorizationToken-MQTT-IDD.tex` |
| `authorizationManagement` | `ConsumerAuthorization-v5.2.2-authorizationManagement-SD.tex` | `ConsumerAuthorization-v5.2.2-authorizationManagement-HTTP-IDD.tex`, `ConsumerAuthorization-v5.2.2-authorizationManagement-MQTT-IDD.tex` |
| `authorizationTokenManagement` | `ConsumerAuthorization-v5.2.2-authorizationTokenManagement-SD.tex` | `ConsumerAuthorization-v5.2.2-authorizationTokenManagement-HTTP-IDD.tex`, `ConsumerAuthorization-v5.2.2-authorizationTokenManagement-MQTT-IDD.tex` |
| `generalManagement` | `ConsumerAuthorization-v5.2.2-generalManagement-SD.tex` | `ConsumerAuthorization-v5.2.2-generalManagement-HTTP-IDD.tex`, `ConsumerAuthorization-v5.2.2-generalManagement-MQTT-IDD.tex` |
| `sysMonitor` | `ConsumerAuthorization-v5.2.2-sysMonitor-SD.tex` | `ConsumerAuthorization-v5.2.2-sysMonitor-AMQP-IDD.tex` |

PDFs are built beside the sources with `./build-docs.sh` (latexmk, pdfLaTeX and BibTeX). See `SOURCES.md` for
what the content was checked against.
