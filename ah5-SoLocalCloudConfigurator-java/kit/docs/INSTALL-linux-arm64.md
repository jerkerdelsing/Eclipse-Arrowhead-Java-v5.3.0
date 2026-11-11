# Installing a member cloud on Linux (arm64)

For 64-bit ARM devices and servers: Raspberry Pi 5 and 4 (8 GB), NVIDIA Jetson Orin, Rockchip RK3588 boards, AWS Graviton
and Ampere servers, running a 64-bit OS (Raspberry Pi OS 64-bit (Bookworm), Ubuntu 24.04 for ARM, Debian 12 arm64).
A 32-bit OS (armhf) is not supported: `uname -m` must print `aarch64`.

## Host requirements

| Resource | Minimum | Recommended |
|---|---|---|
| CPU | 4 cores (Cortex-A72 or newer) | Cortex-A76 / Neoverse |
| Memory | 8 GB for core + 2-3 support systems | 16 GB |
| Storage | 32 GB on SSD or NVMe | 64 GB SSD; avoid SD cards for the databases (wear, latency) |

On small devices choose a lean local-cloud design: the core systems plus the support systems the device needs. Every
system with a database adds a MySQL container.

## Software

As for amd64 ([INSTALL-linux-amd64.md](INSTALL-linux-amd64.md)): Docker Engine with Compose v2, python3, openssl, jq,
curl, tar (git for build on target). On Raspberry Pi OS, `curl -fsSL https://get.docker.com | sudo sh` installs the arm64
Docker packages.

Raspberry Pi: enable the memory cgroup if `docker info` warns about it (append
`cgroup_enable=memory cgroup_memory=1` to `/boot/firmware/cmdline.txt` and reboot); the cloud's containers have memory
limits.

## Images

- **Prebuilt package**: the package must contain `images/arm64/images.tar` (assembled with `--arch arm64`). The installer
  checks it against `images/arm64/images.sha256` and loads it. No network access is needed.
- **Build on target**: the installer builds every image natively from the pinned source. On a Raspberry Pi 5 this takes
  1.5-3 hours (24 Maven builds with tests); prefer prebuilt packages for devices.

The installer refuses to run amd64 images on an arm64 host (and vice versa) unless `SOLC_ALLOW_EMULATION=1` is set;
emulated Java systems are 5-20 times slower and only fit for trials.

## Install

```sh
mkdir -p ~/arrowhead && tar -xzf solc-<name>-r<rev>*.tar.gz -C ~/arrowhead && cd ~/arrowhead/solc-<name>-r<rev>
sha256sum -c SHA256SUMS
clouds/<Cloud>/install.sh
clouds/<Cloud>/check.sh
```

Then [FEDERATION.md](FEDERATION.md) for a cloud that exchanges services with other members.

## Clock

Certificates, CRLs and identity tokens depend on a correct clock. Devices without a real-time clock must synchronise it
before the cloud starts: `timedatectl` should show `System clock synchronized: yes` (systemd-timesyncd or chrony).
