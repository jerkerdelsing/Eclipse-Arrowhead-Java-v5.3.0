from pathlib import Path
import json,re,hashlib,os,zipfile,io,xml.etree.ElementTree as ET
import argparse
parser=argparse.ArgumentParser(description='Generate the Arrowhead 5.2.2 SysML v2 snapshot from local implementation documentation.')
parser.add_argument('--source-root',type=Path,required=True)
parser.add_argument('--core-doc-root',type=Path,required=True,help='Matching Arrowhead source checkout containing ah5-core-java-spring documentation')
parser.add_argument('--output',type=Path,required=True)
cli=parser.parse_args()
R=cli.source_root.resolve()
O=cli.output.resolve(); O.mkdir(parents=True,exist_ok=True)
manifest={}; inventory=[]
def src(p):
 p=Path(p); manifest[os.path.relpath(p,R)]=hashlib.sha256(p.read_bytes()).hexdigest(); return p.read_text()
def q(s):return json.dumps(str(s),ensure_ascii=False)
def doc(s):return 'doc /* '+str(s).replace('*/','* /')+' */'
def ident(s):return "'"+str(s).replace('\\','\\\\').replace("'","\\'")+"'"
def args(t,cmd,n):
 pat=re.compile(r'\\'+cmd+r'\s*(?=\{)');out=[]
 for m in pat.finditer(t):
  i=m.end();a=[]
  for _ in range(n):
   while i<len(t) and t[i].isspace():i+=1
   if i>=len(t) or t[i]!='{': break
   start=i+1;depth=1;i+=1
   while i<len(t) and depth:
    if t[i]=='\\' and i+1<len(t) and t[i+1] in '{}':i+=2;continue
    if t[i]=='{':depth+=1
    elif t[i]=='}':depth-=1
    i+=1
   a.append(t[start:i-1])
  if len(a)==n:out.append(a)
 return out
def clean(t):
 t=t.replace(r'\allowbreak{}','').replace(r'\_','_').replace(r'\{','{').replace(r'\}','}').replace(r'\textless{}','<').replace(r'\textgreater{}','>')
 for _ in range(8):t=re.sub(r'\\(?:texttt|textit|mref|pref|pdef)\{([^{}]*)\}',r'\1',t)
 return t.replace('$','').replace(r'\,',' ').strip()
# Preserve translated datatypes and metadata names. Replace unconstrained local primitives
# with standard scalar aliases, and metaclass-as-annotation constructs with plain metadata.
p=R/'distribution/Papyrus/ArrowheadDSL_2025_v5/SysML v2/ArrowheeadDSL-SysMLv2.library/ArrowheadDSL.sysml'
t=src(p); t=t[:t.index('/*les Association*/')]+'}\n'
t=re.sub(r'\s*@\s*SysML::(?:PartUsage|PortUsage);','',t)
t=t.replace('attribute def String;','private import ScalarValues::String;').replace('attribute def Int;','private alias Int for ScalarValues::Integer;')
t=t.replace(':> SemanticMetadata', '') # tags do not assert implicit semantic specialization
# Inherited design stages are explicit, including device/network stages omitted in old translation.
for name,parent in [('DeviceDesignDescription','DeviceDesign'),('IntracloudNetworkDesignDescription','IntracloudNetworkDesign'),('IntercloudNetworkDesignDescription','IntercloudNetworkDesign')]:
 # retain old duplicate properties as redefinitions
 t=re.sub(r'(metadata def\s+'+name+r')\s*\{',r'\1 :> '+parent+' {',t)
# Avoid duplicate inherited field declarations.
for name,field in [('DeviceDesignDescription','DeviceName'),('IntracloudNetworkDesignDescription','IntraNetworkDomain'),('IntercloudNetworkDesignDescription','InterNetworkDomain')]:
 start=t.index('metadata def '+(' ' if name=='DeviceDesignDescription' else '')+name) if False else re.search(r'metadata def\s+'+name,t).start()
 end=t.index('}',start)
 block=t[start:end];block=re.sub(r'attribute '+field+r':[^;]+;', '',block);t=t[:start]+block+t[end:]
