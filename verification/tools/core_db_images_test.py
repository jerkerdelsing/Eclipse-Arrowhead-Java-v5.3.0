#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""Checks the five core database images of a release on empty volumes.

  core_db_images_test.py [--manifest verification/release-5.4.0/manifest.json]

For each core module (arrowhead-<module>-db image of the manifest, its platform) the image is started with throw-away
root and system-user passwords (AH_DB_USER, AH_DB_USER_PASSWORD_FILE as a file, as the test cloud passes them) on a new
volume and an internal network, and checked: the server is MySQL 8.4, the schema of the module exists with its tables,
the system user logs in with the deployment's password and no longer with the published default from
create_empty_db.sql, it cannot create a table (data access only), and no 'ah-operator' account exists. Everything is
removed afterwards. Result: <manifest dir>/core-db-images-test[-<arch>].json.
"""
from __future__ import annotations

import argparse
import json
import pathlib
import re
import secrets
import subprocess
import tempfile
import time

ROOT = pathlib.Path(__file__).resolve().parents[2]
MODULES = {"serviceregistry": "ah_serviceregistry", "authentication": "ah_authentication", "consumer-authorization": "ah_consumer_authorization",
           "serviceorchestration-dynamic": "ah_serviceorchestration_dynamic", "serviceorchestration-simple": "ah_serviceorchestration_simple"}


def sh(*cmd: str) -> subprocess.CompletedProcess:
    return subprocess.run(list(cmd), capture_output=True, text=True)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--manifest", type=pathlib.Path, default=ROOT / "verification/release-5.4.0/manifest.json")
    a = ap.parse_args()
    m = json.loads(a.manifest.read_text())
    platform = m.get("platform", "host")
    plat = ["--platform", platform] if platform != "host" else []
    arch = a.manifest.stem.removeprefix("manifest").lstrip("-")
    out = {"manifestCommit": m["commit"], "platform": platform, "images": {}}
    for module, schema in MODULES.items():
        image = next(e["image"] for e in m["images"] if e["image"].startswith(f"arrowhead-{module}-db:"))
        props = (ROOT / f"ah5-core-java-spring/{module}/src/main/resources/database/create_empty_db.sql").read_text()
        default = re.search(r"IDENTIFIED BY '([^']+)'", props).group(1)
        user = re.search(r"CREATE USER IF NOT EXISTS '([^']+)'@'%'", props).group(1)
        assert re.search(rf"CREATE DATABASE `{schema}`", props), f"{module}: schema name"
        tag = secrets.token_hex(3)
        net, name, vol = f"ah-cdb-{tag}", f"ah-cdb-{module}-{tag}", f"ah-cdb-{module}-{tag}"
        root_pw, user_pw = secrets.token_urlsafe(18), secrets.token_urlsafe(18)
        checks: dict[str, bool | str] = {}
        with tempfile.TemporaryDirectory(prefix="ah-cdb-", dir=pathlib.Path.home()) as tmp:
            pw_file = pathlib.Path(tmp) / "db-user-password"
            pw_file.write_text(user_pw)
            pw_file.chmod(0o644)
            try:
                sh("docker", "network", "create", "--internal", net)
                run = sh("docker", "run", "-d", *plat, "--name", name, "--network", net, "-v", f"{vol}:/var/lib/mysql", "-v", f"{tmp}:/run/secrets:ro",
                         "-e", f"MYSQL_ROOT_PASSWORD={root_pw}", "-e", f"AH_DB_USER={user}", "-e", "AH_DB_USER_PASSWORD_FILE=/run/secrets/db-user-password", image)
                checks["started"] = run.returncode == 0

                def q(sql: str, who: str = "root", pw: str = root_pw, db: str = "") -> subprocess.CompletedProcess:
                    return sh("docker", "exec", "-e", f"MYSQL_PWD={pw}", name, "mysql", "-h", "127.0.0.1", "-u", who, "-N", "-B", "-e", sql, *([db] if db else []))

                for _ in range(120):
                    if sh("docker", "inspect", "-f", "{{.State.Running}}", name).stdout.strip() != "true":
                        checks["initialised"] = "the container stopped: " + (sh("docker", "logs", "--tail", "3", name).stderr.strip()[-200:])
                        break
                    r = q(f"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='{schema}'")
                    if r.returncode == 0 and r.stdout.strip() not in ("", "0"):
                        logs = sh("docker", "logs", name)
                        if "ready for connections" in (logs.stdout + logs.stderr).split("init process done")[-1]:
                            break
                    time.sleep(2)
                version = q("SELECT VERSION()").stdout.strip()
                checks["mysql 8.4"] = version.startswith("8.4.")
                tables = q(f"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='{schema}'").stdout.strip()
                checks["schema with tables"] = tables.isdigit() and int(tables) > 0
                checks["user logs in with the deployment's password"] = q("SELECT 1", user, user_pw, schema).returncode == 0
                checks["published default password refused"] = q("SELECT 1", user, default, schema).returncode != 0
                checks["user may not create a table"] = q("CREATE TABLE ah_probe (i INT)", user, user_pw, schema).returncode != 0
                checks["no ah-operator account"] = q("SELECT COUNT(*) FROM mysql.user WHERE user='ah-operator'").stdout.strip() == "0"
                out["images"][image] = {"module": module, "schema": schema, "user": user, "mysql": version, "tables": tables, "checks": checks,
                                        "result": "PASS" if all(v is True for v in checks.values()) else "FAIL"}
            finally:
                sh("docker", "rm", "-f", name)
                sh("docker", "volume", "rm", vol)
                sh("docker", "network", "rm", net)
        print(out["images"][image]["result"], image, version, f"{tables} tables", [k for k, v in checks.items() if v is not True], flush=True)
    (a.manifest.parent / (f"core-db-images-test-{arch}.json" if arch else "core-db-images-test.json")).write_text(json.dumps(out, indent=2) + "\n")
    raise SystemExit(0 if all(r["result"] == "PASS" for r in out["images"].values()) else 1)


if __name__ == "__main__":
    main()
