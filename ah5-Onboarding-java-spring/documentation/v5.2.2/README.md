# Onboarding 5.2.2 documentation

Arrowhead LaTeX documentation of the Onboarding support system (implementation `ah5-Onboarding-java-spring` of the primary Java source tree `Eclipse-Arrowhead-java`),
written with the Eclipse Arrowhead templates (`templates/`, from `roadmap/Documentation_templates/LaTeX`).

## System documents

- `Onboarding-v5.2.2-SysD.tex` — System Description (black box).
- `Onboarding-v5.2.2-SysDD.tex` — System Design Description (implementation, deployment).

## Service descriptions and interface design descriptions

| Service | SD | IDD |
|---|---|---|
| `onboarding` | `Onboarding-v5.2.2-onboarding-SD.tex` | `Onboarding-v5.2.2-onboarding-HTTP-IDD.tex` |
| `onboardingManagement` | `Onboarding-v5.2.2-onboardingManagement-SD.tex` | `Onboarding-v5.2.2-onboardingManagement-HTTP-IDD.tex` |
| `generalManagement` | `Onboarding-v5.2.2-generalManagement-SD.tex` | `Onboarding-v5.2.2-generalManagement-HTTP-IDD.tex` |
| `sysMonitor` | `Onboarding-v5.2.2-sysMonitor-SD.tex` | `Onboarding-v5.2.2-sysMonitor-AMQP-IDD.tex` |

PDFs are built beside the sources with `./build-docs.sh` (latexmk, pdfLaTeX and BibTeX). See `SOURCES.md` for
what the content was checked against.
