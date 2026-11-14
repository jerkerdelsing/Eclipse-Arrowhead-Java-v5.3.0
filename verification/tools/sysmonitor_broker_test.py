#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""Runs SysMonitor's BrokerRoundTripTest (AMQP -> store -> MQTT), which the image build skips, against throw-away brokers.

  sysmonitor_broker_test.py [--commit HEAD] [--release 5.4.0] [--out verification/release-<release>]

Everything is created for the run and removed afterwards, on an internal Docker network:
  - a test CA with a CRL; PKCS12 stores (password rt-store, as the test expects) for SysMonitor, the Pump publisher and
    the Watcher subscriber, and the trust store; broker certificates for amqp-broker/amqp and mqtt-broker;
  - RabbitMQ (the Java test cloud's image) with mutual TLS and SASL EXTERNAL (user = certificate CN) and the topic
    exchange "arrowhead"; Mosquitto (the clouds' image) with mutual TLS and use_identity_as_username;
  - SysMonitor's release database image (arrowhead-sysmonitor-standalone-db:<release>) with throw-away passwords.
The commit is exported (git archive) and `mvn test -Dtest=BrokerRoundTripTest` runs in the Maven build image on that
network. Result: <out>/sysmonitor-broker-test.json; log in <out>/build-logs/ (not committed).
"""
from __future__ import annotations

import argparse
import json
import pathlib
import re
import secrets
import subprocess
import tarfile
import tempfile
import time

ROOT = pathlib.Path(__file__).resolve().parents[2]
MAVEN_IMAGE = "maven:3.9-eclipse-temurin-21"
RABBITMQ_IMAGE = "rabbitmq:4.3.7-alpine@sha256:68fd0566c1c5a5aef2d7b3ad55af8be503a767c1ff8646d7088c8c7946270cf9"
MOSQUITTO_IMAGE = "eclipse-mosquitto:2.0.22@sha256:81a6193ae693942a3231cd3ac52875b02b7e3a19da671b2fb4b55cc874c5b279"
STORE_PASSWORD = "rt-store"  # fixed by BrokerRoundTripTest
SUFFIX = ".TestCloud.Company.arrowhead.eu"


def sh(*cmd: str, cwd: pathlib.Path | None = None) -> str:
    r = subprocess.run(list(cmd), cwd=cwd, capture_output=True, text=True)
    if r.returncode:
        raise RuntimeError(f"{cmd[0]} {cmd[1] if len(cmd) > 1 else ''} failed: {r.stderr.strip()[-300:]}")
    return r.stdout


def pki(d: pathlib.Path) -> None:
    sh("openssl", "genpkey", "-algorithm", "RSA", "-pkeyopt", "rsa_keygen_bits:2048", "-out", "ca.key", cwd=d)
    sh("openssl", "req", "-x509", "-new", "-key", "ca.key", "-sha256", "-days", "2", "-subj", "/C=SE/O=Company/CN=TestCloud.Company.arrowhead.eu",
       "-addext", "basicConstraints=critical,CA:true", "-addext", "keyUsage=critical,keyCertSign,cRLSign", "-out", "ca.crt", cwd=d)
    (d / "index.txt").write_text("")
    (d / "crlnumber").write_text("1000\n")
    (d / "ca.cnf").write_text(f"[ca]\ndefault_ca=c\n[c]\ndatabase={d}/index.txt\ncrlnumber={d}/crlnumber\ncertificate={d}/ca.crt\n"
                              f"private_key={d}/ca.key\ndefault_md=sha256\ndefault_crl_days=2\n")

    def issue(name: str, subject: str, ext: str) -> None:
        sh("openssl", "genpkey", "-algorithm", "RSA", "-pkeyopt", "rsa_keygen_bits:2048", "-out", f"{name}.key", cwd=d)
        sh("openssl", "req", "-new", "-key", f"{name}.key", "-subj", subject, "-out", f"{name}.csr", cwd=d)
        (d / f"{name}.ext").write_text("basicConstraints=critical,CA:false\nkeyUsage=critical,digitalSignature,keyEncipherment\n" + ext)
        sh("openssl", "x509", "-req", "-in", f"{name}.csr", "-CA", "ca.crt", "-CAkey", "ca.key", "-CAcreateserial", "-days", "2", "-sha256",
           "-extfile", f"{name}.ext", "-out", f"{name}.crt", cwd=d)

    issue("amqp-broker", "/C=SE/O=Company/CN=amqp-broker" + SUFFIX, "extendedKeyUsage=serverAuth\nsubjectAltName=DNS:amqp-broker,DNS:amqp\n")
    issue("mqtt-broker", "/C=SE/O=Company/CN=mqtt-broker" + SUFFIX, "extendedKeyUsage=serverAuth\nsubjectAltName=DNS:mqtt-broker\n")
    for system in ("SysMonitor", "Pump", "Watcher"):
        issue(system, f"/C=SE/O=Company/OU=TestCloud/CN={system}{SUFFIX}/dnQualifier=sy", "extendedKeyUsage=clientAuth,serverAuth\n")
        sh("openssl", "pkcs12", "-export", "-inkey", f"{system}.key", "-in", f"{system}.crt", "-certfile", "ca.crt", "-name", f"{system}{SUFFIX}",
           "-passout", f"pass:{STORE_PASSWORD}", "-out", f"{system}.p12", cwd=d)
    sh("keytool", "-importcert", "-noprompt", "-alias", "ca", "-file", "ca.crt", "-storetype", "PKCS12", "-keystore", "truststore.p12",
       "-storepass", STORE_PASSWORD, cwd=d)
    sh("openssl", "ca", "-config", "ca.cnf", "-gencrl", "-out", "ca.crl", cwd=d)
    for f in d.iterdir():
        f.chmod(0o644)  # read by the broker users through the bind mount; the directory is removed after the run


def brokers(d: pathlib.Path) -> None:
    (d / "rabbitmq.conf").write_text("listeners.tcp = none\nlisteners.ssl.default = 5671\nauth_mechanisms.1 = EXTERNAL\n"
                                     "ssl_cert_login_from = common_name\nload_definitions = /etc/rabbitmq/definitions.json\nloopback_users = none\n")
    (d / "advanced.config").write_text('[{rabbit, [{ssl_options, [{cacertfile, "/certs/ca.crt"}, {certfile, "/certs/amqp-broker.crt"}, '
                                       '{keyfile, "/certs/amqp-broker.key"}, {verify, verify_peer}, {fail_if_no_peer_cert, true}, '
                                       "{versions, ['tlsv1.3', 'tlsv1.2']}]}]}].\n")
    (d / "enabled_plugins").write_text("[rabbitmq_auth_mechanism_ssl].\n")
    users = [f"{s}{SUFFIX}" for s in ("SysMonitor", "Pump")]
    (d / "definitions.json").write_text(json.dumps({
        "vhosts": [{"name": "/"}],
        "exchanges": [{"name": "arrowhead", "vhost": "/", "type": "topic", "durable": True, "auto_delete": False, "internal": False, "arguments": {}}],
        "users": [{"name": u, "password_hash": "", "hashing_algorithm": "rabbit_password_hashing_sha256", "tags": []} for u in users],
        "permissions": [{"user": u, "vhost": "/", "configure": ".*", "write": ".*", "read": ".*"} for u in users]}))
    (d / "mosquitto.conf").write_text("persistence false\nlog_dest stdout\nlistener 8883\ncafile /certs/ca.crt\ncertfile /certs/mqtt-broker.crt\n"
                                      "keyfile /certs/mqtt-broker.key\nrequire_certificate true\nuse_identity_as_username true\nallow_anonymous false\n")
    for f in d.iterdir():
        f.chmod(0o644)


def wait(cmd: list[str], what: str, tries: int = 90) -> None:
    for _ in range(tries):
        if subprocess.run(cmd, capture_output=True).returncode == 0:
            return
        time.sleep(2)
    raise RuntimeError(f"{what} did not become ready")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--commit", default="HEAD")
    ap.add_argument("--release", default="5.4.0")
    ap.add_argument("--out", type=pathlib.Path, default=None)
    a = ap.parse_args()
    commit = sh("git", "-C", str(ROOT), "rev-parse", a.commit).strip()
    out = a.out or ROOT / f"verification/release-{a.release}"
    logs = out / "build-logs"
    logs.mkdir(parents=True, exist_ok=True)
    db_image = f"arrowhead-sysmonitor-standalone-db:{a.release}"
    tag = secrets.token_hex(3)
    net, names = f"ah-sm-it-{tag}", {k: f"ah-sm-it-{k}-{tag}" for k in ("db", "amqp", "mqtt")}
    password = secrets.token_urlsafe(18)
    result = {"commit": commit, "release": a.release, "mavenImage": MAVEN_IMAGE, "rabbitmqImage": RABBITMQ_IMAGE, "mosquittoImage": MOSQUITTO_IMAGE,
              "databaseImage": db_image, "databaseImageId": sh("docker", "image", "inspect", "-f", "{{.Id}}", db_image).strip()}
    log = logs / "sysmonitor-broker-test.log"
    start = time.time()
    with tempfile.TemporaryDirectory(prefix="ah-sm-it-", dir=pathlib.Path.home()) as tmp:
        base = pathlib.Path(tmp).resolve()
        certs, conf, src = base / "certs", base / "conf", base / "src"
        for p in (certs, conf, src):
            p.mkdir()
        try:
            pki(certs)
            brokers(conf)
            tar = base / "src.tar"
            tar.write_bytes(subprocess.run(["git", "-C", str(ROOT), "archive", "--format=tar", commit], capture_output=True, check=True).stdout)
            with tarfile.open(tar) as t:
                t.extractall(src, filter="data")
            sh("docker", "network", "create", "--internal", net)
            sh("docker", "run", "-d", "--name", names["db"], "--network", net, "--network-alias", "db", "-e", f"MYSQL_ROOT_PASSWORD={secrets.token_urlsafe(18)}",
               "-e", "MYSQL_DATABASE=ah_sysmonitor", "-e", "MYSQL_USER=sysmonitor", "-e", f"MYSQL_PASSWORD={password}", db_image)
            sh("docker", "run", "-d", "--name", names["amqp"], "--network", net, "--network-alias", "amqp-broker", "--network-alias", "amqp",
               "-v", f"{certs}:/certs:ro", "-v", f"{conf / 'rabbitmq.conf'}:/etc/rabbitmq/rabbitmq.conf:ro",
               "-v", f"{conf / 'advanced.config'}:/etc/rabbitmq/advanced.config:ro", "-v", f"{conf / 'enabled_plugins'}:/etc/rabbitmq/enabled_plugins:ro",
               "-v", f"{conf / 'definitions.json'}:/etc/rabbitmq/definitions.json:ro", RABBITMQ_IMAGE)
            sh("docker", "run", "-d", "--name", names["mqtt"], "--network", net, "--network-alias", "mqtt-broker", "-v", f"{certs}:/certs:ro",
               "-v", f"{conf / 'mosquitto.conf'}:/mosquitto/config/mosquitto.conf:ro", MOSQUITTO_IMAGE)
            wait(["docker", "exec", "-e", f"MYSQL_PWD={password}", names["db"], "mysql", "-u", "sysmonitor", "-h", "127.0.0.1", "-e", "SELECT 1", "ah_sysmonitor"], "MySQL")
            wait(["docker", "exec", names["amqp"], "rabbitmq-diagnostics", "-q", "check_port_listener", "5671"], "RabbitMQ")
            time.sleep(3)  # MySQL restarts once after its init scripts
            url = "jdbc:mariadb://db:3306/ah_sysmonitor?sslMode=trust"
            with log.open("w") as f:
                code = subprocess.run(["docker", "run", "--rm", "--network", net, "-v", f"{src / 'ah5-SysMonitor-java-spring'}:/src", "-v", f"{certs}:/pki:ro",
                                       "-v", f"{pathlib.Path.home()}/.m2:/root/.m2", "-w", "/src", MAVEN_IMAGE, "mvn", "-B", "-o", "test",
                                       "-Dtest=BrokerRoundTripTest", "-Dsurefire.failIfNoSpecifiedTests=false", "-Dsysmonitor.it.broker.dir=/pki",
                                       "-Dsysmonitor.it.amqp.host=amqp-broker", "-Dsysmonitor.it.mqtt.host=mqtt-broker",
                                       f"-Dsysmonitor.it.db.url={url}", f"-Dsysmonitor.it.db.password={password}"], stdout=f, stderr=subprocess.STDOUT).returncode
            summary = [re.sub(r"^.*?(Tests run:)", r"\1", l).strip() for l in log.read_text(errors="replace").splitlines() if "Tests run:" in l and " -- in " not in l]
            ok = code == 0 and bool(summary) and "Skipped: 0" in summary[-1] and not summary[-1].startswith("Tests run: 0")
            result |= {"result": "PASS" if ok else "FAIL", "tests": summary[-1] if summary else None}
        except Exception as ex:  # noqa: BLE001 - recorded in the result; messages never carry the passwords
            result |= {"result": "FAIL", "error": str(ex).replace(password, "***")}
        finally:
            for n in names.values():
                subprocess.run(["docker", "rm", "-f", n], capture_output=True)
            subprocess.run(["docker", "network", "rm", net], capture_output=True)
    result |= {"seconds": round(time.time() - start), "log": f"build-logs/{log.name}"}
    (out / "sysmonitor-broker-test.json").write_text(json.dumps(result, indent=2) + "\n")
    print(result["result"], "ah5-SysMonitor-java-spring BrokerRoundTripTest", result.get("tests") or result.get("error"))
    raise SystemExit(0 if result["result"] == "PASS" else 1)


if __name__ == "__main__":
    main()
