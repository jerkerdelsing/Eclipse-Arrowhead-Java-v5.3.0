# Java LocalCloudConfigurator deployment verification

2026-10-30. Implemented in the primary Eclipse-Arrowhead-java source tree using the Go LocalCloudConfigurator as reference.

## Delivered and deployed

Java 21 catalog-based planner, private operator API, persistent revision-guarded designs and signed ColonyOS workflows with a catalog-checking host executor. LocalCloudManager has a graphical LC view: component selection, automatic infrastructure, saved draggable positions, undo/redo, import/export, plan review, explicit apply and workflow progress.

Open http://testcloud.localhost:8490/ and choose LC. Existing manager login remains in place. The private operator API is loopback port 8097; its token never reaches the browser.

New designs contain ServiceRegistry, Authentication, ConsumerAuthorization and DynamicServiceOrchestration, plus four databases and AMQP: nine default nodes. SimpleOrchestration is optional and absent from defaults. Existing running systems are not implicitly removed.

## Verification results

- 19 Java tests passed.
- 60 LocalCloudManager API tests passed; 43 focused API/editor tests passed. These suites overlap.
- Seven live API checks passed: authentication, exact defaults, runtime status, reviewed verification-only plan, stale-plan rejection, successful signed ColonyOS execution and protected-stop rejection.
- Ten browser check groups passed: dependency selection, drag layout, save/update/reopen, stale-edit rejection, reviewed execution, desktop/mobile rendering and test-design cleanup.
- Configurator and manager API/UI containers were healthy. Guarded host executor restart and a subsequent real workflow passed.
- Java baseline container IDs were unchanged except manager API/UI. SimpleOrchestration retained its running container. See runtime-comparison.json.

Live workflows verified the baseline without changing running microsystems. Provision/start/policy/stop paths were unit-tested; destructive actions were not exercised in the shared cloud. No automatic transactional rollback is provided.

## Operational boundaries

Separate colony on the existing ColonyOS server; no new Arrowhead identity or authorization grants. Private file credentials, fixed manager proxy routes, reviewed plan hashes, catalog-validated execution and persistent single-deployment protection. Core, required infrastructure and manager access are protected from stop requests.

API assignment heartbeats retain ColonyOS liveness. Registration checks public identities before reuse. The executor runs an immutable content-addressed JAR to remain safe from subsequent source builds. Compact bounded runtime queries avoid status truncation.

Usage and deployment instructions: ../../README.md and ../../DEPLOYMENT.md. System and interface documentation: ../../documentation/. Sanitized test results and screenshots accompany this report. Credentials and private executor logs are excluded.
