# System of LocalClouds (SoLC) view

The **SoLC** view of LocalCloudManager replaces the former SoS view (PlantDescriptionEngine compositions stay available
in the **Plant descriptions** view). It composes a *System of LocalClouds*: several local clouds, each built from a
local-cloud design stored in LocalCloudConfigurator (the **LC** view), federated through the organisation relay, with
the inter-cloud service exchanges between them. It validates the composition against the organisation's policy and builds
a deployment package that installs the SoLC on other hardware (Linux amd64, Linux arm64, macOS Docker Desktop).

The view is a client of **SoLocalCloudConfigurator** (`soLocalCloudConfiguration`, port 8496), reached through
orchestration like every provider of this console. It is shown when `lcm.so-local-cloud-configuration.enabled=true`
(`SO_LOCAL_CLOUD_CONFIGURATION_ENABLED`) and LocalCloudManager holds ConsumerAuthorization grants for the
`soLocalCloudConfiguration` operations it uses.

## Compose

1. Save one or more local-cloud designs in the LC view.
2. In the SoLC view choose **New SoLC**, name it, check the organisation and give the relay host and port: the host name
   or address that every member host *and its containers* use to reach the organisation relay
   (`host.docker.internal` for clouds on one Docker Desktop machine).
3. Under **Stored local-cloud designs** set the host and architecture of the next member and click a design: it becomes a
   member cloud pinned to the design's current revision, with a unique cloud name and a free port offset on that host.
   Select a member on the canvas to change its name, host, architecture or port offset. A member whose design changed in
   LocalCloudConfigurator shows the newer revision; **Use the stored revision** re-pins it.
   The right column then lists the selected cloud's infrastructure, one component per line:
   - **Inter-cloud infrastructure**: the components a federated member needs (Gatekeeper, GateTunnel), each marked as
     in the local-cloud design, added by this SoLC, or not included. **Add inter-cloud infrastructure** adds the missing
     ones to this member of the SoLC, without changing the stored local-cloud design; SoLocalCloudConfigurator adds
     what they require from the LocalCloudConfigurator catalog when the design is validated or saved.
     **Remove added inter-cloud infrastructure** takes them out again.
   - **Intra-cloud infrastructure**: every other component the cloud runs, grouped as core systems, support systems,
     application systems and brokers and proxies.
4. Under **Inter-cloud exchanges** pick a consumer system of one member and a provider service of another, and the
   operations. Only what the organisation policy allows is offered: systems a member's components offer as inter-cloud
   consumers, services its components export.
5. **Validate** asks SoLocalCloudConfigurator for the plan: structural problems, policy violations (rules S0-S4, A1-A4)
   and, per member, the Gatekeeper neighbours, ConsumerAuthorization grants and revocations and the checks the package
   will run. **Save design** stores it (revisioned; a stale save is refused with "reload before saving").

## Build and download a package

Choose the image mode (prebuilt image archives per architecture, or build on each host from the pinned source), the
architectures and whether global service discovery is verified, save, then **Build package**. SoLocalCloudConfigurator
builds only an allowed plan whose pinned local-cloud designs are still current. The package list links each
`solc-<name>-r<revision>.tar.gz` for download (through this console, with its SHA-256 shown); remove old packages there.

The package holds no secret. Image or source archives are added afterwards on a machine with Docker and the source
checkout: `kit/bin/assemble-package.sh <package> --source <checkout> --arch amd64,arm64`. The package's README and
`docs/` describe the installation on each platform, the federation steps and operations.

## Console routes

| Method | Path | Operation of soLocalCloudConfiguration |
|---|---|---|
| GET | `/api/solc/catalog` | `catalog` (organisation policy) |
| GET | `/api/solc/lc-designs` | `list-lc-designs` (stored LocalCloudConfigurator designs with their components) |
| GET, POST | `/api/solc/designs` | `list-designs`, `create-design` |
| GET, PUT, DELETE | `/api/solc/designs/{id}` | `get-design`, `update-design`, `remove-design` (`expectedRevision`) |
| POST | `/api/solc/plan` | `plan` (`{design, designId?}`) |
| GET, POST | `/api/solc/packages` | `list-packages`, `build-package` (`{designId, expectedRevision}`) |
| GET, DELETE | `/api/solc/packages/{id}` | `get-package` (tar.gz, passed through), `remove-package` |

Writes need the console's action confirmation header, as every write of this console.
