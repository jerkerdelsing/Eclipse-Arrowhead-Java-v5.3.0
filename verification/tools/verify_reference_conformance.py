#!/usr/bin/env python3
"""Static conformance checks of the primary Java tree. No Docker or network is used.

Covers the observations of Section 12 of the Core GSoSDD: framework version, default ports,
monitoring, security packaging, restricted discovery and provider-side inter-cloud authorization.
Exits non-zero and lists every failure.
"""
import filecmp
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
COMMON = ROOT / "ah5-common-java-spring"
CORE = ROOT / "ah5-core-java-spring"
CORE_MODULES = ["serviceregistry", "authentication", "consumer-authorization",
                "serviceorchestration-dynamic", "serviceorchestration-simple"]
# every Core and support system is Java-Spring since 8 November 2026 (no native JDK or Node.js implementation)
SUPPORT = ["AIGym", "Blacklist", "CertificateAuthority", "Choreographer", "Configuration", "DataManager", "DeviceQoSEvaluator",
           "EventHandler", "GateTunnel", "Gatekeeper", "GraphDBManager", "LocalCloudConfigurator", "LocalCloudManager", "Onboarding",
           "PlantDescriptionEngine", "SoSConfigurator", "SysMonitor", "TimeManager", "TranslationManager"]


def support_dir(system):
    matches = sorted(ROOT.glob(f"ah5-{system}-java*"))
    return matches[0] if matches else ROOT / f"ah5-{system}-java-spring"
PORTS = {"SimpleStoreServiceOrchestration": 8442, "TranslationManager": 8465, "Onboarding": 8435}
# GraphDBManager and LocalCloudManager have no JPA, so their copies of the library omit these on purpose
GRAPHDB_ALLOWED = {"Constants.java"}
GRAPHDB_MISSING = {"jpa", "log4j2", "LogService.java", "LogValidation.java"}
SKIP_DIRS = {"target", "node_modules", ".git", "legacy", "verification", "distribution", "__pycache__"}

failures = []
checks = 0


def check(condition, message):
    global checks
    checks += 1
    if not condition:
        failures.append(message)


def walk(names):
    for path in ROOT.rglob("*"):
        if path.is_file() and path.name in names and not SKIP_DIRS.intersection(path.relative_to(ROOT).parts):
            yield path


def pom_version(path):
    project = ET.parse(path).getroot()
    tag = next(c for c in project if c.tag.endswith("}version") or c.tag == "version")
    return tag.text.strip()


def properties(path):
    result = {}
    for line in path.read_text(errors="replace").splitlines():
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            result[key.strip()] = value.strip()
    return result


def framework_version():
    version = pom_version(COMMON / "pom.xml")
    text = (COMMON / "common-utils/src/main/java/eu/arrowhead/common/Constants.java").read_text()
    check(f'AH_FRAMEWORK_VERSION = "{version}"' in text, f"library constant differs from library version {version}")
    core_pom = (CORE / "pom.xml").read_text()
    check(f"<ah.common.version>{version}</ah.common.version>" in core_pom, "Core does not use the library version in this tree")
    for vendor in sorted(ROOT.glob("ah5-*/vendor")):
        system = vendor.parent
        constants = (vendor / "common-utils/src/main/java/eu/arrowhead/common/Constants.java").read_text()
        check(f'AH_FRAMEWORK_VERSION = "{pom_version(system / "pom.xml")}"' in constants, f"{system.name}: constant differs from its POM version")
        for part in ["common-utils", "data-transfer-objects"]:
            compare_tree(COMMON / part / "src/main/java", vendor / part / "src/main/java", system.name)


def compare_tree(left, right, system):
    comparison = filecmp.dircmp(left, right)
    no_jpa = "GraphDBManager" in system or "LocalCloudManager" in system
    allowed = GRAPHDB_ALLOWED if no_jpa else set()
    missing = GRAPHDB_MISSING if no_jpa else set()
    check(not (set(comparison.diff_files) - allowed), f"{system}: vendored files differ from the library: {comparison.diff_files} in {right}")
    check(not (set(comparison.left_only) - missing), f"{system}: vendored copy lacks {comparison.left_only}")
    check(not comparison.right_only, f"{system}: vendored copy adds {comparison.right_only}")
    for name in comparison.common_dirs:
        compare_tree(left / name, right / name, system)


def ports():
    samples = {
        "SimpleStoreServiceOrchestration": CORE / "serviceorchestration-simple/src/main/resources/application.properties",
        "TranslationManager": ROOT / "ah5-TranslationManager-java-spring/src/main/resources/application.properties",
        "Onboarding": ROOT / "ah5-Onboarding-java-spring/src/main/resources/application.properties",
    }
    for system, path in samples.items():
        check(properties(path).get("server.port") == str(PORTS[system]), f"{system}: default port is not {PORTS[system]}")
    dockerfile = CORE / "serviceorchestration-simple/docker/Dockerfile-SimpleStoreServiceOrchestration"
    check("EXPOSE 8442" in dockerfile.read_text(), "SimpleStore image does not expose 8442")
    owners = {}
    for path in walk({"application.properties"}):
        if path.parts[-2] == "resources":
            port = properties(path).get("server.port")
            if port:
                check(port not in owners, f"port {port} used by {owners.get(port)} and {path.relative_to(ROOT)}")
                owners[port] = path.relative_to(ROOT)


