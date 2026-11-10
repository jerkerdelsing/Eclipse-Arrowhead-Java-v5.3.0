> **Superseded as current documentation — 10 November 2026.** See [Documentation-V5.3.0.md](Documentation-V5.3.0.md) for the consolidated overview, retained shared follow-up and links to the maintained per-microsystem LaTeX/PDF editions. The original text below is retained as historical evidence; its dated implementation, availability and release statements are not current guidance.

# Java core and support system documentation — update of 30 October 2026

Scope: the Arrowhead 5.2.2 System Description (SysD), System Design Description (SysDD), Service Descriptions (SD) and
Interface Design Descriptions (IDD) of the Java systems in this primary source tree, in each system's
`documentation/v5.2.2/` folder. Only documentation was changed: no source code, configuration or deployment was
modified or redeployed. The Arrowhead 5.2.2 template structure, macros, class and bibliography are unchanged.

## Method

1. Systems were listed from this tree (`JAVA-SUPPORT-FOLDERS.md` and the `ah5-*` folders), after reading
   `../PRIMARY-SOURCES.md`.
2. Services, operations, HTTP paths, MQTT topics and data records were re-extracted from the primary source with a
   scratch copy of the historical extractor (`Arrowhead/ah5-support-java-spring/documentation-tools/extract.py`, paths
   pointed at the `ah5-<System>-java-spring` folders and their private `vendor/` common library). The result was
   compared with the `model.json` from which the 22–23 October documents were generated.
3. Configuration, security and deployment statements were compared with `src/main/resources`, `config/`,
   `compose.yaml`, `.env.example`, `Dockerfile` and `DEPLOYMENT.md` of each folder, and with the common-library
   filters (authentication policies, management filter, Blacklist filter, token/certificate binding, CRL).
4. Expected SD/IDD files were derived from the registered services and interfaces and compared with the files present.
5. All documents were rebuilt with each folder's `build-docs.sh` (latexmk, pdfLaTeX, BibTeX).

## Findings that apply to all Spring-based support systems

- The service contracts are unchanged: same services, versions, metadata, operations, methods, paths, MQTT topics
  and records as the generated documents. No SD or IDD is missing (the `sysMonitor` SD/AMQP IDD pairs document the
  native publisher, which is not registered as a service).
- SysD/SD/IDD texts still pointed at the historical combined tree (`ah5-support-java-spring/<module>`) and at the
  `Arrowhead-test-cloud-java` deployment. They now name the primary folder and the folder's standalone deployment.
- The configuration tables list source defaults, which match the source. The standalone `config/application.properties`
  differs from them, and each Spring SysD now has a paragraph on it: `outsourced` authentication,
  `CERT_AUTH` interfaces, `management.policy=sysop-only`, `service.authorization.enabled=true`,
  `mqtt.api.enabled=false`, CRL `/crl/ca.crl`, and secrets from Compose file secrets via
  `spring.config.import=configtree:/run/secrets/`.
- The standalone deployments do not set `enable.blacklist.filter`, so the source default `false` applies: incoming
  requests are not checked against the Blacklist there. Blacklisted providers are still removed by
  DynamicServiceOrchestration. This is now stated in each Spring SysD.
- MQTT IDDs now say that the standalone deployment does not register the MQTT interfaces. They no longer refer to the
  test cloud.
- Every document has the date 2026-10-30 and a new amendment row. The row says whether statements changed or the
  contract was checked and found unchanged.
- `README.md`/`SOURCES.md` of each documentation folder now name the primary folder. They also say the `.tex` files
  are maintained directly against this tree and that the combined-tree generator is now only a reference.

## Per system