# Make old protocol spelling a compatibility alias (new code uses Protocol).
t=t.replace('port Port[1];', 'attribute Port : ScalarValues::Integer[0..1];')
t=t.replace('attribute Proyocol:ApplicationProtocolKind[1];','attribute Protocol:ApplicationProtocolKind[1];\n        attribute Proyocol:ApplicationProtocolKind[0..1]; // legacy spelling')
# protocol additions needed by current monitoring / interface registrations
t=t.replace("enum 'websocket';","enum 'websocket';\n            enum 'https';\n            enum 'mqtts';\n            enum 'amqp';\n            enum 'amqps';")
t=t.replace("enum 'PUT';\n\t\t\tenum 'HEAD';","enum 'PUT';\n            enum 'PATCH';\n\t\t\tenum 'HEAD';")
t=t.replace("enum 'InterCloud';","enum 'InterCloud';\n            enum 'Onboarding';\n            enum 'Operator';\n            enum 'System';")
insert='''
    // SysML v1.6 stereotype names retained alongside the previous v2 translation names.
    metadata def ServiceProtocol_HTTP :> ArrowHeadServiceInterfaceHTTP { attribute StatusCode : StatusKodKind[0..*]; }
    metadata def ServiceProtocol_MQTT :> ArrowHeadServiceInterfaceMQTT { alias Topic for SubURL; attribute StatusCode : StatusKodKind[0..*]; }
    metadata def ServiceProtocol_COAP :> ArrowHeadServiceInterfaceCOAP { attribute StatusCode : StatusKodKind[0..*]; }
    metadata def ServiceIProtocol_Websocket :> ArrowHeadServiceInterfaceWebsocket;
    metadata def FullPort :> AFPVNDatatypes::FullPort;
    metadata def ProxyPort :> AFPVNDatatypes::ProxyPort;
    metadata def System_of_LocalcloudsDesignDescription :> System_of_LocacloudsDesignDescription;
    metadata def ArrowHeadServiceInterfaceAMQP {
        attribute Method : String;
        attribute RoutingKey : String;
    }
    metadata def WireOperation {
        attribute protocol : String;
        attribute method : String[0..1];
        attribute address : String;
        attribute requestType : String[0..1];
        attribute responseType : String[0..1];
        attribute queryParameters : String[0..1];
        attribute pathParameters : String[0..1];
        attribute statusCodes : String[0..1];
    }
    metadata def SourceTrace {
        attribute path : String;
        attribute modelVersion : String = "5.2.2";
        attribute status : String = "RELEASE CANDIDATE";
    }
'''
t=t.replace("enum 'RST_STREAM';", "enum 'PRIORITY';\n            enum 'RST_STREAM';")
for marker,aliases in [
 ('metadata def System_of_LocalcloudsDesign ', 'alias SoLC_Name for SoLCName;'),
 ('metadata def IDD ', 'alias CertificateLevel for Certificate_level; alias CryptoAlgorithm for Crypto_algorithm;'),
 ('metadata def DeviceDesignDescription', "alias 'I/Oports' for IOports;"),
 ('metadata def IntercloudNetworkDesign', 'alias InterNetworkDomainName for InterNetworkDomain;'),
 ('metadata def LocalcloudDesignDescription', 'alias LC_workflow for LC_Workflow; alias LC_orchestration for LC_Orchestration;')]:
 start=re.search(re.escape(marker.strip()).replace('\\ ',r'\s+'),t).start();brace=t.index('{',start);t=t[:brace+1]+'\n        '+aliases+t[brace+1:]
# Public v1.6 datatype spellings resolve to the retained translated definitions.
pos=t.index('/*les ports */')
t=t[:pos]+"alias 'HTTP1.1_MethodKind' for HTTP11MethodKind;\n        alias 'HTTP/2FrameKind' for HTTP2FrameKind;\n        alias 'MQTT3.1.1_MessageTypeKind' for MQTT311MessageTypeKind;\n        alias 'MQTT5.0_MessageTypeKind' for MQTT50MessageTypeKind;\n        "+t[pos:]
t=t.rstrip()[:-1]+insert+'}\n'

