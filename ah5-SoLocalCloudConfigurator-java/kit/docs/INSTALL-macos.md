# Installing a member cloud on macOS (Docker Desktop)

For development, demonstrations and several member clouds on one machine (Apple silicon: arm64; Intel Macs: amd64).

## Requirements

- Unpack the package in a short path such as `~/arrowhead` (the installer refuses paths whose cloud folder exceeds 120 characters: `openssl ca` cannot handle them).
- macOS 14 or newer, Docker Desktop 4.30 or newer with **at least 8 GB memory** (Settings > Resources) for one cloud and
  about 6 GB more for each further cloud on the same Mac; 40 GB of free disk for images.
- Command line tools: `xcode-select --install` (git, tar), and with Homebrew: `brew install jq` (python3, openssl/LibreSSL
  and curl come with macOS; `openssl` 3 from Homebrew is also fine).
- `keytool` is optional (`brew install openjdk@21`); without it the kit runs keytool in a container.

## Install

```sh
mkdir -p ~/arrowhead && tar -xzf solc-<name>-r<rev>*.tar.gz -C ~/arrowhead && cd ~/arrowhead/solc-<name>-r<rev>
shasum -a 256 -c SHA256SUMS
clouds/<Cloud>/install.sh
clouds/<Cloud>/check.sh
```

Keep the package outside iCloud Drive, Dropbox and similar synchronised folders (they change file modes and may upload
the generated keys).

The console password is stored in the macOS keychain (service `arrowhead-console-<project>`):
`security find-generic-password -s arrowhead-console-<project> -w`. The console is at
`http://127.0.0.1:<8490 + port offset>` and `http://<cloud>.localhost:<port>`.

## Several clouds and the organisation host on one Mac

A design may place several members on the same host with different port offsets (multiples of 1000). Each member gets its
own Compose project, networks (10.200+offset/1000.x.0/28) and ports. Give the relay a host name that the containers can
reach: on Docker Desktop that is `host.docker.internal` (the relay publishes its port on the Mac; the cloud containers
reach it through Docker Desktop's host gateway). Do not use `localhost` as relay host: inside a container it is the
container itself.

## File sharing

Docker Desktop shares `/Users` by default, which the cloud's bind mounts need. A package unpacked elsewhere (for example
`/opt`) must be added under Settings > Resources > File sharing.

## Sleep

A sleeping Mac stops the clouds' clocks; tokens and CRLs that expire meanwhile are renewed after wake-up, but federation
sessions are dropped. Keep the Mac awake while it serves other members.
