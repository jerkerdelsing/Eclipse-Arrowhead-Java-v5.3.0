#!/usr/bin/env python3
"""Arrowhead 5 test DataManager with persistent SenML storage."""

import json
import os
import sqlite3
import ssl
import threading
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

try:
    import paho.mqtt.client as mqtt
    import paho.mqtt.publish as mqtt_publish
except ImportError:
    mqtt = None
    mqtt_publish = None

PORT = int(os.getenv("PORT", "8461"))
TLS_PORT = int(os.getenv("TLS_PORT", "8462"))
DB_PATH = os.getenv("DB_PATH", "/data/datamanager.db")
REGISTRY_URL = os.getenv("SERVICE_REGISTRY_URL", "http://serviceregistry:8443")
AUTHENTICATION_URL = os.getenv("AUTHENTICATION_URL", "https://authentication:8444")
AUTHENTICATION_PASSWORD = os.getenv("AUTHENTICATION_PASSWORD", "DM-test-52")
SYSTEM_NAME = os.getenv("ARROWHEAD_SYSTEM_NAME", "DataManager")
CA_CERT = os.getenv("TLS_CA_CERT", "/certs/ca.crt")
SERVER_CERT = os.getenv("TLS_SERVER_CERT", "/certs/datamanager.crt")
SERVER_KEY = os.getenv("TLS_SERVER_KEY", "/certs/datamanager.key")
MQTT_HOST = os.getenv("MQTT_HOST", "mqtt-broker")
MQTT_PORT = int(os.getenv("MQTT_PORT", "8883"))
LOCAL_CLOUD = os.getenv("LOCAL_CLOUD", "TestCloud")
MONITOR_INTERVAL_SECONDS = int(os.getenv("MONITOR_INTERVAL_SECONDS", "30"))
MONITOR_BASE_TOPIC = f"arrowhead/{LOCAL_CLOUD}/sysmonitor/{SYSTEM_NAME}/historianMonitoring"
MAX_BODY_BYTES = 1024 * 1024
VALUE_FIELDS = ("v", "vs", "vb", "vd", "s")


class Store:
    def __init__(self, path):
        self.path = path
        os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
        with self.connect() as db:
            db.execute("""
                CREATE TABLE IF NOT EXISTS measurements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    measured_at REAL NOT NULL,
                    unit TEXT,
                    value_json TEXT NOT NULL,
                    record_json TEXT NOT NULL,
                    received_at REAL NOT NULL
                )
            """)
            db.execute("CREATE INDEX IF NOT EXISTS idx_measurements_name_time ON measurements(name, measured_at DESC)")

    def connect(self):
        db = sqlite3.connect(self.path, timeout=10)
        db.row_factory = sqlite3.Row
        return db

    def ingest(self, payload):
        records = payload if isinstance(payload, list) else [payload]
        if not records or not all(isinstance(record, dict) for record in records):
            raise ValueError("SenML payload must be an object or a non-empty array")
        now = time.time()
        base_name = ""
        base_time = 0.0
        rows = []
        for record in records:
            base_name = str(record.get("bn", base_name))
            base_time = float(record.get("bt", base_time))
            name = f"{base_name}{record.get('n', '')}"
            if not name:
                raise ValueError("Each SenML record must resolve to a name using bn and/or n")
            value_field = next((field for field in VALUE_FIELDS if field in record), None)
            if not value_field:
                raise ValueError(f"SenML record '{name}' has no value field")
            measured_at = base_time + float(record.get("t", 0)) if base_time or "t" in record else now
            value = {value_field: record[value_field]}
            rows.append((name, measured_at, record.get("u"), json.dumps(value), json.dumps(record), now))
        with self.connect() as db:
            db.executemany(
                "INSERT INTO measurements(name, measured_at, unit, value_json, record_json, received_at) VALUES(?,?,?,?,?,?)",
                rows,
            )
        return len(rows)

    @staticmethod
    def serialize(row):
        return {
            "id": row["id"],
            "name": row["name"],
            "time": row["measured_at"],
            "unit": row["unit"],
            "value": json.loads(row["value_json"]),
            "record": json.loads(row["record_json"]),
            "receivedAt": row["received_at"],
        }

    def query(self, name=None, start=None, end=None, limit=200):
        clauses, values = [], []
        if name:
            clauses.append("name = ?")
            values.append(name)
        if start is not None:
            clauses.append("measured_at >= ?")
            values.append(start)
        if end is not None:
            clauses.append("measured_at <= ?")
            values.append(end)
        where = f"WHERE {' AND '.join(clauses)}" if clauses else ""
        values.append(limit)
        with self.connect() as db:
            rows = db.execute(
                f"SELECT * FROM measurements {where} ORDER BY measured_at DESC, id DESC LIMIT ?", values
            ).fetchall()
        return [self.serialize(row) for row in rows]

    def latest(self, name=None, limit=100):
        condition = "WHERE name = ?" if name else ""
        values = [name] if name else []
        values.append(limit)
        sql = f"""
            SELECT m.* FROM measurements m
            JOIN (SELECT name, MAX(id) id FROM measurements {condition} GROUP BY name) latest ON latest.id = m.id
            ORDER BY m.name LIMIT ?
        """
        with self.connect() as db:
            return [self.serialize(row) for row in db.execute(sql, values).fetchall()]

    def count(self):
        with self.connect() as db:
            return db.execute("SELECT COUNT(*) FROM measurements").fetchone()[0]


