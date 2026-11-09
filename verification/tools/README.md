# Repeating the isolated verification

Run from a complete Eclipse-Arrowhead distribution using Python 3, Docker/Compose and OpenSSL. Build images beforehand using each selected project's own `docker compose build`. The project-local `scripts/verify.sh` is the portable single-folder build check. Third-party Maven dependencies must already be cached for offline checks.

- `python3 verification/tools/verify_reference_conformance.py`: static checks of the primary tree, with no Docker or network. It exits non-zero and lists each failure. Run it before packaging.
- `python3 verification/tools/verify_offline_builds.py`: all 15 Java builds/tests with network disabled, using only each project source mount and a Maven cache. Spring caches default to ~/.m2; native caches use the existing arrowhead-java-support-maven-cache Docker volume. Populate those caches first or change only the cache location.
- `python3 verification/tools/verify_spring_deployments.py`: the 11 Spring applications, one support system at a time with its own database and mTLS protocol fixtures. `AH_SYSTEM_FILTER=TranslationManager,DataManager` selects a subset and merges the latest results.
- `python3 verification/tools/verify_native_deployments.py`: the four native Java deployment/storage checks. This retains the original native-result location under verification/independent-support-2026-10-25.
- `python3 verification/tools/verify_console.py`: the Node/nginx console, with no Docker host-control service.

Run deployment scripts serially; they use explicitly named temporary fixture directories, networks and Compose projects. They remove their own disposable volumes/networks/credentials on exit and must never be pointed at a production project or reused production volume. Test private keys are generated locally solely for these fixtures. The core fixture is deliberately permissive test infrastructure and must never be deployed as a production authority. It validates startup/integration wiring, not real authority policy correctness. Normal production application authentication/authorization and TLS filters remain active in the tested systems.

The scripts use existing named images and write current results to verification/all-support-2026-10-25 (or the native location noted above). Results are snapshots for the code/image IDs recorded in the report. Repeating a subset replaces that system's latest record. No live-cloud rollout is performed.
