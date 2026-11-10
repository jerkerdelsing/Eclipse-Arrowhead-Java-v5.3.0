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

The `src/main/resources/application.properties` of each system is a development sample with TLS off, so `java -jar` with no other configuration is refused. This is intended. Deployments use the secure configuration: `config/application.properties` in each support folder, and the matching properties or environment variables for Core.

Other systems in the tree work differently:

- The four native Java systems (CertificateAuthority, Gatekeeper, GateTunnel, SysMonitor) have no plaintext mode. They refuse any endpoint that is not HTTPS.
- AIGym does not use the shared library. It defaults to TLS with client certificates, and its security filter rejects a request without one.
- LocalCloudManager is a Node.js console, not a Spring system. It has its own login and proxy checks.

## Development exception

For an isolated test, select the exception explicitly when you start the system:

```sh
ARROWHEAD_SECURITY_PROFILE=development java -jar arrowhead-serviceregistry-5.2.2.jar
```

The value can be `secure` (the default) or `development`. Any other value stops the system. With `development`, the system starts and logs a `NON-CONFORMING` warning that lists the deviations. Never expose such a system or connect it to a production cloud.

Do not put this setting in a `config/application.properties`, `compose.yaml`, `Dockerfile` or `.env.example`. The static check fails if you do.

## Static check

```sh
python3 verification/tools/verify_reference_conformance.py
```

It checks the packaged configuration of every folder, and also the version, port, monitoring, discovery and inter-cloud rules of this reference tree.

## Limits

- The DeviceQoSEvaluator device agent (`device_agent/python`) is not a Java system and has no guard. It enforces its own default: mutual TLS, a client allow-list and a CRL check, and plain HTTP only on a loopback address.
- Outbound calls do not check the server certificate against a CRL; only incoming connections do.
- The guard covers the Java reference tree only. The Go and Rust trees have their own startup behaviour.
- A running system is not changed until it is rebuilt from this tree and redeployed.
- A running test cloud that keeps insecure values in `application.properties` and overrides them with environment variables passes, because the guard reads the effective values.
