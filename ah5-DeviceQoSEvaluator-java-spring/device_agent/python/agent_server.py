# SPDX-License-Identifier: EPL-2.0
"""HTTPS server of the DeviceQoSEvaluator device agent (2026-11-09).

The agent serves the device's load figures (GET /device-qos) to the DeviceQoSEvaluator only:

- mutual TLS (TLS 1.2+): the agent presents a certificate of the cloud CA whose subject alternative names hold the
  address the DeviceQoSEvaluator uses for the device; the client must present a certificate of the same CA;
- the client certificate is checked against the cloud CA's CRL; a missing, unreadable or expired CRL refuses every
  client (fail closed); the CRL file is re-read when it changes;
- only the certificate common names listed in tls.allowed_clients get data (normally the DeviceQoSEvaluator,
  DeviceQoSEvaluator.<cloud>.<organization>.arrowhead.eu); others get 403.

Plain HTTP is refused unless the agent listens on a loopback address only (isolated development).
Standard library only, so the TLS identity of the caller is available to the request handler.
"""
from __future__ import annotations

import ipaddress
import json
import logging
import os
import socket
import ssl
import threading
from dataclasses import dataclass, field
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Callable, Dict, List, Optional
from urllib.parse import parse_qs, urlsplit

DEFAULT_ADDRESS = "0.0.0.0"
DEFAULT_PORT = 59473
PORT_FLAG = "--server.port"
PORT_ENV = "SERVER_PORT"
PATH = "/device-qos"
HANDSHAKE_TIMEOUT = 10  # seconds
CRL_POLL_SECONDS = 30

logger = logging.getLogger("device-agent")

# batch size -> {oid: [values]}, for the requested OIDs
Provider = Callable[[int, List[str]], Dict[str, List[float]]]


@dataclass
class TlsSettings:
    enabled: bool = True
    certificate: str = ""
    private_key: str = ""
    ca: str = ""
    crl: str = ""
    allowed_clients: List[str] = field(default_factory=list)


@dataclass
class Settings:
    address: str = DEFAULT_ADDRESS
    port: int = DEFAULT_PORT
    tls: TlsSettings = field(default_factory=TlsSettings)


def parse_port(raw, source: str) -> int:
    try:
        port = int(str(raw).strip())
    except ValueError:
        raise ValueError(f"{source}: listening port must be a number, got {raw!r}") from None
    if not 1 <= port <= 65535:
        raise ValueError(f"{source}: listening port must be 1..65535, got {port}")
    return port


def resolve_port(config_port, env: Dict[str, str], argv: List[str]) -> int:
    """Listening port: --server.port (CLI) > SERVER_PORT (environment) > server.port (config file) > 59473."""
    for i, arg in enumerate(argv):
        if arg.startswith(PORT_FLAG + "="):
            return parse_port(arg.split("=", 1)[1], PORT_FLAG)
        if arg == PORT_FLAG:
            if i + 1 >= len(argv):
                raise ValueError(f"{PORT_FLAG} needs a value")
            return parse_port(argv[i + 1], PORT_FLAG)
    if env.get(PORT_ENV, "").strip():
        return parse_port(env[PORT_ENV], PORT_ENV)
    if config_port is not None:
        return parse_port(config_port, "server.port")
    return DEFAULT_PORT


def settings_from_config(config: dict, env: Dict[str, str], argv: List[str], base_dir: str = ".") -> Settings:
    server = config.get("server") or {}
    tls = config.get("tls") or {}

    def path(key: str) -> str:
        value = str(tls.get(key) or "")
        return value if not value or os.path.isabs(value) else os.path.join(base_dir, value)

    settings = Settings(
        address=str(server.get("address") or DEFAULT_ADDRESS),
        port=resolve_port(server.get("port"), env, argv),
        tls=TlsSettings(
            enabled=tls.get("enabled", True) is not False,
            certificate=path("certificate"),
            private_key=path("private_key"),
            ca=path("ca"),
            crl=path("crl"),
            allowed_clients=[str(c) for c in (tls.get("allowed_clients") or [])],
        ),
    )
    validate(settings)
    return settings


def is_loopback(address: str) -> bool:
    if address == "localhost":
        return True
    try:
        return ipaddress.ip_address(address).is_loopback
    except ValueError:
        return False


def validate(settings: Settings) -> None:
    tls = settings.tls
    if not tls.enabled:
        if not is_loopback(settings.address):
            raise ValueError("tls.enabled=false is only allowed with a loopback server.address (isolated development); "
                             "a network-reachable agent needs mutual TLS")
        return
    missing = [k for k in ("certificate", "private_key", "ca", "crl") if not getattr(tls, k)]
    if missing:
        raise ValueError("tls." + ", tls.".join(missing) + " must be set (mutual TLS is the default)")
    for key in ("certificate", "private_key", "ca", "crl"):
        if not os.path.isfile(getattr(tls, key)):
            raise ValueError(f"tls.{key}: file not found: {getattr(tls, key)}")
    if not tls.allowed_clients:
        raise ValueError("tls.allowed_clients must name the DeviceQoSEvaluator certificate common name(s)")


