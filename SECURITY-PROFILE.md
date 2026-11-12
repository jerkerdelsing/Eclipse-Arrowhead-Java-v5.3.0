# Secure profile

Since v5.0, full AA security and encrypted information exchange are the default of the Arrowhead architecture. Every system built on the shared library checks this when it starts, and the static check below covers packaging. A configuration that falls short stops the system. It does not fall back to a weaker mode.

## What stops a system

`SecurityProfileGuard` in the common library reads the effective configuration, so property files, environment variables and command-line arguments all count. The system refuses to start if any of these apply:

| Setting | Why it is refused |
|---|---|
| `server.ssl.enabled=false` | Plain HTTP and MQTT give no peer identity or encryption. |
| `authentication.policy=declared` | The requester states its own name, with no proof. |
| `service.authorization.enabled=false` | The system does not enforce authorization. |
| `sysmonitor.ssl.enabled=false` while `sysmonitor.enabled=true` | Monitoring observations would travel unencrypted. |
| `device.agent.ssl.enabled=false` (DeviceQoSEvaluator) | Device load figures would travel without encryption or authentication, so anyone on the network could read them or forge them and skew how providers are scored. |

The check runs before Spring creates any bean, so the system never opens a port or a database connection. The error lists each setting that caused the refusal.

Many common-library applications ship `src/main/resources/application.properties` as development samples with TLS off; those settings are refused in the default secure profile. Inspect the effective configuration rather than assuming every packaged sample is identical. Deployments use the secure configuration in `config/application.properties` for each support folder, and the matching properties or environment variables for Core.

All current core and support applications are Java-Spring, including CertificateAuthority, Gatekeeper, GateTunnel, SysMonitor and LocalCloudManager. Most support systems compile private common-library copies; security fixes must reach those copies as well as the shared library.

- AIGym has a standalone Spring implementation rather than the common-library guard. Its packaged deployment uses mutual TLS and its own security/authorization filters; the guard table above is not a claim about AIGym's implementation.
- LocalCloudManager has an Arrowhead mutual-TLS connector on 8491 and a separate password/session-protected plain-HTTP operator console on 8490, published on host loopback. The console's checks and constrained Docker proxy are described in its [deployment guide](ah5-LocalCloudManager-java-spring/DEPLOYMENT.md).
- LocalCloudConfigurator's host worker is a separate loopback mutual-TLS agent, not another Arrowhead support system. SoLocalCloudConfigurator is the current SoLC service; SoSConfigurator is retained legacy.

## Development exception

For an isolated test, select the exception explicitly when you start the system:

```sh
ARROWHEAD_SECURITY_PROFILE=development java -jar arrowhead-serviceregistry-5.3.0.jar
```

The value can be `secure` (the default) or `development`. Any other value stops the system. With `development`, the system starts and logs a `NON-CONFORMING` warning that lists the deviations. Never expose such a system or connect it to a production cloud.

Do not put this setting in a `config/application.properties`, `compose.yaml`, `Dockerfile` or `.env.example`. The static check fails if you do.

## Static check

```sh
python3 verification/tools/verify_reference_conformance.py
```

It checks version, port, monitoring, security packaging, discovery and inter-cloud rules in its configured scope. Its explicit support list predates SoLocalCloudConfigurator; see the [tool coverage limits](verification/tools/README.md). A pass is not full release qualification.

## Limits

- The DeviceQoSEvaluator device agent (`device_agent/python`) is not a Java system and has no guard. It enforces its own default: mutual TLS, a client allow-list and a CRL check, and plain HTTP only on a loopback address.
- Outbound revocation handling is client-specific. LocalCloudManager explicitly checks provider certificates against the cloud CRL in its [TLS context](ah5-LocalCloudManager-java-spring/src/main/java/eu/arrowhead/localcloudmanager/client/TlsContextFactory.java). Do not infer equivalent handling for every outbound client from the startup guard.
- The guard covers the Java reference tree only. The Go and Rust trees have their own startup behaviour.
- A running system is not changed until it is rebuilt from this tree and redeployed.
- A running test cloud that keeps insecure values in `application.properties` and overrides them with environment variables passes, because the guard reads the effective values.
