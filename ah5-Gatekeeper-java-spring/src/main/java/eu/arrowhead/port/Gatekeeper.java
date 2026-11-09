package eu.arrowhead.port;

import com.sun.net.httpserver.HttpExchange;
import java.util.*;
import static eu.arrowhead.port.Json.*;

final class Gatekeeper extends Service {
 final Messaging.Relay relay;final Messaging.Publisher monitor=new Messaging.Publisher(this);
 Gatekeeper()throws Exception{
  super("gatekeeper","Gatekeeper","gatekeeper",8449,8480,"/data/gatekeeper.db");
  db.schema("CREATE TABLE IF NOT EXISTS clouds(name TEXT NOT NULL,operator TEXT NOT NULL,gatekeeper_address TEXT,gatekeeper_port INTEGER,metadata TEXT NOT NULL DEFAULT '{}',created_at REAL NOT NULL,updated_at REAL NOT NULL,PRIMARY KEY(name,operator))","CREATE TABLE IF NOT EXISTS relays(name TEXT PRIMARY KEY,address TEXT NOT NULL,port INTEGER NOT NULL,type TEXT NOT NULL,secure INTEGER NOT NULL DEFAULT 1,metadata TEXT NOT NULL,created_at REAL NOT NULL)");
  if(!Set.of("legacy-mtls","ah-gt-1").contains(env("FEDERATION_PROFILE","legacy-mtls")))throw new IllegalArgumentException("Unsupported federation profile: legacy-mtls or ah-gt-1 (J5); never silently downgrade an encrypted peer");
  PeerNames.organization(this,cloud);relay=new Messaging.Relay(this,role,this::trustedPeer,this::incoming);
 }
 List<Map<String,Object>>clouds()throws Exception{var rows=db.query("SELECT * FROM clouds ORDER BY operator,name");return rows.stream().map(r->m("name",r.get("name"),"operator",r.get("operator"),"neighbor",true,"secure",true,"gatekeeperAddress",r.get("gatekeeper_address"),"gatekeeperPort",r.get("gatekeeper_port"),"metadata",parse(str(r.get("metadata"))))).toList();}
 List<Map<String,Object>>relays()throws Exception{return db.query("SELECT * FROM relays ORDER BY name").stream().map(r->m("name",r.get("name"),"address",r.get("address"),"port",r.get("port"),"type",r.get("type"),"secure",integer(r.get("secure"),0)!=0,"metadata",parse(str(r.get("metadata"))),"createdAt",r.get("created_at"))).toList();}
 boolean trustedPeer(String peer){String[]p=peer.split("\\.");if(p.length!=5||!peer.equals("Gatekeeper."+p[1]+"."+PeerNames.organization(this,p[1])+".arrowhead.eu")||p[1].equals(cloud))return false;try{return clouds().stream().anyMatch(c->str(c.get("name")).equals(p[1])&&str(c.get("operator")).equals(p[2]));}catch(Exception e){return false;}}
 List<Object>instances(String definition)throws Exception{if(definition.isBlank())throw new IllegalArgumentException("serviceDefinition is required");return list(obj(call(registry+"/serviceregistry/service-discovery/lookup","POST",m("serviceDefinitionNames",List.of(definition),"interfaceTemplateNames",List.of("generic_https")))).get("entries"));}
 static Map<String,Object>properties(Map<String,Object>instance){for(Object raw:list(instance.get("interfaces"))){var i=obj(raw);if(str(i.get("templateName")).equals("generic_https")&&str(i.get("protocol")).equalsIgnoreCase("https"))return obj(i.get("properties"));}return m();}
 boolean authorized(String provider,String consumer,String consumerCloud,String definition,List<String>operations){
  if(provider.isBlank()||consumer.isBlank()||definition.isBlank())return false;
  var checks=new ArrayList<Object>();for(String operation:operations.isEmpty()?List.of(""):operations){var check=m("provider",provider,"consumer",consumer,"cloud",consumerCloud,"targetType","SERVICE_DEF","target",definition);if(!operation.isEmpty())check.put("scope",operation);checks.add(check);}
  try{var entries=list(obj(consume("authorizationManagement","check-policies","ConsumerAuthorization","",m("list",checks))).get("entries"));return entries.size()==checks.size()&&entries.stream().allMatch(e->Boolean.TRUE.equals(obj(e).get("granted")));}catch(Exception e){return false;}
 }
 Map<String,Object>incoming(String peer,Map<String,Object>message)throws Exception{
  String[]p=peer.split("\\.");String foreign=p[1]+"|"+p[2],consumer=str(message.get("consumer"));String type=str(message.get("type"));
  if(type.equals("gsd-request")){
   var requirement=obj(message.get("serviceRequirement"));String definition=str(requirement.get("serviceDefinition"));var result=new ArrayList<Object>();
   for(Object raw:instances(definition)){var instance=obj(raw);var props=properties(instance);String provider=str(obj(instance.get("provider")).get("name"));if(!props.isEmpty()&&authorized(provider,consumer,foreign,definition,strings(requirement.get("operations"))))result.add(m("providerName",provider,"serviceDefinition",definition,"version",instance.get("version"),"metadata",instance.getOrDefault("metadata",m()),"operations",new TreeSet<>(obj(props.get("operations")).keySet())));}
   return m("cloud",cloud+"|"+organization,"results",result);
  }
  if(!type.equals("icn-request"))throw new ApiError(400,"Unknown relay request");
  String definition=str(message.get("serviceDefinition")),provider=str(message.get("providerName"));List<String>requested=strings(message.get("operations"));Map<String,Object>instance=m(),props=m();
  for(Object raw:instances(definition)){var candidate=obj(raw);if(str(obj(candidate.get("provider")).get("name")).equals(provider)){var offered=properties(candidate);if(!offered.isEmpty()){instance=candidate;props=offered;break;}}}
  if(props.isEmpty())return m("accepted",false,"reason","Provider not found");
  var offered=obj(props.get("operations"));var negotiated=new LinkedHashMap<String,Object>();for(String operation:requested.isEmpty()?new TreeSet<>(offered.keySet()):requested){if(!offered.containsKey(operation))return m("accepted",false,"reason","Operation not offered");negotiated.put(operation,offered.get(operation));}
  if(negotiated.isEmpty()||!authorized(provider,consumer,foreign,definition,new ArrayList<>(negotiated.keySet())))return m("accepted",false,"reason","Not authorized by provider cloud");
  List<String>addresses=strings(props.get("accessAddresses"));if(addresses.isEmpty())return m("accepted",false,"reason","Provider address missing");
  var session=obj(consume("gateTunnelConnection","open-provider-session",null,"",m("consumer",consumer,"consumerCloud",foreign,"provider",provider,"serviceDefinition",definition,"peer","GateTunnel."+p[1]+"."+p[2]+".arrowhead.eu","target",m("address",addresses.getFirst(),"port",props.get("accessPort"),"basePath",props.get("basePath"),"operations",negotiated))));
  var accepted=new LinkedHashMap<String,Object>(m("accepted",true,"sessionId",session.get("sessionId"),"providerName",provider,"serviceDefinition",definition,"version",instance.get("version"),"metadata",instance.getOrDefault("metadata",m()),"basePath",props.get("basePath"),"operations",negotiated));
   // AH-GT-1 (J5): the provider GateTunnel's signed ephemeral key travels to the consumer GateTunnel
   if(session.get("keyExchange")!=null)accepted.put("keyExchange",session.get("keyExchange"));return accepted;
 }
 String target(Object value){String name=value instanceof String s?s.split("\\|",2)[0]:str(obj(value).get("name"));String requested=value instanceof String s?(s.contains("|")?s.split("\\|",2)[1]:""):str(obj(value).get("operator"));if(!requested.isEmpty()&&!requested.equals(PeerNames.organization(this,name)))throw new ApiError(400,"Target organisation does not match configured peer");return name;}
 Map<String,Object>gsd(Map<String,Object>payload,String consumer)throws Exception{
  var requirement=obj(payload.getOrDefault("serviceRequirement",payload.get("requestedService")));if(str(requirement.get("serviceDefinition")).isEmpty())throw new IllegalArgumentException("serviceRequirement.serviceDefinition is required");
  List<Object>targets=list(payload.get("targetClouds"));if(targets.isEmpty()){targets=new ArrayList<>();for(var c:clouds())if(str(c.get("operator")).equals(PeerNames.organization(this,str(c.get("name"))))&&!str(c.get("name")).equals(cloud))targets.add(c.get("name"));}
  var results=new ArrayList<Object>();int failures=0;
  for(Object t:targets){try{var reply=relay.request(target(t),m("type","gsd-request","consumer",consumer,"serviceRequirement",requirement));if(!str(reply.get("cloud")).equals(target(t)+"|"+PeerNames.organization(this,target(t))))throw new ApiError(502,"Peer cloud identity mismatch");for(Object raw:list(reply.get("results"))){var item=new LinkedHashMap<>(obj(raw));item.put("cloud",reply.get("cloud"));results.add(item);}}catch(Exception e){failures++;}}
  return m("results",results,"unsuccessfulRequests",failures,"queriedClouds",targets.size());
 }
 Map<String,Object>icn(Map<String,Object>payload,String consumer)throws Exception{
  Object target=payload.getOrDefault("targetCloud",payload.get("providerCloud"));String foreign=target(target);String provider=str(payload.get("providerName")),definition=str(payload.get("serviceDefinition"));if(foreign.isEmpty()||provider.isEmpty()||definition.isEmpty())throw new IllegalArgumentException("targetCloud, providerName and serviceDefinition are required");
  var reply=relay.request(foreign,m("type","icn-request","consumer",consumer,"providerName",provider,"serviceDefinition",definition,"operations",payload.getOrDefault("operations",List.of())));
  if(!Boolean.TRUE.equals(reply.get("accepted")))return m("negotiated",false,"targetCloud",target,"reason",reply.get("reason"));boolean e2e=env("FEDERATION_PROFILE","legacy-mtls").equals("ah-gt-1");if(!e2e&&reply.get("keyExchange")!=null)throw new ApiError(502,"Peer requires AH-GT-1; legacy transport will not downgrade it");if(e2e&&reply.get("keyExchange")==null)throw new ApiError(502,"Peer does not offer AH-GT-1; it is not downgraded to legacy-mTLS");
  String providerCloud=foreign+"|"+PeerNames.organization(this,foreign);var tunnel=obj(consume("gateTunnelConnection","open-consumer-session",null,"",m("sessionId",reply.get("sessionId"),"consumer",consumer,"providerCloud",providerCloud,"provider",reply.get("providerName"),"serviceDefinition",reply.get("serviceDefinition"),"peer","GateTunnel."+foreign+"."+PeerNames.organization(this,foreign)+".arrowhead.eu","keyExchange",reply.get("keyExchange"))));
  return m("negotiated",true,"providerCloud",providerCloud,"providerName",reply.get("providerName"),"serviceDefinition",reply.get("serviceDefinition"),"version",reply.get("version"),"metadata",reply.get("metadata"),"sessionId",reply.get("sessionId"),"interface",m("templateName","generic_https","protocol","https","policy","CERT_AUTH","properties",m("accessAddresses",List.of(tunnel.get("address")),"accessPort",tunnel.get("port"),"basePath",reply.get("basePath"),"operations",reply.get("operations"))));
 }
 @Override void route(HttpExchange x)throws Exception{
  String path=path(x),method=x.getRequestMethod();boolean management=path.contains("/mgmt/");String actor=management?allowed(x,"LocalCloudManager","Sysop"):allowed(x,"DynamicServiceOrchestration","SimpleStoreServiceOrchestration","LocalCloudManager","Gatekeeper","Sysop");
  // relay link and inter-cloud status for the console, over mTLS (J11: the plain-HTTP health port is loopback-only)
  if(method.equals("GET")&&path.equals("/gatekeeper/mgmt/status")){send(x,200,health());return;}
  if(method.equals("GET")&&(path.equals("/gatekeeper/echo")||path.equals("/gatekeeper/monitor/echo"))){send(x,200,m("status","ok","system",name,"version","5.2.2"));return;}
  if(management&&method.equals("GET")&&(path.equals("/gatekeeper/mgmt/clouds")||path.equals("/gatekeeper/mgmt/relays"))){var rows=path.endsWith("clouds")?clouds():relays();send(x,200,m("entries",rows,"count",rows.size()));return;}
  if(management&&method.equals("POST")&&(path.equals("/gatekeeper/mgmt/clouds")||path.equals("/gatekeeper/mgmt/relays"))){Object raw=body(x);var values=raw instanceof List<?>?list(raw):list(obj(raw).getOrDefault("list",List.of(raw)));boolean cloudList=path.endsWith("clouds");
   db.transaction(c->{for(Object value:values){var item=obj(value);String n=str(item.get("name"));if(n.isBlank())throw new IllegalArgumentException("Name required");double time=System.currentTimeMillis()/1000.0;
    if(cloudList){String operator=str(item.get("operator"));if(operator.isEmpty())throw new IllegalArgumentException("Operator required");Database.update(c,"INSERT INTO clouds VALUES(?,?,?,?,?,?,?) ON CONFLICT(name,operator) DO UPDATE SET gatekeeper_address=excluded.gatekeeper_address,gatekeeper_port=excluded.gatekeeper_port,metadata=excluded.metadata,updated_at=excluded.updated_at",n,operator,item.getOrDefault("gatekeeperAddress",item.get("address")),item.getOrDefault("gatekeeperPort",item.get("port")),json(item.getOrDefault("metadata",m())),time,time);}
    else{String address=str(item.get("address"));int p=integer(item.get("port"),0);if(address.isEmpty()||p<1||p>65535)throw new IllegalArgumentException("Relay address and valid port required");Database.update(c,"INSERT INTO relays VALUES(?,?,?,?,?,?,?) ON CONFLICT(name) DO UPDATE SET address=excluded.address,port=excluded.port,type=excluded.type,secure=excluded.secure,metadata=excluded.metadata",n,address,p,item.getOrDefault("type","MQTT"),Boolean.FALSE.equals(item.get("secure"))?0:1,json(item.getOrDefault("metadata",m())),time);}}
    return null;});var rows=cloudList?clouds():relays();send(x,201,m("entries",rows,"count",rows.size()));return;
  }
  if(management&&method.equals("DELETE")&&path.startsWith("/gatekeeper/mgmt/clouds/")){String[]p=path.substring("/gatekeeper/mgmt/clouds/".length()).split("/",-1);if(p.length!=2)throw new IllegalArgumentException("operator/name required");boolean removed=db.update("DELETE FROM clouds WHERE operator=? AND name=?",p[0],p[1])>0;send(x,removed?200:404,m("removed",removed));return;}
  if(method.equals("POST")&&Set.of("/gatekeeper/init_gsd","/gatekeeper/init_multi_gsd","/gatekeeper/init_icn").contains(path)){var payload=obj(body(x));String consumer=str(payload.getOrDefault("consumerSystem",actor));if(!consumer.equals(actor)&&!Set.of("DynamicServiceOrchestration","SimpleStoreServiceOrchestration","LocalCloudManager","Sysop").contains(actor))throw new ApiError(403,"Caller cannot act for another consumer");send(x,200,path.endsWith("init_icn")?icn(payload,consumer):gsd(payload,consumer));return;}
  throw new ApiError(404,"Not found");
 }
 @Override void register()throws Exception{registerSystem(m("role","inter-cloud-gatekeeper","sourceCompatibility","4.6.2"));var ids=new ArrayList<String>();ids.add(registerService("globalServiceDiscovery","5.2.2","/gatekeeper",m("query",op("POST","/init_gsd"),"multi-query",op("POST","/init_multi_gsd")),m("generation","5","v4Compatibility",true)));ids.add(registerService("interCloudNegotiation","5.2.2","/gatekeeper",m("negotiate",op("POST","/init_icn")),m("generation","5","v4Compatibility",true)));ids.add(registerService("gatekeeperManagement","5.2.2","/gatekeeper",m("clouds",op("GET","/mgmt/clouds"),"relays",op("GET","/mgmt/relays"),"status",op("GET","/mgmt/status")),m("generation","5","v4Compatibility",true)));registerMonitor(name,ids);registered(ids);}
 @Override void startWorkers(){periodic(relay::connect,10);periodic(()->Messaging.nativeMonitor(this,name,monitor,m("state","running","health",relay.client.isConnected()?"healthy":"degraded")),30);}
 @Override Map<String,Object>health(){var h=super.health();h.put("relay",relay.status());return h;}
 @Override public void close(){relay.close();monitor.close();super.close();}
}
