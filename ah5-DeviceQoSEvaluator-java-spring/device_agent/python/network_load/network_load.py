from typing import List
import psutil
import time

from collections import deque
from utils.utils import new_thread
from utils.global_constants import FREQUENCY, WINDOW_SIZE

# MEMBERS
QUEUE_OUT: deque[float] = deque(maxlen=WINDOW_SIZE)
QUEUE_IN: deque[float] = deque(maxlen=WINDOW_SIZE)
do_measuring = False

# METHODS

def start_measuring_network(link_address):
    global do_measuring
    do_measuring = True
    new_thread(_run(link_address))

def stop_measuring_network():
    global do_measuring
    do_measuring = False
    time.sleep(FREQUENCY)

def get_batch_network_out(front: int) -> List[float]:
    return list(QUEUE_OUT)[:front]

def get_batch_network_in(front: int) -> List[float]:
    return list(QUEUE_IN)[:front]

# ASSISTANT METHODS

async def _run(link_address):
    while do_measuring:
        _calculate_network_load_percent(link_address)

def _calculate_network_load_percent(link_address):
    # Get initial counters
    net1 = psutil.net_io_counters(pernic=True)
    time.sleep(FREQUENCY)
    net2 = psutil.net_io_counters(pernic=True)
    
    # Get NIC stats (including speed in Mbps)
    stats = psutil.net_if_stats()
    
    # Get NIC addresses
    addrs = psutil.net_if_addrs()
    
    for iface in net1:
        if iface not in stats or stats[iface].speed == 0:
            continue  # skip interfaces with unknown speed

        # Gather assigned IP addresses
        if iface in addrs:
            for addr in addrs[iface]:
                if addr.address == link_address:
                    # Calculate ingress (received) and egress (sent) in bits/sec
                    bytes_out = net2[iface].bytes_sent - net1[iface].bytes_sent
                    bytes_in = net2[iface].bytes_recv - net1[iface].bytes_recv
                    bps_out = (bytes_out * 8) / FREQUENCY
                    bps_in = (bytes_in * 8) / FREQUENCY

                    link_speed_bps = stats[iface].speed * 1_000_000  # Mbps → bps
                    egress_percent = round((bps_out / link_speed_bps) * 100, 2)
                    ingress_percent = round((bps_in / link_speed_bps) * 100, 2)
                    QUEUE_OUT.append(egress_percent)
                    QUEUE_IN.append(ingress_percent)
                    return
