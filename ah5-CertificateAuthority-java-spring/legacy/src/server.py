from __future__ import annotations

import base64
import json
import ipaddress
import os
import re
import sqlite3
import ssl
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timedelta, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec, padding, rsa
from cryptography.x509.oid import NameOID

from .amqp_client import EXCHANGE, AmqpPublisher


SYSTEM_NAME = os.getenv("ARROWHEAD_SYSTEM_NAME", "CertificateAuthority")
LOCAL_CLOUD = os.getenv("LOCAL_CLOUD", "TestCloud")
PORT = int(os.getenv("PORT", "8448"))
DB_PATH = os.getenv("DB_PATH", "/data/certificate-authority.db")
CA_CERT_PATH = os.getenv("TLS_CA_CERT", "/certs/ca.crt")
CA_KEY_PATH = os.getenv("TLS_CA_KEY", "/certs/ca.key")
SERVER_CERT_PATH = os.getenv("TLS_CERT", "/certs/certificate-authority.crt")
SERVER_KEY_PATH = os.getenv("TLS_KEY", "/certs/certificate-authority.key")
SERVICE_REGISTRY_URL = os.getenv("SERVICE_REGISTRY_URL", "https://serviceregistry:8443").rstrip("/")
AUTHENTICATION_URL = os.getenv("AUTHENTICATION_URL", "https://authentication:8444").rstrip("/")
AUTHENTICATOR_PASSWORD = os.getenv("AUTHENTICATOR_PASSWORD", "")
AMQP_HOST = os.getenv("AMQP_HOST", "amqp-broker")
AMQP_PORT = int(os.getenv("AMQP_PORT", "5671"))
MONITOR_INTERVAL = int(os.getenv("MONITOR_INTERVAL_SECONDS", "30"))
DEFAULT_VALIDITY_DAYS = int(os.getenv("CERT_VALIDITY_DAYS", "365"))
ONBOARDING_VALIDITY_HOURS = int(os.getenv("ONBOARDING_CERT_VALIDITY_HOURS", "24"))
# Certificate revocation list (security finding H3): published from the revocation register to CRL_DIR/ca.crl, where
# the MQTT and AMQP brokers and the inter-cloud relay read it. Rewritten atomically at start, after every revocation and
# every CRL_REFRESH_SECONDS; it stays valid CRL_VALIDITY_HOURS, so the brokers fail closed if the CA stops refreshing it.
CRL_DIR = os.getenv("CRL_DIR", "")
CRL_REFRESH_SECONDS = int(os.getenv("CRL_REFRESH_SECONDS", "3600"))
CRL_VALIDITY_HOURS = int(os.getenv("CRL_VALIDITY_HOURS", "168"))
# Arrowhead 5.2.2 certificate profiles (subject dnQualifier) that each authenticated caller may request.
# The CA builds subject and extensions itself, so a CSR can never choose a profile or a CA capability.
SIGNING_PROFILES = {
    "Onboarding": {"on", "de", "sy"},
    "Sysop": {"on", "de", "sy", "op"},
    "LocalCloudManager": {"sy"},
    "CertificateAuthority": {"sy"},
}
PROFILE_NAMES = {"on": "ON_BOARDING", "de": "DEVICE", "sy": "SYSTEM", "op": "OPERATOR"}
# Reserved names (security finding M1): the core and support systems and the operator identities. Only Sysop may
# obtain certificates for them; Onboarding and LocalCloudManager may not, so no device or system can be named like one.
RESERVED_NAMES = frozenset(name.strip() for name in os.getenv("RESERVED_NAMES", ",".join([
    "ServiceRegistry", "Authentication", "ConsumerAuthorization", "DynamicServiceOrchestration",
    "SimpleStoreServiceOrchestration", "Blacklist", "TranslationManager", "DeviceQoSEvaluator", "Onboarding",
    "CertificateAuthority", "DataManager", "TimeManager", "EventHandler", "PlantDescriptionEngine", "Configuration",
    "Choreographer", "SysMonitor", "Gatekeeper", "GateTunnel", "AIGym", "LocalCloudManager", "Sysop",
    "RandomDataProducer", "SmokeClient", "Relay"])).split(",") if name.strip())
RESERVED_NAME_ISSUERS = {"Sysop"}
NAME_PATTERN = re.compile(r"^[A-Z][A-Za-z0-9]{0,62}$")
DNS_PATTERN = re.compile(r"^(?=.{1,253}$)([A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?)(\.[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*$")
BASE_PATH = "/certificate-authority"
SYSMONITOR_ROUTING_KEY = f"{LOCAL_CLOUD}.sysmonitor.{SYSTEM_NAME}.certificateAuthority"