store = None
registration = {"registered": False, "instances": [], "error": None, "checkedAt": None}
identity_lock = threading.Lock()
identity_token = {"value": None, "refreshAt": 0}
monitoring_state = {"publishedCount": 0, "lastPublishedAt": None, "lastError": None}
monitoring_lock = threading.Lock()


def authenticated_request(url, method="GET", payload=None, token=None):
    context = ssl.create_default_context(ssl.Purpose.SERVER_AUTH, cafile=CA_CERT)
    context.minimum_version = ssl.TLSVersion.TLSv1_2
    context.load_cert_chain(SERVER_CERT, SERVER_KEY)
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer IDENTITY-TOKEN//{token}"
    request = urllib.request.Request(url, data=None if payload is None else json.dumps(payload).encode(), headers=headers, method=method)
    with urllib.request.urlopen(request, context=context, timeout=5) as response:
        body = response.read()
        return json.loads(body) if body else None


def get_identity_token():
    with identity_lock:
        if identity_token["value"] and identity_token["refreshAt"] > time.time():
            return identity_token["value"]
        result = authenticated_request(
            f"{AUTHENTICATION_URL}/authentication/identity/login", "POST",
            {"systemName": SYSTEM_NAME, "credentials": {"password": AUTHENTICATION_PASSWORD}},
        )
        identity_token.update({"value": result["token"], "refreshAt": time.time() + 240})
        return identity_token["value"]


def verify_caller(handler):
    header = handler.headers.get("Authorization", "")
    prefix = "Bearer IDENTITY-TOKEN//"
    if not header.startswith(prefix):
        raise PermissionError("A valid Arrowhead identity token is required")
    caller_token = header[len(prefix):]
    result = authenticated_request(
        f"{AUTHENTICATION_URL}/authentication/identity/verify/{caller_token}", token=get_identity_token()
    )
    if not result.get("verified"):
        raise PermissionError("Invalid or expired Arrowhead identity token")
    peer = handler.connection.getpeercert()
    common_name = next((value for group in peer.get("subject", ()) for key, value in group if key == "commonName"), "")
    expected = f"{result['systemName']}.TestCloud.Company.arrowhead.eu"
    if common_name != expected:
        raise PermissionError("X.509 identity does not match the authenticated Arrowhead system")
    return result["systemName"]


