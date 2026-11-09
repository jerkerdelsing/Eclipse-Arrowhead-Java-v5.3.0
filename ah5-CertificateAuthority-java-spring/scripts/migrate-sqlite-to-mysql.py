#!/usr/bin/env python3
# Copyright (c) 2026 Lulea University of Technology
# SPDX-License-Identifier: EPL-2.0
"""Convert the native 5.2.2 CertificateAuthority SQLite store into SQL for the Spring MySQL schema.

The native CA kept issued certificates (including revocations) and trusted keys in
/data/certificate-authority.db. This script reads that file with the Python standard library and
writes INSERT statements for ah_certificate_authority. Run the output as the database root user
(the application account holds data-manipulation privileges only), before the new CA starts:

    python3 -I scripts/migrate-sqlite-to-mysql.py certificate-authority.db > ca-import.sql
    docker compose exec -T certificate-authority-db sh -c \
        'mysql -uroot -p"$(cat /run/secrets/db-root-password)" ah_certificate_authority' < ca-import.sql

Rows keep their ids, so ids reported earlier (for example to Onboarding) stay valid. The import
refuses to run into a schema that already holds certificates or keys.
"""

import argparse
import hashlib
import sqlite3
import sys
from datetime import datetime, timezone


def mysql_time(value):
    """ISO-8601 text of the native store -> 'YYYY-MM-DD HH:MM:SS.ffffff' in UTC, or NULL."""
    if value is None or str(value).strip() == "":
        return "NULL"
    text = str(value).strip().replace("Z", "+00:00")
    # Java Instant.toString() may carry nanoseconds; Python parses at most microseconds
    if "." in text:
        head, rest = text.split(".", 1)
        digits = "".join(ch for ch in rest if ch.isdigit())
        zone = rest[len(digits):]
        text = head + "." + digits[:6].ljust(6, "0") + zone
    parsed = datetime.fromisoformat(text)
    if parsed.tzinfo is None:
        parsed = parsed.replace(tzinfo=timezone.utc)
    return "'" + parsed.astimezone(timezone.utc).strftime("%Y-%m-%d %H:%M:%S.%f") + "'"


def quote(value):
    if value is None:
        return "NULL"
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("sqlite_file", help="native certificate-authority.db")
    args = parser.parse_args()

    source = sqlite3.connect("file:" + args.sqlite_file + "?mode=ro", uri=True)
    source.row_factory = sqlite3.Row
    certificates = source.execute("SELECT * FROM issued_certificates ORDER BY id").fetchall()
    keys = source.execute("SELECT * FROM trusted_keys ORDER BY id").fetchall()

    out = sys.stdout
    out.write("-- CertificateAuthority import from %s: %d certificate(s), %d trusted key(s)\n" % (args.sqlite_file, len(certificates), len(keys)))
    out.write("SET time_zone = '+00:00';\n")
    out.write("START TRANSACTION;\n")
    # stop on a non-empty target instead of mixing two stores
    out.write("SELECT IF((SELECT COUNT(*) FROM issued_certificate) + (SELECT COUNT(*) FROM trusted_key) = 0, 1,"
              " (SELECT 1 FROM information_schema.tables LIMIT 2)) INTO @empty_target_required;\n")
    for row in certificates:
        out.write("INSERT INTO issued_certificate (id, serial_number, common_name, created_by, valid_from, valid_until, revoked_at, certificate_pem) VALUES (%d, %s, %s, %s, %s, %s, %s, %s);\n" % (
            row["id"], quote(row["serial_number"]), quote(row["common_name"]), quote(row["created_by"]),
            mysql_time(row["valid_from"]), mysql_time(row["valid_until"]), mysql_time(row["revoked_at"]), quote(row["certificate_pem"])))
    for row in keys:
        pem = row["public_key_pem"]
        out.write("INSERT INTO trusted_key (id, public_key_pem, public_key_hash, description, valid_after, valid_before, created_at) VALUES (%d, %s, %s, %s, %s, %s, %s);\n" % (
            row["id"], quote(pem), quote(hashlib.sha256(pem.encode("utf-8")).hexdigest()), quote(row["description"] or ""),
            mysql_time(row["valid_after"]), mysql_time(row["valid_before"]), mysql_time(row["created_at"])))
    out.write("COMMIT;\n")


if __name__ == "__main__":
    main()
