import psutil
import time

from typing import List
from collections import deque
from utils.utils import new_thread
from utils.global_constants import FREQUENCY, WINDOW_SIZE

# MEMBERS
QUEUE: deque[float] = deque(maxlen=WINDOW_SIZE)
do_measuring = False

# METHODS

def start_measuring_cpu():
    global do_measuring
    do_measuring = True
    new_thread(_run())

def stop_measuring_cpu():
    global do_measuring
    do_measuring = False
    time.sleep(FREQUENCY)

def get_batch_cpu(front: int) -> List[float]:
    return list(QUEUE)[:front]
    
    
# ASSISTANT METHODS

async def _run():
    while do_measuring:
       QUEUE.append(_get_cpu_load())

def _get_cpu_load() -> float:
    return psutil.cpu_percent(interval = FREQUENCY) # thread blocking call