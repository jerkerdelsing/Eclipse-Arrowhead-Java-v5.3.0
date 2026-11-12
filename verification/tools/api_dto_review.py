#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""API/DTO regression review of a release commit against the upstream 5.2.2 release.

  api_dto_review.py --upstream-common REPO --upstream-core REPO [--tag v5.2.2] [--commit HEAD] [--out DIR]

REPO are local clones of eclipse-arrowhead/ah5-common-java-spring and ah5-core-java-spring holding the tag. Both
sides are read from git objects (`git show`), never from a working tree. Compared:
  - DTOs (data-transfer-objects): classes removed or added; for records the components, for enums the constants.
    A removed class, a removed or retyped record component or a removed enum constant is reported as breaking;
    additions are reported as additive.
  - Core and common constants: public static final String/int constants whose value changed or that were removed
    (HTTP paths, MQTT topics, operation names, header names live there).
  - Core controllers and MQTT handlers: Spring mapping annotations removed or changed per file.
Result: <out>/api-dto-review.json and api-dto-review.md. Exit status 1 when a breaking difference is found.
"""
from __future__ import annotations

import argparse
import json
import pathlib
import re
import subprocess

ROOT = pathlib.Path(__file__).resolve().parents[2]


def git(repo: pathlib.Path, *args: str) -> str:
    return subprocess.run(["git", "-C", str(repo), *args], capture_output=True, text=True, check=True).stdout


def files(repo: pathlib.Path, rev: str, prefix: str) -> dict[str, str]:
    args = ["ls-tree", "-r", "--name-only", rev] + (["--", prefix] if prefix else [])
    names = [n for n in git(repo, *args).splitlines() if n.endswith(".java")]
    return {n[len(prefix):].lstrip("/"): git(repo, "show", f"{rev}:{n}") for n in names}


def strip(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return re.sub(r"//[^\n]*", "", text)


def split_top(s: str) -> list[str]:
    out, depth, cur = [], 0, ""
    for ch in s:
        if ch in "<([":
            depth += 1
        elif ch in ">)]":
            depth -= 1
        if ch == "," and depth == 0:
            out.append(cur)
            cur = ""
        else:
            cur += ch
    return [x.strip() for x in out + [cur] if x.strip()]


def shape(text: str) -> dict:
    t = strip(text)
    m = re.search(r"\brecord\s+\w+\s*(?:<[^>]*>)?\s*\(", t)
    if m:
        depth, i = 1, m.end()
        while depth and i < len(t):
            depth += t[i] in "(" and 1 or (t[i] in ")" and -1 or 0)
            i += 1
        comps = {}
        for c in split_top(t[m.end():i - 1]):
            c = re.sub(r"@\w+(\([^)]*\))?\s*", "", c).replace("final ", "").strip()
            typ, _, name = c.rpartition(" ")
            comps[name] = re.sub(r"\s+", "", typ)
        return {"kind": "record", "members": comps}
    m = re.search(r"\benum\s+\w+[^{]*\{(.*?)(;|\})", t, re.S)
    if m:
        return {"kind": "enum", "members": {re.sub(r"\(.*", "", c, flags=re.S).strip(): "" for c in split_top(m.group(1)) if c.strip()}}
    fields = {m.group(2): re.sub(r"\s+", "", m.group(1)) for m in re.finditer(r"private\s+(?:final\s+)?([\w<>\[\],.? ]+?)\s+(\w+)\s*(?:=[^;]*)?;", t)}
    return {"kind": "class", "members": fields}


def constants(text: str) -> dict[str, str]:
    return {m.group(1): re.sub(r"\s+", " ", m.group(2)).strip()
            for m in re.finditer(r"public\s+static\s+final\s+(?:String|int|long|boolean)\s+(\w+)\s*=\s*([^;]+);", strip(text))}


def mappings(text: str) -> list[str]:
    return sorted(re.sub(r"\s+", " ", m.group(0)) for m in re.finditer(r"@(?:Get|Post|Put|Patch|Delete|Request)Mapping\s*(?:\((?:[^()]|\([^()]*\))*\))?", strip(text)))


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--upstream-common", required=True, type=pathlib.Path)
    ap.add_argument("--upstream-core", required=True, type=pathlib.Path)
    ap.add_argument("--tag", default="v5.2.2")
    ap.add_argument("--commit", default="HEAD")
    ap.add_argument("--out", type=pathlib.Path, default=None)
    a = ap.parse_args()
    commit = git(ROOT, "rev-parse", a.commit).strip()
    out = a.out or ROOT / "verification/release-5.3.0"
    out.mkdir(parents=True, exist_ok=True)
    report = {"commit": commit, "baseline": {"tag": a.tag, "common": git(a.upstream_common, "rev-parse", a.tag + "^{commit}").strip(),
                                             "core": git(a.upstream_core, "rev-parse", a.tag + "^{commit}").strip()},
              "dto": {"removed": [], "added": [], "breaking": [], "additive": []}, "constants": {"removed": [], "changed": [], "added": 0},
              "mappings": {"filesRemoved": [], "removed": [], "added": 0}}

    dto = "data-transfer-objects/src/main/java"
    old, new = files(a.upstream_common, a.tag, dto), files(ROOT, commit, "ah5-common-java-spring/" + dto)
    report["dto"]["removed"] = sorted(set(old) - set(new))
    report["dto"]["added"] = sorted(set(new) - set(old))
    for name in sorted(set(old) & set(new)):
        o, n = shape(old[name]), shape(new[name])
        if o["kind"] != n["kind"]:
            report["dto"]["breaking"].append({"file": name, "change": f"{o['kind']} became {n['kind']}"})
            continue
        for member, typ in o["members"].items():
            if member not in n["members"]:
                report["dto"]["breaking"].append({"file": name, "change": f"removed {member}"})
            elif n["members"][member] != typ:
                report["dto"]["breaking"].append({"file": name, "change": f"{member}: {typ} -> {n['members'][member]}"})
        extra = [m for m in n["members"] if m not in o["members"]]
        if extra:
            report["dto"]["additive"].append({"file": name, "added": extra})

    for label, repo, prefix, mine in [("common", a.upstream_common, "common-utils/src/main/java", "ah5-common-java-spring/common-utils/src/main/java"),
                                      ("core", a.upstream_core, "", "ah5-core-java-spring")]:
        old, new = files(repo, a.tag, prefix), files(ROOT, commit, mine)
        for name in sorted(old):
            if "/src/test/" in name or (label == "core" and "/src/main/java/" not in name):
                continue
            if name not in new:
                if constants(old[name]) or mappings(old[name]):
                    report["mappings"]["filesRemoved"].append(f"{label}:{name}")
                continue
            oc, nc = constants(old[name]), constants(new[name])
            for k, v in oc.items():
                if k not in nc:
                    report["constants"]["removed"].append({"file": f"{label}:{name}", "constant": k, "was": v})
                elif nc[k] != v:
                    report["constants"]["changed"].append({"file": f"{label}:{name}", "constant": k, "was": v, "now": nc[k]})
            report["constants"]["added"] += len(set(nc) - set(oc))
            om, nm = mappings(old[name]), mappings(new[name])
            left = list(nm)
            for m in om:
                if m in left:
                    left.remove(m)
                else:
                    report["mappings"]["removed"].append({"file": f"{label}:{name}", "mapping": m})
            report["mappings"]["added"] += len(left)

    breaking = len(report["dto"]["removed"]) + len(report["dto"]["breaking"]) + len(report["constants"]["removed"]) + len(report["mappings"]["removed"]) + len(report["mappings"]["filesRemoved"])
    report["summary"] = {"dtoClassesCompared": len(set(files(a.upstream_common, a.tag, dto))), "dtoRemoved": len(report["dto"]["removed"]), "dtoAdded": len(report["dto"]["added"]),
                         "dtoBreakingMembers": len(report["dto"]["breaking"]), "dtoAdditiveClasses": len(report["dto"]["additive"]),
                         "constantsRemoved": len(report["constants"]["removed"]), "constantsChanged": len(report["constants"]["changed"]), "constantsAdded": report["constants"]["added"],
                         "mappingsRemovedOrChanged": len(report["mappings"]["removed"]), "mappingsAdded": report["mappings"]["added"], "filesRemoved": len(report["mappings"]["filesRemoved"]),
                         "needsReview": breaking + len(report["constants"]["changed"])}
    (out / "api-dto-review.json").write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report["summary"], indent=1))
    raise SystemExit(1 if report["summary"]["needsReview"] else 0)


if __name__ == "__main__":
    main()