def utcnow() -> datetime:
    return datetime.now(timezone.utc)


def iso(value: datetime) -> str:
    return value.astimezone(timezone.utc).isoformat().replace("+00:00", "Z")


def parse_time(value: Any, fallback: datetime) -> datetime:
    if not value:
        return fallback
    parsed = datetime.fromisoformat(str(value).replace("Z", "+00:00"))
    return parsed if parsed.tzinfo else parsed.replace(tzinfo=timezone.utc)


def b64_der(cert: x509.Certificate) -> str:
    return base64.b64encode(cert.public_bytes(serialization.Encoding.DER)).decode()


def load_certificate(value: str) -> x509.Certificate:
    raw = value.encode()
    if "BEGIN CERTIFICATE" in value:
        return x509.load_pem_x509_certificate(raw)
    return x509.load_der_x509_certificate(base64.b64decode(value, validate=True))


def load_csr(value: str) -> x509.CertificateSigningRequest:
    raw = value.encode()
    if "BEGIN CERTIFICATE REQUEST" in value or "BEGIN NEW CERTIFICATE REQUEST" in value:
        return x509.load_pem_x509_csr(raw)
    return x509.load_der_x509_csr(base64.b64decode(value, validate=True))


def verify_signed_by_ca(cert: x509.Certificate, ca: x509.Certificate) -> None:
    public_key = ca.public_key()
    if isinstance(public_key, rsa.RSAPublicKey):
        public_key.verify(cert.signature, cert.tbs_certificate_bytes, padding.PKCS1v15(), cert.signature_hash_algorithm)
    elif isinstance(public_key, ec.EllipticCurvePublicKey):
        public_key.verify(cert.signature, cert.tbs_certificate_bytes, ec.ECDSA(cert.signature_hash_algorithm))
    else:
        public_key.verify(cert.signature, cert.tbs_certificate_bytes)


