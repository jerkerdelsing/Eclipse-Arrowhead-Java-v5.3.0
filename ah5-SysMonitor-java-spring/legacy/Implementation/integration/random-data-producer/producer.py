#!/usr/bin/env python3
"""X.509 authenticated Arrowhead random-number producer."""

import json
import os
import secrets
import ssl
import threading
import time
import urllib.request
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

try:
    import paho.mqtt.client as mqtt
    import paho.mqtt.publish as mqtt_publish
except ImportError:
    mqtt = None
    mqtt_publish = None

SYSTEM_NAME = os.getenv("ARROWHEAD_SYSTEM_NAME", "RandomDataProducer")
REGISTRY_URL = os.getenv("SERVICE_REGISTRY_URL", "http://serviceregistry:8443")
ORCHESTRATOR_URL = os.getenv("ORCHESTRATOR_URL", "http://serviceorchestration-dynamic:8441")
AUTHENTICATION_URL = os.getenv("AUTHENTICATION_URL", "https://authentication:8444")
AUTHENTICATION_PASSWORD = os.getenv("AUTHENTICATION_PASSWORD", "RDP-test-52")
INTERVAL_SECONDS = int(os.getenv("PUBLISH_INTERVAL_SECONDS", "30"))
PORT = int(os.getenv("PORT", "8463"))
CA_CERT = os.getenv("TLS_CA_CERT", "/certs/ca.crt")
CLIENT_CERT = os.getenv("TLS_CLIENT_CERT", "/certs/random-data-producer.crt")
CLIENT_KEY = os.getenv("TLS_CLIENT_KEY", "/certs/random-data-producer.key")
MQTT_HOST = os.getenv("MQTT_HOST", "mqtt-broker")
MQTT_PORT = int(os.getenv("MQTT_PORT", "8883"))
LOCAL_CLOUD = os.getenv("LOCAL_CLOUD", "TestCloud")

state_lock = threading.Lock()
state = {
    "status": "starting",
    "lastValue": None,
    "lastPublishedAt": None,
    "publishedCount": 0,
    "lastError": None,
    "orchestration": {"provider": None, "serviceInstanceId": None, "endpoint": None, "resolvedAt": None},
    "registration": {"registered": False, "instance": None, "error": None},
}
identity_lock = threading.Lock()
identity_token = {"value": None, "refreshAt": 0}


def random_value():
    return secrets.randbelow(101)


def tls_context():
    context = ssl.create_default_context(ssl.Purpose.SERVER_AUTH, cafile=CA_CERT)
    context.minimum_version = ssl.TLSVersion.TLSv1_2
    context.load_cert_chain(CLIENT_CERT, CLIENT_KEY)
    context.check_hostname = True
    context.verify_mode = ssl.CERT_REQUIRED
    return context


def get_identity_token():
    with identity_lock:
        if identity_token["value"] and identity_token["refreshAt"] > time.time():
            return identity_token["value"]
        payload = {"systemName": SYSTEM_NAME, "credentials": {"password": AUTHENTICATION_PASSWORD}}
        request = urllib.request.Request(
            f"{AUTHENTICATION_URL}/authentication/identity/login",
            data=json.dumps(payload).encode(), headers={"Content-Type": "application/json"}, method="POST",
        )
        with urllib.request.urlopen(request, context=tls_context(), timeout=5) as response:
            result = json.loads(response.read())
        identity_token.update({"value": result["token"], "refreshAt": time.time() + 240})
        return identity_token["value"]


def authorization_header():
    return {"Authorization": f"Bearer IDENTITY-TOKEN//{get_identity_token()}"}


