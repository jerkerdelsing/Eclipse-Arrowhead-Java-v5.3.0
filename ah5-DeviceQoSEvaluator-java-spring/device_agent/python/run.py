from typing import List
from fastapi import FastAPI, Query
from utils.global_constants import PATH, WINDOW_SIZE
from cpu_total_load.cpu_total_load import start_measuring_cpu, get_batch_cpu, stop_measuring_cpu
from ram_used.ram_used import start_measuring_ram, get_batch_ram, stop_measuring_ram
from network_load.network_load import start_measuring_network, stop_measuring_network, get_batch_network_out, get_batch_network_in
from utils.oid import OID

import uvicorn
import yaml
import logging

# CONFIGURATION

with open('config.yaml', 'r') as f:
    config = yaml.safe_load(f)

cpu_enabled = config.get('measurements', {}).get('cpu_total_load', {}).get('enabled', False)
ram_enabled = config.get('measurements', {}).get('ram_used', {}).get('enabled', False)
net_enabled = config.get('measurements', {}).get('network_load', {}).get('enabled', False)
net_link_address = config.get('measurements', {}).get('network_load', {}).get('link_address')

logger = logging.getLogger("uvicorn")

# SERVER
    
async def on_startup():
    logger.info("Application is initializing...")
    if cpu_enabled:
        start_measuring_cpu()
    if ram_enabled:
        start_measuring_ram()
    if net_enabled:
        start_measuring_network(net_link_address)
    logger.info("Application has been initialized.")

async def on_shutdown():
    logger.info("Application is cleaning up...")
    if cpu_enabled:
        stop_measuring_cpu()
    if ram_enabled:
        stop_measuring_ram()
    if net_enabled:
        stop_measuring_network()
    logger.info("Application has been cleaned up.")

app = FastAPI(docs_url=None, redoc_url=None, openapi_url=None, on_startup=[on_startup], on_shutdown=[on_shutdown])

@app.get(PATH)
async def get(batch: int = Query(default=WINDOW_SIZE), oids: List[str] = Query(default=OID.values())):
    response = {}
    if OID.CPU_TOTAL_LOAD.value in oids:
        response[OID.CPU_TOTAL_LOAD.value] = get_batch_cpu(batch)
    if OID.RAM_FREE.value in oids:
        response[OID.RAM_FREE.value] = get_batch_ram(batch)
    if OID.NET_OUT.value in oids:
        response[OID.NET_OUT.value] = get_batch_network_out(batch)
    if OID.NET_IN.value in oids:
        response[OID.NET_IN.value] = get_batch_network_in(batch)
    return response

def main():
    logger.info("Arrowhead DeviceQoSClient")
    serverConfig = uvicorn.Config(app, host="0.0.0.0", port=59473, timeout_keep_alive=30)
    server = uvicorn.Server(serverConfig)
    server.run()

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("Server stopped by user.")