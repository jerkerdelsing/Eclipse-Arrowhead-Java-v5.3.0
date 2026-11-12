# Verification tools and evidence boundaries

Reviewed against the current Java-Spring layout on 12 November 2026. The October reports under [_superseeded](../../_superseeded/README.md) are dated evidence, not acceptance of the current release candidate.

## Current entry points

- `python3 verification/tools/verify_reference_conformance.py`: static primary-tree checks; no Docker or network. Covers version consistency, common copies, security packaging, discovery and inter-cloud rules. Its explicit support list predates SoLocalCloudConfigurator, so a pass is not complete release coverage.
- `python3 verification/tools/verify_listening_ports.py`: checks all 25 core/support packaged listening defaults. Choreographer/Configuration deployment-port differences are real; LocalCloudManager console warnings reflect its intentional second connector. See [listening ports](../../LISTENING-PORTS.md).
- Build/test each selected project with its own documented Maven/Makefile or `scripts/verify.sh` workflow. Third-party dependencies must be cached before an offline build. Use the [deployment guide](../../DEPLOYMENT-GUIDE.md) for fresh-cloud prerequisites and the separate [v530 procedure](../../../Arrowhead-test-clouds/Arrowhead-test-cloud-java/v530/README.md) for complete-cloud provisioning and smoke checks.
- `release_manifest.py`, `dependency_review.py`, `api_dto_review.py` and `negative_access_review.py` produce scoped review records. Inspect their inputs, options, source/image identities and output timestamps before relying on existing files under `verification/release-5.3.0/`; file presence alone is not acceptance.

## Historical fixture scripts

The following scripts remain for provenance and require review/adaptation before use against current packaging. They have not been relocated, so their existing path assumptions are preserved.

| Script | Historical scope / limitation |
|---|---|
| `verify_offline_builds.py` | Reads the October 15-system inventory and its native/Spring cache classification; does not cover the current 20 support folders. Writes back into the October result directory. |
| `verify_spring_deployments.py` | October eleven-Spring-system fixtures; not a complete current-system deployment suite. |
| `verify_native_deployments.py` | Four former native-JDK/SQLite systems; current applications are Spring/MySQL. |
| `verify_console.py` | Former Node.js/nginx console layout; does not verify the current Spring console/proxy. |

These deployment scripts create temporary networks, identities and volumes and remove their own fixtures. Do not run them against production projects or volumes. Their core fixtures are deliberately permissive protocol doubles, not production authorities or evidence of real authorization policy correctness. Historical logs remain under `verification/all-support-2026-10-25/` and `verification/independent-support-2026-10-25/`.

No application build, container startup or deployment was performed for this documentation cleanup. Static checks and document/link checks do not replace fresh release-artifact qualification.
