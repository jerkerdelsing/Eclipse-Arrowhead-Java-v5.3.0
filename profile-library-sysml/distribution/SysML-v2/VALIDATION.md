# Validation — 2026-10-27

Result: **0 SysML errors, 0 SysML warnings** across all 41 delivered library files.

- Parser and semantic validator: installed OMG SysML v2 Xtext reference tooling `0.52.1.202510201816`, in `/Applications/Eclipse-2503.app/Contents/Eclipse/plugins`.
- Standard library: `SysON/SysMLv2-dev/sysml.library`.
- Java: Zulu OpenJDK 25.0.2.
- Loaded 135 resources (41 Arrowhead model files plus standard library dependencies), resolved cross references, and ran Xtext `CheckMode.ALL` for every Arrowhead model resource.
- Separate parse-only pass: 0 syntax errors.
- Coverage checks: 21 systems, 91 services, 124 protocol interfaces, 424 protocol-specific operations.
- Regenerated the complete model set and `sources.json` into a second directory with the delivered generator; all 41 models and the inventory matched byte for byte.
- Compared the two core catalogues byte for byte. Compared the native `SysMonitorPublisher.class` in all five target system JARs against the matching source build; all five matched byte for byte.

The Java/Guice runtime prints an `Unsafe` deprecation notice; it is not a SysML diagnostic. The complete model-check result was `ERRORS=0 WARNINGS=0`.

This establishes syntax, reference resolution and the installed validator's semantic checks. It does not establish deployment correctness, executable behavior, complete DTO required-field constraints, or interchange compatibility with every SysML v2 tool. No live system was changed or deployed.

To repeat validation:

```sh
ECLIPSE_HOME=/Applications/Eclipse-2503.app/Contents/Eclipse \
SYSML_STANDARD_LIBRARY=/path/to/sysml.library \
sh tools/validate.sh
```

The validator source and launcher are included under `tools/`. Both the reference tool and the standard library must be available locally; they are not bundled or redistributed here.