class Store:
    def __init__(self, path: str):
        Path(path).parent.mkdir(parents=True, exist_ok=True)
        self.path = path
        self.lock = threading.Lock()
        with self.connection() as db:
            db.executescript("""
                CREATE TABLE IF NOT EXISTS issued_certificates (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  serial_number TEXT UNIQUE NOT NULL,
                  common_name TEXT NOT NULL,
                  created_by TEXT NOT NULL,
                  valid_from TEXT NOT NULL,
                  valid_until TEXT NOT NULL,
                  revoked_at TEXT,
                  certificate_pem TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS trusted_keys (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  public_key_pem TEXT UNIQUE NOT NULL,
                  description TEXT NOT NULL DEFAULT '',
                  valid_after TEXT NOT NULL,
                  valid_before TEXT NOT NULL,
                  created_at TEXT NOT NULL
                );
            """)

    def connection(self):
        db = sqlite3.connect(self.path)
        db.row_factory = sqlite3.Row
        return db

    def add_certificate(self, cert: x509.Certificate, common_name: str, created_by: str) -> int:
        with self.lock, self.connection() as db:
            cursor = db.execute(
                "INSERT INTO issued_certificates(serial_number,common_name,created_by,valid_from,valid_until,certificate_pem) VALUES(?,?,?,?,?,?)",
                (str(cert.serial_number), common_name, created_by, iso(cert.not_valid_before_utc),
                 iso(cert.not_valid_after_utc), cert.public_bytes(serialization.Encoding.PEM).decode()))
            return int(cursor.lastrowid)

    def certificate_by_serial(self, serial: int):
        with self.connection() as db:
            return db.execute("SELECT * FROM issued_certificates WHERE serial_number=?", (str(serial),)).fetchone()

    def list_certificates(self):
        with self.connection() as db:
            return [dict(row) for row in db.execute("SELECT * FROM issued_certificates ORDER BY id DESC")]

    def record_revocation(self, cert: x509.Certificate, created_by: str) -> tuple[int, bool]:
        """Revokes a certificate of this CA by content, also one that this CA did not issue through /sign (for
        example the deployment certificates of generate-x509.sh). Returns (record id, changed)."""
        with self.lock, self.connection() as db:
            row = db.execute("SELECT id, revoked_at FROM issued_certificates WHERE serial_number=?",
                             (str(cert.serial_number),)).fetchone()
            if row:
                if row["revoked_at"]:
                    return int(row["id"]), False
                db.execute("UPDATE issued_certificates SET revoked_at=? WHERE id=?", (iso(utcnow()), row["id"]))
                return int(row["id"]), True
            names = cert.subject.get_attributes_for_oid(NameOID.COMMON_NAME)
            cursor = db.execute(
                "INSERT INTO issued_certificates(serial_number,common_name,created_by,valid_from,valid_until,revoked_at,certificate_pem) "
                "VALUES(?,?,?,?,?,?,?)",
                (str(cert.serial_number), str(names[0].value) if names else "", created_by, iso(cert.not_valid_before_utc),
                 iso(cert.not_valid_after_utc), iso(utcnow()), cert.public_bytes(serialization.Encoding.PEM).decode()))
            return int(cursor.lastrowid), True

    def other_holder(self, common_name: str, caller: str) -> str | None:
        """Who else holds a valid (not revoked, not expired) certificate for this name, if anyone."""
        now = utcnow()
        with self.connection() as db:
            rows = db.execute("SELECT created_by, valid_until FROM issued_certificates WHERE common_name=? "
                              "AND revoked_at IS NULL AND created_by<>?", (common_name, caller)).fetchall()
        return next((row["created_by"] for row in rows if parse_time(row["valid_until"], now) > now), None)

    def revoked_certificates(self) -> list[tuple[int, datetime]]:
        """Serial numbers and revocation times of revoked certificates that have not expired yet."""
        now = utcnow()
        with self.connection() as db:
            rows = db.execute("SELECT serial_number, revoked_at, valid_until FROM issued_certificates "
                              "WHERE revoked_at IS NOT NULL").fetchall()
        return [(int(row["serial_number"]), parse_time(row["revoked_at"], now)) for row in rows
                if parse_time(row["valid_until"], now) > now]

    def revoke(self, cert_id: int, created_by: str | None = None) -> bool:
        """Revokes a certificate; with created_by, only one that this caller requested."""
        with self.lock, self.connection() as db:
            if created_by is None:
                cursor = db.execute("UPDATE issued_certificates SET revoked_at=? WHERE id=? AND revoked_at IS NULL",
                                    (iso(utcnow()), cert_id))
            else:
                cursor = db.execute("UPDATE issued_certificates SET revoked_at=? WHERE id=? AND revoked_at IS NULL "
                                    "AND created_by=?", (iso(utcnow()), cert_id, created_by))
            return cursor.rowcount == 1

    def add_key(self, pem: str, description: str, valid_after: datetime, valid_before: datetime) -> int:
        serialization.load_pem_public_key(pem.encode())
        with self.lock, self.connection() as db:
            cursor = db.execute(
                "INSERT INTO trusted_keys(public_key_pem,description,valid_after,valid_before,created_at) VALUES(?,?,?,?,?)",
                (pem, description, iso(valid_after), iso(valid_before), iso(utcnow())))
            return int(cursor.lastrowid)

    def list_keys(self):
        with self.connection() as db:
            return [dict(row) for row in db.execute("SELECT * FROM trusted_keys ORDER BY id DESC")]

    def delete_key(self, key_id: int) -> bool:
        with self.lock, self.connection() as db:
            return db.execute("DELETE FROM trusted_keys WHERE id=?", (key_id,)).rowcount == 1

    def key_status(self, pem: str) -> tuple[bool, dict[str, Any] | None]:
        with self.connection() as db:
            row = db.execute("SELECT * FROM trusted_keys WHERE public_key_pem=?", (pem,)).fetchone()
        if not row:
            return False, None
        now = utcnow()
        valid = parse_time(row["valid_after"], now) <= now <= parse_time(row["valid_before"], now)
        return valid, dict(row)

    def metrics(self) -> dict[str, int]:
        with self.connection() as db:
            issued = db.execute("SELECT COUNT(*) FROM issued_certificates").fetchone()[0]
            revoked = db.execute("SELECT COUNT(*) FROM issued_certificates WHERE revoked_at IS NOT NULL").fetchone()[0]
            keys = db.execute("SELECT COUNT(*) FROM trusted_keys").fetchone()[0]
        return {"issuedCertificates": issued, "revokedCertificates": revoked, "trustedKeys": keys}


