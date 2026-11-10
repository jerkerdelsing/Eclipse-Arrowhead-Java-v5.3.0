# SPDX-License-Identifier: EPL-2.0
"""Arrowhead DeviceQoSEvaluator device agent: measures CPU load, memory and network load of this device and serves
them to the DeviceQoSEvaluator over mutual TLS (agent_server.py; see ReadMe.md).

    python run.py [--config config.yaml] [--server.port 59473]
"""
import logging
import os
import signal
import sys
import threading

import yaml

from agent_server import AgentServer, settings_from_config
from utils.global_constants import WINDOW_SIZE
from utils.oid import OID

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
logger = logging.getLogger("device-agent")


def config_path(argv):
    for i, arg in enumerate(argv):
        if arg.startswith("--config="):
            return arg.split("=", 1)[1]
        if arg == "--config" and i + 1 < len(argv):
            return argv[i + 1]
    return os.environ.get("DEVICE_AGENT_CONFIG", os.path.join(os.path.dirname(os.path.abspath(__file__)), "config.yaml"))


def main(argv):
    path = config_path(argv)
    with open(path, "r") as f:
        config = yaml.safe_load(f) or {}
    settings = settings_from_config(config, dict(os.environ), argv, os.path.dirname(os.path.abspath(path)))

    # imported here: psutil is needed for measuring, not for validating the configuration
    from cpu_total_load.cpu_total_load import start_measuring_cpu, get_batch_cpu, stop_measuring_cpu
    from ram_used.ram_used import start_measuring_ram, get_batch_ram, stop_measuring_ram
    from network_load.network_load import (start_measuring_network, stop_measuring_network,
                                           get_batch_network_out, get_batch_network_in)

    measurements = config.get("measurements", {}) or {}
    enabled = {name: (measurements.get(name) or {}).get("enabled", False)
               for name in ("cpu_total_load", "ram_used", "network_load")}
    sources = {OID.CPU_TOTAL_LOAD.value: get_batch_cpu, OID.RAM_FREE.value: get_batch_ram,
               OID.NET_OUT.value: get_batch_network_out, OID.NET_IN.value: get_batch_network_in}

    def provider(batch, oids):
        requested = oids or OID.values()
        return {oid: source(batch) for oid, source in sources.items() if oid in requested}

    if enabled["cpu_total_load"]:
        start_measuring_cpu()
    if enabled["ram_used"]:
        start_measuring_ram()
    if enabled["network_load"]:
        start_measuring_network((measurements.get("network_load") or {}).get("link_address"))

    server = AgentServer(settings, provider, WINDOW_SIZE)
    mode = "mutual TLS" if settings.tls.enabled else "PLAIN HTTP (loopback development only)"
    logger.info("Arrowhead DeviceQoS agent listening on %s:%d (%s)", settings.address, settings.port, mode)
    signal.signal(signal.SIGTERM, lambda *_: threading.Thread(target=server.shutdown).start())
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
        if enabled["cpu_total_load"]:
            stop_measuring_cpu()
        if enabled["ram_used"]:
            stop_measuring_ram()
        if enabled["network_load"]:
            stop_measuring_network()


if __name__ == "__main__":
    try:
        main(sys.argv[1:])
    except ValueError as error:
        logger.error("Configuration refused: %s", error)
        sys.exit(2)
