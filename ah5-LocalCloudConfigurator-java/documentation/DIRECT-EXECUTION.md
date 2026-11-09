# Direct execution — host worker over mutual TLS (8 November 2026)

Direct local execution without ColonyOS remains the default (`lcc.backend=direct`). Since the Java-Spring conversion the worker is the same JAR started as `worker`, a non-web Spring application, and the connection between the API container and the worker is mutual TLS instead of a shared bearer token.

The API container connects to `https://host.docker.internal:8493`; the worker listens only on host `127.0.0.1:8493` and refuses a non-loopback address. The API presents its LocalCloudConfigurator certificate; the worker presents LocalCloudConfiguratorWorker. Both validate the peer against the cloud truststore (and the cloud CRL when configured) and accept only that peer system name. Both API and worker independently review the exact plan against current cloud state. Individual commands retain catalog ownership checks, protected-component checks and redaction. The API receives no ColonyOS keys, environment variables or network membership.

The worker is installed at `~/Library/LaunchAgents/eu.arrowhead.java-localcloud-direct.plist` with RunAtLoad and KeepAlive enabled (`scripts/install-direct.py`). It starts at login after a reboot and restarts after a crash. Docker Desktop must also start at login; the worker may start first and can answer status after Docker becomes available. `make worker-restart` reinstalls and restarts it.

Immutable JAR/catalog releases, certificates, the store-password config tree and private state are under `~/.local/share/JavaLocalCloudConfigurator/`. Workflow results are saved atomically after every step. A state-directory lock prevents duplicate workers. Interrupted deployments are marked failed on restart and are never replayed automatically; inspect the runtime and review a fresh plan. Completed actions are not automatically rolled back. Installation refuses to interrupt a running deployment.

The API keeps saved designs, deployment records (with the last reported step results) and the active-deployment record in its private MySQL schema.

Open **http://testcloud.localhost:8490/** and select **LC / Local cloud configuration** with the existing manager login.
