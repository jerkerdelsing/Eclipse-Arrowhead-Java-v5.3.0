# SPDX-License-Identifier: EPL-2.0
"""Device agent channel (2026-11-09): mutual TLS, allowed clients, CRL (fail closed), plain HTTP only on loopback,
listening port overrides. Run from device_agent/python:  python3 -m unittest discover -s tests"""
import http.client
import json
import os
import shutil
import ssl
import subprocess
import sys
import tempfile
import threading
import time
import unittest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from agent_server import AgentServer, Settings, TlsSettings, resolve_port, settings_from_config  # noqa: E402

DQE = "DeviceQoSEvaluator.TestCloud.Company.arrowhead.eu"
OTHER = "SysMonitor.TestCloud.Company.arrowhead.eu"


def openssl(*args, cwd):
    subprocess.run(["openssl", *args], cwd=cwd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


@unittest.skipUnless(shutil.which("openssl"), "openssl is needed to build the test PKI")
class AgentChannelTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.dir = d = tempfile.mkdtemp(prefix="device-agent-test-")
        open(os.path.join(d, "index.txt"), "w").close()
        with open(os.path.join(d, "crlnumber"), "w") as f:
            f.write("1000\n")
        with open(os.path.join(d, "ca.cnf"), "w") as f:
            f.write(f"[ca]\ndefault_ca=c\n[c]\ndatabase={d}/index.txt\ncrlnumber={d}/crlnumber\n"
                    f"certificate={d}/ca.crt\nprivate_key={d}/ca.key\ndefault_md=sha256\ndefault_crl_days=30\n")
        openssl("req", "-x509", "-newkey", "ec", "-pkeyopt", "ec_paramgen_curve:P-256", "-nodes", "-days", "30",
                "-subj", "/CN=TestCloud CA", "-keyout", "ca.key", "-out", "ca.crt",
                "-addext", "basicConstraints=critical,CA:TRUE", "-addext", "keyUsage=critical,keyCertSign,cRLSign", cwd=d)
        for name, cn, usage, san in [("agent", "Device1.TestCloud.Company.arrowhead.eu", "serverAuth", "IP:127.0.0.1,DNS:localhost"),
                                     ("dqe", DQE, "clientAuth", "DNS:deviceqosevaluator"),
                                     ("other", OTHER, "clientAuth", "DNS:sysmonitor"),
                                     ("revoked", DQE, "clientAuth", "DNS:deviceqosevaluator")]:
            with open(os.path.join(d, f"{name}.ext"), "w") as f:
                f.write(f"basicConstraints=CA:FALSE\nextendedKeyUsage={usage}\nsubjectAltName={san}\n")
            openssl("req", "-newkey", "ec", "-pkeyopt", "ec_paramgen_curve:P-256", "-nodes", "-subj", f"/CN={cn}",
                    "-keyout", f"{name}.key", "-out", f"{name}.csr", cwd=d)
            openssl("x509", "-req", "-in", f"{name}.csr", "-CA", "ca.crt", "-CAkey", "ca.key", "-CAcreateserial",
                    "-days", "30", "-extfile", f"{name}.ext", "-out", f"{name}.crt", cwd=d)
        openssl("ca", "-config", "ca.cnf", "-revoke", "revoked.crt", "-batch", cwd=d)
        openssl("ca", "-config", "ca.cnf", "-gencrl", "-out", "ca.crl", "-batch", cwd=d)

        cls.settings = Settings(address="127.0.0.1", port=0, tls=TlsSettings(
            enabled=True, certificate=f"{d}/agent.crt", private_key=f"{d}/agent.key", ca=f"{d}/ca.crt",
            crl=f"{d}/ca.crl", allowed_clients=[DQE]))
        cls.server = AgentServer(cls.settings, lambda batch, oids: {"1.4": [12.5] * batch}, 30)
        cls.port = cls.server.server_address[1]
        threading.Thread(target=cls.server.serve_forever, daemon=True).start()

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()
        shutil.rmtree(cls.dir, ignore_errors=True)

    def get(self, client=None, path="/device-qos?batch=3", method="GET"):
        context = ssl.create_default_context(cafile=f"{self.dir}/ca.crt")
        if client:
            context.load_cert_chain(f"{self.dir}/{client}.crt", f"{self.dir}/{client}.key")
        connection = http.client.HTTPSConnection("127.0.0.1", self.port, context=context, timeout=10)
        try:
            connection.request(method, path)
            response = connection.getresponse()
            return response.status, response.read()
        finally:
            connection.close()

    def test_the_device_qos_evaluator_reads_the_measurements(self):
        status, body = self.get("dqe")
        self.assertEqual(status, 200)
        self.assertEqual(json.loads(body), {"1.4": [12.5, 12.5, 12.5]})

    def test_a_client_without_certificate_is_refused(self):
        with self.assertRaises((ssl.SSLError, ConnectionError, http.client.RemoteDisconnected, OSError)):
            self.get(None)

    def test_another_system_is_refused(self):
        status, _ = self.get("other")
        self.assertEqual(status, 403)

    def test_a_revoked_certificate_is_refused(self):
        with self.assertRaises((ssl.SSLError, ConnectionError, http.client.RemoteDisconnected, OSError)):
            self.get("revoked")

    def test_plain_http_gets_no_answer(self):
        connection = http.client.HTTPConnection("127.0.0.1", self.port, timeout=10)
        with self.assertRaises((ConnectionError, http.client.RemoteDisconnected, http.client.BadStatusLine, OSError)):
            connection.request("GET", "/device-qos")
            connection.getresponse().read()
        connection.close()

    def test_only_get_on_device_qos_is_served(self):
        self.assertEqual(self.get("dqe", path="/other")[0], 404)
        self.assertEqual(self.get("dqe", method="POST")[0], 405)
        self.assertEqual(self.get("dqe", path="/device-qos?batch=31")[0], 400)

    def test_an_expired_crl_refuses_every_client(self):
        d = self.dir
        openssl("ca", "-config", "ca.cnf", "-gencrl", "-crlsec", "1", "-out", "expired.crl", "-batch", cwd=d)
        time.sleep(2)  # past nextUpdate
        expired = TlsSettings(**{**self.settings.tls.__dict__, "crl": f"{d}/expired.crl"})
        server = AgentServer(Settings("127.0.0.1", 0, expired), lambda b, o: {}, 30)
        threading.Thread(target=server.serve_forever, daemon=True).start()
        try:
            context = ssl.create_default_context(cafile=f"{d}/ca.crt")
            context.load_cert_chain(f"{d}/dqe.crt", f"{d}/dqe.key")
            connection = http.client.HTTPSConnection("127.0.0.1", server.server_address[1], context=context, timeout=10)
            with self.assertRaises((ssl.SSLError, ConnectionError, http.client.RemoteDisconnected, OSError)):
                connection.request("GET", "/device-qos")
                connection.getresponse().read()
            connection.close()
        finally:
            server.shutdown()
            server.server_close()


class ConfigurationTest(unittest.TestCase):

    def test_port_precedence(self):
        self.assertEqual(resolve_port(None, {}, []), 59473)
        self.assertEqual(resolve_port(1234, {}, []), 1234)
        self.assertEqual(resolve_port(1234, {"SERVER_PORT": "2345"}, []), 2345)
        self.assertEqual(resolve_port(1234, {"SERVER_PORT": "2345"}, ["--server.port", "3456"]), 3456)
        self.assertEqual(resolve_port(1234, {}, ["--server.port=4567"]), 4567)
        with self.assertRaises(ValueError):
            resolve_port(70000, {}, [])

    def test_plain_http_is_refused_on_a_network_address(self):
        with self.assertRaises(ValueError):
            settings_from_config({"server": {"address": "0.0.0.0"}, "tls": {"enabled": False}}, {}, [])

    def test_plain_http_is_allowed_on_loopback_only(self):
        settings = settings_from_config({"server": {"address": "127.0.0.1"}, "tls": {"enabled": False}}, {}, [])
        self.assertFalse(settings.tls.enabled)

    def test_tls_is_the_default_and_needs_its_files(self):
        with self.assertRaises(ValueError) as refused:
            settings_from_config({}, {}, [])
        self.assertIn("tls.certificate", str(refused.exception))

    def test_allowed_clients_are_required(self):
        d = tempfile.mkdtemp()
        try:
            for name in ("a.crt", "a.key", "ca.crt", "ca.crl"):
                open(os.path.join(d, name), "w").close()
            tls = {"certificate": "a.crt", "private_key": "a.key", "ca": "ca.crt", "crl": "ca.crl"}
            with self.assertRaises(ValueError) as refused:
                settings_from_config({"tls": tls}, {}, [], d)
            self.assertIn("allowed_clients", str(refused.exception))
        finally:
            shutil.rmtree(d)


if __name__ == "__main__":
    unittest.main()
