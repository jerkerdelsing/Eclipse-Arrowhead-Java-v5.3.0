package eu.arrowhead.port;

import com.sun.net.httpserver.*;
import javax.net.ssl.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.security.cert.X509Certificate;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static eu.arrowhead.port.Json.*;

final class GateTunnel extends Service {
 final ControlStore controls;final Messaging.Relay relay;final Messaging.Publisher monitor=new Messaging.Publisher(this);
 final Map<String,Session>providers=new ConcurrentHashMap<>(),consumers=new ConcurrentHashMap<>();final Map<String,HttpClient>providerClients=new ConcurrentHashMap<>();
 static final class Session{String id,consumer,consumerCloud,provider,providerCloud,definition,peer;Map<String,Object>target;long created=System.currentTimeMillis();volatile long lastUsed=created;final AtomicLong requests=new AtomicLong();int port;HttpsServer server;
  // AH-GT-1 (J5): transcript parameters, own ephemeral key until the exchange completes, per-direction keys, sequence
  Map<String,Object>params;java.security.KeyPair ephemeral;Map<String,byte[]>keys;String consumerKey;Map<String,Object>ownExchange;final AtomicLong sequence=new AtomicLong();final SessionSecurity.Replay replay=new SessionSecurity.Replay();}
 // FEDERATION_PROFILE: legacy-mtls (relay-visible JSON, the earlier wire protocol) or ah-gt-1 (end-to-end protected, J5).
 // There is no fallback between them: a peer on the other profile is refused.
 final boolean e2e;final SessionSecurity security;
 GateTunnel()throws Exception{super("gatetunnel","GateTunnel","gatetunnel",8453,8481,"/data/gatetunnel.db");controls=new ControlStore(db);String profile=env("FEDERATION_PROFILE","legacy-mtls");if(!Set.of("legacy-mtls","ah-gt-1").contains(profile))throw new IllegalArgumentException("Unsupported federation profile");PeerNames.organization(this,cloud);e2e=profile.equals("ah-gt-1");security=e2e?SessionSecurity.fromEnvironment(role):null;relay=new Messaging.Relay(this,role,this::trustedPeer,this::incoming);}
 boolean trustedPeer(String peer){String[]p=peer.split("\\.");return p.length==5&&peer.equals("GateTunnel."+p[1]+"."+PeerNames.organization(this,p[1])+".arrowhead.eu")&&!p[1].equals(cloud)&&csv(env("NEIGHBOR_CLOUDS","GoCloud")).contains(p[1]);}
 static boolean operationAllowed(String method,String raw,String base,Map<String,Object>operations){
  try{URI uri=URI.create(raw);String path=uri.getRawPath();if(uri.isAbsolute()||uri.getRawAuthority()!=null||path==null||!path.startsWith("/")||path.contains("\\")||path.contains("//"))return false;String lower=path.toLowerCase(Locale.ROOT);for(String unsafe:List.of("%2e","%2f","%5c","%00","%25"))if(lower.contains(unsafe))return false;String decoded=uri.getPath();if(decoded.indexOf(0)>=0)return false;for(String part:decoded.split("/",-1))if(part.equals(".")||part.equals(".."))return false;
   for(Object value:operations.values()){var op=obj(value);if(!str(op.get("method")).equalsIgnoreCase(method))continue;String prefix=base.replaceAll("/+$","")+"/"+str(op.get("path")).replaceAll("^/+|/+$","");String[]pattern=prefix.replaceAll("^/+|/+$","").split("/",-1),actual=decoded.replaceAll("^/+|/+$","").split("/",-1);if(actual.length<pattern.length)continue;boolean match=true;for(int i=0;i<pattern.length;i++){if(pattern[i].matches("\\{[^{}]+}")){if(actual[i].isEmpty())match=false;}else if(!pattern[i].equals(actual[i]))match=false;}if(match)return true;}
  }catch(Exception ignored){}return false;
 }
 boolean expired(Session s){return System.currentTimeMillis()-s.created>envInt("SESSION_MAX_AGE_SECONDS",43200)*1000L||System.currentTimeMillis()-s.lastUsed>envInt("SESSION_IDLE_SECONDS",3600)*1000L;}
 Map<String,Object>summary(Session s,String side){return m("sessionId",s.id,"side",side,"consumer",s.consumer,"consumerCloud",s.consumerCloud,"provider",s.provider,"providerCloud",s.providerCloud,"serviceDefinition",s.definition,"requests",s.requests.get(),"createdAt",s.created/1000.0);}
 List<Object>summaries(){var out=new ArrayList<Object>();providers.values().forEach(s->out.add(summary(s,"provider")));consumers.values().forEach(s->out.add(summary(s,"consumer")));return out;}
 Session session(Map<String,Object>payload){var s=new Session();s.id=str(payload.getOrDefault("sessionId",id()));s.consumer=str(payload.get("consumer"));s.provider=str(payload.get("provider"));s.definition=str(payload.get("serviceDefinition"));s.consumerCloud=str(payload.get("consumerCloud"));s.providerCloud=str(payload.get("providerCloud"));s.peer=str(payload.get("peer"));s.target=new LinkedHashMap<>(obj(payload.get("target")));if(s.id.isEmpty()||s.consumer.isEmpty()||s.provider.isEmpty()||s.definition.isEmpty()||!trustedPeer(s.peer))throw new IllegalArgumentException("Required session fields or trusted peer missing");if(!e2e&&payload.get("keyExchange")!=null)throw new ApiError(400,"AH-GT-1 session cannot be downgraded to legacy-mTLS");return s;}
 Map<String,Object>params(Session s,String consumerGateTunnel,String providerGateTunnel,Object providerKey){return new LinkedHashMap<>(m("sessionId",s.id,"consumer",s.consumer,"consumerCloud",s.consumerCloud,"provider",s.provider,"serviceDefinition",s.definition,"consumerGateTunnel",consumerGateTunnel,"providerGateTunnel",providerGateTunnel,"providerKey",providerKey));}
 synchronized Map<String,Object>openProvider(Map<String,Object>payload){var s=session(payload);if(!s.consumerCloud.equals(PeerNames.cloudIdentity(s.peer)))throw new IllegalArgumentException("consumerCloud does not match peer");if(obj(s.target.get("operations")).isEmpty())throw new IllegalArgumentException("Negotiated operations required");String address=str(s.target.get("address"));int targetPort=integer(s.target.get("port"),0);if(address.isEmpty()||address.matches(".*[/@?#\\s].*")||targetPort<1||targetPort>65535)throw new IllegalArgumentException("Invalid provider endpoint");
  if(providers.size()>=256)throw new ApiError(503,"Provider session limit reached");providers.entrySet().removeIf(e->{Session old=e.getValue();return old.consumer.equals(s.consumer)&&old.consumerCloud.equals(s.consumerCloud)&&old.provider.equals(s.provider)&&old.definition.equals(s.definition);});if(e2e){try{s.ephemeral=SessionSecurity.ephemeral();s.params=params(s,s.peer,commonName(name),SessionSecurity.raw(s.ephemeral.getPublic()));providers.put(s.id,s);return m("sessionId",s.id,"keyExchange",security.exchange("provider",s.params));}catch(Exception e){throw new ApiError(500,"AH-GT-1 key exchange failed");}}
  providers.put(s.id,s);return m("sessionId",s.id);
 }
 synchronized Map<String,Object>openConsumer(Map<String,Object>payload)throws Exception{
  var s=session(payload);if(!s.providerCloud.equals(PeerNames.cloudIdentity(s.peer)))throw new IllegalArgumentException("providerCloud does not match peer");s.consumerCloud=cloud+"|"+organization;
  if(e2e){
   // the provider GateTunnel's signed ephemeral key, verified against the organization CA; then the own half (J5)
   var exchange=obj(payload.get("keyExchange"));if(exchange.isEmpty())throw new ApiError(400,"AH-GT-1 key exchange of the provider GateTunnel is required");
   s.params=params(s,commonName(name),s.peer,exchange.get("providerKey"));
   try{security.verifyPeer(exchange.get("certificateChain"),s.peer,exchange.get("signature"),SessionSecurity.transcript("provider",s.params));}catch(SessionSecurity.SecurityError e){throw new ApiError(502,"Peer key exchange refused: "+e.getMessage());}
   var own=SessionSecurity.ephemeral();s.params.put("consumerKey",SessionSecurity.raw(own.getPublic()));s.ownExchange=security.exchange("consumer",s.params);s.keys=SessionSecurity.derive(own.getPrivate(),exchange.get("providerKey"),s.id);
  }
  for(Session old:new ArrayList<>(consumers.values()))if(old.consumer.equals(s.consumer)&&old.providerCloud.equals(s.providerCloud)&&old.provider.equals(s.provider)&&old.definition.equals(s.definition)){consumers.remove(old.id);old.server.stop(0);}
  Set<Integer>used=new HashSet<>();consumers.values().forEach(old->used.add(old.port));HttpsServer listener=null;
  for(int p=envInt("INTERCLOUD_MIN_PORT",8200);p<=envInt("INTERCLOUD_MAX_PORT",8299);p++){if(used.contains(p))continue;try{listener=HttpsServer.create(new InetSocketAddress("0.0.0.0",p),32);s.port=p;break;}catch(java.io.IOException ignored){}}
  if(listener==null)throw new ApiError(503,"No tunnel ports available");listener.setHttpsConfigurator(new HttpsConfigurator(tls){@Override public void configure(HttpsParameters p){var parameters=tls.getDefaultSSLParameters();parameters.setNeedClientAuth(true);parameters.setProtocols(new String[]{"TLSv1.3","TLSv1.2"});p.setSSLParameters(parameters);}});
  listener.createContext("/",x->{boolean acquired=requests.tryAcquire();try{if(!acquired)throw new ApiError(503,"Too many requests");forward(s,x);}catch(ApiError e){try{send(x,e.status,m("error",e.getMessage()));}catch(Exception ignored){}}catch(Exception e){try{send(x,502,m("error","Tunnel forwarding failed"));}catch(Exception ignored){}}finally{if(acquired)requests.release();x.close();}});listener.setExecutor(Executors.newVirtualThreadPerTaskExecutor());s.server=listener;consumers.put(s.id,s);listener.start();return m("address",env("ADVERTISED_ADDRESS","gatetunnel"),"port",s.port,"sessionId",s.id);
 }
 void forward(Session s,HttpExchange x)throws Exception{
  if(expired(s)||consumers.get(s.id)!=s)throw new ApiError(401,"Session expired; orchestrate again");if(!caller(x).equals(s.consumer))throw new ApiError(403,"Caller is not the negotiated consumer");byte[]body=x.getRequestBody().readNBytes(1024*1024+1);if(body.length>1024*1024)throw new ApiError(413,"Request too large");
  var content=m("method",x.getRequestMethod(),"path",x.getRequestURI().toASCIIString(),"contentType",Objects.toString(x.getRequestHeaders().getFirst("Content-Type"),"application/json"),"body",Base64.getEncoder().encodeToString(body));
  Map<String,Object>reply;
  if(e2e){
   // method, path and body travel sealed with the session key (J5)
   long sequence=s.sequence.incrementAndGet();var message=new LinkedHashMap<String,Object>(m("type","http-request","sessionId",s.id,"keyExchange",s.ownExchange));message.putAll(SessionSecurity.seal(s.keys.get("c2p"),s.id,"c2p",sequence,content));
   var sealed=relay.request(s.peer.split("\\.")[1],message);if(sealed.get("ciphertext")==null)throw new ApiError(401,"Peer GateTunnel refused the request: "+str(sealed.getOrDefault("error","no reason")));
   SessionSecurity.Opened opened;try{opened=SessionSecurity.open(s.keys.get("p2c"),s.id,"p2c",sealed);}catch(SessionSecurity.SecurityError e){throw new ApiError(401,e.getMessage());}
   if(opened.sequence()!=sequence)throw new ApiError(401,"Reply does not belong to this request");reply=opened.content();
  }else{var legacy=new LinkedHashMap<String,Object>(m("type","http-request","sessionId",s.id));legacy.putAll(content);reply=relay.request(s.peer.split("\\.")[1],legacy);}
  int status=integer(reply.get("status"),502);if(status<100||status>599)throw new ApiError(502,"Invalid peer status");byte[]data=Base64.getDecoder().decode(str(reply.get("body")));if(data.length>1024*1024)throw new ApiError(502,"Peer response too large");s.requests.incrementAndGet();s.lastUsed=System.currentTimeMillis();sendBytes(x,status,data,str(reply.getOrDefault("contentType","application/json")));
 }
 Map<String,Object>answer(int status,byte[]body,String contentType){return m("status",status,"body",Base64.getEncoder().encodeToString(body),"contentType",contentType);}
 Map<String,Object>refusal(String reason){return m("error",reason,"accepted",false);}
 Map<String,Object>incoming(String peer,Map<String,Object>message)throws Exception{
  Session s=providers.get(str(message.get("sessionId")));
  if(!e2e){if(!str(message.get("type")).equals("http-request")||s==null||!s.peer.equals(peer)||expired(s))return answer(403,bytes(m("error","Unknown, expired or wrong-peer session")),"application/json");if(message.get("ciphertext")!=null)return answer(403,bytes(m("error","Encrypted profile cannot be downgraded")),"application/json");return execute(s,message);}
  // AH-GT-1 (J5): refusals carry no session data, only why the request was not executed
  if(!str(message.get("type")).equals("http-request")||s==null||!s.peer.equals(peer))return refusal("Unknown inter-cloud session or wrong peer");
  if(expired(s)){providers.remove(s.id,s);return refusal("Inter-cloud session has expired");}
  if(message.get("ciphertext")==null)return refusal("AH-GT-1 is required; a legacy request is not executed");
  SessionSecurity.Opened opened;
  try{
   var exchange=obj(message.get("keyExchange"));
   synchronized(s){
    if(s.keys==null){
     // first request: the consumer GateTunnel's signed ephemeral key completes the key exchange
     var candidate=new LinkedHashMap<>(s.params);candidate.put("consumerKey",exchange.get("consumerKey"));
     security.verifyPeer(exchange.get("certificateChain"),s.peer,exchange.get("signature"),SessionSecurity.transcript("consumer",candidate));
     s.consumerKey=str(exchange.get("consumerKey"));s.keys=SessionSecurity.derive(s.ephemeral.getPrivate(),s.consumerKey,s.id);s.ephemeral=null;
    }else if(!str(exchange.get("consumerKey")).equals(s.consumerKey))throw new SessionSecurity.SecurityError("key exchange differs from the established one");
   }
   opened=SessionSecurity.open(s.keys.get("c2p"),s.id,"c2p",message);
   if(!s.replay.accept(opened.sequence()))throw new SessionSecurity.SecurityError("replayed or outdated request");
  }catch(SessionSecurity.SecurityError e){return refusal(e.getMessage());}
  Map<String,Object>result;try{result=execute(s,opened.content());}catch(Exception e){result=answer(502,bytes(m("error","Provider call failed")),"application/json");}
  return SessionSecurity.seal(s.keys.get("p2c"),s.id,"p2c",opened.sequence(),result);
 }
 Map<String,Object>execute(Session s,Map<String,Object>message)throws Exception{
  String method=str(message.get("method")),path=str(message.get("path"));if(!operationAllowed(method,path,str(s.target.get("basePath")),obj(s.target.get("operations"))))return answer(403,bytes(m("error","Operation outside negotiated session")),"application/json");
  byte[]body=Base64.getDecoder().decode(str(message.get("body")));if(body.length>1024*1024)throw new IllegalArgumentException("Request too large");String address=str(s.target.get("address"));String endpoint=new URI("https",null,address,integer(s.target.get("port"),0),null,null,null).toASCIIString()+path;
  HttpClient providerClient=providerClients.get(s.provider);if(providerClient==null){providerClient=httpClient(Tls.context(certFile,keyFile,caFile,commonName(s.provider)));providerClients.put(s.provider,providerClient);}
  for(int attempt=0;attempt<2;attempt++){
   String identity=token(false);var req=HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(15)).header("Authorization","Bearer IDENTITY-TOKEN//"+identity).header("Content-Type",str(message.getOrDefault("contentType","application/json"))).header("X-Arrowhead-Remote-Consumer",s.consumer+"|"+s.consumerCloud).method(method,body.length==0?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofByteArray(body)).build();
   var response=providerClient.send(req,HttpResponse.BodyHandlers.ofInputStream());try(var in=response.body()){if(response.statusCode()==401&&attempt==0){invalidate(identity);continue;}byte[]data=in.readNBytes(1024*1024+1);if(data.length>1024*1024)throw new ApiError(502,"Provider response too large");s.requests.incrementAndGet();s.lastUsed=System.currentTimeMillis();return answer(response.statusCode(),data,response.headers().firstValue("Content-Type").orElse("application/json"));}
  }throw new IllegalStateException("Unreachable provider identity retry");
 }
 @Override void route(HttpExchange x)throws Exception{
  String path=path(x).replaceFirst("^/gateway(?=/|$)","/gatetunnel"),method=x.getRequestMethod();boolean management=path.contains("/mgmt/");String actor=management?allowed(x,"LocalCloudManager","Sysop"):allowed(x,"Gatekeeper","DynamicServiceOrchestration","SimpleStoreServiceOrchestration","LocalCloudManager","Sysop");
  // relay link and inter-cloud session status for the console, over mTLS (J11: the plain-HTTP health port is loopback-only)
  if(method.equals("GET")&&path.equals("/gatetunnel/mgmt/status")){send(x,200,health());return;}
  if(method.equals("GET")&&(path.equals("/gatetunnel/echo")||path.equals("/gatetunnel/monitor/echo"))){send(x,200,m("status","ok","system",name,"version","5.2.2"));return;}
  if(method.equals("GET")&&path.equals("/gatetunnel/publickey")){var key=Tls.cert(Path.of(certFile)).getPublicKey();send(x,200,m("algorithm",key.getAlgorithm(),"format","X.509","publicKey",Base64.getEncoder().encodeToString(key.getEncoded())));return;}
  if(method.equals("GET")&&path.equals("/gatetunnel/mgmt/sessions")){int limit=Math.min(1000,Math.max(1,integer(query(x).getOrDefault("limit",List.of("200")).getFirst(),200)));var rows=controls.all(limit);send(x,200,m("entries",rows,"count",rows.size()));return;}
  if(!method.equals("POST"))throw new ApiError(404,"Not found");Object raw=body(x);var payload=obj(raw);
  if(path.equals("/gatetunnel/intercloud/provider")||path.equals("/gatetunnel/intercloud/consumer")){if(!actor.equals("Gatekeeper"))throw new ApiError(403,"Only the local Gatekeeper opens relay sessions");send(x,201,path.endsWith("provider")?openProvider(payload):openConsumer(payload));return;}
  if(path.equals("/gatetunnel/connect_provider")){var s=controls.create(payload);send(x,201,m("sessionId",s.get("sessionId"),"queueId",s.get("queueId"),"peerName",s.get("peerName"),"serverPort",s.get("tunnelPort"),"state",s.get("state")));return;}
  if(path.equals("/gatetunnel/connect_consumer")){var s=controls.connect(str(payload.get("queueId")));send(x,201,m("sessionId",s.get("sessionId"),"serverPort",s.get("tunnelPort"),"state",s.get("state")));return;}
  if(path.equals("/gatetunnel/close_sessions")){var errors=new ArrayList<Object>();for(Object p:new LinkedHashSet<>(raw instanceof List<?>?list(raw):list(payload.get("ports")))){int value=integer(p,0);if(!controls.close(null,value))errors.add(m("port",value,"error","Active session not found"));}send(x,200,errors);return;}
  if(path.equals("/gatetunnel/mgmt/sessions/close")){String id=str(payload.get("sessionId"));boolean changed=controls.close(id,integer(payload.get("tunnelPort"),0));Session s=consumers.remove(id);if(s!=null){s.server.stop(0);changed=true;}if(providers.remove(id)!=null)changed=true;send(x,changed?200:404,m("closed",changed));return;}
  throw new ApiError(404,"Not found");
 }
 @Override void register()throws Exception{registerSystem(m("role","secure-inter-cloud-tunnel","replaces","Gateway-4.6.2"));var ids=new ArrayList<String>();// open-*-session: the relay sessions the local Gatekeeper opens for a negotiated inter-cloud service (it orchestrates them)
  ids.add(registerService("gateTunnelConnection","5.2.2","/gatetunnel",m("connect-provider",op("POST","/connect_provider"),"connect-consumer",op("POST","/connect_consumer"),"close-sessions",op("POST","/close_sessions"),"open-provider-session",op("POST","/intercloud/provider"),"open-consumer-session",op("POST","/intercloud/consumer")),m("generation","5","v4Compatibility",true)));ids.add(registerService("gateTunnelKey","5.2.2","/gatetunnel",m("public-key",op("GET","/publickey")),m("generation","5","v4Compatibility",true)));ids.add(registerService("gateTunnelManagement","5.2.2","/gatetunnel",m("sessions",op("GET","/mgmt/sessions"),"close",op("POST","/mgmt/sessions/close"),"status",op("GET","/mgmt/status")),m("generation","5","v4Compatibility",true)));registerMonitor(name,ids);registered(ids);}
 @Override void startWorkers(){periodic(relay::connect,10);periodic(()->Messaging.nativeMonitor(this,name,monitor,m("state","running","health",relay.client.isConnected()?"healthy":"degraded","providerSessions",providers.size(),"consumerSessions",consumers.size())),30);periodic(()->{providers.entrySet().removeIf(e->expired(e.getValue()));for(var s:consumers.values())if(expired(s)&&consumers.remove(s.id,s))s.server.stop(0);},60);}
 @Override Map<String,Object>health(){var h=super.health();h.put("relay",relay.status());h.put("interCloud",m("providerSessions",providers.size(),"consumerSessions",consumers.size(),"sessions",summaries()));try{h.put("activeSessions",controls.all(1000).stream().filter(s->!str(s.get("state")).equals("CLOSED")).count());}catch(Exception ignored){}return h;}
 @Override public void close(){relay.close();monitor.close();consumers.values().forEach(s->s.server.stop(0));providerClients.values().forEach(HttpClient::close);super.close();}

 static final class ControlStore{
  final Database db;ControlStore(Database db)throws Exception{this.db=db;db.schema("CREATE TABLE IF NOT EXISTS sessions(id TEXT PRIMARY KEY,queue_id TEXT UNIQUE NOT NULL,peer_name TEXT NOT NULL,service_definition TEXT NOT NULL,tunnel_port INTEGER UNIQUE NOT NULL,state TEXT NOT NULL,consumer_json TEXT NOT NULL,provider_json TEXT NOT NULL,consumer_cloud_json TEXT NOT NULL,provider_cloud_json TEXT NOT NULL,relay_json TEXT NOT NULL,created_at REAL NOT NULL,updated_at REAL NOT NULL)");}
  static Map<String,Object>view(Map<String,Object>r){return m("sessionId",r.get("id"),"queueId",r.get("queue_id"),"peerName",r.get("peer_name"),"serviceDefinition",r.get("service_definition"),"tunnelPort",r.get("tunnel_port"),"state",r.get("state"),"consumer",parse(str(r.get("consumer_json"))),"provider",parse(str(r.get("provider_json"))),"consumerCloud",parse(str(r.get("consumer_cloud_json"))),"providerCloud",parse(str(r.get("provider_cloud_json"))),"relay",parse(str(r.get("relay_json"))),"sessionStartedAt",r.get("created_at"));}
  List<Map<String,Object>>all(int limit)throws Exception{return db.query("SELECT * FROM sessions ORDER BY created_at DESC LIMIT ?",limit).stream().map(ControlStore::view).toList();}
  Map<String,Object>create(Map<String,Object>p)throws Exception{for(String required:List.of("consumer","provider","consumerCloud","providerCloud","relay","serviceDefinition"))if(p.get(required)==null||str(p.get(required)).isBlank())throw new IllegalArgumentException(required+" is required");return db.transaction(c->{var used=new HashSet<Integer>();Database.query(c,"SELECT tunnel_port FROM sessions").forEach(r->used.add(integer(r.get("tunnel_port"),0)));int port=0;for(int v=envInt("MIN_TUNNEL_PORT",8000);v<=envInt("MAX_TUNNEL_PORT",8099);v++)if(!used.contains(v)){port=v;break;}if(port==0)throw new ApiError(503,"No control ports available");String id=id(),queue=str(p.getOrDefault("queueId","gatetunnel."+id())),peer=str(p.getOrDefault("peerName","GateTunnel-"+id()));double now=System.currentTimeMillis()/1000.0;Database.update(c,"INSERT INTO sessions VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",id,queue,peer,p.get("serviceDefinition"),port,"PROVIDER_READY",json(p.get("consumer")),json(p.get("provider")),json(p.get("consumerCloud")),json(p.get("providerCloud")),json(p.get("relay")),now,now);return view(Database.query(c,"SELECT * FROM sessions WHERE id=?",id).getFirst());});}
  Map<String,Object>connect(String queue)throws Exception{if(queue.isBlank())throw new IllegalArgumentException("queueId required");return db.transaction(c->{var rows=Database.query(c,"SELECT * FROM sessions WHERE queue_id=? AND state<>'CLOSED'",queue);if(rows.isEmpty())throw new ApiError(404,"Session not found");Database.update(c,"UPDATE sessions SET state='ACTIVE',updated_at=? WHERE queue_id=?",System.currentTimeMillis()/1000.0,queue);return view(Database.query(c,"SELECT * FROM sessions WHERE queue_id=?",queue).getFirst());});}
  boolean close(String id,int port)throws Exception{return id!=null&&!id.isBlank()?db.update("UPDATE sessions SET state='CLOSED',updated_at=? WHERE id=? AND state<>'CLOSED'",System.currentTimeMillis()/1000.0,id)>0:db.update("UPDATE sessions SET state='CLOSED',updated_at=? WHERE tunnel_port=? AND state<>'CLOSED'",System.currentTimeMillis()/1000.0,port)>0;}
 }
}
