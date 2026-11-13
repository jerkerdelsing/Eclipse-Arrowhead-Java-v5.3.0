#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""Compares the images of two platform builds of a release (release_manifest.py, manifest.json and manifest-<arch>.json).

  compare_platforms.py [--arch amd64] [--out verification/release-5.3.0]

For every image in both manifests (same commit required): the architecture recorded by Docker, the user, and for
application images the packaged JARs entry by entry (content SHA-256 of every file in the JAR and in the JARs nested in
it, so differing timestamps and compression do not count). Each image of the other platform is also started once
under that platform (`java -version`, no network). Result: <out>/platform-comparison-<arch>.json.
"""
from __future__ import annotations

import argparse
import hashlib
import io
import json
import pathlib
import subprocess
import tempfile
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]


def entries(data: bytes, prefix: str = "") -> dict[str, str]:
    out = {}
    with zipfile.ZipFile(io.BytesIO(data)) as z:
        for info in z.infolist():
            if info.is_dir():
                continue
            content = z.read(info)
            if info.filename.endswith(".jar"):
                out.update(entries(content, prefix + info.filename + "!/"))
            else:
                out[prefix + info.filename] = hashlib.sha256(content).hexdigest()
    return out


def jars(image: str, platform: str | None, tmp: pathlib.Path) -> dict[str, dict[str, str]]:
    cid = subprocess.run(["docker", "create", *(["--platform", platform] if platform else []), image], capture_output=True, text=True, check=True).stdout.strip()
    try:
        listing = subprocess.run(["docker", "export", cid], capture_output=True, check=True).stdout
        import tarfile
        out = {}
        with tarfile.open(fileobj=io.BytesIO(listing)) as t:
            for m in t.getmembers():
                if m.isfile() and m.name.startswith("app/") and m.name.endswith(".jar") and m.name.count("/") == 1:
                    out[m.name] = entries(t.extractfile(m).read())
        return out
    finally:
        subprocess.run(["docker", "rm", cid], capture_output=True)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--arch", default="amd64")
    ap.add_argument("--out", type=pathlib.Path, default=ROOT / "verification/release-5.3.0")
    a = ap.parse_args()
    host = json.loads((a.out / "manifest.json").read_text())
    other = json.loads((a.out / f"manifest-{a.arch}.json").read_text())
    if host["commit"] != other["commit"]:
        raise SystemExit("the two manifests are of different commits")
    platform = other["platform"]
    by_name = {e["image"]: e for e in host["images"]}
    report = {"commit": host["commit"], "host": host["host"], "platform": platform, "images": {}}
    with tempfile.TemporaryDirectory() as tmp:
        for e in other["images"]:
            base = e["image"].removesuffix(f"-{a.arch}")
            h = by_name.get(base)
            r = {"hostImage": base, "hostArchitecture": h and h.get("architecture"), "architecture": e.get("architecture"),
                 "sameUser": h is not None and h.get("user") == e.get("user")}
            if h is None or e["result"] != "PASS":
                r["result"] = "MISSING" if h is None else "BUILD FAILED"
            else:
                hj, oj = jars(base, None, pathlib.Path(tmp)), jars(e["image"], platform, pathlib.Path(tmp))
                diffs = []
                for name in sorted(set(hj) | set(oj)):
                    he, oe = hj.get(name, {}), oj.get(name, {})
                    diffs += [f"{name}:{k}" for k in sorted(set(he) | set(oe)) if he.get(k) != oe.get(k)]
                r |= {"jars": sorted(oj), "entriesCompared": sum(len(v) for v in oj.values()), "differingEntries": diffs[:20], "differing": len(diffs)}
                if oj:
                    run = subprocess.run(["docker", "run", "--rm", "--network", "none", "--platform", platform, "--entrypoint", "java", e["image"], "-version"],
                                         capture_output=True, text=True)
                    r["javaStart"] = (run.stderr.splitlines() or ["?"])[0] if run.returncode == 0 else f"FAIL {run.returncode}"
                ok = r["architecture"] == a.arch and r["sameUser"] and not diffs and (not oj or not r["javaStart"].startswith("FAIL"))
                r["result"] = "PASS" if ok else "FAIL"
            report["images"][e["image"]] = r
            print(r["result"], e["image"], r.get("entriesCompared", ""), r.get("differing", ""), flush=True)
    report["summary"] = {"images": len(report["images"]), "pass": sum(r["result"] == "PASS" for r in report["images"].values())}
    (a.out / f"platform-comparison-{a.arch}.json").write_text(json.dumps(report, indent=2) + "\n")
    print(report["summary"])
    raise SystemExit(0 if report["summary"]["pass"] == report["summary"]["images"] else 1)


if __name__ == "__main__":
    main()
