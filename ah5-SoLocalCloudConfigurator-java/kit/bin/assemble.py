#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""Second packaging stage, on a machine with Docker (and git for --source): complete an SoLC package.

  assemble.py <package.tar.gz | package-dir> [--source <checkout>] [--arch amd64,arm64] [--out <file.tar.gz>] [--verify-only]

--source  adds source/source.tar.gz: `git archive` of the package's pinned commit from <checkout> (the checkout must
          contain that commit); build-on-target hosts then need neither git nor network.
--arch    adds images/<arch>/images.tar and images.sha256 for each architecture: every image of every member cloud of that
          architecture, built from the pinned source (<checkout> at the pinned commit, or source/source.tar.gz) and the
          brokers pulled at their pinned digests. Architectures other than this machine's are built with buildx emulation.
Then manifest.json (assembled) and SHA256SUMS are updated and the package is written again (--out, default
<name>-assembled.tar.gz beside the input; a directory is updated in place). The package's checksums are verified first.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import pathlib
import platform
import shutil
import subprocess
import sys
import tarfile
import tempfile

HERE = pathlib.Path(__file__).resolve().parent


def sha256(p: pathlib.Path) -> str:
    h = hashlib.sha256()
    with p.open("rb") as f:
        for b in iter(lambda: f.read(1 << 20), b""):
            h.update(b)
    return h.hexdigest()


def safe_extract(archive: pathlib.Path, dest: pathlib.Path) -> pathlib.Path:
    with tarfile.open(archive, "r:gz") as t:
        roots = set()
        for m in t.getmembers():
            p = pathlib.PurePosixPath(m.name)
            if p.is_absolute() or ".." in p.parts or not (m.isfile() or m.isdir()):
                raise SystemExit(f"unsafe archive member: {m.name}")
            roots.add(p.parts[0])
        if len(roots) != 1:
            raise SystemExit("a package has exactly one top-level folder")
        for m in t.getmembers():
            target = dest / m.name
            if m.isdir():
                target.mkdir(parents=True, exist_ok=True)
            else:
                target.parent.mkdir(parents=True, exist_ok=True)
                with t.extractfile(m) as src, target.open("wb") as out:
                    shutil.copyfileobj(src, out)
                target.chmod(0o755 if m.mode & 0o100 else 0o644)
    return dest / roots.pop()


def files(root: pathlib.Path) -> list[pathlib.Path]:
    """Package files: everything except SHA256SUMS and what a host adds when it uses the package (source/tree, the
    clouds' work and out folders, the organisation's state and out folders)."""
    def packaged(rel: tuple[str, ...]) -> bool:
        if rel == ("SHA256SUMS",) or rel[:2] == ("source", "tree") or rel[:2] in (("org", "state"), ("org", "out")):
            return False
        return not (rel[0] == "clouds" and len(rel) > 2 and rel[2] in ("work", "out"))
    return sorted(p for p in root.rglob("*") if p.is_file() and packaged(p.relative_to(root).parts))


def verify(root: pathlib.Path) -> None:
    sums = root / "SHA256SUMS"
    for line in sums.read_text().splitlines():
        digest, name = line.split("  ", 1)
        f = root / name
        if not f.is_file() or sha256(f) != digest:
            raise SystemExit(f"checksum mismatch: {name}")
    print(f"checksums ok ({len(sums.read_text().splitlines())} files)")


def resum(root: pathlib.Path) -> None:
    lines = [f"{sha256(p)}  {p.relative_to(root).as_posix()}" for p in files(root)]
    (root / "SHA256SUMS").write_text("\n".join(lines) + "\n")