class CertificateAuthority:
    def __init__(self, store: Store):
        self.store = store
        self.ca_cert = x509.load_pem_x509_certificate(Path(CA_CERT_PATH).read_bytes())
        self.ca_key = serialization.load_pem_private_key(Path(CA_KEY_PATH).read_bytes(), password=None)

    def cloud_suffix(self) -> str:
        return self.ca_cert.subject.get_attributes_for_oid(NameOID.COMMON_NAME)[0].value

    def sign(self, payload: dict[str, Any], caller: str) -> dict[str, Any]:
        encoded = payload.get("encodedCSR") or payload.get("csr")
        if not encoded:
            raise ValueError("encodedCSR is required")
        csr = load_csr(str(encoded))
        if not csr.is_signature_valid:
            raise ValueError("CSR signature is invalid")
        common_names = csr.subject.get_attributes_for_oid(NameOID.COMMON_NAME)
        if len(common_names) != 1:
            raise ValueError("CSR must carry exactly one common name")
        common_name = str(common_names[0].value)
        name, _, suffix = common_name.partition(".")
        if suffix != self.cloud_suffix() or not NAME_PATTERN.match(name):
            raise ValueError(f"common name must be <Name>.{self.cloud_suffix()} with a PascalCase name")
        if name in RESERVED_NAMES and caller not in RESERVED_NAME_ISSUERS:
            raise PermissionError(f"{name} is a reserved system name")
        holder = self.store.other_holder(common_name, caller)
        if holder:
            raise PermissionError(f"{name} already has a valid certificate requested by {holder}")
        profile = str(payload.get("profile") or requested_profile(csr) or "sy")
        if profile not in PROFILE_NAMES:
            raise ValueError("profile must be one of " + ", ".join(sorted(PROFILE_NAMES)))
        if profile not in SIGNING_PROFILES.get(caller, set()):
            raise PermissionError(f"{caller} may not request {PROFILE_NAMES[profile]} certificates")
        now = utcnow()
        limit = timedelta(hours=ONBOARDING_VALIDITY_HOURS) if profile == "on" else timedelta(days=DEFAULT_VALIDITY_DAYS)
        start = parse_time(payload.get("validAfter"), now - timedelta(minutes=1))
        end = parse_time(payload.get("validBefore"), start + limit)
        end = min(end, self.ca_cert.not_valid_after_utc)
        if end <= start or end > start + limit:
            raise ValueError(f"validity must be positive and no longer than {limit}")
        subject = [attribute for attribute in self.ca_cert.subject
                   if attribute.oid in (NameOID.COUNTRY_NAME, NameOID.ORGANIZATION_NAME)]
        subject += [x509.NameAttribute(NameOID.ORGANIZATIONAL_UNIT_NAME, "Arrowhead Systems"),
                    x509.NameAttribute(NameOID.COMMON_NAME, common_name),
                    x509.NameAttribute(NameOID.DN_QUALIFIER, profile)]
        public_key = csr.public_key()
        builder = (x509.CertificateBuilder().subject_name(x509.Name(subject)).issuer_name(self.ca_cert.subject)
                   .public_key(public_key).serial_number(x509.random_serial_number())
                   .not_valid_before(start).not_valid_after(end)
                   .add_extension(x509.BasicConstraints(ca=False, path_length=None), critical=True)
                   .add_extension(x509.KeyUsage(digital_signature=True, key_encipherment=True, content_commitment=False,
                                                data_encipherment=False, key_agreement=False, key_cert_sign=False,
                                                crl_sign=False, encipher_only=False, decipher_only=False), critical=True)
                   .add_extension(x509.ExtendedKeyUsage([x509.oid.ExtendedKeyUsageOID.SERVER_AUTH,
                                                         x509.oid.ExtendedKeyUsageOID.CLIENT_AUTH]), critical=False)
                   .add_extension(x509.SubjectKeyIdentifier.from_public_key(public_key), critical=False)
                   .add_extension(x509.AuthorityKeyIdentifier.from_issuer_public_key(self.ca_cert.public_key()), critical=False))
        alternative_names = subject_alternative_names(payload.get("addresses"), csr)
        if alternative_names:
            builder = builder.add_extension(x509.SubjectAlternativeName(alternative_names), critical=False)
        cert = builder.sign(self.ca_key, hashes.SHA256())
        record_id = self.store.add_certificate(cert, common_name, caller)
        return {"id": record_id, "certificateChain": [b64_der(cert), b64_der(self.ca_cert)],
                "commonName": common_name, "profile": PROFILE_NAMES[profile],
                "serialNumber": str(cert.serial_number), "validAfter": iso(start), "validBefore": iso(end)}

    def check_certificate(self, payload: dict[str, Any]) -> dict[str, Any]:
        encoded = payload.get("certificate") or payload.get("encodedCertificate")
        if not encoded:
            raise ValueError("certificate is required")
        cert = load_certificate(str(encoded))
        try:
            if cert.issuer != self.ca_cert.subject:
                raise ValueError("unknown issuer")
            verify_signed_by_ca(cert, self.ca_cert)
        except Exception:
            return {"status": "UNKNOWN", "valid": False, "serialNumber": str(cert.serial_number)}
        row = self.store.certificate_by_serial(cert.serial_number)
        now = utcnow()
        if row and row["revoked_at"]:
            status = "REVOKED"
        elif not (cert.not_valid_before_utc <= now <= cert.not_valid_after_utc):
            status = "EXPIRED"
        else:
            status = "GOOD"
        return {"status": status, "valid": status == "GOOD", "serialNumber": str(cert.serial_number),
                "known": row is not None}

    def own_certificate(self, encoded: str) -> x509.Certificate:
        cert = load_certificate(encoded)
        if cert.issuer != self.ca_cert.subject:
            raise ValueError("certificate was not issued by this cloud CA")
        verify_signed_by_ca(cert, self.ca_cert)
        return cert

    def crl(self) -> x509.CertificateRevocationList:
        now = utcnow()
        builder = (x509.CertificateRevocationListBuilder().issuer_name(self.ca_cert.subject)
                   .last_update(now - timedelta(minutes=1)).next_update(now + timedelta(hours=CRL_VALIDITY_HOURS))
                   .add_extension(x509.CRLNumber(int(now.timestamp() * 1000)), critical=False)
                   .add_extension(x509.AuthorityKeyIdentifier.from_issuer_public_key(self.ca_cert.public_key()), critical=False))
        for serial, revoked_at in self.store.revoked_certificates():
            builder = builder.add_revoked_certificate(
                x509.RevokedCertificateBuilder().serial_number(serial).revocation_date(revoked_at).build())
        return builder.sign(self.ca_key, hashes.SHA256())

    def publish_crl(self) -> None:
        """Writes CRL_DIR/ca.crl atomically (temporary file + rename): a reader never sees a missing or partial file."""
        if not CRL_DIR:
            return
        pem = self.crl().public_bytes(serialization.Encoding.PEM)
        target = Path(CRL_DIR) / "ca.crl"
        temporary = target.with_suffix(".crl.tmp")
        temporary.write_bytes(pem)
        temporary.chmod(0o644)
        os.replace(temporary, target)


