#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""Runs the relay integration tests that the image builds skip, against the relay of Arrowhead-test-clouds/Arrowhead-relay.

  relay_integration_tests.py [--commit HEAD] [--relay-dir ../Arrowhead-test-clouds/Arrowhead-relay] [--out DIR]

Start the relay first (`make up` in the relay folder: test PKI and broker on the Docker network arrowhead-relay-network).
For each test the commit is exported (git archive) and `mvn test -Dtest=<class>` runs in the Maven build image on the
relay network, with the relay's test PKI mounted read-only:
  - Gatekeeper RelayRoundTripIT: two relay clients (TestCloud, OtherCloud) and an observer; topics, fields, dedup.
  - GateTunnel RelayRoundTripTest: AH-GT-1 session over the relay with the committed fixture identities.
Result: <out>/relay-integration-tests.json; logs in <out>/build-logs/ (not committed).
"""
from __future__ import annotations

import argparse
import json
import pathlib
import re
import subprocess
import tarfile
import tempfile
import time

ROOT = pathlib.Path(__file__).resolve().parents[2]
MAVEN_IMAGE = "maven:3.9-eclipse-temurin-21"
NETWORK = "arrowhead-relay-network"
TESTS = [
    ("ah5-Gatekeeper-java-spring", "RelayRoundTripIT",
     ["-Dgk.it.relay.host=arrowhead-relay", "-Dgk.it.relay.port=8883", "-Dgk.it.relay.dir=/relay/clients/gatekeeper"]),
    ("ah5-GateTunnel-java-spring", "RelayRoundTripTest",
     ["-Dgt.it.relay.host=arrowhead-relay", "-Dgt.it.relay.port=8883", "-Dgt.it.relay.ca=/relay/clients/gatetunnel/ca-bundle.crt",
      "-Dgt.it.relay.crl=/relay/clients/gatetunnel/relay-ca.crl"]),
]


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--commit", default="HEAD")
    ap.add_argument("--relay-dir", type=pathlib.Path, default=ROOT.parent / "Arrowhead-test-clouds/Arrowhead-relay")
    ap.add_argument("--out", type=pathlib.Path, default=ROOT / "verification/release-5.4.0")
    a = ap.parse_args()
    commit = subprocess.run(["git", "-C", str(ROOT), "rev-parse", a.commit], capture_output=True, text=True, check=True).stdout.strip()
    security = (a.relay_dir / "security").resolve()
    relay = subprocess.run(["docker", "ps", "--filter", f"network={NETWORK}", "--format", "{{.Image}} {{.Status}}"], capture_output=True, text=True).stdout.strip()
    if "healthy" not in relay:
        raise SystemExit(f"the relay is not running on {NETWORK}: run `make up` in {a.relay_dir}")
    logs = a.out / "build-logs"
    logs.mkdir(parents=True, exist_ok=True)
    results = {"commit": commit, "relay": {"image": relay.split()[0], "network": NETWORK, "host": "arrowhead-relay", "port": 8883,
                                           "pki": "Arrowhead-relay/scripts/generate-pki.sh (test PKI)"}, "mavenImage": MAVEN_IMAGE, "tests": {}}
    with tempfile.TemporaryDirectory(prefix="ah-relay-it-", dir=pathlib.Path.home()) as tmp:
        src = pathlib.Path(tmp).resolve()
        tar = src / "src.tar"
        tar.write_bytes(subprocess.run(["git", "-C", str(ROOT), "archive", "--format=tar", commit], capture_output=True, check=True).stdout)
        with tarfile.open(tar) as t:
            t.extractall(src, filter="data")
        tar.unlink()
        for folder, test, props in TESTS:
            log = logs / f"relay-it-{folder}.log"
            start = time.time()
            with log.open("w") as f:
                code = subprocess.run(["docker", "run", "--rm", "--network", NETWORK, "-v", f"{src / folder}:/src", "-v", f"{security}:/relay:ro",
                                       "-v", f"{pathlib.Path.home()}/.m2:/root/.m2", "-w", "/src", MAVEN_IMAGE, "mvn", "-B", "-o", "test",
                                       f"-Dtest={test}", "-Dsurefire.failIfNoSpecifiedTests=false", *props], stdout=f, stderr=subprocess.STDOUT).returncode
            text = log.read_text(errors="replace")
            summary = [re.sub(r"^.*?(Tests run:)", r"\1", l).strip() for l in text.splitlines() if "Tests run:" in l and " -- in " not in l]
            ok = code == 0 and bool(summary) and "Skipped: 0" in summary[-1] and not summary[-1].startswith("Tests run: 0")
            results["tests"][f"{folder}:{test}"] = {"result": "PASS" if ok else "FAIL", "tests": summary[-1] if summary else None,
                                                    "seconds": round(time.time() - start), "log": f"build-logs/{log.name}"}
            print("PASS" if ok else "FAIL", folder, test, summary[-1] if summary else "(no test summary)", flush=True)
    (a.out / "relay-integration-tests.json").write_text(json.dumps(results, indent=2) + "\n")
    raise SystemExit(0 if all(t["result"] == "PASS" for t in results["tests"].values()) else 1)


if __name__ == "__main__":
    main()
