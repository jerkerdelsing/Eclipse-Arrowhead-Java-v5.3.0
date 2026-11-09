# Documentation generator

Generates the Arrowhead-template LaTeX documentation (SysD, SysDD, SD, HTTP/MQTT/AMQP IDD) of the five core systems
into `<module>/documentation/v5.2.2/` and builds the PDFs.

## Regenerate

From the repository root:

```sh
documentation-tools/regenerate.sh
```

`--no-pdf` writes only the `.tex` files. `DOC_DATE=YYYY-MM-DD` sets the document date and revision-history entry
(default 2026-10-22). Requirements: Python 3, a TeX distribution with `latexmk` and BibTeX, and these sibling checkouts
next to `ah5-core-java-spring`:

- `ah5-common-java-spring` (DTO records, enums, constants)
- `roadmap/Documentation_templates/LaTeX` (Arrowhead templates, `arrowhead.cls`, logo, `IEEEtran.bst`)

`check-docs.sh <ah5-core-java-spring>` compiles all documents in parallel and lists failures, undefined references
and overfull boxes; it leaves the auxiliary files in place (`build-docs.sh` removes them).

## How it works

1. `extract.py` reads the Java sources and writes `model.json`: the registered services and operations
   (`*SystemInfo.java`), HTTP methods, paths, parameters, bodies and status codes (`api/http/*API.java`), MQTT topics and
   payloads (`api/mqtt/*Handler.java`), and the DTO records and enums of `ah5-common-java-spring`.
2. `generate.py` combines the model with the hand-written text and writes the documents and the package files
   (class, logo, bibliography, templates, README, SOURCES, `build-docs.sh`).

Operations, paths, parameters and data-model fields therefore follow the code. The prose does not:

| File | Content |
|---|---|
| `content_fields.py` | Field and data-type descriptions, JSON example values |
| `content_services.py` | Service purpose, prior art, usage, delimitations; operation descriptions |
| `content_systems.py` | SysD/SysDD text per system: properties, configuration, data, consumed services, components, tables, local modifications, test counts |

When the code changes, regenerate and check the result: a new operation or field appears automatically but has an empty
description until it is added to these files. Update the configuration, local-modification and test entries in
`content_systems.py` by hand. Edit the generator, not the generated `.tex` files, which are overwritten.

To regenerate from the primary tree, `roadmap/Documentation_templates/LaTeX` must also be next to `ah5-core-java-spring`. A copy of the workspace `roadmap/Documentation_templates` folder is enough; `ah5-common-java-spring` is already in the tree.