def requested_profile(csr: x509.CertificateSigningRequest) -> str | None:
    """The dnQualifier of the CSR subject; only a request, the caller's allowed profiles decide."""
    values = csr.subject.get_attributes_for_oid(NameOID.DN_QUALIFIER)
    return str(values[0].value) if values else None


def subject_alternative_names(addresses: Any, csr: x509.CertificateSigningRequest) -> list[x509.GeneralName]:
    """DNS names and IP addresses from the request (or, if none are given, from the CSR); nothing else is copied."""
    if addresses is None:
        try:
            requested = csr.extensions.get_extension_for_class(x509.SubjectAlternativeName).value
            addresses = [str(v) for v in requested.get_values_for_type(x509.DNSName)]
            addresses += [str(v) for v in requested.get_values_for_type(x509.IPAddress)]
        except x509.ExtensionNotFound:
            addresses = []
    if not isinstance(addresses, list) or len(addresses) > 16:
        raise ValueError("addresses must be a list of at most 16 DNS names or IP addresses")
    names: list[x509.GeneralName] = []
    for address in addresses:
        try:
            names.append(x509.IPAddress(ipaddress.ip_address(str(address))))
        except ValueError:
            if not DNS_PATTERN.match(str(address)):
                raise ValueError(f"invalid address {address!r}")
            names.append(x509.DNSName(str(address)))
    return names


STORE: Store | None = None
AUTHORITY: CertificateAuthority | None = None


def caller_name(handler: BaseHTTPRequestHandler) -> str:
    peer = handler.connection.getpeercert() or {}
    for rdns in peer.get("subject", []):
        for key, value in rdns:
            if key == "commonName":
                return value
    return "unknown"


class Identity:
    """The CertificateAuthority's own Arrowhead identity token (outsourced authentication)."""

    def __init__(self) -> None:
        self.lock = threading.Lock()
        self.value: str | None = None

    def token(self, renew: bool = False) -> str:
        with self.lock:
            if self.value is None or renew:
                payload = {"systemName": SYSTEM_NAME, "credentials": {"password": AUTHENTICATOR_PASSWORD}}
                request = urllib.request.Request(f"{AUTHENTICATION_URL}/authentication/identity/login",
                                                 data=json.dumps(payload).encode(), method="POST",
                                                 headers={"Content-Type": "application/json"})
                with urllib.request.urlopen(request, context=client_tls_context(), timeout=5) as response:
                    self.value = json.loads(response.read())["token"]
            return self.value

    def call(self, request: urllib.request.Request, timeout: int = 5) -> bytes:
        """Sends the request with this system's token, logging in again once if the token was refused."""
        for renew in (False, True):
            request.add_unredirected_header("Authorization", f"Bearer IDENTITY-TOKEN//{self.token(renew)}")
            try:
                with urllib.request.urlopen(request, context=client_tls_context(), timeout=timeout) as response:
                    return response.read()
            except urllib.error.HTTPError as exc:
                if exc.code != 401 or renew:
                    raise
        raise RuntimeError("unreachable")