def registry_request(path, payload, identity=SYSTEM_NAME):
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
    global registration
    try:
        registry_request("/serviceregistry/system-discovery/register", {
            "metadata": {"role": "senml-data-store", "storage": "sqlite"},
            "version": "1.0.0",
            "addresses": ["datamanager"],
            "deviceName": None,
        })
        definitions = [
            ("historian", {"ingest": {"method": "POST", "path": "/historian"}, "query": {"method": "GET", "path": "/historian"}}),
            ("monitoringHistorian", {"ingest": {"method": "POST", "path": "/historian"}}),
            ("dataCache", {"latest": {"method": "GET", "path": "/cache"}}),
        ]
        instances = []
        for service_name, operations in definitions:
            result = registry_request("/serviceregistry/service-discovery/register", {
                "serviceDefinitionName": service_name,
                "version": "1.0.0",
                "expiresAt": None,
                "metadata": {"dataModel": "SenML"},
                "interfaces": [{
                    "templateName": "generic_https",
                    "protocol": "https",
                    "policy": "CERT_AUTH",
                    "properties": {
                        "accessAddresses": ["datamanager"],
                        "accessPort": TLS_PORT,
                        "basePath": "/datamanager",
                        "operations": operations,
                    },
                }],
            })
            instances.append(result["instanceId"])
        result = registry_request("/serviceregistry/service-discovery/register", {
            "serviceDefinitionName": "sysMonitor",
            "version": "1.0.0",
            "expiresAt": None,
            "metadata": {
                "observationTypes": ["echo", "power"],
                "intervalSeconds": MONITOR_INTERVAL_SECONDS,
                "contract": "SysMonitor-MQTT-IDD-5.2.2",
                "publisherMode": "native",
            },
            "interfaces": [{
                "templateName": "generic_mqtts",
                "protocol": "ssl",
                "policy": "CERT_AUTH",
                "properties": {
                    "accessAddresses": [MQTT_HOST],
                    "accessPort": MQTT_PORT,
                    "baseTopic": MONITOR_BASE_TOPIC,
                    "operations": ["echo", "power"],
                },
            }],
        })
        instances.append(result["instanceId"])
        registration = {"registered": True, "instances": instances, "error": None, "checkedAt": time.time()}
        print(f"Registered {SYSTEM_NAME}: {', '.join(instances)}", flush=True)
    except Exception as error:
        registration = {"registered": False, "instances": [], "error": str(error), "checkedAt": time.time()}
        print(f"ServiceRegistry registration failed: {error}", flush=True)
        timer = threading.Timer(10, register)
        timer.daemon = True
        timer.start()


class Handler(BaseHTTPRequestHandler):
    server_version = "ArrowheadDataManager/1.0"

    def send_json(self, status, payload):
        body = json.dumps(payload, separators=(",", ":")).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.end_headers()
        self.wfile.write(body)

    def read_json(self):
        length = int(self.headers.get("Content-Length", "0"))
        if length <= 0 or length > MAX_BODY_BYTES:
            raise ValueError("Request body is empty or exceeds 1 MiB")
        return json.loads(self.rfile.read(length))

    def do_GET(self):
        parsed = urlparse(self.path)
        params = parse_qs(parsed.query)
        try:
            if parsed.path == "/health":
                return self.send_json(200, {
                    "status": "ok", "system": SYSTEM_NAME, "records": store.count(),
                    "tls": {"enabled": True, "mutual": True, "port": TLS_PORT}, "registration": registration,
                    "monitoring": dict(monitoring_state),
                })
            if not getattr(self.server, "secure_channel", False):
                return self.send_json(426, {"error": "HTTPS with a trusted client certificate is required"})
            caller = verify_caller(self)
            if parsed.path == "/datamanager/echo":
                return self.send_json(200, {"status": "ok", "system": SYSTEM_NAME, "time": time.time()})
            if parsed.path == "/datamanager/historian":
                if caller != "LocalCloudManager":
                    return self.send_json(403, {"error": "Caller is not authorized to query historian data"})
                limit = min(max(int(params.get("limit", ["200"])[0]), 1), 1000)
                start = float(params["from"][0]) if "from" in params else None
                end = float(params["to"][0]) if "to" in params else None
                name = params.get("name", [None])[0]
                entries = store.query(name, start, end, limit)
                return self.send_json(200, {"count": len(entries), "entries": entries})
            if parsed.path == "/datamanager/cache":
                if caller != "LocalCloudManager":
                    return self.send_json(403, {"error": "Caller is not authorized to query cache data"})
                name = params.get("name", [None])[0]
                entries = store.latest(name)
                return self.send_json(200, {"count": len(entries), "entries": entries})
            return self.send_json(404, {"error": "Not found"})
        except (ValueError, json.JSONDecodeError) as error:
            return self.send_json(400, {"error": str(error)})
        except PermissionError as error:
            return self.send_json(401, {"error": str(error)})
        except Exception as error:
            return self.send_json(500, {"error": str(error)})

    def do_POST(self):
        try:
            if not getattr(self.server, "secure_channel", False):
                return self.send_json(426, {"error": "HTTPS with a trusted client certificate is required"})
            if urlparse(self.path).path != "/datamanager/historian":
                return self.send_json(404, {"error": "Not found"})
            if verify_caller(self) not in ("RandomDataProducer", "SysMonitor"):
                return self.send_json(403, {"error": "Caller is not authorized to ingest historian data"})
            count = store.ingest(self.read_json())
            return self.send_json(201, {"stored": count, "total": store.count()})
        except (ValueError, json.JSONDecodeError) as error:
            return self.send_json(400, {"error": str(error)})
        except PermissionError as error:
            return self.send_json(401, {"error": str(error)})
        except Exception as error:
            return self.send_json(500, {"error": str(error)})

    def log_message(self, fmt, *args):
        print(f"{self.address_string()} - {fmt % args}", flush=True)


