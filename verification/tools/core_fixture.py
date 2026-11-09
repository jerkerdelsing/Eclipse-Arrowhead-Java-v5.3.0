# Test-only core-service protocol fixture. Never use as a production authority.
import http.server,ssl,json,threading,uuid,datetime
from pathlib import Path
root=Path('/fixtures')
roles={8443:'ServiceRegistry',8444:'Authentication',8441:'DynamicServiceOrchestration',8445:'ConsumerAuthorization'}
def now():return datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z')
class Handler(http.server.BaseHTTPRequestHandler):
 def log_message(self,*a):pass
 def response(self,code,data):
  payload=json.dumps(data).encode();self.send_response(code);self.send_header('Content-Type','application/json');self.send_header('Content-Length',str(len(payload)));self.end_headers();self.wfile.write(payload)
 def handle_request(self):
  try:
   data=json.loads(self.rfile.read(int(self.headers.get('Content-Length',0))) or b'{}')
   peer=self.connection.getpeercert();cn=next(v for rdn in peer['subject'] for k,v in rdn if k=='commonName');system=cn.split('.')[0]
   path=self.path.split('?')[0]
   print(json.dumps({'method':self.command,'path':path,'system':system}),flush=True)
   if path.endswith('/login'):self.response(200,{'token':'fixture-'+system,'expirationTime':'2099-01-01T00:00:00Z'});return
   if 'consumerauthorization' in path:self.response(200,True);return
   if '/verify' in path:self.response(200,{'verified':True,'systemName':'Sysop','sysop':True,'expirationTime':'2099-01-01T00:00:00Z'});return
   if path.endswith('/pull'):
    definition=data.get('serviceRequirement',{}).get('serviceDefinition')
    if definition=='authorization':
     interface={'templateName':'generic_https','protocol':'https','policy':'CERT_AUTH','properties':{'accessAddresses':['consumerauthorization'],'accessPort':8445,'basePath':'/consumerauthorization/authorization','operations':{'verify':{'method':'POST','path':'/verify'}}}}
     self.response(200,{'results':[{'serviceInstanceId':'fixture-authorization','providerName':'ConsumerAuthorization','serviceDefinitition':'authorization','version':'5.2.2','metadata':{},'interfaces':[interface]}]});return
    self.response(200,{'results':[]});return
   if path.endswith('/lookup') and 'service-discovery' in path:
    definition=data.get('serviceDefinitionNames',['identity'])[0]
    spec={'identity':(8444,'authentication/identity',{'identity-login':('POST','/login'),'identity-verify':('GET','/verify'),'identity-logout':('POST','/logout')}),'systemDiscovery':(8443,'serviceregistry/system-discovery',{'register':('POST','/register'),'revoke':('DELETE','/revoke'),'lookup':('POST','/lookup'),'query':('POST','/query')}),'serviceDiscovery':(8443,'serviceregistry/service-discovery',{'register':('POST','/register'),'revoke':('DELETE','/{instanceId}'),'lookup':('POST','/lookup')}),'serviceOrchestration':(8441,'serviceorchestration/orchestration',{'pull':('POST','/pull')})}
    if definition not in spec:self.response(200,{'entries':[],'count':0});return
    port,base,operations=spec[definition];provider=roles[port]
    interface={'templateName':'generic_https','protocol':'https','policy':'CERT_AUTH','properties':{'accessAddresses':['serviceregistry' if port==8443 else 'authentication' if port==8444 else 'serviceorchestration-dynamic'],'accessPort':port,'basePath':'/'+base,'operations':{op:{'method':method,'path':p} for op,(method,p) in operations.items()}}}
    self.response(200,{'entries':[{'instanceId':str(uuid.uuid4()),'provider':{'name':provider,'addresses':[]},'serviceDefinition':{'name':definition},'version':'5.2.2','metadata':{},'interfaces':[interface]}],'count':1});return
   if path.endswith('/register'):
    if 'system-discovery' in path:self.response(201,{'name':system,'metadata':{},'version':'5.2.2','addresses':[]});return
    self.response(201,{'instanceId':str(uuid.uuid4()),'provider':{'name':system},'serviceDefinition':{'name':data.get('serviceDefinitionName','fixture')},'version':'5.2.2','metadata':{},'interfaces':data.get('interfaces',[])});return
   self.response(200,{})
  except Exception as error:print(type(error).__name__,str(error),flush=True);self.response(500,{'error':'fixture failure'})
 do_POST=handle_request;do_GET=handle_request;do_DELETE=handle_request
servers=[]
for port,system in roles.items():
 server=http.server.ThreadingHTTPServer(('0.0.0.0',port),Handler);context=ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER);context.load_cert_chain(str(root/(system+'.crt')),str(root/(system+'.key')));context.load_verify_locations(str(root/'ca.crt'));context.verify_mode=ssl.CERT_REQUIRED;server.socket=context.wrap_socket(server.socket,server_side=True);threading.Thread(target=server.serve_forever,daemon=True).start();servers.append(server)
threading.Event().wait()
