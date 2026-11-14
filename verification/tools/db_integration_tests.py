#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""Runs the MySQL integration tests that the image builds skip, against the database images of a release.

  db_integration_tests.py [--commit HEAD] [--release 5.4.0] [--out verification/release-<release>]

For each system with a MySqlEndToEndTest: exports the commit (git archive), starts the system's own database image
(<image>-db:<release>, built by release_manifest.py) on a throw-away Docker network with random passwords, waits until
the restricted application user can log in, runs `mvn test -Dtest=MySqlEndToEndTest` in the Maven build image on that
network, and removes the container and the network. The passwords exist only in this process and the containers.
Result: <out>/db-integration-tests.json; logs in <out>/build-logs/ (not committed).
"""
from __future__ import annotations

import argparse
import json
import pathlib
import re
import secrets
import subprocess
import tarfile
import tempfile
import time

ROOT = pathlib.Path(__file__).resolve().parents[2]
MAVEN_IMAGE = "maven:3.9-eclipse-temurin-21"
# folder, system property prefix, image prefix
SYSTEMS = [("ah5-CertificateAuthority-java-spring", "ca", "arrowhead-certificate-authority-standalone"),
           ("ah5-Gatekeeper-java-spring", "gk", "arrowhead-gatekeeper-standalone"),
           ("ah5-GateTunnel-java-spring", "gt", "arrowhead-gatetunnel-standalone"),
           ("ah5-LocalCloudConfigurator-java", "lcc", "arrowhead-localcloudconfigurator-standalone"),
           ("ah5-SoSConfigurator-java", "sos", "arrowhead-sosconfigurator-standalone"),
           ("ah5-SysMonitor-java-spring", "sysmonitor", "arrowhead-sysmonitor-standalone")]


def run(*cmd: str, **kw) -> subprocess.CompletedProcess:
    return subprocess.run(list(cmd), capture_output=True, text=True, **kw)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--commit", default="HEAD")
    ap.add_argument("--release", default="5.4.0")
    ap.add_argument("--out", type=pathlib.Path, default=None)
    a = ap.parse_args()
    commit = run("git", "-C", str(ROOT), "rev-parse", a.commit, check=True).stdout.strip()
    out = a.out or ROOT / f"verification/release-{a.release}"
    logs = out / "build-logs"
    logs.mkdir(parents=True, exist_ok=True)
    results = {"commit": commit, "release": a.release, "mavenImage": MAVEN_IMAGE, "systems": {}}
    with tempfile.TemporaryDirectory(prefix="ah-it-", dir=pathlib.Path.home()) as tmp:
        src = pathlib.Path(tmp).resolve()
        tar = src / "src.tar"
        tar.write_bytes(subprocess.run(["git", "-C", str(ROOT), "archive", "--format=tar", commit], capture_output=True, check=True).stdout)
        with tarfile.open(tar) as t:
            t.extractall(src, filter="data")
        tar.unlink()
        for folder, prefix, image in SYSTEMS:
            props = (src / folder / "src/main/resources/application.properties").read_text()
            schema = re.search(r"^spring\.datasource\.url=.*/(\w+)\?", props, re.M).group(1)
            user = re.search(r"^spring\.datasource\.username=(\S+)", props, re.M).group(1)
            db_image = f"{image}-db:{a.release}"
            net, db = f"ah-it-{prefix}-{secrets.token_hex(3)}", f"ah-it-{prefix}-db"
            password = secrets.token_urlsafe(18)
            log = logs / f"it-{folder}.log"
            entry = {"databaseImage": db_image, "databaseImageId": run("docker", "image", "inspect", "-f", "{{.Id}}", db_image).stdout.strip(),
                     "schema": schema, "user": user}
            start = time.time()
            try:
                run("docker", "network", "create", "--internal", net, check=True)
                run("docker", "run", "-d", "--name", db, "--network", net, "--network-alias", "db",
                    "-e", f"MYSQL_ROOT_PASSWORD={secrets.token_urlsafe(18)}", "-e", f"MYSQL_DATABASE={schema}",
                    "-e", f"MYSQL_USER={user}", "-e", f"MYSQL_PASSWORD={password}", db_image, check=True)
                for _ in range(120):
                    ping = run("docker", "exec", "-e", f"MYSQL_PWD={password}", db, "mysql", "-u", user, "-h", "127.0.0.1", "-e", f"SELECT 1 FROM information_schema.tables WHERE table_schema='{schema}' LIMIT 1", schema)
                    if ping.returncode == 0 and "1" in ping.stdout:
                        break
                    time.sleep(2)
                else:
                    raise RuntimeError("database did not become ready")
                time.sleep(3)  # the entrypoint restarts mysqld once after the init scripts
                url = f"jdbc:mariadb://db:3306/{schema}?sslMode=trust"
                with log.open("w") as f:
                    code = subprocess.run(["docker", "run", "--rm", "--network", net, "-v", f"{src / folder}:/src", "-v", f"{pathlib.Path.home()}/.m2:/root/.m2",
                                           "-w", "/src", MAVEN_IMAGE, "mvn", "-B", "-o", "test", "-Dtest=MySqlEndToEndTest", "-Dsurefire.failIfNoSpecifiedTests=false",
                                           f"-D{prefix}.it.db.url={url}", f"-D{prefix}.it.db.password={password}"], stdout=f, stderr=subprocess.STDOUT).returncode
                text = log.read_text(errors="replace")
                summary = [re.sub(r"^.*?(Tests run:)", r"\1", l).strip() for l in text.splitlines() if "Tests run:" in l and " -- in " not in l]
                entry |= {"result": "PASS" if code == 0 and summary and "Skipped: 0" in summary[-1] else "FAIL", "tests": summary[-1] if summary else None}
            except subprocess.CalledProcessError as ex:  # the command line holds the passwords: record only the step
                entry |= {"result": "FAIL", "error": f"{ex.cmd[1]} {ex.cmd[2]} failed ({ex.returncode}): {(ex.stderr or '').strip()[:200]}"}
            except Exception as ex:  # noqa: BLE001 - recorded in the result
                entry |= {"result": "FAIL", "error": str(ex)}
            finally:
                run("docker", "rm", "-f", db)
                run("docker", "network", "rm", net)
            entry |= {"seconds": round(time.time() - start), "log": f"build-logs/{log.name}"}
            results["systems"][folder] = entry
            print(entry["result"], folder, entry.get("tests") or entry.get("error"), flush=True)
    (out / "db-integration-tests.json").write_text(json.dumps(results, indent=2) + "\n")
    raise SystemExit(0 if all(e["result"] == "PASS" for e in results["systems"].values()) else 1)


if __name__ == "__main__":
    main()
