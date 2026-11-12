#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""Check the endpoint principles for every Java core and support system.

1. Configured listening port: each system is a Spring Boot application, so its port is ``server.port``;
   Spring Boot binds it from (highest precedence first) ``--server.port=N``, ``SERVER_PORT=N``, an external
   ``config/application.properties`` and the packaged ``src/main/resources/application.properties``.
2. Predefined default: the packaged ``application.properties`` must set ``server.port`` to the Arrowhead default
   below, and no two systems may share a default.

Deployment packaging (``config/application.properties``, Dockerfile EXPOSE, compose port mapping) that uses a
different port is reported as a warning: it is a deliberate deployment choice, not a missing default.

Usage: python3 verification/tools/verify_listening_ports.py   (exit status 1 on a failed check)
"""
from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]

# Arrowhead 5 default listening ports; the Go and Rust primary trees use the same built-in defaults.
DEFAULTS = {
    "ah5-core-java-spring/serviceorchestration-dynamic": 8441,
    "ah5-core-java-spring/serviceorchestration-simple": 8442,
    "ah5-core-java-spring/serviceregistry": 8443,
    "ah5-core-java-spring/authentication": 8444,
    "ah5-core-java-spring/consumer-authorization": 8445,
    "ah5-PlantDescriptionEngine-java-spring": 8432,
    "ah5-Onboarding-java-spring": 8435,
    "ah5-CertificateAuthority-java-spring": 8448,
    "ah5-Gatekeeper-java-spring": 8449,
    "ah5-GateTunnel-java-spring": 8453,
    "ah5-EventHandler-java-spring": 8455,
    "ah5-Choreographer-java-spring": 8457,
    "ah5-Configuration-java-spring": 8459,
    "ah5-DataManager-java-spring": 8461,
    "ah5-TimeManager-java-spring": 8463,
    "ah5-Blacklist-java-spring": 8464,
    "ah5-TranslationManager-java-spring": 8465,
    "ah5-GraphDBManager-java-spring": 8467,
    "ah5-DeviceQoSEvaluator-java-spring": 8472,
    "ah5-SysMonitor-java-spring": 8475,
    "ah5-AIGym-java-spring": 8478,
    "ah5-LocalCloudManager-java-spring": 8491,
    "ah5-LocalCloudConfigurator-java": 8492,
    "ah5-SoSConfigurator-java": 8494,
    "ah5-SoLocalCloudConfigurator-java": 8496,
}

PORT = re.compile(r"^\s*server\.port\s*[=:]\s*(\S+)\s*$", re.M)
YAML_PORT = re.compile(r"^server:\s*\n(?:[ \t]+.*\n)*?[ \t]+port:\s*(\S+)", re.M)
PLACEHOLDER = re.compile(r"^\$\{[A-Z_]+:(\d+)\}$")


def port_value(raw: str) -> int | None:
    """Literal port, or the default of a ``${ENV:default}`` placeholder."""
    m = PLACEHOLDER.match(raw)
    raw = m.group(1) if m else raw
    return int(raw) if raw.isdigit() else None


def packaged_port(system: pathlib.Path) -> int | None:
    res = system / "src/main/resources"
    props = res / "application.properties"
    if props.is_file() and (m := PORT.search(props.read_text())):
        return port_value(m.group(1))
    for name in ("application.yml", "application.yaml"):
        yml = res / name
        if yml.is_file() and (m := YAML_PORT.search(yml.read_text())):
            return port_value(m.group(1))
    return None


def deployment_ports(system: pathlib.Path) -> dict[str, int]:
    found: dict[str, int] = {}
    props = system / "config/application.properties"
    if props.is_file() and (m := PORT.search(props.read_text())) and (p := port_value(m.group(1))):
        found["config/application.properties"] = p
    docker = system / "Dockerfile"
    if docker.is_file() and (m := re.search(r"^EXPOSE\s+(\d+)", docker.read_text(), re.M)):
        found["Dockerfile EXPOSE"] = int(m.group(1))
    compose = system / "compose.yaml"
    if compose.is_file() and (m := re.search(r"HOST_PORT:-\d+\}:(\d+)", compose.read_text())):
        found["compose.yaml container port"] = int(m.group(1))
    return found


def main() -> int:
    failures, warnings = [], []
    discovered = {
        str(p.parent.parent.parent.parent.relative_to(ROOT))
        for p in ROOT.glob("ah5-*/**/src/main/resources/application.properties")
        if "/target/" not in str(p) and "/vendor/" not in str(p) and "docker-proxy" not in str(p)
    }
    discovered |= {
        str(p.parent.parent.parent.parent.relative_to(ROOT))
        for p in ROOT.glob("ah5-*/src/main/resources/application.y*ml")
    }
    for name in sorted(discovered - DEFAULTS.keys()):
        failures.append(f"{name}: system without an expected default port in this check")
    seen: dict[int, str] = {}
    print(f"{'System':52} {'default':>7}  overrides")
    for name, expected in sorted(DEFAULTS.items(), key=lambda kv: kv[1]):
        system = ROOT / name
        if not system.is_dir():
            failures.append(f"{name}: missing")
            continue
        port = packaged_port(system)
        if port is None:
            failures.append(f"{name}: packaged application.properties has no server.port default")
        elif port != expected:
            failures.append(f"{name}: packaged server.port={port}, expected Arrowhead default {expected}")
        if port is not None:
            if port in seen:
                failures.append(f"{name}: default port {port} also used by {seen[port]}")
            seen[port] = name
        for where, p in deployment_ports(system).items():
            if p != port:
                warnings.append(f"{name}: {where} uses {p}, packaged default is {port}")
        print(f"{name:52} {port!s:>7}  --server.port, SERVER_PORT, config/application.properties")
    for w in warnings:
        print("WARN", w)
    for f in failures:
        print("FAIL", f)
    print("listening ports:", "FAIL" if failures else "PASS", f"({len(DEFAULTS)} systems, {len(warnings)} warnings)")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