(O/'ArrowheadDSL.sysml').write_text('// Arrowhead DSL translation updated 2026-10-27. See README.md for migration notes.\n'+t)
src(R/'distribution/Papyrus/ArrowheadDSL_2025_v5/SysML v1.6/Metamodel and Profile/SysML v1.6/ArrowheadProfile_v5.0.profile.uml')
# Snapshot all documentation inputs used.
corep=R/'ah5-core-java-spring/documentation-tools/model.json'; core=json.loads(src(corep))
# library design uses parts/ports for composition, metadata for DSL stereotypes.
(O/'ArrowheadFoundation.sysml').write_text('''library package ArrowheadFoundation {
    private import ScalarValues::*;
    private import ArrowheadProfile::*;
    item def JSONValue { doc /* An unconstrained JSON value; used where the source has an open map or unknown schema. */ }
    item def JSONMessage :> JSONValue;
    abstract part def ArrowheadSystem {
        attribute implementationVersion : String = "5.2.2";
        attribute releaseStatus : String = "RELEASE CANDIDATE";
    }
    abstract port def ArrowheadService;
    abstract part def Device;
    abstract part def IntraCloudNetwork;
    abstract part def InterCloudNetwork;
    abstract part def LocalCloud { part systems : ArrowheadSystem[0..*]; part devices : Device[0..*]; }
    abstract part def SystemOfLocalClouds { part clouds : LocalCloud[0..*]; }
    connection def DeviceNetworkConnection {
        end device : Device;
        end network : IntraCloudNetwork;
    }
    connection def InterCloudDeviceNetworkConnection {
        end device : Device;
        end network : InterCloudNetwork;
    }
}
''')
# Generate DTOs from core JSON and support IDD field tables, scoped per system.
def scalar(ty):
 ty=ty.strip();arr=ty.startswith(('List<','Set<','Collection<','Array<')) or ty.endswith('[]')
 base=re.sub(r'^(?:List|Set|Collection|Array)<(.*)>$',r'\1',ty).removesuffix('[]')
 mapping={'String':'String','string':'String','UUID':'String','boolean':'Boolean','Boolean':'Boolean','int':'Integer','Integer':'Integer','long':'Integer','Long':'Integer','Number':'Real','double':'Real','Double':'Real','float':'Real','Float':'Real','BigDecimal':'Real'}
 return mapping.get(base),base,arr

def datafile(package,dtos,enums,source):
 lines=['library package '+package+' {','    private import ScalarValues::*;','    private import ArrowheadFoundation::*;']
 for en,vals in sorted(enums.items()):
  lines+=['    enum def '+ident(en)+' {']+['        enum '+ident(v)+';' for v in vals]+['    }']
 for name,fields in sorted(dtos.items()):
  lines+=['    item def '+ident(name)+' :> JSONMessage {', '        '+doc('Source: '+source+'. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive.')]
  seen=set()
  for f in fields:
   if f['name'] in seen:continue
   seen.add(f['name']);ty=f['type'];s,base,arr=scalar(ty);mult='[0..*]' if arr else '[0..1]'
   if s:decl=f'attribute {ident(f["name"])} : {s}{mult};'
   elif base in enums:decl=f'attribute {ident(f["name"])} : {ident(base)}{mult};'
   elif base in dtos:decl=f'ref item {ident(f["name"])} : {ident(base)}{mult};'
   else:decl=f'ref item {ident(f["name"])} : JSONValue{mult};'
   lines+=['        '+decl[:-1]+' { '+doc('Source type: '+ty+'. '+f.get('description',''))+' }']
  lines+=['    }']
 lines+=['}'];(O/(package+'.sysml')).write_text('\n'.join(lines)+'\n')
