# Installing a member cloud on Linux (amd64)

For x86-64 servers and PCs running a current 64-bit Linux distribution (tested reference: Ubuntu 24.04 LTS; Debian 12,
RHEL/Rocky/Alma 9 and Fedora work the same way with their package manager).

## Host requirements

| Resource | Minimum | Recommended |
|---|---|---|
| CPU | 2 cores | 4 cores |
| Memory available to Docker | 4 GB (core only) | 8 GB (core + 5 support systems) |
| Disk | 15 GB (prebuilt images) | 30 GB (build on target: Maven and image layers) |
| Network | the organisation relay host and port (outbound TCP) | |

Each Arrowhead system runs as one container plus, for most, a private MySQL container; budget about 300 MB of memory
per system with a database.

## Software

```sh
sudo apt-get update
sudo apt-get install -y ca-certificates curl jq openssl python3 tar git
# Docker Engine and the Compose v2 plugin from Docker's repository (https://docs.docker.com/engine/install/)
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker "$USER"     # log out and in again
docker compose version              # must print v2.x
```

`keytool` is optional: without a JDK the kit runs it from the `eclipse-temurin:21-jre-alpine` image. To use a local one:
`sudo apt-get install -y openjdk-21-jre-headless`.

Build-on-target packages also need Docker Buildx (part of the Docker packages above) and, for `fetch-source.sh`, git and
access to the source repository. Prebuilt packages need neither.

## Install

1. Copy the package to the host and unpack it in a folder owned by the operating user, outside any shared folder:

   ```sh
   mkdir -p ~/arrowhead && tar -xzf solc-<name>-r<rev>*.tar.gz -C ~/arrowhead && cd ~/arrowhead/solc-<name>-r<rev>
   sha256sum -c SHA256SUMS
   ```

2. Run the cloud's installer (the cloud name is in the package README's member table):

   ```sh
   clouds/<Cloud>/install.sh
   ```

   It checks the host, loads or builds the images, generates the cloud CA, certificates, passwords and keystores in
   `clouds/<Cloud>/work/`, starts the cloud in bootstrap order and provisions identities and policies (10-30 minutes; a
   build on target adds the Maven builds, 30-90 minutes on 4 cores).

3. Check it: `clouds/<Cloud>/check.sh`.

4. In a federation, continue with [FEDERATION.md](FEDERATION.md).

The cloud's published ports listen on 127.0.0.1 only (port offset of the design added). Reach the console from another
machine through an SSH tunnel, e.g. `ssh -L 8490:127.0.0.1:<console port> <host>`; never publish the management ports.

## Start at boot

Containers restart with Docker (`restart: unless-stopped` where the systems define it). Enable Docker itself:
`sudo systemctl enable --now docker`.

## Firewall

Inbound: nothing for a member host (the relay connection is outbound). The organisation host accepts the relay port from
the member hosts only, for example with ufw: `sudo ufw allow from <member-ip> to any port <relay-port> proto tcp`.
Docker publishes ports past ufw's INPUT chain; the relay port is the only port the package publishes on all interfaces.
