#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""Build (or pull) the images a set of member clouds needs, for one CPU architecture.

  build-images.py --source <checkout> --platform linux/<arch> --tag <tag> [--save <images.tar>] <compose.yaml>...

<checkout> is the Eclipse-Arrowhead-java source at the package's pinned commit. Support system images come from the build
sections of each ah5-<System> folder's compose.yaml (their Dockerfiles run the folder's Maven build and tests); core
images from ah5-core-java-spring/<module>/docker/Dockerfile-* plus the kit's read-only adapter and database image;
RandomDataProducer from the kit; brokers are pulled at their pinned digests. Existing images are kept. With --save, all
images are written to one archive (docker save) for hosts without the source or a network.
"""
from __future__ import annotations

import argparse
import json
import pathlib
import shutil
import subprocess
import sys
import tempfile

KIT = pathlib.Path(__file__).resolve().parents[1]
REGISTRY = "arrowhead-solc"
CORE = {"serviceregistry", "authentication", "consumer-authorization", "serviceorchestration-dynamic", "serviceorchestration-simple"}


def run(cmd: list[str], log: pathlib.Path | None = None) -> None:
    print("+", " ".join(cmd[:8]), "..." if len(cmd) > 8 else "", flush=True)
    if log:
        with log.open("w") as out:
            code = subprocess.run(cmd, stdout=out, stderr=subprocess.STDOUT).returncode
        if code:
            sys.stderr.write(log.read_text()[-4000:])
            raise SystemExit(f"failed ({code}): see {log}")
    else:
        subprocess.run(cmd, check=True)


def exists(image: str, platform: str) -> bool:
    r = subprocess.run(["docker", "image", "inspect", "--format", "{{.Os}}/{{.Architecture}}", image], capture_output=True, text=True)
    return r.returncode == 0 and r.stdout.strip() == platform


def build(tag: str, context: pathlib.Path, dockerfile: pathlib.Path | None, platform: str, logs: pathlib.Path, args: list[str] | None = None) -> None:
    if exists(tag, platform):
        print("exists", tag)
        return
    cmd = ["docker", "buildx", "build", "--platform", platform, "--load", "--progress=plain", "-t", tag] + (args or [])
    if dockerfile:
        cmd += ["-f", str(dockerfile)]
    run(cmd + [str(context)], logs / (tag.split("/")[-1].replace(":", "_") + ".log"))


def folders(source: pathlib.Path) -> dict[str, pathlib.Path]:
    return {p.name.lower(): p for p in source.iterdir() if p.is_dir() and p.name.startswith("ah5-") and (p / "compose.yaml").exists()}


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--source", required=True)
    ap.add_argument("--platform", required=True, choices=["linux/amd64", "linux/arm64"])
    ap.add_argument("--tag", required=True)
    ap.add_argument("--save")
    ap.add_argument("compose", nargs="+")
    a = ap.parse_args()
    source = pathlib.Path(a.source).resolve()
    images = sorted({s["image"] for c in a.compose for s in json.loads(pathlib.Path(c).read_text())["services"].values()})
    logs = pathlib.Path(tempfile.mkdtemp(prefix="solc-build-logs-"))
    by_folder = folders(source)
    rendered: dict[str, dict] = {}
    for image in images:
        if not image.startswith(REGISTRY + "/"):
            if not exists(image, a.platform):
                run(["docker", "pull", "--platform", a.platform, image])
            continue
        name, tag = image[len(REGISTRY) + 1:].rsplit(":", 1)
        if tag != a.tag:
            raise SystemExit(f"{image}: tag differs from {a.tag}")
        if name == "random-data-producer":
            build(image, KIT / "images/random-data-producer", None, a.platform, logs)
        elif name.endswith("-java") and name[:-5] in CORE:
            module = name[:-5]
            native = f"{REGISTRY}/{module}-java-native:{a.tag}"
            with tempfile.TemporaryDirectory(prefix="solc-core-") as tmp:
                for part in ("ah5-common-java-spring", "ah5-core-java-spring"):
                    shutil.copytree(source / part, pathlib.Path(tmp) / part, ignore=shutil.ignore_patterns("target", "*.log"))
                dockerfile = next(p for p in (pathlib.Path(tmp) / "ah5-core-java-spring" / module / "docker").glob("Dockerfile-*") if not p.name.endswith("-DB"))
                build(native, pathlib.Path(tmp), dockerfile, a.platform, logs, ["--build-arg", "AH_VERSION=5.3.0"])
            build(image, KIT / "images/core-adapter", None, a.platform, logs, ["--build-arg", f"BASE_IMAGE={native}"])
        elif name.endswith("-db") and name[:-3] in CORE:
            module = name[:-3]
            build(image, source / "ah5-core-java-spring", KIT / "images/core-db/Dockerfile", a.platform, logs, ["--build-arg", f"MODULE={module}"])
        else:
            folder = next((f for f in sorted(by_folder, key=len, reverse=True) if name == f or name.startswith(f + "-")), None)
            if not folder:
                raise SystemExit(f"{image}: no source folder")
            if folder not in rendered:
                out = subprocess.run(["docker", "compose", "config", "--format", "json"], cwd=by_folder[folder], check=True, capture_output=True, text=True).stdout
                rendered[folder] = json.loads(out)
            suffix = name[len(folder):]
            svc = next((n for n, s in rendered[folder]["services"].items() if "build" in s and (
                (suffix == "" and not n.endswith(("-db", "-graphdb")) and n != "docker-proxy") or (suffix and (n == suffix[1:] or n.endswith(suffix))))), None)
            if not svc:
                raise SystemExit(f"{image}: no build section in {folder}/compose.yaml")
            b = rendered[folder]["services"][svc]["build"]
            ctx = pathlib.Path(b["context"])
            build(image, ctx, ctx / b.get("dockerfile", "Dockerfile"), a.platform, logs)
    if a.save:
        pathlib.Path(a.save).parent.mkdir(parents=True, exist_ok=True)
        run(["docker", "save", "-o", a.save] + images)
        print(f"{len(images)} images -> {a.save}")
    else:
        print(f"{len(images)} images ready for {a.platform}")


if __name__ == "__main__":
    main()
