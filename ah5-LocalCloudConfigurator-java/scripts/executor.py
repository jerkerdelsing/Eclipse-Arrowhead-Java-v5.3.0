#!/usr/bin/env python3
"""Start/stop the Java cloud's catalog-checked ColonyOS executor (no secrets in argv or output)."""
from pathlib import Path
import argparse,subprocess,os,signal,shutil,hashlib,json,urllib.request,time
r=Path(__file__).resolve().parents[1];p=argparse.ArgumentParser();p.add_argument('action',choices=['start','stop','restart','status']);p.add_argument('--cloud-dir',type=Path,default=r.parents[1]/'Arrowhead/Arrowhead-test-cloud-java');p.add_argument('--private-dir',type=Path,default=Path.home()/'.local/share/JavaLocalCloudConfigurator');a=p.parse_args();state=a.private_dir;state.mkdir(mode=0o700,parents=True,exist_ok=True);os.chmod(state,0o700);pidfile=state/'executor.pid';jar=r/'target/localcloudconfigurator.jar'
def active():
 if not pidfile.exists():return None
 try:
  pid=int(pidfile.read_text());cmd=subprocess.check_output(['ps','-p',str(pid),'-o','command='],text=True,stderr=subprocess.DEVNULL)
  if (str(jar) in cmd or str(state/'runtime/localcloudconfigurator-') in cmd) and ' executor ' in cmd:return pid
 except (ValueError,subprocess.CalledProcessError):return None
 return None
pid=active()
if a.action=='restart' and pid:
 docker=shutil.which('docker')or str(Path.home()/'.docker/bin/docker')
 probe=subprocess.run([docker,'exec','arrowhead-v52-test-cloud-localcloudconfigurator-1','cat','/state/active-deployment.json'],capture_output=True,text=True)
 if probe.returncode==0:
  workflow=json.loads(probe.stdout)['id'];token=(state/'api-token').read_text().strip();req=urllib.request.Request('http://127.0.0.1:8097/api/deployments/'+workflow,headers={'Authorization':'Bearer '+token})
  with urllib.request.urlopen(req,timeout=30)as reply:phase=json.load(reply)['state']
  if phase not in ['success','failed']:raise SystemExit('A deployment is active; finish it before restarting the executor')
 os.kill(pid,signal.SIGTERM)
 for _ in range(50):
  if not active():break
  time.sleep(.1)
 if active():raise SystemExit('Previous executor has not stopped; no second executor started')
 pidfile.unlink(missing_ok=True);pid=None
if a.action=='status':print('running'if pid else'stopped');raise SystemExit(0 if pid else 1)
if a.action=='stop':
 if pid:os.kill(pid,signal.SIGTERM)
 pidfile.unlink(missing_ok=True);print('Java local-cloud executor stopped');raise SystemExit()
if pid:print('Java local-cloud executor already running');raise SystemExit()
if not jar.exists():raise SystemExit('Build target/localcloudconfigurator.jar first')
java=os.environ.get('JAVA')
if not java:
 java=str(Path(subprocess.check_output(['/usr/libexec/java_home','-v','21'],text=True).strip())/'bin/java') if Path('/usr/libexec/java_home').exists() else shutil.which('java')
if not java:raise SystemExit('Java 21 or newer is required')
runtime=state/'runtime';runtime.mkdir(mode=0o700,exist_ok=True);digest=hashlib.sha256(jar.read_bytes()).hexdigest();deployed=runtime/('localcloudconfigurator-'+digest+'.jar')
if not deployed.exists():
 temporary=runtime/'executor.next';shutil.copy2(jar,temporary);os.chmod(temporary,0o600);os.replace(temporary,deployed)
docker=shutil.which('docker')or str(Path.home()/'.docker/bin/docker')
env={**os.environ,'COLONIES_SERVER_HOST':'127.0.0.1','COLONIES_SERVER_PORT':'50081','COLONIES_COLONY_NAME':'arrowhead-java-localcloud','COLONIES_PRVKEY_FILE':str(state/'executor-key')}
logfile=state/'executor.log';log=logfile.open('ab');os.chmod(logfile,0o600)
child=subprocess.Popen([java,'-Xmx128m','-XX:+UseSerialGC','-jar',str(deployed),'executor','-catalog',str(r/'catalogs/javacloud.json'),'-cloud-dir',str(a.cloud_dir),'-docker',docker],cwd=r,env=env,stdout=log,stderr=subprocess.STDOUT,start_new_session=True);pidfile.write_text(str(child.pid));os.chmod(pidfile,0o600);print('Java local-cloud executor started')
