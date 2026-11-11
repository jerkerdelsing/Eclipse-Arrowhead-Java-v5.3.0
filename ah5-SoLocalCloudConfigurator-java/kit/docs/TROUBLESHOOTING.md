# Troubleshooting

| Symptom | Cause and remedy |
|---|---|
| `install.sh`: "this host runs amd64, the design places X on arm64" | The member's architecture in the design does not match the host. Fix the design, or `SOLC_ALLOW_EMULATION=1` for a trial. |
| "images/<arch>/images.tar is missing" | A prebuilt package was not assembled for that architecture: `kit/bin/assemble-package.sh <package> --source <checkout> --arch <arch>`. |
| A container stays `unhealthy` / `--wait` times out | `docker compose -p <project> logs --tail 200 <service>` in `clouds/<Cloud>/work`. Memory: `docker stats`; raise Docker's memory. |
| "Bind for 127.0.0.1:NNNN failed: port is already allocated" | Another cloud uses the same port offset on this host; give each member on a host its own offset (multiple of 1000). |
| "Pool overlaps with other one on this network" | Two clouds with the same offset on one host, or an unrelated network in 10.2xx.0.0/16: `docker network ls`, remove stale networks of an earlier install. |
| Policy script: "Unable to configure policy" | ConsumerAuthorization not reachable or Sysop login failed: check `authentication` and `consumerauthorization` logs; re-run `install.sh` (idempotent). |
| Relay: Gatekeeper logs "unable to get certificate CRL" / TLS alert | A CRL expired or is missing: OPERATIONS.md (CRLs). Also check the clock of both hosts. |
| Relay: "certificate verify failed" (hostname) | The relay host the members use is not in the relay certificate: the design's relay host must be the name the members resolve; re-run `org/init-org.sh` after changing it. |
| Relay connection refused / timeout | Firewall or wrong host/port; from the member host: `openssl s_client -connect <relay-host>:<port> </dev/null`. Inside containers the name must resolve too (`host.docker.internal` on Docker Desktop). |
| `intercloud.sh`: "authorization preconditions not met" | The cloud's policy scripts did not run fully; re-run `install.sh`. |
| `intercloud.sh discover` finds no provider | Run `intercloud.sh` on the provider cloud first; check the provider's grants (`intercloud.sh verify` there) and that both bundles list each other as peers (`org/sign-cloud.sh --bundles`, then import again). |
| `check.sh` skips the security suite | `smoke-security.sh` is the reference cloud's suite and probes with the console, SysMonitor, CertificateAuthority and both brokers; a leaner design runs `smoke-test.sh` and the federation verification only. `FULL_SECURITY=1` runs the suite anyway (its checks of absent systems then fail). |
| Console login fails | Password: keychain (macOS) or `work/operator/console-password`. Host header: use `127.0.0.1`, `localhost` or `<cloud>.localhost`. |
| `install.sh`: "the package path is too long" / openssl "File name too long" | `openssl ca` handles only short paths: unpack the package in a short folder such as `~/arrowhead`. |
| A database container is unhealthy; its log shows `io_setup() failed with EAGAIN` | The Docker host ran out of Linux AIO contexts (`fs.aio-max-nr`; each MySQL takes about 4096). The installer detects this and starts the databases without native AIO; on a Linux server raise the limit (`sudo sysctl -w fs.aio-max-nr=1048576`, persisted in `/etc/sysctl.d/`), remove the failed database volume (`docker volume rm <project>_<service>-state`, it holds no data yet) and re-run `install.sh`. Force the fallback with `SOLC_DB_NATIVE_AIO=off`. |
