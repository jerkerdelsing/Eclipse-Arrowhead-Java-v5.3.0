#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""Source-to-image manifest of a release commit.

  release_manifest.py [--commit REV] [--jobs N] [--out DIR] [--only NAME ...] [--skip-tests] [--skip-dependencies]

Exports the commit with `git archive` into a temporary directory, so every build input is exactly the committed
source (no working-tree changes, no target/ leftovers), then:
  - builds every image defined by the support folders' compose.yaml build sections, SoSConfigurator's Dockerfile and
    database Dockerfile (it has no compose.yaml), and
    the five core modules' docker/Dockerfile-* files, each with `docker build --no-cache --pull=false`;
  - runs the common-library and core test suites (the core image builds skip tests) in the Maven build image;
  - lists the resolved Maven dependencies of every project.
Result: <out>/manifest.json (commit, tree hashes, per image: source folder and its git tree hash, Dockerfile, pinned
base images, image id, size, jar checksums, test summary, build log checksum) and <out>/build-logs/ (not committed).
Images are not bit-for-bit reproducible (timestamps, package upgrades in the runtime layer); the manifest pins what
was built from which source, so a rebuild can be compared jar by jar.
"""
from __future__ import annotations

import argparse
import concurrent.futures
import datetime
import hashlib
import json
import pathlib
import re
import subprocess
import tarfile
import tempfile
import time

import yaml

ROOT = pathlib.Path(__file__).resolve().parents[2]
MAVEN_IMAGE = "maven:3.9-eclipse-temurin-21"
CORE = ["serviceregistry", "authentication", "consumer-authorization", "serviceorchestration-dynamic", "serviceorchestration-simple"]


def git(*args: str) -> str:
    return subprocess.run(["git", "-C", str(ROOT), *args], capture_output=True, text=True, check=True).stdout.strip()


def sha256(path: pathlib.Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def pom_version(pom: pathlib.Path) -> str | None:
    text = re.sub(r"<parent>.*?</parent>", "", pom.read_text(), flags=re.S)
    m = re.search(r"<version>([^<$]+)</version>", text)
    return m.group(1) if m else None


def base_images(dockerfile: pathlib.Path) -> list[str]:
    stages = set()
    out = []
    for line in dockerfile.read_text().splitlines():
        m = re.match(r"\s*FROM\s+(\S+)(?:\s+AS\s+(\S+))?", line, re.I)
        if m:
            if m.group(1) not in stages:
                out.append(m.group(1))
            if m.group(2):
                stages.add(m.group(2))
    return out


def targets(src: pathlib.Path, version: str) -> list[dict]:
    out = []
    for folder in sorted(p for p in src.glob("ah5-*") if p.is_dir()):
        compose = folder / "compose.yaml"
        if compose.exists():
            for name, svc in yaml.safe_load(compose.read_text())["services"].items():
                b = svc.get("build")
                if not b:
                    continue
                b = {"context": b} if isinstance(b, str) else b
                context = (folder / b.get("context", ".")).resolve()
                out.append({"name": svc["image"].split(":")[0], "image": svc["image"], "source": folder.name, "service": name,
                            "context": context, "dockerfile": context / b.get("dockerfile", "Dockerfile"), "args": {}})
        elif (folder / "Dockerfile").exists():
            slug = folder.name.removeprefix("ah5-").removesuffix("-java").removesuffix("-java-spring").lower()
            out.append({"name": f"arrowhead-{slug}-standalone", "image": f"arrowhead-{slug}-standalone:{version}", "source": folder.name,
                        "service": slug, "context": folder, "dockerfile": folder / "Dockerfile", "args": {}})
            if (folder / "deployment/database/Dockerfile").exists():
                out.append({"name": f"arrowhead-{slug}-standalone-db", "image": f"arrowhead-{slug}-standalone-db:{version}", "source": folder.name,
                            "service": slug + "-db", "context": folder, "dockerfile": folder / "deployment/database/Dockerfile", "args": {}})
    for module in CORE:
        docker = src / "ah5-core-java-spring" / module / "docker"
        for df in sorted(docker.glob("Dockerfile-*")):
            db = df.name.endswith("-DB")
            out.append({"name": f"arrowhead-{module}" + ("-db" if db else ""), "image": f"arrowhead-{module}" + ("-db" if db else "") + f":{version}",
                        "source": f"ah5-core-java-spring/{module}", "service": module + ("-db" if db else ""), "context": src,
                        "dockerfile": df, "args": {} if db else {"AH_VERSION": version}})
    return out


def build(t: dict, src: pathlib.Path, logs: pathlib.Path, commit: str) -> dict:
    log = logs / (t["name"] + ".log")
    cmd = ["docker", "build", "--no-cache", "--progress=plain", "-t", t["image"], "-f", str(t["dockerfile"]),
           "--label", f"org.opencontainers.image.revision={commit}"]
    for k, v in t["args"].items():
        cmd += ["--build-arg", f"{k}={v}"]
    start = time.time()
    with log.open("w") as out:
        code = subprocess.run(cmd + [str(t["context"])], stdout=out, stderr=subprocess.STDOUT).returncode
    text = log.read_text(errors="replace")
    tests = [re.sub(r"^.*?(Tests run:)", r"\1", l).strip() for l in text.splitlines() if "Tests run:" in l and "Time elapsed" not in l and " -- in " not in l]
    entry = {"image": t["image"], "result": "PASS" if code == 0 else "FAIL", "seconds": round(time.time() - start),
             "source": t["source"], "sourceTree": git("rev-parse", f"{commit}:{t['source']}"), "service": t["service"],
             "dockerfile": str(t["dockerfile"].relative_to(src)), "context": str(t["context"].relative_to(src)) or ".",
             "buildArgs": t["args"], "baseImages": base_images(t["dockerfile"]), "tests": tests[-1] if tests else None,
             "buildLog": {"file": f"build-logs/{log.name}", "sha256": sha256(log)}}
    if code == 0:
        info = json.loads(subprocess.run(["docker", "image", "inspect", t["image"]], capture_output=True, text=True, check=True).stdout)[0]
        entry |= {"imageId": info["Id"], "created": info["Created"], "sizeBytes": info["Size"], "architecture": info["Architecture"],
                  "user": info["Config"].get("User") or None}
        jars = subprocess.run(["docker", "run", "--rm", "--network", "none", "--entrypoint", "sh", t["image"], "-c",
                               "sha256sum /app/*.jar 2>/dev/null"], capture_output=True, text=True).stdout
        entry["jars"] = {line.split()[1]: line.split()[0] for line in jars.splitlines() if len(line.split()) == 2}
    print(entry["result"], t["image"], entry.get("tests") or "", f"{entry['seconds']}s", flush=True)
    return entry


def maven(src: pathlib.Path, workdir: str, goals: list[str], log: pathlib.Path) -> int:
    cmd = ["docker", "run", "--rm", "-v", f"{src}:/src", "-v", f"{pathlib.Path.home()}/.m2:/root/.m2", "-w", f"/src/{workdir}", MAVEN_IMAGE, "mvn", "-B", *goals]
    with log.open("w") as out:
        return subprocess.run(cmd, stdout=out, stderr=subprocess.STDOUT).returncode


def test_summary(log: pathlib.Path) -> str | None:
    lines = [re.sub(r"^.*?(Tests run:)", r"\1", l).strip() for l in log.read_text(errors="replace").splitlines()
             if "Tests run:" in l and "Time elapsed" not in l and " -- in " not in l]
    if not lines:
        return None
    total = [0, 0, 0, 0]  # a reactor prints one summary per module
    for l in lines:
        m = re.search(r"Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)", l)
        if m:
            total = [a + int(b) for a, b in zip(total, m.groups())]
    return "Tests run: %d, Failures: %d, Errors: %d, Skipped: %d" % tuple(total)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--commit", default="HEAD")
    ap.add_argument("--jobs", type=int, default=3)
    ap.add_argument("--out", default=None)
    ap.add_argument("--only", nargs="*")
    ap.add_argument("--skip-tests", action="store_true")
    ap.add_argument("--skip-dependencies", action="store_true")
    a = ap.parse_args()
    commit = git("rev-parse", a.commit)
    version = pom_version(ROOT / "ah5-common-java-spring/pom.xml")
    out = pathlib.Path(a.out) if a.out else ROOT / f"verification/release-{version}"
    logs = out / "build-logs"
    logs.mkdir(parents=True, exist_ok=True)
    dirty = [l for l in git("status", "--porcelain").splitlines() if not l.endswith(".DS_Store")]
    with tempfile.TemporaryDirectory(prefix="ah-release-", dir=pathlib.Path.home()) as tmp:
        src = pathlib.Path(tmp).resolve()
        archive = subprocess.run(["git", "-C", str(ROOT), "archive", "--format=tar", commit], capture_output=True, check=True).stdout
        tar_path = src / "source.tar"
        tar_path.write_bytes(archive)
        source_sha = sha256(tar_path)
        with tarfile.open(tar_path) as tar:
            tar.extractall(src, filter="data")
        tar_path.unlink()
        version = pom_version(src / "ah5-common-java-spring/pom.xml")
        todo = [t for t in targets(src, version) if not a.only or t["name"] in a.only or t["source"] in a.only]
        manifest = {"format": "arrowhead-release-manifest/1", "release": version, "commit": commit, "tree": git("rev-parse", f"{commit}^{{tree}}"),
                    "commitDate": git("show", "-s", "--format=%cI", commit), "sourceArchiveSha256": source_sha,
                    "sourceArchive": f"git archive --format=tar {commit}", "workingTreeChangesIgnored": dirty,
                    "generated": datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="seconds"),
                    "host": subprocess.run(["docker", "version", "--format", "{{.Server.Version}} {{.Server.Os}}/{{.Server.Arch}}"],
                                           capture_output=True, text=True).stdout.strip(),
                    "mavenImage": MAVEN_IMAGE, "images": [], "tests": {}, "dependencies": {}}
        with concurrent.futures.ThreadPoolExecutor(a.jobs) as pool:
            manifest["images"] = sorted(pool.map(lambda t: build(t, src, logs, commit), todo), key=lambda e: e["image"])
        if not a.skip_tests and not a.only:
            for name, workdir, goals in [("ah5-common-java-spring", "ah5-common-java-spring", ["clean", "install"]),
                                         ("ah5-core-java-spring", "ah5-core-java-spring", ["clean", "verify"])]:
                log = logs / f"tests-{name}.log"
                code = maven(src, workdir, goals, log)
                manifest["tests"][name] = {"result": "PASS" if code == 0 else "FAIL", "command": "mvn -B " + " ".join(goals), "tests": test_summary(log),
                                           "log": {"file": f"build-logs/{log.name}", "sha256": sha256(log)}}
                print(manifest["tests"][name]["result"], "tests", name, manifest["tests"][name]["tests"], flush=True)
        if not a.skip_dependencies and not a.only:
            for pom in sorted(src.glob("ah5-*/pom.xml")):
                log = logs / f"dependencies-{pom.parent.name}.log"
                code = maven(src, pom.parent.name, ["dependency:list", "-DincludeScope=runtime", "-Dsort=true"], log)
                deps = sorted({m.group(1) for m in re.finditer(r"^\[INFO\]\s+([\w.\-]+:[\w.\-]+:[\w.\-]+(?::[\w.\-]+)?:[\w.\-]+:(?:compile|runtime))", log.read_text(errors="replace"), re.M)})
                manifest["dependencies"][pom.parent.name] = {"result": "PASS" if code == 0 else "FAIL", "runtime": deps}
                print(manifest["dependencies"][pom.parent.name]["result"], "dependencies", pom.parent.name, len(deps), flush=True)
    name = "manifest.json" if not a.only else "manifest-partial.json"
    (out / name).write_text(json.dumps(manifest, indent=2) + "\n")
    failed = [e["image"] for e in manifest["images"] if e["result"] != "PASS"] + [k for k, v in manifest["tests"].items() if v["result"] != "PASS"]
    print(f"{len(manifest['images'])} images, {len(failed)} failed -> {out / name}")
    raise SystemExit(1 if failed else 0)


if __name__ == "__main__":
    main()
