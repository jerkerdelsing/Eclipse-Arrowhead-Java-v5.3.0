from pathlib import Path
import json,subprocess,concurrent.futures,xml.etree.ElementTree as ET
b=Path(__file__).resolve().parents[2];out=b/'verification/all-support-2026-10-25';projects=json.loads((b/'verification/all-support-inventory.json').read_text())
def build(item):
 n,m=item;path=b/m['folder'];log=out/f'{m["slug"]}-offline-verify.log';cache='arrowhead-java-support-maven-cache' if m['runtime']=='native-java' else str(Path.home()/'.m2')
 with log.open('w') as f:r=subprocess.run(['docker','run','--rm','--network','none','--memory','2g','-v',str(path)+':/source','-v',cache+':/root/.m2','-w','/source','maven:3.9-eclipse-temurin-21','sh','scripts/verify.sh','-o'],stdout=f,stderr=subprocess.STDOUT)
 totals={k:0 for k in ['tests','failures','errors','skipped']}
 for file in (path/'target/surefire-reports').glob('TEST-*.xml'):
  root=ET.parse(file).getroot()
  for k in totals:totals[k]+=int(root.attrib.get(k,0))
 result={'system':n,'exit':r.returncode,**totals,'log':str(log)};print(json.dumps(result),flush=True);return result
with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:results=list(pool.map(build,projects.items()))
(out/'offline-build-results.json').write_text(json.dumps(results,indent=2)+'\n')