IDENTITY = Identity()


def authenticated_caller(handler: BaseHTTPRequestHandler) -> str:
    """Verifies the caller's identity token with Authentication and binds it to the TLS client certificate:
    the token's system must be the system named by the certificate CN."""
    header = handler.headers.get("Authorization", "")
    prefix = "Bearer IDENTITY-TOKEN//"
    if not header.startswith(prefix):
        raise LookupError("identity token required")
    token = header[len(prefix):].strip()
    request = urllib.request.Request(f"{AUTHENTICATION_URL}/authentication/identity/verify/{urllib.parse.quote(token, safe='')}")
    result = json.loads(IDENTITY.call(request))
    if not result.get("verified"):
        raise LookupError("invalid identity token")
    system = result.get("systemName") or ""
    if caller_name(handler).split(".", 1)[0].lower() != system.lower():
        raise LookupError("identity token does not belong to the system of the client certificate")
    # M1: only system (sy) and operator (op) certificates act as systems; on and de certificates are refused
    peer = handler.connection.getpeercert() or {}
    profile = next((v for group in peer.get("subject", ()) for k, v in group if k == "dnQualifier"), "")
    if profile not in ("sy", "op"):
        raise PermissionError(f"certificate profile '{profile}' may not call this service")
    return system


class Handler(BaseHTTPRequestHandler):
    server_version = "Arrowhead-CertificateAuthority/5.2.2"

    def log_message(self, fmt: str, *args: Any) -> None:
        print(f"{self.address_string()} {fmt % args}", flush=True)

    def respond(self, status: int, payload: Any) -> None:
        body = json.dumps(payload, default=str).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def body(self) -> dict[str, Any]:
        length = int(self.headers.get("Content-Length", "0"))
        if length > 1024 * 1024:
            raise ValueError("request body is too large")
        return json.loads(self.rfile.read(length) or b"{}")

    def route(self) -> tuple[str, list[str]]:
        path = urlparse(self.path).path
        if path.startswith(BASE_PATH):
            path = path[len(BASE_PATH):]
        return path or "/", [p for p in path.split("/") if p]

    def require_manager(self) -> str:
        caller = authenticated_caller(self)
        if caller not in ("LocalCloudManager", "Sysop", "CertificateAuthority"):
            raise PermissionError("management operation requires an operator identity")
        return caller

    def require_signer(self) -> str:
        caller = authenticated_caller(self)
        if caller not in SIGNING_PROFILES:
            raise PermissionError("certificate signing requires an authorized provisioning identity")
        return caller

    def do_GET(self) -> None:
        assert STORE is not None
        path, parts = self.route()
        try:
            if path == "/health":
                self.respond(200, {"status": "ok", "system": SYSTEM_NAME, "version": "5.2.2"})
            elif path == "/crl":
                assert AUTHORITY is not None
                body = AUTHORITY.crl().public_bytes(serialization.Encoding.PEM)
                self.send_response(200)
                self.send_header("Content-Type", "application/pkix-crl")
                self.send_header("Content-Length", str(len(body)))
                self.send_header("Cache-Control", "no-store")
                self.end_headers()
                self.wfile.write(body)
            elif path == "/mgmt/certificates":
                self.require_manager(); self.respond(200, STORE.list_certificates())
            elif path == "/mgmt/keys":
                self.require_manager(); self.respond(200, STORE.list_keys())
            else:
                self.respond(404, {"error": "not found"})
        except LookupError as exc:
            self.respond(401, {"error": str(exc)})
        except PermissionError as exc:
            self.respond(403, {"error": str(exc)})
        except Exception as exc:
            self.respond(400, {"error": str(exc)})

    def do_POST(self) -> None:
        assert AUTHORITY is not None and STORE is not None
        path, _ = self.route()
        try:
            payload = self.body()
            if path == "/sign":
                caller = self.require_signer()
                self.respond(201, AUTHORITY.sign(payload, caller))
            elif path == "/checkCertificate":
                self.respond(200, AUTHORITY.check_certificate(payload))
            elif path == "/mgmt/revocations":
                caller = self.require_manager()
                encoded = payload.get("certificate") or payload.get("encodedCertificate")
                if not encoded:
                    raise ValueError("certificate is required")
                cert = AUTHORITY.own_certificate(str(encoded))
                record_id, changed = STORE.record_revocation(cert, f"revoked-by:{caller}")
                if changed:
                    AUTHORITY.publish_crl()
                self.respond(200, {"id": record_id, "serialNumber": str(cert.serial_number), "revoked": True,
                                   "changed": changed})
            elif path == "/checkTrustedKey":
                pem = payload.get("publicKey") or payload.get("publicKeyPem")
                if not pem:
                    raise ValueError("publicKey is required")
                trusted, record = STORE.key_status(str(pem))
                self.respond(200, {"trusted": trusted, "key": record})
            else:
                self.respond(404, {"error": "not found"})
        except LookupError as exc:
            self.respond(401, {"error": str(exc)})
        except PermissionError as exc:
            self.respond(403, {"error": str(exc)})
        except Exception as exc:
            self.respond(400, {"error": str(exc)})

    def do_PUT(self) -> None:
        assert STORE is not None
        path, _ = self.route()
        try:
            self.require_manager()
            if path != "/mgmt/keys":
                return self.respond(404, {"error": "not found"})
            payload = self.body()
            pem = payload.get("publicKey") or payload.get("publicKeyPem")
            if not pem:
                raise ValueError("publicKey is required")
            now = utcnow()
            start = parse_time(payload.get("validAfter"), now)
            end = parse_time(payload.get("validBefore"), now + timedelta(days=DEFAULT_VALIDITY_DAYS))
            key_id = STORE.add_key(str(pem), str(payload.get("description", "")), start, end)
            self.respond(201, {"id": key_id, "trusted": True})
        except LookupError as exc:
            self.respond(401, {"error": str(exc)})
        except PermissionError as exc:
            self.respond(403, {"error": str(exc)})
        except Exception as exc:
            self.respond(400, {"error": str(exc)})

    def do_DELETE(self) -> None:
        assert STORE is not None
        _, parts = self.route()
        try:
            if len(parts) == 3 and parts[:2] == ["mgmt", "certificates"]:
                caller = authenticated_caller(self)
                if caller == "Onboarding":  # off-boarding: only the certificates Onboarding requested itself
                    changed = STORE.revoke(int(parts[2]), created_by="Onboarding")
                else:
                    self.require_manager()
                    changed = STORE.revoke(int(parts[2]))
            elif len(parts) == 3 and parts[:2] == ["mgmt", "keys"]:
                self.require_manager()
                changed = STORE.delete_key(int(parts[2]))
            else:
                return self.respond(404, {"error": "not found"})
            if changed and parts[1] == "certificates":
                assert AUTHORITY is not None
                AUTHORITY.publish_crl()
            self.respond(200 if changed else 404, {"success": changed})
        except LookupError as exc:
            self.respond(401, {"error": str(exc)})
        except PermissionError as exc:
            self.respond(403, {"error": str(exc)})
        except Exception as exc:
            self.respond(400, {"error": str(exc)})


