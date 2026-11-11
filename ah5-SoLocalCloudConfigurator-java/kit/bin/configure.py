#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""Turn the portable kit into the deployment of one member cloud on this host.

  configure.py <package-dir> <cloud.json> <work-dir>

Copies the kit's scripts and configuration into <work-dir> (the cloud's folder on this host; state such as .env,
secrets/ and security/ is never overwritten) and adapts them to the cloud: its name and organisation replace the
reference cloud's (TestCloud, Company), its Compose project the reference project, and every published port is moved by
the cloud's port offset (127.0.0.1:<port> and the *_PORT settings). Writes <work-dir>/compose.yaml (JSON, which Compose
reads as YAML) from the template with only the services of the cloud's components and what they depend on, its own
private subnets, its own image tag, and the federation settings (relay address, peers, organisation CRL) once the
federation material is imported (<work-dir>/federation/imported). Re-run after an import; it is idempotent.
"""
from __future__ import annotations

import json
import os
import pathlib
import re
import shutil
import sys

REFERENCE_CLOUD, REFERENCE_ORG = "TestCloud", "Company"
REGISTRY = "arrowhead-solc"
CORE_SYSTEMS = {"serviceregistry": "ServiceRegistry", "authentication": "Authentication", "consumerauthorization": "ConsumerAuthorization",
                "serviceorchestration-dynamic": "DynamicServiceOrchestration", "serviceorchestration-simple": "SimpleStoreServiceOrchestration"}
STATE = (".env", ".env.previous", "secrets", "security/x509", "security/ca", "security/crl", "security/bundles", "security/primary-stores",
         "security/manufacturer-crl", "security/org-crl", "security/peer-crl", "runtime", "federation", "operator", "out", "logs")
TEXT = (".sh", ".py", ".ext", ".conf", ".config", ".sql", ".example", "acl", "enabled_plugins", ".txt", ".json", "Dockerfile")


def load(p: pathlib.Path) -> dict:
    return json.loads(p.read_text())


def is_text(p: pathlib.Path) -> bool:
    return p.name.endswith(TEXT) or p.name in TEXT


def remap_ports(text: str, ports: set[int], offset: int) -> str:
    if offset == 0:
        return text

    def sub(m: re.Match) -> str:
        p = int(m.group(2))
        return m.group(1) + str(p + offset if p in ports else p)
    for pattern in (r"(127\.0\.0\.1:)(\d{4})\b", r"(localhost:)(\d{4})\b", r"(:-)(\d{4})(?=\})", r"(_PORT=)(\d{4})\b", r"(_port:-)(\d{4})\b",
                    r"(\bPORT:-)(\d{4})\b"):
        text = re.sub(pattern, sub, text)
    return text


def substitute(text: str, cloud: dict, project: str, ports: set[int]) -> str:
    text = re.sub(r"\b" + REFERENCE_CLOUD + r"\b", cloud["cloud"], text)
    text = text.replace(REFERENCE_CLOUD.lower() + ".localhost", cloud["cloud"].lower() + ".localhost")
    text = re.sub(r"\b" + REFERENCE_ORG + r"\b", cloud["organization"], text)
    text = text.replace("arrowhead-v53-test-cloud", project)
    return remap_ports(text, ports, cloud["portOffset"])


def install_files(kit: pathlib.Path, work: pathlib.Path, cloud: dict, project: str, ports: set[int]) -> int:
    n = 0
    for src in sorted(kit.rglob("*")):
        rel = src.relative_to(kit)
        if src.is_dir() or rel.parts[0] in ("templates", "images", "bin") or rel.name in (".DS_Store", "lc-catalog.json"):
            continue
        dst = work / rel
        if any(str(rel) == s or str(rel).startswith(s + "/") for s in STATE):
            continue
        dst.parent.mkdir(parents=True, exist_ok=True)
        if is_text(src):
            dst.write_text(substitute(src.read_text(), cloud, project, ports))
        else:
            shutil.copyfile(src, dst)
        dst.chmod(0o755 if src.suffix in (".sh", ".py") else 0o644)
        n += 1
    return n


def write_env(kit: pathlib.Path, work: pathlib.Path, cloud: dict, project: str, ports: set[int]) -> None:
    env = work / ".env"
    wanted = {}
    for line in substitute((kit / ".env.example").read_text(), cloud, project, ports).splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            k, v = line.split("=", 1)
            wanted[k] = v
    wanted["COMPOSE_PROJECT_NAME"] = project
    wanted["DOCKER_PLATFORM"] = "linux/" + cloud["architecture"]
    wanted["ARROWHEAD_CLOUD_NAME"] = cloud["cloud"]
    wanted["ARROWHEAD_ORGANIZATION"] = cloud["organization"]
    lines = env.read_text().splitlines() if env.exists() else [l for l in (kit / ".env.example").read_text().splitlines() if l.lstrip().startswith("#")]
    seen = set()
    out = []
    for line in lines:
        k = line.split("=", 1)[0] if "=" in line and not line.lstrip().startswith("#") else None
        if k in wanted:
            out.append(f"{k}={wanted[k]}")
            seen.add(k)
        else:
            out.append(line)
    out += [f"{k}={v}" for k, v in wanted.items() if k not in seen]
    env.write_text("\n".join(out) + "\n")
    env.chmod(0o600)


def closure(services: dict, wanted: set[str]) -> set[str]:
    out, todo = set(), list(wanted)
    while todo:
        s = todo.pop()
        if s in out or s not in services:
            continue
        out.add(s)
        todo += list((services[s].get("depends_on") or {}).keys())
    return out


def compose(kit: pathlib.Path, pkg: pathlib.Path, work: pathlib.Path, cloud: dict, project: str) -> tuple[dict, list[str]]:
    text = (kit / "templates/compose-template.json").read_text()
    text = text.replace("@CLOUD_DIR@", str(work)).replace("@PROJECT@", project).replace("@REGISTRY@", REGISTRY).replace("@TAG@", cloud["imageTag"])
    t = json.loads(substitute(text, {**cloud, "portOffset": 0}, project, set()))
    catalog = load(pkg / "kit/lc-catalog.json")
    by_id = {c["id"]: c for c in catalog.get("systems", [])}
    wanted, missing = set(), []
    for comp in cloud["components"]:
        for svc in by_id.get(comp, {}).get("services", []) + by_id.get(comp, {}).get("internalAssets", []):
            if svc in t["services"]:
                wanted.add(svc)
            else:
                missing.append(f"{comp}:{svc}")
    keep = closure(t["services"], wanted)
    services = {k: v for k, v in t["services"].items() if k in keep}
    offset = cloud["portOffset"]
    # host settings decided by install-cloud.sh (work/host-settings.json), so that every later configure.py keeps them
    settings_file = work / "host-settings.json"
    settings = json.loads(settings_file.read_text()) if settings_file.exists() else {}
    if os.environ.get("SOLC_DB_NATIVE_AIO") in ("on", "off"):
        settings["dbNativeAio"] = os.environ["SOLC_DB_NATIVE_AIO"] == "on"
        settings_file.write_text(json.dumps(settings, indent=2) + "\n")
    native_aio = settings.get("dbNativeAio", True)
    federated = (work / "federation/imported").exists()
    peers = cloud.get("peers", []) if federated else []
    relay = cloud.get("relay") or {}
    for name, s in services.items():
        # every image is for the member's architecture (the reference cloud pinned some services to linux/amd64)
        s["platform"] = "linux/" + cloud["architecture"]
        for p in s.get("ports", []):
            p["published"] = str(int(p["published"]) + offset)
        if "depends_on" in s:
            s["depends_on"] = {d: c for d, c in s["depends_on"].items() if d in services}
        if name.endswith("-db") and not native_aio and isinstance(s.get("command"), list) and s["command"][:1] == ["mysqld"]:
            # too few Linux AIO contexts left on this Docker host (fs.aio-max-nr): MySQL then uses its own I/O threads
            s["command"] = [c for c in s["command"] if not c.startswith("--innodb-use-native-aio")] + ["--innodb-use-native-aio=OFF"]
        env = s.get("environment")
        if not isinstance(env, dict):
            continue
        if name in CORE_SYSTEMS:
            # the core image adapter names its key alias after arrowhead.cloud.name (default: the reference cloud), and
            # the organisation is part of every certificate name
            env["ARROWHEAD_CLOUD_NAME"] = cloud["cloud"]
            env["SERVER_SSL_KEY_ALIAS"] = f"{CORE_SYSTEMS[name]}.{cloud['cloud']}.{cloud['organization']}.arrowhead.eu"
        if name == "cloud-monitor-api":
            env["CONSOLE_ALLOWED_HOSTS"] = f"127.0.0.1,localhost,[::1],{cloud['cloud'].lower()}.localhost"
            env["LOCAL_CLOUD_CONFIGURATION_ENABLED"] = "false"
        if name == "serviceorchestration-dynamic":
            env["ENABLE_INTERCLOUD"] = "true" if federated else "false"
        if name == "random-data-producer":
            env["INTERCLOUD_ENABLED"] = "true" if federated and "RandomDataProducer" in cloud.get("intercloudConsumers", []) else "false"
        if name in ("gatekeeper", "gatetunnel"):
            s.setdefault("extra_hosts", ["host.docker.internal:host-gateway"])
            env["RELAY_HOST"] = relay.get("host", "arrowhead-relay") if federated else "arrowhead-relay"
            env["RELAY_PORT"] = str(relay.get("port", 8883)) if federated else "8883"
            env["FEDERATION_PEER_ORGANIZATIONS"] = ""
            env["FEDERATION_FOREIGN_TARGETS"] = ""
            org_crl = "/org-crl/org-ca.crl" if federated else "/org-crl/ca.crl"
            env["RELAY_CRL_FILE"] = org_crl
            for v in s.get("volumes", []):
                if v.get("target") == "/org-crl":
                    v["source"] = str(work / ("security/org-crl" if federated else "security/crl"))
            if name == "gatekeeper":
                env["GATEKEEPER_RELAY_ENABLED"] = "true" if federated else "false"
            else:
                env["NEIGHBOR_CLOUDS"] = ",".join(peers)
                env["RELAY_CRL_FILES"] = ",".join([org_crl] + [f"/peer-crl/{p}/ca.crl" for p in peers])
                s["volumes"] = [v for v in s.get("volumes", []) if not v.get("target", "").startswith("/peer-crl")]
                for p in peers:
                    s["volumes"].append({"type": "bind", "source": str(work / "security/peer-crl" / p), "target": f"/peer-crl/{p}",
                                         "read_only": True, "bind": {"create_host_path": False}})
    # private subnets of this cloud: 10.<200 + offset/1000>.x.0/28, so clouds on one host never collide
    second = 200 + offset // 1000
    nets = {}
    used = {n for s in services.values() for n in (s.get("networks") or {})}
    for k, v in t["networks"].items():
        if k not in used:
            continue
        v = json.loads(json.dumps(v))
        for c in (v.get("ipam") or {}).get("config", []) or []:
            if "subnet" in c:
                if not c["subnet"].startswith("10.253."):
                    raise SystemExit(f"unexpected subnet {c['subnet']} in the template")
                c["subnet"] = c["subnet"].replace("10.253.", f"10.{second}.", 1)
        nets[k] = v
    vols = {v["source"] for s in services.values() for v in s.get("volumes", []) if v.get("type") == "volume"}
    secs = {x["source"] for s in services.values() for x in s.get("secrets", [])}
    out = {"name": project, "services": dict(sorted(services.items())), "networks": nets,
           "volumes": {k: v for k, v in t.get("volumes", {}).items() if k in vols},
           "secrets": {k: v for k, v in t.get("secrets", {}).items() if k in secs}}
    return out, missing


def main() -> None:
    if len(sys.argv) != 4:
        raise SystemExit(__doc__)
    pkg, cloud_file, work = (pathlib.Path(a).resolve() for a in sys.argv[1:])
    cloud = load(cloud_file)
    if not re.fullmatch(r"[A-Za-z][A-Za-z0-9]{0,62}", cloud["cloud"]) or not re.fullmatch(r"[A-Za-z][A-Za-z0-9]{0,62}", cloud["organization"]):
        raise SystemExit("cloud.json: invalid cloud or organization name")
    project = cloud["composeProject"]
    if not re.fullmatch(r"[a-z0-9][a-z0-9_-]{0,62}", project):
        raise SystemExit("cloud.json: invalid composeProject")
    kit = pkg / "kit"
    work.mkdir(parents=True, exist_ok=True)
    os.chmod(work, 0o700)
    t = load(kit / "templates/compose-template.json")
    ports = {int(p["published"]) for s in t["services"].values() for p in s.get("ports", [])}
    n = install_files(kit, work, cloud, project, ports)
    write_env(kit, work, cloud, project, ports)
    c, missing = compose(kit, pkg, work, cloud, project)
    target = work / "compose.yaml"
    target.write_text(json.dumps(c, indent=2) + "\n")
    target.chmod(0o600)
    (work / "cloud.json").write_text(json.dumps(cloud, indent=2) + "\n")
    published = sorted(int(p["published"]) for s in c["services"].values() for p in s.get("ports", []))
    print(f"{cloud['cloud']}: {n} kit files, {len(c['services'])} services, ports {published[0]}-{published[-1]}, "
          f"federation {'imported' if (work / 'federation/imported').exists() else 'not imported'} -> {target}")
    for m in missing:
        print(f"  note: component service {m} is not part of the kit template and is left out")


if __name__ == "__main__":
    main()