def run(cmd: list[str], **kw) -> subprocess.CompletedProcess:
    return subprocess.run(cmd, check=True, **kw)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("package")
    ap.add_argument("--source")
    ap.add_argument("--arch")
    ap.add_argument("--out")
    ap.add_argument("--verify-only", action="store_true")
    a = ap.parse_args()
    src = pathlib.Path(a.package).resolve()
    tmp = None
    if src.is_dir():
        root = src
    else:
        tmp = pathlib.Path(tempfile.mkdtemp(prefix="solc-assemble-"))
        root = safe_extract(src, tmp)
    verify(root)
    if a.verify_only:
        return
    manifest = json.loads((root / "manifest.json").read_text())
    commit = (manifest.get("source") or {}).get("commit")
    tag = manifest["imageTag"]
    assembled = manifest.setdefault("assembled", {"source": False, "images": []})
    checkout = pathlib.Path(a.source).resolve() if a.source else None
    if checkout:
        if not commit:
            raise SystemExit("the package pins no source commit")
        run(["git", "-C", str(checkout), "cat-file", "-e", commit + "^{commit}"])
        out = root / "source/source.tar.gz"
        run(["git", "-C", str(checkout), "archive", "--format=tar.gz", "-o", str(out), commit])
        (root / "source/source.sha256").write_text(f"{sha256(out)}  source.tar.gz\n")
        assembled["source"] = True
        print(f"source/source.tar.gz: {commit}")
    if a.arch:
        archs = [x for x in a.arch.split(",") if x]
        if any(x not in ("amd64", "arm64") for x in archs):
            raise SystemExit("--arch: amd64 and/or arm64")
        host = {"x86_64": "amd64", "amd64": "amd64", "aarch64": "arm64", "arm64": "arm64"}.get(platform.machine().lower())
        archs.sort(key=lambda x: x == host)  # the host architecture last: its tags stay in the local image store
        work = pathlib.Path(tempfile.mkdtemp(prefix="solc-tree-"))
        try:
            tree = work / "tree"
            if checkout and commit:
                tree.mkdir()
                p = subprocess.Popen(["git", "-C", str(checkout), "archive", "--format=tar", commit], stdout=subprocess.PIPE)
                run(["tar", "-x", "-C", str(tree)], stdin=p.stdout)
                if p.wait():
                    raise SystemExit("git archive failed")
            elif checkout:
                tree = checkout
            elif (root / "source/source.tar.gz").exists():
                tree.mkdir()
                run(["tar", "-xzf", str(root / "source/source.tar.gz"), "-C", str(tree)])
            else:
                raise SystemExit("--arch needs the source: --source <checkout> or source/source.tar.gz")
            for arch in archs:
                composes = []
                for cloud_json in sorted((root / "clouds").glob("*/cloud.json")):
                    c = json.loads(cloud_json.read_text())
                    if c["architecture"] != arch:
                        continue
                    w = work / "render" / c["cloud"]
                    run([sys.executable, str(HERE / "configure.py"), str(root), str(cloud_json), str(w)], stdout=subprocess.DEVNULL)
                    composes.append(str(w / "compose.yaml"))
                if not composes:
                    print(f"{arch}: no member cloud runs on {arch}; skipped")
                    continue
                d = root / "images" / arch
                d.mkdir(parents=True, exist_ok=True)
                run([sys.executable, str(HERE / "build-images.py"), "--source", str(tree), "--platform", f"linux/{arch}", "--tag", tag,
                     "--save", str(d / "images.tar")] + composes)
                (d / "images.sha256").write_text(f"{sha256(d / 'images.tar')}  images.tar\n")
                if arch not in assembled["images"]:
                    assembled["images"].append(arch)
        finally:
            shutil.rmtree(work, ignore_errors=True)
    (root / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
    resum(root)
    if src.is_dir():
        print(f"package updated in place: {root}")
        return
    out = pathlib.Path(a.out).resolve() if a.out else src.with_name(src.name.replace(".tar.gz", "") + "-assembled.tar.gz")
    with tarfile.open(out, "w:gz") as t:
        t.add(root, arcname=root.name)
    print(f"{out} ({out.stat().st_size >> 20} MiB), sha256 {sha256(out)}")
    shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    main()
