#!/usr/bin/env python3
"""Put the Java-Spring LocalCloudConfigurator (and its private MySQL service) into a cloud's Compose file; other services are kept.

The services are rendered from this folder's compose.yaml with an env file of the cloud (--env-file, the variables of
.env.example: certificate, secret and network locations of that cloud), so the cloud gets exactly the hardened definition of this
folder: port 8492 with mutual TLS, the private database, read-only root filesystem, no shared operator token. The previous
definition (plain HTTP 8097, JAVA_LCC_API_TOKEN) is replaced. A 0600 backup of the cloud file is written once.
"""
import argparse, json, os, subprocess
from pathlib import Path

p = argparse.ArgumentParser()
p.add_argument('--cloud-dir', type=Path, required=True)
p.add_argument('--env-file', type=Path, required=True, help='variables of .env.example for this cloud')
p.add_argument('--source-compose', type=Path)
a = p.parse_args()

root = Path(__file__).resolve().parents[1]
target = a.cloud_dir / 'compose.yaml'
source = a.source_compose or target
cloud = json.loads(subprocess.check_output(['docker', 'compose', '-f', str(source), 'config', '--format', 'json'], cwd=a.cloud_dir))
mine = json.loads(subprocess.check_output(['docker', 'compose', '--env-file', str(a.env_file.resolve()), '-f', str(root / 'compose.yaml'),
                                           'config', '--format', 'json'], cwd=root))
for name in ['localcloudconfigurator', 'localcloudconfigurator-db']:
    service = mine['services'][name]
    service['restart'] = 'unless-stopped'
    cloud['services'][name] = service
for section in ['secrets', 'volumes', 'networks']:
    for key, value in (mine.get(section) or {}).items():
        cloud.setdefault(section, {}).setdefault(key, value)


def uses_secret(key):
    return any(any((s if isinstance(s, str) else s.get('source')) == key for s in svc.get('secrets', [])) for svc in cloud['services'].values())


# the shared operator token of the native configurator no longer exists
for key in ['JAVA_LCC_API_TOKEN', 'JAVA_LCC_API_KEY']:
    if not uses_secret(key):
        (cloud.get('secrets') or {}).pop(key, None)

backup = target.with_name(target.name + '.before-spring-lcc')
if target.exists() and not backup.exists():
    with open(os.open(backup, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), 'w') as f:
        f.write(target.read_text())
tmp = target.with_name(target.name + '.next')
with open(os.open(tmp, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600), 'w') as f:
    f.write(json.dumps(cloud, indent=2) + '\n')
os.replace(tmp, target)
print('Cloud Compose file now runs the Java-Spring LocalCloudConfigurator (8492, mutual TLS) with its private database')
