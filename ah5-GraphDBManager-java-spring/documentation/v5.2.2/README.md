# GraphDBManager 5.2.2 documentation

Arrowhead LaTeX documentation of the GraphDBManager support system (implementation `ah5-GraphDBManager-java-spring`,
a copy of DataManager whose historian is kept in Ontotext GraphDB), written with the Eclipse Arrowhead templates
(`templates/`, from `roadmap/Documentation_templates/LaTeX`).

## System documents

- `GraphDBManager-v5.2.2-SysD.tex` — System Description (black box).
- `GraphDBManager-v5.2.2-SysDD.tex` — System Design Description (implementation, RDF model, deployment).

## Service descriptions and interface design descriptions

| Service | SD | IDD |
|---|---|---|
| `graphDataProxy` | `GraphDBManager-v5.2.2-graphDataProxy-SD.tex` | `GraphDBManager-v5.2.2-graphDataProxy-HTTP-IDD.tex` |
| `graphDataHistorian` | `GraphDBManager-v5.2.2-graphDataHistorian-SD.tex` | `GraphDBManager-v5.2.2-graphDataHistorian-HTTP-IDD.tex` |
| `sysMonitor` | `GraphDBManager-v5.2.2-sysMonitor-SD.tex` | `GraphDBManager-v5.2.2-sysMonitor-AMQP-IDD.tex` |

PDFs are built beside the sources with `./build-docs.sh` (latexmk, pdfLaTeX and BibTeX). See `SOURCES.md` for
where the content comes from.
