#!/usr/bin/env python3
"""Install the host worker of LocalCloudConfigurator as a per-user macOS login service (launchd).

The worker is the same JAR as the Arrowhead system, started as a non-web Spring application ("worker"). It listens on host
loopback 8493 with mutual TLS: it presents its own Arrowhead system certificate (LocalCloudConfiguratorWorker, issued by the
cloud CA) and accepts only clients with a valid LocalCloudConfigurator certificate of the same cloud. There is no shared token.
No ColonyOS keys are read. It does not register in the ServiceRegistry.

Certificates are not created here: issue LocalCloudConfiguratorWorker.<cloud>.<organization>.arrowhead.eu from the cloud CA and
place the PKCS12 key store, the cloud truststore and the store password in the private directory (see DEPLOYMENT.md).
"""
import argparse, hashlib, json, os, plistlib, shutil, subprocess, time
from pathlib import Path

p = argparse.ArgumentParser()
p.add_argument('--cloud-dir', type=Path, required=True)
p.add_argument('--compose-file', type=Path, required=True)
p.add_argument('--private-dir', type=Path, default=Path.home() / '.local/share/JavaLocalCloudConfigurator')
p.add_argument('--keystore', type=Path, help='PKCS12 of LocalCloudConfiguratorWorker (default <private>/certs/LocalCloudConfiguratorWorker.p12)')
p.add_argument('--truststore', type=Path, help='cloud truststore PKCS12 (default <private>/certs/truststore.p12)')
p.add_argument('--store-password-file', type=Path, help='password of both stores (default <private>/secrets/store-password)')
p.add_argument('--crl-file', type=Path, help='cloud CRL; client certificates are checked against it (recommended)')
p.add_argument('--allowed-clients', default='LocalCloudConfigurator')
p.add_argument('--port', type=int, default=8493)
# a cloud of another Compose project needs its own catalog (composeProject); the configurator API must load the same file
p.add_argument('--catalog', type=Path, help='component catalog (default catalogs/javacloud.json of this folder)')
a = p.parse_args()

root = Path(__file__).resolve().parents[1]
state = a.private_dir
state.mkdir(mode=0o700, parents=True, exist_ok=True)
os.chmod(state, 0o700)
keystore = (a.keystore or state / 'certs/LocalCloudConfiguratorWorker.p12').resolve()
truststore = (a.truststore or state / 'certs/truststore.p12').resolve()
password = (a.store_password_file or state / 'secrets/store-password').resolve()
for f in [keystore, truststore, password] + ([a.crl_file] if a.crl_file else []):
    if not Path(f).is_file():
        raise SystemExit(f'Missing {f}; the worker refuses to start without its certificate, truststore and store password')

# Do not interrupt a reviewed deployment that is executing: the worker's own file state says whether it is still running.
direct_state = state / 'direct-state'
marker = direct_state / 'active-deployment.json'
if marker.exists():
    active = json.loads(marker.read_text()).get('id')
    record = direct_state / 'workflows' / f'{active}.json'
    if active and record.exists() and json.loads(record.read_text()).get('state') not in ('success', 'failed'):
        raise SystemExit('A local deployment is active; finish it before reinstalling')

# The worker reads both store passwords from a config tree (one file per property), never from its command line.
secrets = state / 'worker-secrets'
secrets.mkdir(mode=0o700, exist_ok=True)
for name in ['lcc.worker.ssl.key-store-password', 'lcc.worker.ssl.trust-store-password']:
    dst = secrets / name
    tmp = secrets / (name + '.next')
    with open(os.open(tmp, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600), 'w') as f:
        f.write(password.read_text().strip())
    os.replace(tmp, dst)

runtime = state / 'runtime'
runtime.mkdir(mode=0o700, exist_ok=True)
jar = root / 'target/localcloudconfigurator.jar'
catalog = (a.catalog or root / 'catalogs/javacloud.json').resolve()
if catalog.name != 'javacloud.json':
    raise SystemExit('The catalog file must be named javacloud.json')
digest = hashlib.sha256(jar.read_bytes() + catalog.read_bytes()).hexdigest()
release = runtime / ('direct-' + digest)
release.mkdir(mode=0o700, exist_ok=True)
for src in [jar, catalog]:
    dst = release / src.name
    if not dst.exists():
        shutil.copy2(src, dst)
        dst.chmod(0o600)

view = state / 'direct-cloud'
view.mkdir(mode=0o700, exist_ok=True)
for name, target in {'compose.yaml': a.compose_file.resolve(), 'scripts': (a.cloud_dir / 'scripts').resolve(), '.env': (a.cloud_dir / '.env').resolve()}.items():
    dst = view / name
    if dst.is_symlink():
        dst.unlink()
    elif dst.exists():
        raise SystemExit('Refusing to replace a non-symlink deployment view entry')
    dst.symlink_to(target)

java = Path(subprocess.check_output(['/usr/libexec/java_home', '-v', '21'], text=True).strip()) / 'bin/java'
docker = shutil.which('docker')
if not docker:
    raise SystemExit('Docker CLI is required')

label = 'eu.arrowhead.java-localcloud-direct'
plist = Path.home() / 'Library/LaunchAgents' / f'{label}.plist'
plist.parent.mkdir(parents=True, exist_ok=True)
arguments = [str(java), '-Xmx160m', '-XX:+UseSerialGC', '-jar', str(release / jar.name), 'worker',
             f'--lcc.catalog.file={release / catalog.name}',
             f'--lcc.worker.cloud-dir={view}',
             f'--lcc.worker.state={direct_state}',
             f'--lcc.worker.docker={docker}',
             '--lcc.worker.address=127.0.0.1',
             f'--lcc.worker.port={a.port}',
             f'--lcc.worker.allowed-clients={a.allowed_clients}',
             f'--lcc.worker.ssl.key-store={keystore}',
             f'--lcc.worker.ssl.trust-store={truststore}',
             f'--spring.config.import=configtree:{secrets}/']
if a.crl_file:
    arguments.append(f'--lcc.worker.ssl.crl-file={a.crl_file.resolve()}')
agent = {'Label': label, 'ProgramArguments': arguments, 'RunAtLoad': True, 'KeepAlive': True, 'ThrottleInterval': 10,
         'ProcessType': 'Background', 'WorkingDirectory': str(state),
         'StandardOutPath': str(state / 'direct-worker.log'), 'StandardErrorPath': str(state / 'direct-worker-error.log')}
for log in ['direct-worker.log', 'direct-worker-error.log']:
    (state / log).touch(mode=0o600, exist_ok=True)
    (state / log).chmod(0o600)
domain = f'gui/{os.getuid()}'
subprocess.run(['launchctl', 'bootout', domain + '/' + label], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
# bootout returns before launchd has removed the service; bootstrapping meanwhile fails with error 5 (seen 2026-11-11)
for _ in range(50):
    if subprocess.run(['launchctl', 'print', domain + '/' + label], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode != 0:
        break
    time.sleep(0.2)
plist.write_bytes(plistlib.dumps(agent))
plist.chmod(0o600)
subprocess.run(['launchctl', 'enable', domain + '/' + label], check=True)
subprocess.run(['launchctl', 'bootstrap', domain, str(plist)], check=True)
print(f'LocalCloudConfigurator host worker installed on 127.0.0.1:{a.port} (mutual TLS): login startup and crash restart enabled')
