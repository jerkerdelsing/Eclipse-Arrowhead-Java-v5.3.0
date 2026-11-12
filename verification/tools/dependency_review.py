#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""Known-vulnerability review of the runtime Maven dependencies recorded in a release manifest.

  dependency_review.py [--manifest verification/release-5.3.0/manifest.json]

Queries the public OSV database (https://api.osv.dev, querybatch) with the Maven coordinates (group:artifact and
version) of every runtime dependency; nothing else is sent. Writes dependency-review.json next to the manifest: per
vulnerable package its version, the OSV/GHSA/CVE ids, severity where OSV gives one, the fixed versions, and which
projects use it. Exit status 1 when any vulnerability is found. Base-image (OS package) vulnerabilities are not
covered; the runtime images apply the distribution's security updates at build time.
"""
from __future__ import annotations

import argparse
import json
import pathlib
import urllib.request

API = "https://api.osv.dev/v1"


def post(path: str, body: dict) -> dict:
    req = urllib.request.Request(API + path, data=json.dumps(body).encode(), headers={"Content-Type": "application/json", "User-Agent": "dependency-review"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.load(r)


def get(path: str) -> dict:
    with urllib.request.urlopen(urllib.request.Request(API + path, headers={"User-Agent": "dependency-review"}), timeout=60) as r:
        return json.load(r)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--manifest", type=pathlib.Path, default=pathlib.Path(__file__).resolve().parents[1] / "release-5.3.0/manifest.json")
    a = ap.parse_args()
    m = json.loads(a.manifest.read_text())
    users: dict[tuple[str, str], set[str]] = {}
    for project, d in m["dependencies"].items():
        for coord in d["runtime"]:
            p = coord.split(":")
            users.setdefault((f"{p[0]}:{p[1]}", p[-2]), set()).add(project)
    pkgs = sorted(users)
    results = []
    for i in range(0, len(pkgs), 500):
        chunk = pkgs[i:i + 500]
        res = post("/querybatch", {"queries": [{"package": {"ecosystem": "Maven", "name": n}, "version": v} for n, v in chunk]})
        results += list(zip(chunk, res["results"]))
    findings = []
    for (name, version), r in results:
        for v in r.get("vulns", []):
            d = get(f"/vulns/{v['id']}")
            sev = (d.get("database_specific") or {}).get("severity")
            fixed = sorted({e["fixed"] for aff in d.get("affected", []) if aff.get("package", {}).get("name") == name
                            for rng in aff.get("ranges", []) for e in rng.get("events", []) if "fixed" in e})
            findings.append({"package": name, "version": version, "id": d["id"], "aliases": d.get("aliases", []), "severity": sev,
                             "summary": d.get("summary", "")[:200], "fixed": fixed, "projects": sorted(users[(name, version)])})
    out = {"manifestCommit": m["commit"], "source": "OSV querybatch, Maven ecosystem", "packages": len(pkgs), "findings": findings}
    (a.manifest.parent / "dependency-review.json").write_text(json.dumps(out, indent=2) + "\n")
    print(f"{len(pkgs)} packages, {len(findings)} vulnerabilities")
    for f in findings:
        print(f"  {f['severity'] or '?':8} {f['package']}:{f['version']} {f['id']} {','.join(x for x in f['aliases'] if x.startswith('CVE'))} fixed={f['fixed'][-3:]} in {len(f['projects'])} projects")
    raise SystemExit(1 if findings else 0)


if __name__ == "__main__":
    main()