datafile('ArrowheadCoreData',core['dtos'],core['enums'],str(corep.relative_to(R)))
# Parse current support document set. Source macros are balanced, not split on nested braces.
systems=[]
for name,sy in core['systems'].items():
 systems.append(dict(name=name,group='Core',source=str(corep.relative_to(R)),summary='Arrowhead core system; module '+sy['module'],services=sy['services'],dtos=core['dtos'],enums=core['enums'],data='ArrowheadCoreData',consumed=[],unregistered=sy.get('unregisteredEndpoints',[])))
for sysd in sorted(R.glob('ah5-*/documentation/v5.2.2/*-SysD.tex')):
 text=src(sysd);name=sysd.name.split('-v5.2.2-')[0];base=sysd.parent
 summary=re.search(r'This document describes.*?(?=\n\n)',text,re.S).group()
 consumed=re.search(r'\\textbf\{Consumed\}(.*?)};',text,re.S)
 consumed=clean(consumed.group(1)).split('\\\\') if consumed else []
 services={};dtos={}
 for p in sorted(base.glob('*-IDD.tex')):
  tx=src(p);service=args(tx,'ArrowheadServiceID',1)[0][0];protocol=p.stem.rsplit('-',2)[-2].lower()
  service_data=services.setdefault(service,{'serviceDefinition':service,'version':'1.0.0','metadata':{}})
  ops=[]
  for a in args(tx,'osubsection',5):
   opname,method,path,req,res=map(clean,a)
   ops.append(dict(name=opname,method=method,path=path,fullPath=path,requestBody=req or None,response=res or None,queryParams=[],pathVars=[]))
  key={'http':'http','mqtt':'mqtt','amqp':'amqp'}.get(protocol,protocol)
  service_data[key]={'ops':ops,'source':str(p.relative_to(R))}
  # Preserve the registration paragraph verbatim as decoded text, including policies/base paths.
  reg=tx.split('\\subsection{Interface registration}',1)
  if len(reg)>1:service_data[key]['registration']=clean(reg[1].split('\\section{Service Operations}',1)[0])
  # Each data model subsection owns its immediately following table.
  positions=list(re.finditer(r'\\msubsection\{struct\}\{([^}]+)\}',tx))
  for idx,m in enumerate(positions):
   body=tx[m.end():positions[idx+1].start() if idx+1<len(positions) else tx.find('\\subsection{Primitives}',m.end()) if '\\subsection{Primitives}' in tx[m.end():] else len(tx)]
   fields=[]
   for line in body.splitlines():
    if line.startswith('"') and ' & ' in line:
     parts=line.split(' & ',2)
     if len(parts)==3:
      field=clean(parts[0]).strip('"');typ=clean(parts[1]);desc=clean(parts[2].split('\\\\')[0]);fields.append({'name':field,'type':typ,'description':desc})
   n=m.group(1)
   if n in dtos and dtos[n]!=fields:raise ValueError('Conflicting schema '+name+' '+n)
   dtos[n]=fields
 # Include SDs and SysDD in provenance even though content is primarily taken from IDDs.
 for p in base.glob('*-SD.tex'):src(p)
 for p in base.glob('*-SysDD.tex'):src(p)
 data='Arrowhead'+name+'Data';datafile(data,dtos,{},str(base.relative_to(R)))
 systems.append(dict(name=name,group='Support',source=str(sysd.relative_to(R)),summary=clean(summary),services=list(services.values()),dtos=dtos,enums={},data=data,consumed=[x.strip() for x in consumed if x.strip()],unregistered=[]))
