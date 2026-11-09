from pathlib import Path
import subprocess,json,os,time,ssl,urllib.request,urllib.error,shutil,datetime,concurrent.futures
b=Path(__file__).resolve().parents[2];out=b/'verification/all-support-2026-10-25';projects=json.loads((b/'verification/all-support-projects-2026-10-25.json').read_text());fixtures=Path('/tmp/ah5-all-support-fixtures');fixtures.mkdir(exist_ok=True)
network='ah5-all-support-isolation';core='ah5-all-support-core-fixture';results=[]
selection=os.environ.get('AH_SYSTEM_FILTER','')
prior=json.loads((out/'spring-deployment-results.json').read_text()) if selection and (out/'spring-deployment-results.json').exists() else []
if selection:projects={n:m for n,m in projects.items() if n in selection.split(',')}
def run(args,**kw):
 r=subprocess.run(args,text=True,capture_output=True,**kw)
 if r.returncode:raise RuntimeError(str(args[:5])+' '+r.stderr[-4000:])
 return r

def issue(name,cn=None,san=None,profile='sy',directory=None):
 d=directory or fixtures;d.mkdir(exist_ok=True);cn=cn or name+'.TestCloud.Company.arrowhead.eu';san=san or 'DNS:localhost,IP:127.0.0.1'
 run(['openssl','req','-new','-newkey','rsa:2048','-nodes','-keyout',str(d/(name+'.key')),'-out',str(d/(name+'.csr')),'-subj','/C=SE/O=Company/CN='+cn+'/dnQualifier='+profile]);(d/(name+'.ext')).write_text('basicConstraints=critical,CA:FALSE\nkeyUsage=critical,digitalSignature,keyEncipherment\nextendedKeyUsage=serverAuth,clientAuth\nsubjectAltName='+san+'\n')
 run(['openssl','x509','-req','-in',str(d/(name+'.csr')),'-CA',str(fixtures/'ca.crt'),'-CAkey',str(fixtures/'ca.key'),'-set_serial',str(int.from_bytes(os.urandom(12),'big')),'-out',str(d/(name+'.crt')),'-days','2','-extfile',str(d/(name+'.ext'))]);return d/(name+'.crt'),d/(name+'.key')
