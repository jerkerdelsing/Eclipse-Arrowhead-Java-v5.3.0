from pathlib import Path
import subprocess,os,json,time,shutil,hashlib,zipfile,xml.etree.ElementTree as ET
base=Path(__file__).resolve().parents[2]
roles={'CertificateAuthority':('certificate-authority',8448,8483),'Gatekeeper':('gatekeeper',8449,8480),'GateTunnel':('gatetunnel',8453,8481),'SysMonitor':('sysmonitor',8475,8482)}
results=[]
fixtures=Path('/tmp/arrowhead-independent-fixtures-20261025');fixtures.mkdir(exist_ok=True)
report=base/'verification/independent-support-2026-10-25';report.mkdir(parents=True,exist_ok=True)
def run(args,**kw):return subprocess.run(args,text=True,capture_output=True,check=True,**kw)
def openssl(*args):return run(['openssl',*args])
openssl('req','-x509','-newkey','ec','-pkeyopt','ec_paramgen_curve:P-256','-nodes','-keyout',str(fixtures/'ca.key'),'-out',str(fixtures/'ca.crt'),'-days','2','-subj','/C=SE/O=Company/CN=TestCloud.Company.arrowhead.eu','-addext','basicConstraints=critical,CA:TRUE','-addext','keyUsage=critical,keyCertSign,cRLSign')
(fixtures/'index').write_text('');(fixtures/'crlnumber').write_text('1000\n')
(fixtures/'openssl.cnf').write_text(f'''[ca]
default_ca=issuer
[issuer]
database={fixtures}/index
private_key={fixtures}/ca.key
certificate={fixtures}/ca.crt
crlnumber={fixtures}/crlnumber
default_md=sha256
default_crl_days=2
''')
(fixtures/'identity-password').write_text('isolated-generated-fixture-only\n')
network='ah5-independent-verification-20261025'
run(['docker','network','create','--subnet','10.253.250.0/24',network])
try:
 for name,(slug,port,health) in roles.items():
  p=base/f'ah5-{name}-java-spring'; own=fixtures/slug;own.mkdir(exist_ok=True)
  openssl('req','-new','-newkey','ec','-pkeyopt','ec_paramgen_curve:P-256','-nodes','-keyout',str(own/'system.key'),'-out',str(own/'system.csr'),'-subj',f'/C=SE/O=Company/CN={name}.TestCloud.Company.arrowhead.eu/dnQualifier=sy')
  (own/'extensions.cnf').write_text(f'basicConstraints=critical,CA:FALSE\nkeyUsage=critical,digitalSignature,keyEncipherment\nextendedKeyUsage=serverAuth,clientAuth\nsubjectAltName=DNS:localhost,DNS:{slug},IP:127.0.0.1\n')
  openssl('x509','-req','-in',str(own/'system.csr'),'-CA',str(fixtures/'ca.crt'),'-CAkey',str(fixtures/'ca.key'),'-set_serial',str(port),'-out',str(own/'system.crt'),'-days','1','-extfile',str(own/'extensions.cnf'))
  crldir=own/'crl';crldir.mkdir(exist_ok=True);os.chmod(crldir,0o777)
  openssl('ca','-gencrl','-config',str(fixtures/'openssl.cnf'),'-out',str(crldir/'ca.crl'))
  for file in fixtures.rglob('*'):
   if file.is_file():os.chmod(file,0o644) # Generated isolated fixtures only; no production credentials.
  env=os.environ.copy();env.update({'ARROWHEAD_NETWORK':network,'SERVICE_REGISTRY_URL':'https://127.0.0.1:1','HOST_PORT':'0','LOCAL_CLOUD':'TestCloud','ORGANIZATION':'Company','IDENTITY_PASSWORD_FILE':str(fixtures/'identity-password'),'SYSTEM_CERT_FILE':str(own/'system.crt'),'SYSTEM_KEY_FILE':str(own/'system.key'),'CLOUD_CA_FILE':str(fixtures/'ca.crt'),'CRL_DIRECTORY':str(crldir),'ISSUER_KEY_FILE':str(fixtures/'ca.key'),'FEDERATION_PROFILE':'legacy-mtls','RELAY_CHAIN_FILE':str(own/'system.crt'),'RELAY_KEY_FILE':str(own/'system.key'),'ORGANIZATION_CA_FILE':str(fixtures/'ca.crt')})
  command=['docker','compose','--project-directory',str(p),'-p','ah5-independent-test-'+slug,'-f',str(p/'compose.yaml')]
  compose=json.loads(run(command+['config','--format','json'],env=env).stdout)
  service=compose['services'][slug]
  assert Path(service['build']['context'])==p
  assert service['read_only'] and service['user']=='10001:10001'
  assert all(v.get('target')!='/certs/ca.key' for v in service['volumes']) if name!='CertificateAuthority' else True
  jar=p/f'target/arrowhead-{slug}-5.4.0.jar'
  with zipfile.ZipFile(jar) as archive:
   classes=set(archive.namelist());assert f'eu/arrowhead/port/{name}.class' in classes
   for other in roles:
    if other!=name:assert not any(c.startswith(f'eu/arrowhead/port/{other}$') or c==f'eu/arrowhead/port/{other}.class' for c in classes)
   assert 'eu.arrowhead.port.Main' in archive.read('META-INF/MANIFEST.MF').decode()
  try:
   run(command+['up','-d','--no-build'],env=env)
   cid=run(command+['ps','-q'],env=env).stdout.strip();assert cid
   expected=1 if name=='SysMonitor' else 0
   probe=None
   for attempt in range(10):
    probe=subprocess.run(['docker','exec',cid,'java','-jar',f'/app/{slug}.jar','healthcheck'],capture_output=True,text=True)
    state=json.loads(run(['docker','inspect',cid]).stdout)[0]
    if state['State']['Running'] and probe.returncode==expected:
     # Ensure startup created the role's private database, not simply a failed HTTP probe.
     db=run(['docker','exec',cid,'sh','-c',f'test -f /data/{slug}.db && id -u']).stdout.strip()
     assert db=='10001';break
    time.sleep(.5)
   else:raise AssertionError(f'{name}: startup failed: {probe.stderr}')
   run(['docker','run','--rm','--network','none','--read-only','--tmpfs','/tmp:rw,noexec,nosuid,nodev,size=64m,mode=1777',f'arrowhead-{slug}-standalone:5.4.0','storage-selftest'])
   (report/f'{slug}-container.log').write_text(run(['docker','logs',cid]).stdout)
   tests=0
   for xml in (p/'target/surefire-reports').glob('TEST-*.xml'):
    suite=ET.parse(xml).getroot();assert int(suite.attrib['failures'])==0 and int(suite.attrib['errors'])==0;tests+=int(suite.attrib['tests'])
   item={'system':name,'tests':tests,'jar':str(jar),'jar_sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'only_own_system_classes':True,'compose_local_build_context':True,'compose_isolated_start':True,'uid':10001,'read_only_root':True,'private_database':True,'healthcheck_exit':probe.returncode,'health_expectation':'degraded without AMQP/MQTT' if name=='SysMonitor' else 'process liveness','image':f'arrowhead-{slug}-standalone:5.4.0','image_id':state['Image'],'sqlite_image_smoke_test':True}
   results.append(item);print(json.dumps(item),flush=True)
  finally:
   (report/f'{slug}-compose.log').write_text(subprocess.run(command+['logs','--no-color'],env=env,text=True,capture_output=True).stdout)
   run(command+['down','-v','--remove-orphans'],env=env)
finally:
 run(['docker','network','rm',network])
 (report/'results.json').write_text(json.dumps(results,indent=2)+'\n')
 shutil.rmtree(fixtures)
assert len(results)==4