def monitoring():
    for path in walk({"DeviceQoSEvaluatorSystemInfo.java", "ServiceRegistrySystemInfo.java"}):
        check("add monitor service" not in path.read_text(), f"{path.name}: stale monitor TODO")
    for module in CORE_MODULES:
        sample = CORE / module / "src/main/resources/application.properties"
        check("sysmonitor.enabled" in properties(sample), f"{module}: no sysmonitor.enabled in the sample")
    for system in ["CertificateAuthority", "Gatekeeper", "GateTunnel", "SysMonitor", "LocalCloudConfigurator", "LocalCloudManager", "SoSConfigurator"]:
        config = support_dir(system) / "config/application.properties"
        check(config.is_file() and "sysmonitor.enabled" in properties(config), f"{system}: deployment configuration has no sysmonitor.enabled")


def spring():
    for system in SUPPORT:
        folder = support_dir(system)
        check((folder / "pom.xml").is_file() and "spring-boot-starter-parent" in (folder / "pom.xml").read_text(), f"{system}: not a Spring Boot build")
        native = [p for p in folder.rglob("*.java") if "/eu/arrowhead/port/" in p.as_posix() and not SKIP_DIRS.intersection(p.parts)]
        check(not native, f"{system}: native eu.arrowhead.port sources remain")
        node = [p for p in folder.rglob("package.json") if not SKIP_DIRS.intersection(p.parts)]
        check(not node, f"{system}: Node.js sources remain ({', '.join(str(p.relative_to(folder)) for p in node)})")


def security():
    guard = "src/main/java/eu/arrowhead/common/init/SecurityProfileGuard.java"
    check((COMMON / "common-utils" / guard).is_file(), "library has no SecurityProfileGuard")
    for vendor in sorted(ROOT.glob("ah5-*/vendor")):
        check((vendor / "common-utils" / guard).is_file(), f"{vendor.parent.name}: no SecurityProfileGuard")
    for path in sorted(ROOT.glob("ah5-*/config/application.properties")):
        values = properties(path)
        name = path.parent.parent.name
        check(values.get("server.ssl.enabled") == "true", f"{name}: deployment configuration has no TLS")
        check(values.get("authentication.policy", "").lower() != "declared", f"{name}: deployment configuration declares identities")
        check(values.get("service.authorization.enabled") != "false", f"{name}: deployment configuration disables authorization")
        check(values.get("device.agent.ssl.enabled", "true").lower() != "false", f"{name}: deployment configuration uses plain HTTP to the device agents")
    development = re.compile(r"(arrowhead\.security\.profile|ARROWHEAD_SECURITY_PROFILE)\s*[=:]\s*\"?development", re.I)
    plaintext = re.compile(r"(server\.ssl\.enabled|SERVER_SSL_ENABLED|device\.agent\.ssl\.enabled|DEVICE_AGENT_SSL_ENABLED)\s*[=:]\s*\"?false", re.I)
    for path in walk({"Dockerfile", "compose.yaml", ".env.example"}):
        text = path.read_text(errors="replace")
        check(not development.search(text), f"{path.relative_to(ROOT)}: selects the development exception")
        check(not plaintext.search(text), f"{path.relative_to(ROOT)}: disables TLS")
    for path in ROOT.glob("ah5-*/config/application.properties"):
        check(not development.search(path.read_text()), f"{path.relative_to(ROOT)}: selects the development exception")
    for path in walk({"application.properties"}):
        if path.parts[-2] == "resources":
            uncommented = "\n".join(l for l in path.read_text().splitlines() if not l.lstrip().startswith("#"))
            check(not development.search(uncommented), f"{path.relative_to(ROOT)}: sample pre-selects the development exception")


def device_agent():
    folder = ROOT / "ah5-DeviceQoSEvaluator-java-spring"
    driver = (folder / "src/main/java/eu/arrowhead/deviceqosevaluator/driver/AugmentedMeasurementDriver.java").read_text()
    check("Constants.HTTPS" in driver and "httpService.sendRequest" in driver, "DeviceQoSEvaluator: device agent driver is not on the mutual-TLS client")
    agent = (folder / "device_agent/python/agent_server.py").read_text()
    check("ssl.CERT_REQUIRED" in agent and "VERIFY_CRL_CHECK_LEAF" in agent and "allowed_clients" in agent, "device agent: no mutual TLS, CRL check or client allow-list")
    check(properties(folder / "config/application.properties").get("device.agent.ssl.enabled") == "true", "DeviceQoSEvaluator: deployment configuration does not enable device agent TLS")


def discovery():
    values = properties(CORE / "serviceregistry/src/main/resources/application.properties")
    check(values.get("service.discovery.policy") == "restricted", "ServiceRegistry sample is not restricted")
    check("DynamicServiceOrchestration" in values.get("service.discovery.direct.access", ""), "DSO has no direct access")
    content = (CORE / "documentation-tools/content_systems.py").read_text()
    check("service.discovery.direct.access" in content and "Three combinations" in content, "SimpleStore discovery combinations are undocumented")


def intercloud():
    gatekeeper = ROOT / "ah5-Gatekeeper-java-spring"
    source = (gatekeeper / "src/main/java/eu/arrowhead/gatekeeper/service/FederationService.java").read_text()
    check("SERVICE_OP_AUTHORIZATION_CHECK_POLICIES" in source, "Gatekeeper does not use check-policies")
    check("SERVICE_OP_VERIFY" not in source, "Gatekeeper uses authorization.verify")
    tests = (gatekeeper / "src/test/java/eu/arrowhead/gatekeeper/service/FederationServiceTest.java").read_text()
    check("providerSideFailsClosedWithoutConsumerAuthorization" in tests, "provider-side authorization has no fail-closed test")


for section in [framework_version, ports, monitoring, spring, security, device_agent, discovery, intercloud]:
    section()

for failure in failures:
    print(f"FAIL {failure}")
print(f"{checks - len(failures)} of {checks} checks passed")
sys.exit(1 if failures else 0)