run(['openssl','req','-x509','-newkey','rsa:2048','-nodes','-keyout',str(fixtures/'ca.key'),'-out',str(fixtures/'ca.crt'),'-days','3','-subj','/C=SE/O=Company/CN=TestCloud.Company.arrowhead.eu','-addext','basicConstraints=critical,CA:TRUE','-addext','keyUsage=critical,keyCertSign,cRLSign'])
(fixtures/'index').write_text('');(fixtures/'crlnumber').write_text('1000\n');(fixtures/'openssl.cnf').write_text(f'[ca]\ndefault_ca=issuer\n[issuer]\ndatabase={fixtures}/index\nprivate_key={fixtures}/ca.key\ncertificate={fixtures}/ca.crt\ncrlnumber={fixtures}/crlnumber\ndefault_md=sha256\ndefault_crl_days=2\n')
run(['openssl','ca','-gencrl','-config',str(fixtures/'openssl.cnf'),'-out',str(fixtures/'ca.crl')])
for name,host in [('ServiceRegistry','serviceregistry'),('Authentication','authentication'),('DynamicServiceOrchestration','serviceorchestration-dynamic'),('ConsumerAuthorization','consumerauthorization')]:issue(name,san='DNS:'+host+',DNS:localhost,IP:127.0.0.1')
issue('Sysop',profile='op')
for secret in ['store-password','identity-password','db-password','db-root-password']:(fixtures/secret).write_text('isolated-fixture-'+secret+'-38!\n')
run(['docker','run','--rm','--network','none','-v',str(fixtures)+':/fixtures','maven:3.9-eclipse-temurin-21','keytool','-importcert','-noprompt','-storetype','PKCS12','-keystore','/fixtures/truststore.p12','-storepass','isolated-fixture-store-password-38!','-alias','cloud-ca','-file','/fixtures/ca.crt'])
shutil.copy2(Path(__file__).with_name('core_fixture.py'),fixtures/'core.py')
run(['docker','network','create','--subnet','10.253.249.0/24',network])
run(['docker','run','-d','--name',core,'--network',network,'--network-alias','serviceregistry','--network-alias','authentication','--network-alias','serviceorchestration-dynamic','--network-alias','consumerauthorization','-v',str(fixtures)+':/fixtures:ro','python:3.13-alpine','python','-u','/fixtures/core.py'])
try:
 for name,m in projects.items():
  slug=m['slug'];p=b/m['folder'];own=fixtures/slug;own.mkdir(exist_ok=True);cert,key=issue(name,san='DNS:'+slug+',DNS:localhost,IP:127.0.0.1',directory=own)
  run(['openssl','pkcs12','-export','-in',str(cert),'-inkey',str(key),'-certfile',str(fixtures/'ca.crt'),'-name',name+'.TestCloud.Company.arrowhead.eu','-out',str(own/(name+'.p12')),'-passout','pass:isolated-fixture-store-password-38!'])
  crldir=own/'crl';crldir.mkdir(exist_ok=True);shutil.copy2(fixtures/'ca.crl',crldir/'ca.crl')
  env=os.environ.copy();env.update({'COMPOSE_PROJECT_NAME':'ah5-isolation-'+slug,'ARROWHEAD_NETWORK':network,'HOST_PORT':'0','LOCAL_CLOUD':'TestCloud','ORGANIZATION':'Company','SERVICE_REGISTRY_ADDRESS':'serviceregistry','SERVICE_REGISTRY_PORT':'8443','SERVICE_REGISTRY_URL':'https://serviceregistry:8443','SYSTEM_KEYSTORE_FILE':str(own/(name+'.p12')),'TRUSTSTORE_FILE':str(fixtures/'truststore.p12'),'STORE_PASSWORD_FILE':str(fixtures/'store-password'),'IDENTITY_PASSWORD_FILE':str(fixtures/'identity-password'),'DB_PASSWORD_FILE':str(fixtures/'db-password'),'DB_ROOT_PASSWORD_FILE':str(fixtures/'db-root-password'),'DB_CA_FILE':str(fixtures/'ca.crt'),'CRL_DIRECTORY':str(crldir),'AMQP_HOST':'127.0.0.1','MQTT_HOST':'127.0.0.1'})
  if 'database' in m:
   cert,key=issue('db',cn=slug+'-db',san='DNS:'+slug+'-db',directory=own);env.update({'DB_CERT_FILE':str(cert),'DB_KEY_FILE':str(key)})
  for file in fixtures.rglob('*'):
   if file.is_file():file.chmod(0o644) # Disposable generated fixtures, removed after verification.
  command=['docker','compose','--project-directory',str(p),'-p','ah5-isolation-'+slug,'-f',str(p/'compose.yaml')]
  entry={'system':name,'passed':False}
  try:
   config=json.loads(run(command+['config','--format','json'],env=env).stdout);assert Path(config['services'][slug]['build']['context'])==p
   run(command+['up','-d','--no-build'],env=env)
   cid=run(command+['ps','-q',slug],env=env).stdout.strip();assert cid
   started=False
   for attempt in range(100):
    state=json.loads(run(['docker','inspect',cid]).stdout)[0]
    logs=run(['docker','logs',cid]).stdout
    if 'Started ' in logs and ('Main in ' in logs or 'AiGymApplication in ' in logs):started=True;break
    if not state['State']['Running']:raise RuntimeError('Process exited before application-ready')
    time.sleep(1)
   if not started:raise RuntimeError('Application did not finish startup')
   bound=run(command+['port',slug,str(m['port'])],env=env).stdout.strip();port=bound.rsplit(':',1)[1]
   prefix='translation' if name=='TranslationManager' else slug.replace('-','')
   endpoint='/aigym/status' if name=='AIGym' else '/datamanager/data-historian/systems' if name=='DataManager' else '/'+prefix+'/general/mgmt/get-config?keys=server.port'
   context=ssl.create_default_context(cafile=str(fixtures/'ca.crt'));context.load_cert_chain(str(fixtures/'Sysop.crt'),str(fixtures/'Sysop.key'))
   request=urllib.request.Request('https://127.0.0.1:'+port+endpoint,headers={'Authorization':'Bearer IDENTITY-TOKEN//fixture-Sysop'})
   try:
    with urllib.request.urlopen(request,context=context,timeout=10) as response:authorized=response.status;body=response.read()
   except urllib.error.HTTPError as e:authorized=e.code;body=e.read()
   assert authorized==200,(authorized,body[:500])
   try:
    with urllib.request.urlopen(request.full_url,context=context,timeout=10) as response:unauthorized=response.status
   except urllib.error.HTTPError as e:unauthorized=e.code
   assert unauthorized in [401,403],unauthorized
   no_certificate=ssl.create_default_context(cafile=str(fixtures/'ca.crt'))
   refused=False
   try:urllib.request.urlopen(request,context=no_certificate,timeout=5)
   except (ssl.SSLError,urllib.error.URLError,ConnectionError):refused=True
   assert refused,'Missing TLS identity accepted'
   uid=run(['docker','exec',cid,'id','-u']).stdout.strip();assert uid=='10001'
   if 'database' in m:
    dbid=run(command+['ps','-q',slug+'-db'],env=env).stdout.strip();dbstate=json.loads(run(['docker','inspect',dbid]).stdout)[0];assert all(mount.get('Source') not in [v.get('Source') for v in state['Mounts']] for mount in dbstate['Mounts'] if mount.get('Destination')=='/var/lib/mysql')
   entry.update({'passed':True,'application_ready':True,'authorized_https_status':authorized,'missing_token_status':unauthorized,'missing_tls_certificate_refused':refused,'uid':uid,'read_only_root':state['HostConfig']['ReadonlyRootfs'],'image_id':state['Image'],'private_database':bool('database' in m),'cloud_dependencies':'isolated mTLS registry/authentication/DSO protocol fixtures; no other support system','brokers':'deliberately absent; monitoring/federation readiness not certified'})
  except Exception as e:entry['error']=str(e)
  finally:
   (out/f'{slug}-deployment.log').write_text(subprocess.run(command+['logs','--no-color'],env=env,text=True,capture_output=True).stdout)
   run(command+['down','-v','--remove-orphans'],env=env)
  results.append(entry);merged={r['system']:r for r in prior};merged.update({r['system']:r for r in results});(out/'spring-deployment-results.json').write_text(json.dumps(list(merged.values()),indent=2)+'\n');print(json.dumps(entry),flush=True)
finally:
 (out/'core-fixture.log').write_text(run(['docker','logs',core]).stdout)
 run(['docker','rm','-f',core]);run(['docker','network','rm',network]);shutil.rmtree(fixtures)
