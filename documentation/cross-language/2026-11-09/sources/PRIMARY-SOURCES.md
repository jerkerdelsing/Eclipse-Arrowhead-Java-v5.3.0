# Primary sources for Eclipse Arrowhead

Effective 2026-10-30, at the user's explicit request, implementation updates within this Eclipse workspace must start from these primary source trees:

| Language | Primary source |
|---|---|
| Java | `Eclipse-Arrowhead-java/` |
| Go | `Eclipse-Arrowhead-go/` |
| Rust | `Eclipse-Arrowhead-rust/` |

Make implementation, UI, test, model and documentation changes in the appropriate primary tree. Build and validate the primary implementation. Treat older implementations under `Arrowhead/`, `EA-fork/`, and other historical folders as references or deployment consumers, not as the source of new development.

Before reusing an older implementation, compare it with the primary tree. Reconcile task-relevant changes into the primary tree deliberately; never overwrite the primary tree wholesale from an older copy. Do not assume previously reorganized copies contain every subsequent change.

Existing test-cloud folders may continue to hold deployment configuration. For future authorized builds/deployments, ensure the implementation and assets come from the primary tree; adjust build contexts, scripts or explicit packaging steps as needed. Do not silently build an older implementation just because an existing deployment script points there. Distinguish primary-source code from generated or deployment-specific copies in reports.

This designation does not itself migrate, delete or overwrite historical trees, synchronize all implementations, or redeploy running clouds. Keep unrelated user changes and current runtime configuration intact. Projects outside this Eclipse workspace retain their own source ownership unless the user specifies otherwise.