def client_tls_context() -> ssl.SSLContext:
    context = ssl.create_default_context(cafile=CA_CERT_PATH)
    context.load_cert_chain(SERVER_CERT_PATH, SERVER_KEY_PATH)
    context.minimum_version = ssl.TLSVersion.TLSv1_2
    return context


def request_json(url: str, payload: dict[str, Any]) -> None:
    request = urllib.request.Request(url, data=json.dumps(payload).encode(), method="POST",
                                     headers={"Content-Type": "application/json"})
    try:
        # ServiceRegistry requires this system's identity token, bound to its client certificate.
        IDENTITY.call(request)
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode(errors="replace")
        raise RuntimeError(f"registration failed: HTTP {exc.code}: {detail}") from exc


def register() -> None:
    system = {"metadata": {"role": "certificate-authority", "security": "X.509/mTLS"},
              "version": "5.2.2", "addresses": ["certificate-authority"], "deviceName": None}
    try:
        request_json(f"{SERVICE_REGISTRY_URL}/serviceregistry/system-discovery/register", system)
    except RuntimeError as exc:
        # Registration is idempotent across container restarts. A prior version may
        # already have established the authenticated system identity.
        if "already exists" not in str(exc):
            raise
    common = {"version": "1.0.0", "expiresAt": None}
    services = [
        ("certificateSigning", {"sign": {"method": "POST", "path": "/sign"}}),
        ("certificateValidation", {
            "check-certificate": {"method": "POST", "path": "/checkCertificate"},
            "check-trusted-key": {"method": "POST", "path": "/checkTrustedKey"},
            "get-crl": {"method": "GET", "path": "/crl"},
        }),
        ("certificateAuthorityManagement", {
            "list-certificates": {"method": "GET", "path": "/mgmt/certificates"},
            "revoke-certificate": {"method": "DELETE", "path": "/mgmt/certificates"},
            "revoke-certificate-by-content": {"method": "POST", "path": "/mgmt/revocations"},
            "list-keys": {"method": "GET", "path": "/mgmt/keys"},
            "add-key": {"method": "PUT", "path": "/mgmt/keys"},
            "delete-key": {"method": "DELETE", "path": "/mgmt/keys"},
        }),
    ]
    for definition, operations in services:
        payload = {**common, "serviceDefinitionName": definition,
                   "metadata": {"systemVersion": "5.2.2"},
                   "interfaces": [{"templateName": "generic_https", "protocol": "https", "policy": "CERT_AUTH",
                                   "properties": {"accessAddresses": ["certificate-authority"], "accessPort": PORT,
                                                  "basePath": BASE_PATH, "operations": operations}}]}
        request_json(f"{SERVICE_REGISTRY_URL}/serviceregistry/service-discovery/register", payload)
    monitor = {**common, "serviceDefinitionName": "sysMonitor",
               "metadata": {"contract": "SysMonitor-AMQP-IDD-5.2.2", "schemaVersion": "1.0",
                            "publisherMode": "native", "observationTypes": ["echo", "power", "status"],
                            "delivery": "persistent, publisher-confirmed"},
               "interfaces": [{"templateName": "generic_amqps", "protocol": "amqps", "policy": "CERT_AUTH",
                               "properties": {"accessAddresses": [AMQP_HOST], "accessPort": AMQP_PORT,
                                              "exchange": EXCHANGE, "baseRoutingKey": SYSMONITOR_ROUTING_KEY,
                                              "operations": ["echo", "power", "status"]}}]}
    request_json(f"{SERVICE_REGISTRY_URL}/serviceregistry/service-discovery/register", monitor)


