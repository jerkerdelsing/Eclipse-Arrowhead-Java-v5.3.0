# SD — Operator/configuration services

Service definition `localCloudConfiguration` 1.0.0, interface `generic_https` (base path `/api`, policy CERT_AUTH) on port 8492. Every operation needs a client certificate of the cloud, an identity token bound to it (outsourced authentication) and Sysop, `lcc.api.whitelist` or a ConsumerAuthorization grant of the operation (`catalog`, `status`, `plan`, `submit-deployment`, `get-deployment`, `list-configurations`, `create-configuration`, `get-configuration`, `update-configuration`, `remove-configuration`). The native `/healthz` is not served on the Arrowhead port (the container health check is a TCP probe); the host worker keeps it. LocalCloudManager exposes equivalent allowlisted routes below `/api/localcloud` under its existing console authentication and consumes this service through orchestration. Its POST/PUT/DELETE proxy operations also require `X-Arrowhead-Action: confirm`. The operator credential is never returned to the browser.

| Method | API | Contract |
|---|---|---|
| GET | `/api/catalog` | Cloud/project/colony identity, defaults, executor type and component descriptions |
| GET | `/api/status` | Running Compose services and per-component running/partial/stopped state |
| POST | `/api/plan` | `{systems, remove, reapply, smoke}` → `{plan, hash}` using fresh runtime state |
| POST | `/api/deployments` | Same selection plus `expectedPlanHash`; rejects stale plans and concurrent active deployments; returns an accepted workflow ID or a successful no-op |
| GET | `/api/deployments/{id}` | Real step state reported by the host worker (or ColonyOS) for a recorded submitted deployment; the latest report is stored |
| GET/POST | `/api/configurations` | List saved designs (up to 1,000), or create a named design |
| GET/PUT/DELETE | `/api/configurations/{uuid}` | Read, replace with `expectedRevision`, or delete with `expectedRevision` |

A design has `name`, explicit `systems`, explicit `remove`, `reapply`, `smoke` and `layout: {componentId: {x,y}}`; stored responses add UUID, revision and update time. Defaults are restored even when requests omit them. Up to 200 selection/removal IDs, 200-character names and finite graphical coordinates in [0,10000] are accepted, within the 64 KiB request limit. Unknown JSON fields and catalog IDs are rejected. A deletion is design-only.

Status is cached briefly for display; plan/apply use fresh executor reads. Infrastructure dependency links are catalog-defined, not arbitrary browser-editable connections. No selected component is implicitly removed because it was absent from a new design.

HTTP errors include 401 authentication (common ErrorMessageDTO body), 403 missing grant, 400 invalid/catalog-conflicting input, 404 unavailable record/operation, 409 stale design/plan or active deployment, 413 oversized request, and 502 unavailable backend/executor. An accepted deployment can later fail; its earlier successful operational steps remain applied.

### Database ownership

A defined database is a private integrated asset of its host microsystem. `internalAssets` identifies the owned database Compose services; `services` orders database startup before host startup (reverse for stopping). It is not a catalog component or external dependency. Other microsystems access data only through the host’s produced or consumed services. Dedicated internal database networks admit only the host and its database, with no published database port. The default design has four microsystems plus one AMQP infrastructure node.