# The packaged core catalogue omits common-utils' conditional native monitoring service.
# Read its matching source-checkout IDD only after confirming the publisher in each target JAR.
S=cli.core_doc_root.resolve()
assert (S/'ah5-core-java-spring/documentation-tools/model.json').read_bytes()==corep.read_bytes(), 'Core source and target catalogues do not match'
for sy in [x for x in systems if x['group']=='Core']:
 module=core['systems'][sy['name']]['module']
 jar=next((R/'ah5-core-java-spring'/module/'target').glob('arrowhead-*-5.2.2.jar'))
 manifest[str(jar.relative_to(R))]=hashlib.sha256(jar.read_bytes()).hexdigest()
 with zipfile.ZipFile(jar) as z:
  common=z.read('BOOT-INF/lib/arrowhead-common-utils-5.2.2.jar')
  with zipfile.ZipFile(io.BytesIO(common)) as c:
   assert 'eu/arrowhead/common/sysmonitor/SysMonitorPublisher.class' in c.namelist()
 base=S/'ah5-core-java-spring'/module/'documentation/v5.2.2'
 p=base/(sy['name']+'-v5.2.2-sysMonitor-AMQP-IDD.tex');tx=src(p)
 ops=[]
 for a in args(tx,'osubsection',5):
  on,method,path,req,res=map(clean,a)
  ops.append(dict(name=on,method=method,path=path,fullPath=path,requestBody=req or None,response=res or None))
 reg=tx.split('\\subsection{Interface registration}',1)[1].split('\\section{Service Operations}',1)[0]
 sy['services'].append({'serviceDefinition':'sysMonitor','version':'1.0.0','amqp':{'ops':ops,'registration':clean(reg),'source':os.path.relpath(p,R)}})
 for m in re.finditer(r'\\msubsection\{struct\}\{([^}]+)\}',tx):
  if m.group(1)!='Observation':continue
  body=tx[m.end():tx.find('\\subsection{Primitives}',m.end())];fields=[]
  for line in body.splitlines():
   if line.startswith('"') and ' & ' in line:
    f,ty,desc=line.split(' & ',2);fields.append({'name':clean(f).strip('"'),'type':clean(ty),'description':clean(desc.split('\\\\')[0])})
  core['dtos']['Observation']=fields
 sysdp=base/(sy['name']+'-v5.2.2-SysD.tex');sysdt=src(sysdp)
 summary=re.search(r'This document describes.*?(?=\n\n)',sysdt,re.S)
 if summary:sy['summary']=clean(summary.group())
 sy['dtos']=core['dtos']
datafile('ArrowheadCoreData',core['dtos'],core['enums'],str(corep.relative_to(R))+'; native Observation from matching core AMQP IDDs')
# Request/response item type wrapper: standard scalars can be action parameters too.
def parameter(direction,name,ty,sy):
 ty=re.sub(r'^ResponseEntity<(.*)>$',r'\1',ty or '')
 if ty in ['void','Void']:return None
 st,base,arr=scalar(ty);mult='[0..*]' if arr else '[0..1]'
 if st:return direction+' attribute '+ident(name)+' : ScalarValues::'+st+mult+';'
 if base in sy['enums']:return direction+' attribute '+ident(name)+' : '+sy['data']+'::'+ident(base)+mult+';'
 target=sy['data']+'::'+ident(base) if base in sy['dtos'] else 'JSONMessage'
 return direction+' item '+ident(name)+' : '+target+mult+';'
