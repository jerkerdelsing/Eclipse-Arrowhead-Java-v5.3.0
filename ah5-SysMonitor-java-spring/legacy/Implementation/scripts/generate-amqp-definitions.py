#!/usr/bin/env python3
"""Generates the RabbitMQ definitions (users, permissions, topic permissions) from amqp/acl."""

import json
import re
import sys
from pathlib import Path

EXCHANGE = "arrowhead"
# The exchange comes from these definitions. A client may declare server-named queues (amq.gen-*) and queues
# named after its own system (<System>.<name>, e.g. the durable intake queue SysMonitor.observations).
def queues(user):
    return rf"amq\.gen-.*|{re.escape(user.split('.', 1)[0])}\..*"


def pattern_regex(pattern):
    """Topic pattern -> regex that accepts the routing keys, and the binding keys, it covers."""
    words = []
    for word in pattern.split("."):
        if word == "#":
            words.append(r"(?:\.[^#]+)?" if words else r"(?:[^#]+)?")
            continue
        piece = r"[^.#]+" if word == "*" else re.escape(word)
        words.append((r"\." if words else "") + piece)
    return "".join(words)  # "a.#" also covers "a": the optional group carries its leading dot


def parse(acl):
    users, current = {}, None
    for number, raw in enumerate(acl.splitlines(), 1):
        line = "" if raw.lstrip().startswith("#") else raw.strip()  # "#" is also a topic wildcard
        if not line:
            continue
        keyword, _, value = line.partition(" ")
        value = value.strip()
        if keyword == "user":
            current = users.setdefault(value, {"publish": [], "subscribe": []})
        elif keyword in ("publish", "subscribe") and current is not None and value:
            current[keyword].append(value)
        else:
            sys.exit(f"amqp/acl:{number}: cannot parse '{raw}'")
    return users


def alternatives(patterns):
    return "^(?:" + "|".join(pattern_regex(p) for p in patterns) + ")$" if patterns else "^$"


def main():
    root = Path(__file__).resolve().parent.parent
    users = parse((root / "amqp" / "acl").read_text())
    definitions = {
        "vhosts": [{"name": "/"}],
        "exchanges": [{"name": EXCHANGE, "vhost": "/", "type": "topic", "durable": True,
                       "auto_delete": False, "internal": False, "arguments": {}}],
        "users": [], "permissions": [], "topic_permissions": [],
    }
    for name, rights in users.items():
        # No password: these users can only log in with their client certificate (EXTERNAL).
        definitions["users"].append({"name": name, "password_hash": "", "hashing_algorithm":
                                     "rabbit_password_hashing_sha256", "tags": []})
        own = queues(name)
        definitions["permissions"].append({"user": name, "vhost": "/", "configure": f"^(?:{own})$",
                                           "write": f"^(?:{EXCHANGE}|{own})$",
                                           "read": f"^(?:{EXCHANGE}|{own})$"})
        definitions["topic_permissions"].append({"user": name, "vhost": "/", "exchange": EXCHANGE,
                                                 "write": alternatives(rights["publish"]),
                                                 "read": alternatives(rights["subscribe"])})
    target = root / "amqp" / "definitions.json"
    target.write_text(json.dumps(definitions, indent=2) + "\n")
    print(f"{target}: {len(users)} users")


if __name__ == "__main__":
    main()