| System | Documents | What changed | Unresolved |
|---|---:|---|---|
| AIGym | 6 | SysD: abstract; configuration table rewritten to the Spring properties of `application.yml` with their variables (adds authentication URL, orchestration TTL, AMQP exchange, status interval, CRL); the standalone `config/` overrides passwords from secret files. Other documents re-dated. | The bundled `src/main/resources/application.yml` still has development fallback passwords (not used by the standalone configuration). |
| Blacklist | 13 | SysD: abstract, standalone deployment paragraph, security wording; MQTT IDDs: deployment note; SysDD: deployment wording. | — |
| CertificateAuthority | 10 | Already rewritten on 2026-10-25 for the independent port; contract and all 21 environment variables re-checked; SysD/SysDD wording on test-cloud deployments. | — |
| Choreographer | 12 | As Blacklist; HTTP/MQTT IDDs: registered port follows `server.port`, so the standalone deployment registers 8454 (source default 8457); `choreographerManagement` SD: standalone management policy. | Source default (8457, as in v4.6) and standalone port (8454) differ. |
| Configuration | 12 | As Blacklist; IDDs: standalone port 8450 (source default 8459); SysD security bullet and `configurationManagement` SD: standalone policy. | Port difference as for Choreographer. |
| DataManager | 8 | As Blacklist; SysD use text: inter-cloud forwarding described as part of the earlier combined test cloud. | — |
| DeviceQoSEvaluator | 13 | As Blacklist (including three MQTT IDDs). | — |
| EventHandler | 12 | As Blacklist; `eventHandlerManagement` SD: standalone policy. | — |
| GateTunnel | 10 | As CertificateAuthority (26 environment variables re-checked). | — |
| Gatekeeper | 10 | As CertificateAuthority (24 environment variables re-checked). | — |
| GraphDBManager | 8 | SysD: abstract, standalone values, GraphDB URL; SysDD: test-cloud integration and smoke test described as a separate deployment consumer. | Not in Onboarding's default reserved-name list (see Onboarding). |
| LocalCloudManager | 6 | Largest change. SysD: new functions (authorization-policy editor, rule priority, SoS composition on PDE, optional LocalCloudConfigurator and chiplet floorplan views), 11 more configuration variables, consumed services (ConsumerAuthorization grant/revoke, SimpleStore `modify-priorities`, LocalCloudConfigurator, ChipletFloorplanGenerator), Node.js security model (proxy secret, host check, JSON-only login, snapshot conflicts). SysDD: location, new modules, standalone two-container deployment without Docker proxy, tests. SD/IDD: eleven console operations outside the registered list with their data models; success responses of `logout`, `unsubscribeSysMonitor`, `removePlantDescription`, `removeOrchestrationRule` corrected (200 with a body, not 204); error codes 403/405/409/415/503 completed. | The new console endpoints are not in the registered `operations` list. The PDE and SoS views need PDE `management.policy=whitelist` with LocalCloudManager, but the standalone PDE configuration is `sysop-only`. |
| Onboarding | 10 | As Blacklist; SysD: `ONBOARDING_DB_USER` and `ONBOARDING_RESERVED_NAMES`, the 1–32 system approval limit; `onboardingManagement` SD: approval limits and reserved names. | The default reserved-name list in `EnrollmentService` does not include GraphDBManager (source issue, not changed). |
| PlantDescriptionEngine | 10 | As Blacklist; SysD security and `plantDescriptionManagement` SD: standalone `sysop-only` and the whitelist needed by LocalCloudManager. | Deployment gap with LocalCloudManager (see above). |
| SysMonitor | 10 | As CertificateAuthority (21 environment variables re-checked). | — |
| TimeManager | 8 | As Blacklist; SysD security bullet: standalone policy. | — |
| TranslationManager | 16 | As Blacklist (four MQTT IDDs); SysD delimitations: the standalone deployment starts no translator. | — |

## Core systems — unresolved

`ah5-core-java-spring/` in this tree holds only build outputs (`target/` with the 5.2.2 JARs, logs and
`documentation-tools/model.json`). There is no source and no `documentation/` folder. The five JARs are byte-identical (MD5) to
those of `Arrowhead/ah5-core-java-spring`, whose source and SysD/SysDD/SD/IDD documents (ServiceRegistry,
Authentication, ConsumerAuthorization, DynamicServiceOrchestration, SimpleStoreServiceOrchestration) are in the
historical tree. Because the policy makes the historical tree a reference/deployment consumer only, those documents
were not edited, and no core documents were created here without a primary core source to check them against. A
decision is needed: either bring the core source (and its documentation) into this tree, or name the core source that
counts as primary. The core documents can then be checked the same way.

## Build

All 174 documents in the 17 folders were rebuilt with their `build-docs.sh` (latexmk, pdfLaTeX, BibTeX). Every build
succeeded and every PDF is newer than its source. A separate compile that kept the logs found no undefined references
or citations. The undefined primitive references already present in the earlier documents (`Array`, `Object`, `ExceptionType`
used in 26 IDDs without a definition) were fixed by adding the template's primitive rows. The remaining overfull boxes
are the same as before the update: the operation-heading overflow of about 1.9 pt per HTTP operation in the IDD
template, and long names in some tables. The eleven new LocalCloudManager operations add eleven of the same 1.9 pt
boxes. Pages of the new LocalCloudManager IDD section were rendered and inspected.

The documentation folders as they were before this update are backed up outside the repository, in this session's
scratchpad (`backup/`), because the system folders are not yet tracked by git.
