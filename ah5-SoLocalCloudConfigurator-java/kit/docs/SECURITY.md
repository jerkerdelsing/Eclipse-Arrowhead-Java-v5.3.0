# Security model of an SoLC package

- **No secrets in the package.** Keys, certificate requests, keystores and passwords are generated on the host that uses
  them (`install.sh`, `org/init-org.sh`). The package can be stored and transported like source code; its integrity is
  protected by `SHA256SUMS` (compare the checksum of the package file with the one the console shows).
- **Per-cloud CA.** Each member has its own cloud CA (key on that host only). Inside a cloud, systems trust only their own
  cloud CA; mutual TLS with certificate-bound identities, ConsumerAuthorization for every call, Blacklist enforcement,
  per-system database users and file-mounted secrets (the v5.3.0 hardening: read-only root filesystems, no capabilities,
  memory and PID limits).
- **Organisation trust, name-constrained.** The organisation CA cross-signs a cloud CA only from a request signed with
  that CA's key, and constrains it to `C=SE, O=<Org>, OU=<Cloud>`. A cloud CA therefore cannot issue a relay identity
  for another cloud. The relay uses the full verified subject as MQTT user; the generated ACL lets each Gatekeeper and
  GateTunnel read only its own cloud's inbox and write only under its own name.
- **Least privilege between clouds.** SoLocalCloudConfigurator derives the grants from the design and the organisation
  policy (`catalogs/solc-policy.json`): only systems a component offers as inter-cloud consumers, only services it
  exports, only the chosen operations (rules A1-A4); operator, core, inter-cloud and configurator systems never cross a
  cloud boundary; exports the design does not use are revoked and the revocation verified (`intercloud.sh verify`).
- **Revocation, fail closed.** Relay and GateTunnel check cloud and organisation CRLs; an expired CRL refuses
  connections (OPERATIONS.md).
- **Network exposure.** Member clouds publish their ports on 127.0.0.1 only; the relay port on the organisation host is the
  only listening port on other interfaces.
- **Operator access.** Policies are managed as the operator identity Sysop with its certificate (no running system's
  identity is used). The console (LocalCloudManager) runs without sysop rights, with explicit grants.

Report vulnerabilities to the Eclipse Arrowhead project (SECURITY.md of the source repository).