def monitoring_observation(record_count, sequence, now=None):
    observed = now or time.time()
    return {
        "schemaVersion": "1.0",
        "observationId": f"datamanager-echo-{int(observed * 1000)}",
        "providerSystem": SYSTEM_NAME,
        "serviceInstance": "historianMonitoring",
        "observationType": "echo",
        "sourceTime": datetime.fromtimestamp(observed, timezone.utc).isoformat().replace("+00:00", "Z"),
        "sequence": sequence,
        "value": {"status": "alive", "recordCount": record_count},
        "quality": "good",
        "metadata": {"storage": "sqlite", "publishIntervalSeconds": MONITOR_INTERVAL_SECONDS},
    }


def publish_monitoring_once():
    if mqtt_publish is None:
        raise RuntimeError("paho-mqtt is not installed")
    with monitoring_lock:
        sequence = monitoring_state["publishedCount"] + 1
    payload = monitoring_observation(store.count(), sequence)
    mqtt_publish.single(
        f"{MONITOR_BASE_TOPIC}/echo", json.dumps(payload, separators=(",", ":")), qos=1, retain=False,
        hostname=MQTT_HOST, port=MQTT_PORT, protocol=mqtt.MQTTv5,
        tls={"ca_certs": CA_CERT, "certfile": SERVER_CERT, "keyfile": SERVER_KEY,
             "tls_version": ssl.PROTOCOL_TLS_CLIENT},
    )
    power = {**payload, "observationId": f"datamanager-power-{int(time.time() * 1000)}",
             "observationType": "power", "value": {"supplyType": "line", "state": "available"}, "unit": "1"}
    mqtt_publish.single(
        f"{MONITOR_BASE_TOPIC}/power", json.dumps(power, separators=(",", ":")), qos=1, retain=False,
        hostname=MQTT_HOST, port=MQTT_PORT, protocol=mqtt.MQTTv5,
        tls={"ca_certs": CA_CERT, "certfile": SERVER_CERT, "keyfile": SERVER_KEY,
             "tls_version": ssl.PROTOCOL_TLS_CLIENT},
    )
    with monitoring_lock:
        monitoring_state.update({"publishedCount": sequence, "lastPublishedAt": payload["sourceTime"], "lastError": None})
    print(f"Published DataManager sysMonitor echo sequence={sequence}", flush=True)
    return payload


def monitoring_loop():
    while True:
        started = time.monotonic()
        try:
            publish_monitoring_once()
        except Exception as error:
            with monitoring_lock:
                monitoring_state["lastError"] = str(error)
            print(f"Monitoring publish failed: {error}", flush=True)
        time.sleep(max(0, MONITOR_INTERVAL_SECONDS - (time.monotonic() - started)))


def main():
    global store
    store = Store(DB_PATH)
    threading.Thread(target=register, daemon=True).start()
    threading.Thread(target=monitoring_loop, daemon=True).start()
    health_server = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    health_server.secure_channel = False
    threading.Thread(target=health_server.serve_forever, daemon=True).start()

    secure_server = ThreadingHTTPServer(("0.0.0.0", TLS_PORT), Handler)
    secure_server.secure_channel = True
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.minimum_version = ssl.TLSVersion.TLSv1_2
    context.load_cert_chain(SERVER_CERT, SERVER_KEY)
    context.load_verify_locations(CA_CERT)
    context.verify_mode = ssl.CERT_REQUIRED
    secure_server.socket = context.wrap_socket(secure_server.socket, server_side=True)
    print(f"Arrowhead DataManager health on {PORT}; mTLS API on {TLS_PORT}; database {DB_PATH}", flush=True)
    secure_server.serve_forever()


if __name__ == "__main__":
    main()