for sy in systems:
 name=sy['name'];lines=['library package Arrowhead'+name+' {','    private import ScalarValues::*;','    private import ArrowheadFoundation::*;','    private import ArrowheadProfile::*;']
 for sv in sy['services']:
  sn=sv['serviceDefinition'];prefix=name+'_'+sn
  lines+=['    port def '+prefix+'_SD :> ArrowheadService {','        @SD { MicroserviceName = '+q(sn)+'; }','        '+doc('Service version '+sv.get('version','unspecified')+'. Source: '+sy['source']), '    }']
  for protocol in ['http','mqtt','amqp']:
   if protocol not in sv:continue
   it=sv[protocol];pd=prefix+'_'+protocol.upper()+'_IDD'
   lines+=['    port def '+pd+' :> '+prefix+'_SD {','        @IDD { MicroserviceName = '+q(sn)+'; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'+ident(protocol)+'; }', '        @SourceTrace { path = '+q(it.get('source',sy['source']))+'; }']
   if it.get('registration'):
    reg=it['registration']
    try:
     start=re.search(r'\{\s*"serviceDefinitionName"',reg).start();registration=json.JSONDecoder().raw_decode(reg[start:])[0]
     interface=registration['interfaces'][0]
     for key in ['templateName','protocol','policy']:
      if key in interface:lines+=['        attribute '+key+' : String = '+q(interface[key])+';']
     for key,value in interface.get('properties',{}).items():
      if isinstance(value,(str,int)):
       lines+=['        attribute '+ident(key)+' : '+('Integer' if isinstance(value,int) else 'String')+' = '+q(value)+';' if not isinstance(value,int) else '        attribute '+ident(key)+' : Integer = '+str(value)+';']
    except (ValueError,KeyError,TypeError,AttributeError,json.JSONDecodeError):
     for phrase,key in [('base path','basePath'),('interface template','templateName'),('policy','policy')]:
      match=re.search(re.escape(phrase)+r' ([^ ,;]+)',reg)
      if match:lines+=['        attribute '+key+' : String = '+q(match.group(1).rstrip('.'))+';']
   if it.get('basePath'):lines+=['        attribute basePath : String = '+q(it['basePath'])+';']
   if it.get('baseTopic'):lines+=['        attribute baseTopic : String = '+q(it['baseTopic'])+';']
   if it.get('registration'):lines+=['        '+doc('Registration (source documentation): '+it['registration'])]
   seen=set()
   for i,op in enumerate(it['ops']):
    on=op['name'];un=on if on not in seen else on+'_'+str(i);seen.add(on)
    rq=parameter('in',un+'Request',op.get('requestBody',op.get('payload')),sy);rs=parameter('out',un+'Response',op.get('response'),sy)
    lines+=['        '+doc(on+': '+op.get('method','MQTT request/reply')+' '+op.get('fullPath',op.get('path',it.get('baseTopic','')+on))+'. Input: '+str(op.get('requestBody',op.get('payload')))+'; output: '+str(op.get('response'))+'. '+op.get('summary',''))]
    # Direction is from provider: requests in, responses or publications out.
    if (op.get('requestBody') or op.get('payload')) and rq:lines+=['        '+rq]
    if (op.get('response') or protocol=='mqtt') and rs:lines+=['        '+rs]
    inventory.append({'system':name,'service':sn,'protocol':protocol,'operation':on,'method':op.get('method'),'path':op.get('fullPath',op.get('path',it.get('baseTopic','')+on)),'source':it.get('source',sy['source'])})
   lines+=['    }','    port def '+pd+'_Implementation :> '+pd+' { @IDD_Implementation; }']
   for i,op in enumerate(it['ops']):
    an=prefix+'_'+protocol+'_'+op['name']+'_'+str(i)
    lines+=['    action def '+ident(an)+' {','        '+doc(op.get('summary','Operation '+op['name'])+'. Wire method: '+op.get('method','MQTT request/reply')+'; address: '+op.get('fullPath',op.get('path',it.get('baseTopic','')+op['name']))+'. Query parameters: '+json.dumps(op.get('queryParams',[]))+'. Path parameters: '+json.dumps(op.get('pathVars',[]))+'. Status codes: '+json.dumps(op.get('codes',[])))]
    lines+=['        @WireOperation { protocol = '+q(protocol)+'; method = '+q(op.get('method','REQUEST_REPLY'))+'; address = '+q(op.get('fullPath',op.get('path',it.get('baseTopic','')+op['name'])))+'; requestType = '+q(op.get('requestBody',op.get('payload')) or 'unspecified')+'; responseType = '+q(op.get('response') or 'unspecified')+'; queryParameters = '+q(json.dumps(op.get('queryParams',[])))+'; pathParameters = '+q(json.dumps(op.get('pathVars',[])))+'; statusCodes = '+q(json.dumps(op.get('codes',[])))+'; }']
    rq=parameter('in','request',op.get('requestBody',op.get('payload')),sy)
    if (op.get('requestBody') or op.get('payload')) and rq:lines+=['        '+rq]
    rs=parameter('out','response',op.get('response'),sy)
    if (op.get('response') or protocol=='mqtt') and rs:lines+=['        '+rs]
    lines+=['    }']
 lines+=['    part def '+name+'_SysD :> ArrowheadSystem {','        @SysD { MicrosystemName = '+q(name)+'; FunctionalProperties = '+q(sy['summary'])+'; }','        @SourceTrace { path = '+q(sy['source'])+'; }','        '+doc(sy['summary'])]
 for sv in sy['services']:lines+=['        port '+ident(sv['serviceDefinition']+'_SD')+' : '+name+'_'+sv['serviceDefinition']+'_SD;']
 if sy['consumed']:lines+=['        '+doc('Consumed dependencies from SysD: '+', '.join(sy['consumed'])+'. These are external requirements, not assumed co-deployments.')]
 lines+=['    }','    part def '+name+'_SysDD :> '+name+'_SysD {','        @SysDD;']
 for sv in sy['services']:
  for protocol in ['http','mqtt','amqp']:
   if protocol in sv:lines+=['        port '+ident(sv['serviceDefinition']+'_'+protocol)+' : '+name+'_'+sv['serviceDefinition']+'_'+protocol.upper()+'_IDD;']
 lines+=['    }','    part def '+name+' :> '+name+'_SysDD {','        @System_Implementation;','    }','}']
 (O/('Arrowhead'+name+'.sysml')).write_text('\n'.join(lines)+'\n')