def registry_request(path, payload):
    request = urllib.request.Request(
        f"{REGISTRY_URL}{path}",
        data=json.dumps(payload).encode(),
        headers={"Authorization": f"Bearer SYSTEM//{SYSTEM_NAME}", "Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=5) as response:
        body = response.read()
        return json.loads(body) if body else None


def register():
    try:
        registry_request("/serviceregistry/system-discovery/register", {
            "metadata": {"role": "random-data-producer", "authentication": "X.509/mTLS"},
            "version": "1.0.0",
            "addresses": ["random-data-producer"],
            "deviceName": None,
        })
        result = registry_request("/serviceregistry/service-discovery/register", {
            "serviceDefinitionName": "randomNumberProducer",
            "version": "1.0.0",
            "expiresAt": None,
            "metadata": {"range": "0..100", "intervalSeconds": INTERVAL_SECONDS, "transportSecurity": "mutualTLS"},
            "interfaces": [{
                "templateName": "generic_https",
                "protocol": "https",
                "policy": "CERT_AUTH",
                "properties": {
                    "accessAddresses": ["random-data-producer"],
                    "accessPort": PORT,
                    "basePath": "/random-data-producer",
                    "operations": {"latest": {"method": "GET", "path": "/latest"}},
                },
            }],
        })
        registry_request("/serviceregistry/service-discovery/register", {
            "serviceDefinitionName": "sysMonitor", "version": "1.0.0", "expiresAt": None,
            "metadata": {"contract": "SysMonitor-MQTT-IDD-5.2.2", "schemaVersion": "1.0",
                         "publisherMode": "native", "observationTypes": ["echo", "power"]},
            "interfaces": [{"templateName": "generic_mqtts", "protocol": "ssl", "policy": "CERT_AUTH",
                "properties": {"accessAddresses": [MQTT_HOST], "accessPort": MQTT_PORT,
                    "baseTopic": f"arrowhead/{LOCAL_CLOUD}/sysmonitor/{SYSTEM_NAME}/randomNumberProducer",
                    "operations": ["echo", "power"]}}],
        })
        with state_lock:
            state["registration"] = {"registered": True, "instance": result["instanceId"], "error": None}
        print(f"Registered {result['instanceId']}", flush=True)
    except Exception as error:
        with state_lock:
            state["registration"] = {"registered": False, "instance": None, "error": str(error)}
        print(f"Registration failed: {error}", flush=True)
        timer = threading.Timer(10, register)
        timer.daemon = True
        timer.start()


def orchestrate_historian():
    payload = {
        "serviceRequirement": {
            "serviceDefinition": "historian",
            "operations": ["ingest"],
            "versions": ["1.0.0"],
            "interfaceTemplateNames": ["generic_https"],
            "securityPolicies": ["CERT_AUTH"],
        },
        "orchestrationFlags": {},
    }
    request = urllib.request.Request(
        f"{ORCHESTRATOR_URL}/serviceorchestration/orchestration/pull",
        data=json.dumps(payload).encode(),
        headers={**authorization_header(), "Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(request, context=tls_context(), timeout=10) as response:
        result = json.loads(response.read())
    if not result.get("results"):
        raise RuntimeError("Orchestration returned no authorized historian provider")
    selected = result["results"][0]
    interface = next((item for item in selected.get("interfaces", [])
                      if item.get("protocol") == "https" and item.get("policy") == "CERT_AUTH"), None)
    if not interface:
        raise RuntimeError("Orchestration result has no HTTPS/CERT_AUTH interface")
    properties = interface.get("properties", {})
    operation = properties.get("operations", {}).get("ingest")
    if not operation or operation.get("method") != "POST":
        raise RuntimeError("Orchestrated historian does not advertise a POST ingest operation")
    addresses = properties.get("accessAddresses", [])
    if not addresses:
        raise RuntimeError("Orchestration result has no access address")
    endpoint = f"https://{addresses[0]}:{int(properties['accessPort'])}{properties.get('basePath', '')}{operation['path']}"
    orchestration = {
        "provider": selected.get("providerName"),
        "serviceInstanceId": selected.get("serviceInstanceId"),
        "endpoint": endpoint,
        "resolvedAt": time.time(),
    }
    with state_lock:
        state["orchestration"] = orchestration
    return endpoint


def publish_once(context=None):
    endpoint = orchestrate_historian()
    value = random_value()
    now = time.time()
    payload = [{
        "bn": "urn:arrowhead:random-data-producer:",
        "bt": now,
        "n": "random-number",
        "u": "1",
        "v": value,
    }]
    request = urllib.request.Request(
        endpoint,
        data=json.dumps(payload).encode(),
        headers={"Content-Type": "application/senml+json", **authorization_header()},
        method="POST",
    )
    with urllib.request.urlopen(request, context=context or tls_context(), timeout=10) as response:
        result = json.loads(response.read())
    if mqtt_publish is not None:
        monitoring = {
            "schemaVersion": "1.0",
            "observationId": f"rdp-{int(now * 1000)}",
            "providerSystem": SYSTEM_NAME,
            "serviceInstance": "randomNumberProducer",
            "observationType": "power",
            "sourceTime": datetime.fromtimestamp(now, timezone.utc).isoformat().replace("+00:00", "Z"),
            "sequence": state["publishedCount"] + 1,
            "value": {"supplyType": "line", "levelPercent": value},
            "unit": "%",
            "quality": "good",
            "metadata": {"source": "RandomDataProducer"},
        }
        mqtt_publish.single(
            f"arrowhead/{LOCAL_CLOUD}/sysmonitor/{SYSTEM_NAME}/randomNumberProducer/power",
            json.dumps(monitoring, separators=(",", ":")), qos=1, retain=False,
            hostname=MQTT_HOST, port=MQTT_PORT, protocol=mqtt.MQTTv5,
            tls={"ca_certs": CA_CERT, "certfile": CLIENT_CERT, "keyfile": CLIENT_KEY,
                 "tls_version": ssl.PROTOCOL_TLS_CLIENT},
        )
        echo = {**monitoring, "observationId": f"rdp-echo-{int(now * 1000)}",
                "observationType": "echo", "value": {"status": "alive"}, "unit": None}
        mqtt_publish.single(
            f"arrowhead/{LOCAL_CLOUD}/sysmonitor/{SYSTEM_NAME}/randomNumberProducer/echo",
            json.dumps(echo, separators=(",", ":")), qos=1, retain=False,
            hostname=MQTT_HOST, port=MQTT_PORT, protocol=mqtt.MQTTv5,
            tls={"ca_certs": CA_CERT, "certfile": CLIENT_CERT, "keyfile": CLIENT_KEY,
                 "tls_version": ssl.PROTOCOL_TLS_CLIENT},
        )
    with state_lock:
        state.update({
            "status": "running",
            "lastValue": value,
            "lastPublishedAt": now,
            "publishedCount": state["publishedCount"] + 1,
            "lastError": None,
        })
    print(f"Published random-number={value}; DataManager total={result.get('total')}", flush=True)
    return value


def publishing_loop():
    context = tls_context()
    while True:
        started = time.monotonic()
        try:
            publish_once(context)
        except Exception as error:
            with state_lock:
                state.update({"status": "degraded", "lastError": str(error)})
            print(f"Publish failed: {error}", flush=True)
        elapsed = time.monotonic() - started
        time.sleep(max(0, INTERVAL_SECONDS - elapsed))


class StatusHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path not in ("/health", "/random-data-producer/latest"):
            return self.send_json(404, {"error": "Not found"})
        with state_lock:
            payload = dict(state)
        status = 200 if payload["status"] in ("running", "starting") else 503
        self.send_json(status, payload)

    def send_json(self, status, payload):
        body = json.dumps(payload, separators=(",", ":")).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, fmt, *args):
        return


def main():
    threading.Thread(target=register, daemon=True).start()
    threading.Thread(target=publishing_loop, daemon=True).start()
    server = ThreadingHTTPServer(("0.0.0.0", PORT), StatusHandler)
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.minimum_version = ssl.TLSVersion.TLSv1_2
    context.load_cert_chain(CLIENT_CERT, CLIENT_KEY)
    context.load_verify_locations(CA_CERT)
    context.verify_mode = ssl.CERT_REQUIRED
    server.socket = context.wrap_socket(server.socket, server_side=True)
    print(f"RandomDataProducer mTLS status service listening on {PORT}", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
