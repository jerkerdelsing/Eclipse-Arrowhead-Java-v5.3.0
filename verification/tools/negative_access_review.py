#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""Inventory of negative-access tests per project at a commit.

  negative_access_review.py [--commit HEAD] [--out verification/release-5.3.0]

A negative-access test is a test method (@Test, @ParameterizedTest) whose body asserts a refusal: HTTP 401/403
(isUnauthorized, isForbidden, status 401/403), an AuthException/ForbiddenException/AccessDenied, or a denied
authorization decision. Counted from the committed test sources (git show), vendored code excluded. The result shows
which projects have such tests and how many; a project with none is flagged. It does not replace a live check of a
deployed cloud (the test clouds' smoke-security scripts).
"""
from __future__ import annotations

import argparse
import json
import pathlib
import re
import subprocess

ROOT = pathlib.Path(__file__).resolve().parents[2]
REFUSAL = re.compile(r"isUnauthorized\(\)|isForbidden\(\)|status\(\)\.is\((401|403)\)|\b(401|403)\b|AuthException|ForbiddenException|AccessDenied|"
                     r"UNAUTHORIZED|FORBIDDEN|\.denied\b|assertFalse\([^;]*(allowed|authorized|permit|granted)|refus|reject", re.I)


def git(*args: str) -> str:
    return subprocess.run(["git", "-C", str(ROOT), *args], capture_output=True, text=True, check=True).stdout


def methods(text: str):
    for m in re.finditer(r"@(?:Test|ParameterizedTest)\b[^{;]*?\b(?:void|[\w<>]+)\s+(\w+)\s*\([^)]*\)[^{]*\{", text, re.S):
        depth, i = 1, m.end()
        while depth and i < len(text):
            depth += {"{": 1, "}": -1}.get(text[i], 0)
            i += 1
        yield m.group(1), text[m.end():i]


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--commit", default="HEAD")
    ap.add_argument("--out", type=pathlib.Path, default=ROOT / "verification/release-5.3.0")
    a = ap.parse_args()
    commit = git("rev-parse", a.commit).strip()
    report = {"commit": commit, "projects": {}}
    for name in git("ls-tree", "-r", "--name-only", commit).splitlines():
        if "/src/test/" not in name or not name.endswith(".java") or "/vendor/" in name:
            continue
        project = name.split("/")[0] + ("/" + name.split("/")[1] if name.startswith(("ah5-core-java-spring/", "ah5-common-java-spring/")) else "")
        p = report["projects"].setdefault(project, {"testMethods": 0, "negativeAccess": 0, "examples": []})
        for method, body in methods(git("show", f"{commit}:{name}")):
            p["testMethods"] += 1
            if REFUSAL.search(body):
                p["negativeAccess"] += 1
                if len(p["examples"]) < 5:
                    p["examples"].append(f"{name.rsplit('/', 1)[1][:-5]}.{method}")
    systems = sorted({n.split("/")[0] for n in git("ls-tree", "--name-only", commit).splitlines() if n.startswith("ah5-")})
    report["withoutNegativeAccessTests"] = sorted(s for s in systems if s not in ("ah5-core-java-spring", "ah5-common-java-spring", "ah5-support-runtime-java")
                                                  and not report["projects"].get(s, {}).get("negativeAccess"))
    report["projects"] = dict(sorted(report["projects"].items()))
    (a.out / "negative-access-review.json").write_text(json.dumps(report, indent=2) + "\n")
    for k, v in report["projects"].items():
        print(f"{k:58} {v['negativeAccess']:4} of {v['testMethods']:4}")
    print("without negative-access tests:", report["withoutNegativeAccessTests"])


if __name__ == "__main__":
    main()