# Catalog is reusable optional membership; orchestration variants can coexist but are alternatives by deployment choice.
lines=['library package ArrowheadLocalCloudLibrary {','    private import ArrowheadFoundation::*;','    private import ArrowheadProfile::*;','    part def ArrowheadLocalCloud :> LocalCloud {','        @LocalcloudImplementation;']
for sy in systems:
 lines+=['        part '+ident(sy['name'][0].lower()+sy['name'][1:])+' : Arrowhead'+sy['name']+'::'+sy['name']+'[0..1] :> systems;']
lines+=['    }','    part def ArrowheadSystemOfLocalClouds :> SystemOfLocalClouds {','        @System_of_LocalcloudsImplementation;','        part localClouds : ArrowheadLocalCloud[0..*] :> clouds;','    }','}']
(O/'ArrowheadLocalCloudLibrary.sysml').write_text('\n'.join(lines)+'\n')
foundation=O/'ArrowheadFoundation.sysml'
ft=foundation.read_text().rstrip()[:-1]
for cn,left,right in [
 ('LocalCloudDesign_System_of_LocalcloudsDesign','LocalCloud','SystemOfLocalClouds'),
 ('System_of_LocacloudsDesignDescription_LocalcloudDesignDescription','SystemOfLocalClouds','LocalCloud'),
 ('System_of_LocalcloudsImplementation_LocalcloudImplementation','SystemOfLocalClouds','LocalCloud'),
 ('LocalcloudImplementation_System_Implementation','LocalCloud','ArrowheadSystem'),
 ('DeviceDesign_IntracloudNetworkDesign','Device','IntraCloudNetwork'),
 ('DeviceDesign_IntercloudNetworkDesign','Device','InterCloudNetwork'),
 ('DeviceDesignDescription_IntracloudNetworkDesignDescription','Device','IntraCloudNetwork'),
 ('DeviceDesignDescription_IntercloudNetworkDesignDescription','Device','InterCloudNetwork')]:
 ft+='    connection def '+cn+' { end leftEnd : '+left+'; end rightEnd : '+right+'; }\n'
ft+='    connection def System_Implementation_IDD_Implementation { end system : ArrowheadSystem; end port service : ArrowheadService; }\n}\n'
foundation.write_text(ft)
(O/'sources.json').write_text(json.dumps({'modelVersion':'5.2.2','date':'2026-10-27','inputs':manifest,'systems':[{'name':s['name'],'group':s['group'],'services':len(s['services']),'consumedDependencies':s['consumed'],'unregisteredEndpoints':s['unregistered']} for s in systems],'operations':inventory},indent=2)+'\n')
print('Systems',len(systems),'services',sum(len(s['services']) for s in systems),'interfaces',sum(sum(k in sv for k in ['http','mqtt','amqp']) for s in systems for sv in s['services']),'operations',len(inventory),'data files',len(list(O.glob('*Data.sysml'))))