def registration_loop() -> None:
    while True:
        try:
            register()
            print("Registered CertificateAuthority services", flush=True)
            return
        except Exception as exc:
            print(f"ServiceRegistry registration retry: {exc}", flush=True)
            time.sleep(5)


def monitor_loop() -> None:
    assert STORE is not None
    publisher = AmqpPublisher(AMQP_HOST, AMQP_PORT, CA_CERT_PATH, SERVER_CERT_PATH, SERVER_KEY_PATH,
                              f"{SYSTEM_NAME}-sysMonitor")
    while True:
        source_time = iso(utcnow())
        metrics = STORE.metrics()
        observations = {
            "echo": ({"status": "alive"}, None),
            "power": ({"supplyType": "line", "state": "available"}, "1"),
            "status": ({"state": "running", "version": "5.2.2", **metrics}, "1"),
        }
        try:
            for observation_type, (value, unit) in observations.items():
                payload = {"schemaVersion": "1.0", "providerSystem": SYSTEM_NAME,
                           "serviceInstance": "certificateAuthority", "observationType": observation_type,
                           "sourceTime": source_time, "value": value, "unit": unit, "quality": "good",
                           "metadata": {"systemVersion": "5.2.2"}}
                publisher.publish(f"{SYSMONITOR_ROUTING_KEY}.{observation_type}", payload)
        except Exception as exc:
            print(f"sysMonitor publish retry: {exc}", flush=True)
        time.sleep(MONITOR_INTERVAL)


def crl_loop() -> None:
    assert AUTHORITY is not None
    while True:
        try:
            AUTHORITY.publish_crl()
        except Exception as exc:
            print(f"CRL publication failed: {exc}", flush=True)
        time.sleep(CRL_REFRESH_SECONDS)


def main() -> None:
    global STORE, AUTHORITY
    STORE = Store(DB_PATH)
    AUTHORITY = CertificateAuthority(STORE)
    threading.Thread(target=registration_loop, daemon=True).start()
    threading.Thread(target=monitor_loop, daemon=True).start()
    if CRL_DIR:
        threading.Thread(target=crl_loop, daemon=True).start()
    server = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    context = ssl.create_default_context(ssl.Purpose.CLIENT_AUTH)
    context.load_cert_chain(SERVER_CERT_PATH, SERVER_KEY_PATH)
    context.load_verify_locations(CA_CERT_PATH)
    context.verify_mode = ssl.CERT_REQUIRED
    context.minimum_version = ssl.TLSVersion.TLSv1_2
    server.socket = context.wrap_socket(server.socket, server_side=True)
    print(f"CertificateAuthority 5.2.2 listening on {PORT}", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
