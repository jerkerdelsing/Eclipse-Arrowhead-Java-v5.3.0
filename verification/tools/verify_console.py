from pathlib import Path
import subprocess,os,json,hashlib,base64,secrets,time,urllib.request,urllib.error,shutil
b=Path(__file__).resolve().parents[2];p=b/'ah5-LocalCloudManager-java-spring';out=b/'verification/all-support-2026-10-25';f=Path('/tmp/ah5-lcm-isolation-fixtures');f.mkdir(exist_ok=True)
def run(args,**kw):
 r=subprocess.run(args,text=True,capture_output=True,**kw)
 if r.returncode:raise RuntimeError(r.stderr[-3000:])
 return r
run(['openssl','req','-x509','-newkey','rsa:2048','-nodes','-days','2','-keyout',str(f/'ca.key'),'-out',str(f/'ca.crt'),'-subj','/C=SE/O=Company/CN=TestCloud.Company.arrowhead.eu','-addext','basicConstraints=critical,CA:TRUE','-addext','keyUsage=critical,keyCertSign,cRLSign'])
run(['openssl','req','-new','-newkey','rsa:2048','-nodes','-keyout',str(f/'system.key'),'-out',str(f/'system.csr'),'-subj','/C=SE/O=Company/CN=LocalCloudManager.TestCloud.Company.arrowhead.eu/dnQualifier=sy'])
(f/'extensions.cnf').write_text('basicConstraints=critical,CA:FALSE\nkeyUsage=digitalSignature,keyEncipherment\nextendedKeyUsage=clientAuth,serverAuth\n')
run(['openssl','x509','-req','-in',str(f/'system.csr'),'-CA',str(f/'ca.crt'),'-CAkey',str(f/'ca.key'),'-set_serial','1001','-days','1','-out',str(f/'system.crt'),'-extfile',str(f/'extensions.cnf')])
(f/'index').write_text('');(f/'crlnumber').write_text('1000\n');(f/'openssl.cnf').write_text(f'[ca]\ndefault_ca=issuer\n[issuer]\ndatabase={f}/index\nprivate_key={f}/ca.key\ncertificate={f}/ca.crt\ncrlnumber={f}/crlnumber\ndefault_md=sha256\ndefault_crl_days=2\n');(f/'crl').mkdir(exist_ok=True)
run(['openssl','ca','-gencrl','-config',str(f/'openssl.cnf'),'-out',str(f/'crl/ca.crl')])
password='isolated-console-password-only';salt=secrets.token_bytes(16);digest=hashlib.scrypt(password.encode(),salt=salt,n=16384,r=8,p=1,dklen=32)
(f/'console-password-hash').write_text('scrypt:'+base64.b64encode(salt).decode()+':'+base64.b64encode(digest).decode());(f/'console-proxy-secret').write_text(secrets.token_hex(32));(f/'identity-password').write_text(secrets.token_hex(24))
for file in f.rglob('*'):
 if file.is_file():file.chmod(0o644)
network='ah5-lcm-isolation';run(['docker','network','create','--subnet','10.253.248.0/24',network]);env=os.environ.copy();env.update({'ARROWHEAD_NETWORK':network,'HOST_PORT':'0','SERVICE_REGISTRY_URL':'https://127.0.0.1:1','SYSTEM_CERT_FILE':str(f/'system.crt'),'SYSTEM_KEY_FILE':str(f/'system.key'),'CLOUD_CA_FILE':str(f/'ca.crt'),'CRL_DIRECTORY':str(f/'crl'),'IDENTITY_PASSWORD_FILE':str(f/'identity-password'),'CONSOLE_PASSWORD_HASH_FILE':str(f/'console-password-hash'),'CONSOLE_PROXY_SECRET_FILE':str(f/'console-proxy-secret')})
command=['docker','compose','--project-directory',str(p),'-p','ah5-lcm-isolation','-f',str(p/'compose.yaml')];result={'system':'LocalCloudManager','runtime':'Node.js/nginx','jar':'not applicable','passed':False,'host_docker_access':False}
try:
 config=json.loads(run(command+['config','--format','json'],env=env).stdout);assert set(config['services'])=={'cloud-monitor-api','cloud-monitor-ui'}
 run(command+['up','-d','--no-build'],env=env);bound=run(command+['port','cloud-monitor-ui','8490'],env=env).stdout.strip();url='http://127.0.0.1:'+bound.rsplit(':',1)[1]
 for i in range(40):
  try:
   with urllib.request.urlopen(url+'/health',timeout=3) as response:health=json.loads(response.read());assert health['system']=='LocalCloudManager';break
  except Exception:time.sleep(.5)
 else:raise AssertionError('Console health did not become available')
 with urllib.request.urlopen(url+'/',timeout=3) as response:assert response.status==200
 try:urllib.request.urlopen(url+'/api/overview',timeout=3);raise AssertionError('Anonymous API access accepted')
 except urllib.error.HTTPError as error:assert error.code==401
 req=urllib.request.Request(url+'/api/session',data=json.dumps({'password':password}).encode(),headers={'Content-Type':'application/json'})
 with urllib.request.urlopen(req,timeout=5) as response:
  assert response.status==204;cookie=response.headers['Set-Cookie'].split(';')[0];response.read()
 req=urllib.request.Request(url+'/api/session',headers={'Cookie':cookie})
 with urllib.request.urlopen(req,timeout=3) as response:assert json.loads(response.read())['authenticated']
 for service in ['cloud-monitor-api','cloud-monitor-ui']:
  cid=run(command+['ps','-q',service],env=env).stdout.strip();state=json.loads(run(['docker','inspect',cid]).stdout)[0];assert state['HostConfig']['ReadonlyRootfs'];assert all('docker.sock' not in mount['Source'] for mount in state['Mounts'])
 result.update({'passed':True,'ui_http':200,'health_http':200,'anonymous_api_status':401,'operator_session':'login and authenticated session passed','private_generated_credentials':True})
except Exception as error:result['error']=str(error)
finally:
 (out/'localcloudmanager-deployment.log').write_text(subprocess.run(command+['logs','--no-color'],env=env,text=True,capture_output=True).stdout)
 run(command+['down','-v','--remove-orphans'],env=env);run(['docker','network','rm',network]);shutil.rmtree(f)
 (out/'localcloudmanager-results.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
