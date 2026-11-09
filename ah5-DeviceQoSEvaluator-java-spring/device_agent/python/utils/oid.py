from enum import Enum
from typing import List

class OID(Enum):
    CPU_TOTAL_LOAD = "1.4"
    RAM_FREE = "2.1"
    NET_OUT = "3.1"
    NET_IN = "3.2"

    @classmethod
    def values(cls) -> List[str]:
        return [member.value for member in cls]