def build_context(tls: TlsSettings) -> ssl.SSLContext:
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.minimum_version = ssl.TLSVersion.TLSv1_2
    context.load_cert_chain(tls.certificate, tls.private_key)
    context.load_verify_locations(cafile=tls.ca)
    context.load_verify_locations(cafile=tls.crl)
    context.verify_mode = ssl.CERT_REQUIRED
    context.verify_flags |= ssl.VERIFY_CRL_CHECK_LEAF
    return context


class ContextHolder:
    """The current TLS context; rebuilt when the CRL (or a certificate file) changes. A rebuild that fails keeps the
    previous context, which refuses every client once its CRL is past nextUpdate."""

    def __init__(self, tls: TlsSettings):
        self.tls = tls
        self._lock = threading.Lock()
        self._stamp = self._mtimes()
        self._context = build_context(tls)

    def _mtimes(self):
        return tuple(os.stat(p).st_mtime_ns if os.path.exists(p) else None
                     for p in (self.tls.crl, self.tls.ca, self.tls.certificate, self.tls.private_key))

    def current(self) -> ssl.SSLContext:
        with self._lock:
            return self._context

    def refresh(self) -> bool:
        stamp = self._mtimes()
        if stamp == self._stamp:
            return False
        try:
            context = build_context(self.tls)
        except (OSError, ssl.SSLError) as error:
            logger.error("TLS files changed but could not be loaded (%s); keeping the previous configuration", error)
            return False
        with self._lock:
            self._context, self._stamp = context, stamp
        logger.info("TLS configuration reloaded (CRL or certificate changed)")
        return True


def common_name(certificate: Optional[dict]) -> str:
    for rdn in (certificate or {}).get("subject", ()):
        for key, value in rdn:
            if key == "commonName":
                return value
    return ""


class AgentServer(ThreadingHTTPServer):
    daemon_threads = True

    def __init__(self, settings: Settings, provider: Provider, window_size: int):
        self.settings = settings
        self.provider = provider
        self.window_size = window_size
        self.contexts = ContextHolder(settings.tls) if settings.tls.enabled else None
        family = socket.AF_INET6 if ":" in settings.address else socket.AF_INET
        self.address_family = family
        super().__init__((settings.address, settings.port), Handler)
        self._stop = threading.Event()
        if self.contexts:
            threading.Thread(target=self._watch, name="crl-watch", daemon=True).start()

    def _watch(self):
        while not self._stop.wait(CRL_POLL_SECONDS):
            self.contexts.refresh()

    def get_request(self):
        sock, address = self.socket.accept()
        if self.contexts:
            sock.settimeout(HANDSHAKE_TIMEOUT)
            sock = self.contexts.current().wrap_socket(sock, server_side=True, do_handshake_on_connect=False)
        return sock, address

    def handle_error(self, request, client_address):
        logger.debug("Connection with %s failed", client_address[0], exc_info=True)

    def server_close(self):
        self._stop.set()
        super().server_close()


class Handler(BaseHTTPRequestHandler):
    server_version = "ArrowheadDeviceAgent"
    sys_version = ""

    def setup(self):
        self.client, self.refused = "", False
        if isinstance(self.request, ssl.SSLSocket):
            try:
                self.request.do_handshake()
                self.client = common_name(self.request.getpeercert())
            except (ssl.SSLError, OSError) as error:
                # no certificate, another CA, revoked, or no valid CRL: the connection ends without a response
                logger.warning("TLS handshake with %s refused: %s", self.client_address[0], error)
                self.refused = True
        super().setup()

    def handle(self):
        if not self.refused:
            super().handle()

    def finish(self):
        try:
            super().finish()
        except (OSError, AttributeError):
            pass

    def log_message(self, fmt, *args):
        logger.info("%s %s - " + fmt, self.client_address[0], self.client or "-", *args)

    def _json(self, status: int, payload) -> None:
        body = json.dumps(payload).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _authorized(self) -> bool:
        if not self.server.contexts:
            return True  # plain HTTP: loopback only (validate)
        if self.client in self.server.settings.tls.allowed_clients:
            return True
        logger.warning("Client %r (%s) is not in tls.allowed_clients", self.client, self.client_address[0])
        self._json(403, {"error": "client not allowed"})
        return False

    def do_GET(self):
        if not self._authorized():
            return
        url = urlsplit(self.path)
        if url.path != PATH:
            self._json(404, {"error": "not found"})
            return
        query = parse_qs(url.query)
        try:
            batch = int(query.get("batch", [str(self.server.window_size)])[0])
        except ValueError:
            batch = 0
        if not 1 <= batch <= self.server.window_size:
            self._json(400, {"error": f"batch must be 1..{self.server.window_size}"})
            return
        self._json(200, self.server.provider(batch, query.get("oids", [])))

    def _refuse(self):
        if self._authorized():
            self._json(405, {"error": "method not allowed"})

    do_POST = do_PUT = do_DELETE = do_PATCH = do_HEAD = do_OPTIONS = _refuse
