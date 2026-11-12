# SoLocalCloudConfigurator

Arrowhead 5.3 support system for **Systems of LocalClouds (SoLC)**: several local clouds, each built from a local-cloud
design stored in LocalCloudConfigurator, federated through an organisation relay, with the inter-cloud service exchanges
between them. It replaces the console's former SoS view and absorbs SoSConfigurator's policy engine and inter-cloud
planner (SoSConfigurator stays as legacy).

- **Designs** (`solc_design`, revisioned): members (cloud name, pinned LocalCloudConfigurator design and its component
  closure, host, architecture, port offset, added inter-cloud infrastructure), relay, exchanges, package options. Member
  snapshots always come from LocalCloudConfigurator (`localCloudConfiguration` catalog, list-configurations,
  get-configuration, through orchestration), never from the request.
- **Inter-cloud infrastructure**: a member whose local-cloud design lacks the policy's inter-cloud components
  (`intercloudComponents`: Gatekeeper, GateTunnel) can get them from the SoLC (`member.intercloud`). The request names
  only those components; what they require is resolved from the LocalCloudConfigurator catalog and stored with the
  member. The stored local-cloud design is not changed; the member runs its design's components plus these.
- **Plan**: structural checks (`DesignValidator`) and the organisation policy (`PolicyEngine`, rules S0-S4 and A1-A4,
  `catalogs/solc-policy.json`): per member the Gatekeeper neighbours, ConsumerAuthorization grants for exactly the
  exchanges, revocations of unused exports, preconditions, verification checks, discovery; the relay ACL subjects.
- **Packages** (`solc_package`, tar.gz ≤ 32 MiB): `PackageBuilder` renders README, manifest, per-cloud configuration and
  scripts, the organisation scripts, the portable deployment kit and the documentation (`kit/`, on the classpath) with
  SHA256SUMS. No secrets: every key and password is generated on its host. This system has no Docker access; image and
  source archives are added on a machine with Docker by `kit/bin/assemble-package.sh` (second stage).

Service `soLocalCloudConfiguration` (mutual TLS, port 8496): `catalog`, `list-lc-designs`, `list-designs`, `get-design`,
`create-design`, `update-design`, `remove-design`, `plan`, `build-package`, `list-packages`, `get-package`,
`remove-package`. Every call needs Sysop, `solc.api.whitelist` or a ConsumerAuthorization grant of the operation
(`SolcAccessFilter`). The UI is the LocalCloudManager **SoLC** view (`SOLC-CONFIGURATION.md` there).

## Layout

| Path | Content |
|---|---|
| `src/main/java/.../policy`, `design`, `catalog` | policy engine, design model and validation, organisation policy catalog |
| `src/main/java/.../service`, `packaging`, `api/http` | storage, LocalCloudConfigurator client, package builder, REST API |
| `kit/cloud` | portable local-cloud kit (generated: `tools/make-kit.py <Arrowhead-test-cloud-java>`) and its Compose template |
| `kit/bin` | member-host scripts: install, configure, images, federation request/import, inter-cloud, checks, assembler, selftest |
| `kit/org` | organisation host: org CA, cross-signing from requests, CRL, relay with generated ACL |
| `kit/docs` | INSTALL (Linux amd64, Linux arm64, macOS), FEDERATION, OPERATIONS, SECURITY, TROUBLESHOOTING |

## Build and test

```sh
mvn verify                     # unit tests: policy, validation, access filter, package builder (with the real kit)
kit/bin/selftest.sh --module   # kit: shell and Python syntax, no secrets or host paths, configure.py, compose config
```

Deployment: [DEPLOYMENT.md](DEPLOYMENT.md).
