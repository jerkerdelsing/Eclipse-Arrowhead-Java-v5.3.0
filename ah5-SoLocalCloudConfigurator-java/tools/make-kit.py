#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""Refresh kit/cloud/ (the portable local-cloud kit bundled into every SoLC package) from a reference local cloud.

  tools/make-kit.py <reference-cloud-dir>

The reference is the v5.3.0 Java test cloud (Arrowhead-test-clouds/Arrowhead-test-cloud-java), whose deployment is tested
(smoke-test.sh, smoke-security.sh). Copied: its policy, provisioning, credential and smoke scripts, broker and database
configuration, certificate extension files, core image adapters, the RandomDataProducer source, and its rendered Compose
definition as kit/cloud/templates/compose-template.json with every host path, project, registry and tag replaced by a
placeholder (@CLOUD_DIR@, @PROJECT@, @REGISTRY@, @TAG@). Never copied: keys, certificates, stores, secrets, .env,
generated broker definitions, logs. kit/bin/configure.py turns the kit into one cloud's deployment on its host.
"""
from __future__ import annotations

import json
import pathlib
import re
import shutil
import sys

KIT = pathlib.Path(__file__).resolve().parents[1] / "kit" / "cloud"
SCRIPT_EXCLUDE = re.compile(r"(\.before-|^build-|^remove-legacy-|\.pyc$)")
COPY = {
    "amqp": ["acl", "advanced.config", "enabled_plugins", "entrypoint.sh", "rabbitmq.conf"],
    "mqtt": ["acl", "crl-watch.sh", "mosquitto.conf"],
    "db": ["mysql-entrypoint.sh", "zy-dso-plant-rules.sql", "zy-serviceregistry-amqp-templates.sql", "zz-arrowhead-system-user-password.sh"],
}
# services of the reference cloud that a member cloud of a SoLC does not run: LocalCloudConfigurator manages a cloud
# from inside, a SoLC member is installed from its package
DROP = {"localcloudconfigurator", "localcloudconfigurator-db"}
SECRET_MARKERS = ("PRIVATE KEY", "BEGIN CERTIFICATE")
# Changes of the reference scripts for hosts other than the reference Mac, applied while copying (each must match once):
# without a keychain (Linux) the console password goes to operator/console-password (0600 in a 0700 folder) instead of
# being printed once, and lib-secrets.sh reads it from there.
PATCHES = {
    # the kit writes compose.yaml as JSON, which the YAML pattern of write_files does not read: the Compose secret files are
    # written by kit/bin/prepare.py, which also generates the values; an empty secrets/ must not stop init.sh
    "scripts/rotate-secrets.sh": [
        ('  chmod 600 "$SECRETS_DIR"/*\n', '  chmod 600 "$SECRETS_DIR"/* 2>/dev/null || true\n'),
        # files are rewritten in place, never deleted: running containers bind-mount them (a re-run of install.sh)
        ('  find "$SECRETS_DIR" -mindepth 1 -maxdepth 1 -type f -delete\n', ''),
        ('    echo "Console password (shown once, not stored): $password"',
         '    mkdir -p "$ROOT_DIR/operator"; chmod 700 "$ROOT_DIR/operator"\n'
         '    printf \'%s\' "$password" > "$ROOT_DIR/operator/console-password"; chmod 600 "$ROOT_DIR/operator/console-password"\n'
         '    echo "Console password: in $ROOT_DIR/operator/console-password (mode 0600)"')],
    "scripts/lib-secrets.sh": [(
        '  if [ -z "$_secret_value" ]; then\n',
        '  if [ -z "$_secret_value" ] && [ "$1" = CONSOLE_PASSWORD ] && [ -f "$(dirname -- "$SECRETS_ENV_FILE")/operator/console-password" ]; then\n'
        '    _secret_value=$(cat "$(dirname -- "$SECRETS_ENV_FILE")/operator/console-password")\n'
        '  fi\n'
        '  if [ -z "$_secret_value" ]; then\n')],
}


def copy(src: pathlib.Path, dst: pathlib.Path) -> None:
    text = src.read_bytes()
    if any(m.encode() in text for m in SECRET_MARKERS):
        raise SystemExit(f"refusing to copy key or certificate material: {src}")
    rel = dst.relative_to(KIT).as_posix()
    for before, after in PATCHES.get(rel, []):
        t = text.decode()
        if t.count(before) != 1:
            raise SystemExit(f"patch of {rel} does not apply")
        text = t.replace(before, after).encode()
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_bytes(text)
    dst.chmod(0o755 if src.suffix in (".sh", ".py") else 0o644)


def template(ref: pathlib.Path) -> dict:
    compose = json.loads((ref / "v530/compose.json").read_text())
    project = compose["name"]
    registry, tag = None, None
    for s in compose["services"].values():
        m = re.match(r"(arrowhead-java-v\d+)/serviceregistry-java:(.+)$", s.get("image", ""))
        if m:
            registry, tag = m.group(1), m.group(2)
    if not registry:
        raise SystemExit("reference compose has no serviceregistry-java image")
    for name in DROP:
        compose["services"].pop(name, None)
    for s in compose["services"].values():
        s.get("labels", {}).pop("eu.arrowhead.primary-source", None)
        if not s.get("labels"):
            s.pop("labels", None)
        if s.get("image", "").startswith("arrowhead-random-data-producer:"):
            s["image"] = f"{registry}/random-data-producer:{tag}"
        s.pop("container_name", None)
    text = json.dumps(compose, indent=2)
    text = text.replace(str(ref) + "/", "@CLOUD_DIR@/").replace(f"{registry}/", "@REGISTRY@/").replace(f":{tag}\"", ":@TAG@\"")
    text = text.replace(project, "@PROJECT@")
    out = json.loads(text)
    # unused definitions after the drop
    used_vol = {v["source"] for s in out["services"].values() for v in s.get("volumes", []) if v.get("type") == "volume"}
    out["volumes"] = {k: v for k, v in out.get("volumes", {}).items() if k in used_vol}
    used_sec = {x["source"] for s in out["services"].values() for x in s.get("secrets", [])}
    out["secrets"] = {k: v for k, v in out.get("secrets", {}).items() if k in used_sec}
    used_net = {n for s in out["services"].values() for n in (s.get("networks") or {})}
    out["networks"] = {k: v for k, v in out.get("networks", {}).items() if k in used_net}
    leftover = re.findall(r"/(?:Users|home)/[^\"]+", json.dumps(out))
    if leftover:
        raise SystemExit(f"host paths left in the template: {leftover[:3]}")
    return out


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit(__doc__)
    ref = pathlib.Path(sys.argv[1]).resolve()
    if KIT.exists():
        shutil.rmtree(KIT)
    for f in sorted((ref / "scripts").iterdir()):
        if f.is_file() and not SCRIPT_EXCLUDE.search(f.name):
            copy(f, KIT / "scripts" / f.name)
    for d, names in COPY.items():
        for n in names:
            copy(ref / d / n, KIT / d / n)
    for f in sorted((ref / "security").glob("*.ext")):
        copy(f, KIT / "security" / f.name)
    for f in ("runtime/ServiceRegistry/bootstrap-proxy.conf", "runtime/Authentication/registry-mtls.conf"):
        if (ref / f).exists():
            copy(ref / f, KIT / f)
    copy(ref / ".env.example", KIT / ".env.example")
    copy(ref / "v530/core-adapter/Dockerfile", KIT / "images/core-adapter/Dockerfile")
    copy(ref / "v530/core-db/Dockerfile", KIT / "images/core-db/Dockerfile")
    copy(ref / "v530/provision-extra-identities.sh", KIT / "scripts/provision-extra-identities.sh")
    rdp = ref / "random-data-producer"
    for f in sorted(rdp.rglob("*")):
        rel = f.relative_to(rdp)
        if f.is_file() and ".before-" not in f.name and "__pycache__" not in rel.parts and f.name != ".DS_Store":
            copy(f, KIT / "images/random-data-producer" / rel)
    t = template(ref)
    (KIT / "templates").mkdir(parents=True, exist_ok=True)
    (KIT / "templates/compose-template.json").write_text(json.dumps(t, indent=2) + "\n")
    print(f"kit/cloud: {sum(1 for p in KIT.rglob('*') if p.is_file())} files, template with {len(t['services'])} services")


if __name__ == "__main__":
    main()
