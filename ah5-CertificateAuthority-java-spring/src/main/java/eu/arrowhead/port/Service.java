package eu.arrowhead.port;

import com.sun.net.httpserver.*;
import javax.net.ssl.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static eu.arrowhead.port.Json.*;

abstract class Service implements HttpHandler,AutoCloseable {
 final String role,name,host,cloud=env("LOCAL_CLOUD","TestCloud"),organization=env("ORGANIZATION","Company");
 // Only the ServiceRegistry is configured. Authentication is looked up there (an identity token is needed before anything can be
 // orchestrated; AUTHENTICATION_URL only overrides it in tests); every other service is consumed through an orchestration request.
 final String registry=env("SERVICE_REGISTRY_URL","https://serviceregistry:8443");private volatile String auth=env("AUTHENTICATION_URL",null);private final boolean authConfigured=auth!=null;
 final long orchestrationTtl=Math.max(envInt("ORCHESTRATION_TTL_SECONDS",300),0)*1000L;final Map<String,Endpoint>orchestrated=new ConcurrentHashMap<>();private volatile Endpoint orchestrator;private volatile Map<String,Object>identity;
 record Endpoint(String url,String method,long expires){}
 final String certFile,keyFile,caFile=env("TLS_CA_CERT","/certs/ca.crt"),password;
 final int port,healthPort;final SSLContext tls;final HttpClient client,authClient;final Database db;
 final ScheduledExecutorService scheduler=Executors.newScheduledThreadPool(6);final Semaphore requests=new Semaphore(64);
 volatile Map<String,Object> registration=m("registered",false,"instances",List.of(),"error",null);volatile String lastError;
 private String ownToken;private Instant refresh=Instant.EPOCH;HttpsServer server;private HttpServer healthServer;
 Service(String role,String name,String host,int defaultPort,int defaultHealth,String defaultDB)throws Exception{
  this.role=role;this.name=env("ARROWHEAD_SYSTEM_NAME",name);this.host=host;port=envInt("PORT",defaultPort);healthPort=envInt("HEALTH_PORT",defaultHealth);
  certFile=env("TLS_SERVER_CERT",env("TLS_CERT","/certs/"+host+".crt"));keyFile=env("TLS_SERVER_KEY",env("TLS_KEY","/certs/"+host+".key"));
  password=env("AUTHENTICATION_PASSWORD",env("AUTHENTICATOR_PASSWORD",""));if(password.isBlank())throw new IllegalArgumentException("Service authentication password is required");
  var identity=Tls.cert(java.nio.file.Path.of(certFile));if(!cn(identity).equals(commonName(this.name))||!Json.name(identity,"DNQ").equals("sy"))throw new IllegalArgumentException("Configured certificate does not match the service identity/profile");
  tls=Tls.context(certFile,keyFile,caFile,null);client=httpClient(tls);authClient=httpClient(Tls.context(certFile,keyFile,caFile,commonName("Authentication")));
  if(!registry.startsWith("https://")||authConfigured&&!auth.startsWith("https://"))throw new IllegalArgumentException("HTTPS service endpoints are required");
  db=new Database(env("DB_PATH",defaultDB));
 }
 static HttpClient httpClient(SSLContext tls){return HttpClient.newBuilder().sslContext(tls).connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build();}
 String commonName(String system){return system+"."+cloud+"."+organization+".arrowhead.eu";}
 synchronized String token(boolean renew)throws Exception{
  if(ownToken!=null&&!renew&&Instant.now().isBefore(refresh))return ownToken;
  Object out;try{out=raw(authentication("identity-login","/authentication/identity/login"),"POST",m("systemName",name,"credentials",m("password",password)),null);}catch(java.io.IOException e){forgetAuthentication();throw e;}
  ownToken=str(obj(out).get("token"));if(ownToken.isEmpty())throw new IllegalStateException("Authentication returned no identity token");refresh=Instant.now().plusSeconds(240);return ownToken;
 }
 Object call(String url,String method,Object payload)throws Exception{
  for(int attempt=0;attempt<2;attempt++){String identity=token(false);try{return raw(url,method,payload,identity);}catch(ApiError e){if(e.status!=401||attempt==1)throw e;invalidate(identity);}}
  throw new IllegalStateException("Unreachable identity retry");
 }
 synchronized void invalidate(String used){if(Objects.equals(ownToken,used)){ownToken=null;refresh=Instant.EPOCH;}}
 Object raw(String url,String method,Object payload,String token)throws Exception{
  var uri=URI.create(url);if(!uri.getScheme().equals("https")||uri.getUserInfo()!=null)throw new IllegalArgumentException("Refusing insecure endpoint");
  var builder=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15)).header("Content-Type","application/json");if(token!=null)builder.header("Authorization","Bearer IDENTITY-TOKEN//"+token);
  builder.method(method,payload==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofByteArray(bytes(payload)));
  String authBase=auth;var response=(authBase!=null&&url.startsWith(authBase+"/")?authClient:client).send(builder.build(),HttpResponse.BodyHandlers.ofInputStream());
  try(var body=response.body()){byte[]b=body.readNBytes(2*1024*1024+1);if(b.length>2*1024*1024)throw new ApiError(502,"Service response is too large");if(response.statusCode()/100!=2)throw new ApiError(response.statusCode(),"Arrowhead service returned HTTP "+response.statusCode());return b.length==0?null:parse(b);}
 }
 String caller(HttpExchange exchange)throws Exception{
  if(!(exchange instanceof HttpsExchange x))throw new ApiError(401,"Mutual TLS is required");
  var leaf=(X509Certificate)x.getSSLSession().getPeerCertificates()[0];String profile=Json.name(leaf,"DNQ");if(!Set.of("sy","op").contains(profile))throw new ApiError(403,"Only system/operator certificates may call services");
  String header=x.getRequestHeaders().getFirst("Authorization"),prefix="Bearer IDENTITY-TOKEN//";
  if(header==null||!header.startsWith(prefix))throw new ApiError(401,"Arrowhead identity token is required");String t=header.substring(prefix.length());if(t.isEmpty()||t.length()>4096||!t.matches("[A-Za-z0-9._~+-]+"))throw new ApiError(401,"Invalid identity token syntax");
  var verified=obj(call(withPathParam(authentication("identity-verify","/authentication/identity/verify/{token}"),t),"GET",null));String system=str(verified.get("systemName"));if(!Boolean.TRUE.equals(verified.get("verified"))||!cn(leaf).equals(commonName(system)))throw new ApiError(401,"Identity token does not match the client certificate");return system;
 }
 String allowed(HttpExchange x,String...names)throws Exception{String caller=caller(x);if(!Set.of(names).contains(caller))throw new ApiError(403,"Caller is not authorized for this operation");return caller;}
 // Authentication's identity service, looked up in the ServiceRegistry without a token (registered with unrestrictedDiscovery)
 String authentication(String operation,String configuredPath)throws Exception{
  if(authConfigured)return auth+configuredPath;var props=identity;
  if(props==null){for(Object e:list(obj(raw(registry+"/serviceregistry/service-discovery/lookup","POST",m("serviceDefinitionNames",List.of("identity"),"providerNames",List.of("Authentication"),"interfaceTemplateNames",List.of("generic_https")),null)).get("entries"))){props=https(obj(e));if(props!=null)break;}
   if(props==null)throw new ApiError(503,"Authentication is not registered in the ServiceRegistry");identity=props;auth=base(props);}
  return endpoint(props,operation,0).url();
 }
 void forgetAuthentication(){if(!authConfigured){identity=null;auth=null;}}
 // The orchestrator (DynamicServiceOrchestration) is the one service besides Authentication that is looked up in the ServiceRegistry
 Endpoint orchestrator()throws Exception{
  var known=orchestrator;if(known!=null)return known;
  for(Object e:list(obj(call(registry+"/serviceregistry/service-discovery/lookup","POST",m("serviceDefinitionNames",List.of("serviceOrchestration"),"providerNames",List.of(env("ORCHESTRATOR_SYSTEM","DynamicServiceOrchestration")),"interfaceTemplateNames",List.of("generic_https")))).get("entries"))){var props=https(obj(e));if(props!=null)return orchestrator=endpoint(props,"pull",0);}
  throw new ApiError(503,"No orchestrator is registered in the ServiceRegistry");
 }
 // An orchestration request on behalf of this system: the orchestrator returns only providers this system is authorized
 // to consume the operation of. The result is reused for ORCHESTRATION_TTL_SECONDS.
 Endpoint orchestrate(String definition,String operation,String provider)throws Exception{
  String key=definition+"|"+operation+"|"+Objects.toString(provider,"");var known=orchestrated.get(key);if(known!=null&&known.expires()>System.currentTimeMillis())return known;
  // no securityPolicies filter: providers may still register NONE (an override or declared identities); the core systems check mTLS and tokens themselves
  var requirement=m("serviceDefinition",definition,"operations",List.of(operation),"interfaceTemplateNames",List.of("generic_https"));var flags=m("MATCHMAKING",true,"ALLOW_INTERCLOUD",false,"ALLOW_TRANSLATION",false);
  if(provider!=null){requirement.put("preferredProviders",List.of(provider));flags.put("ONLY_PREFERRED",true);}
  var pull=orchestrator();Object response;try{response=call(pull.url(),pull.method(),m("serviceRequirement",requirement,"orchestrationFlags",flags));}catch(java.io.IOException e){orchestrator=null;throw e;}
  for(Object r:list(obj(response).get("results"))){var props=https(obj(r));if(props!=null&&obj(props.get("operations")).containsKey(operation)){var found=endpoint(props,operation,System.currentTimeMillis()+orchestrationTtl);orchestrated.put(key,found);return found;}}
  throw new ApiError(503,"Orchestration found no authorized provider of "+definition+" ("+operation+")");
 }
 // consumes an operation of an orchestrated service; suffix carries path parameters
 Object consume(String definition,String operation,String provider,String suffix,Object payload)throws Exception{
  var target=orchestrate(definition,operation,provider);
  try{return call(target.url()+suffix,target.method(),payload);}catch(java.io.IOException|ApiError e){if(!(e instanceof ApiError a)||a.status>=500)orchestrated.values().remove(target);throw e;}
 }
 static Map<String,Object>https(Map<String,Object>service){for(Object raw:list(service.get("interfaces"))){var i=obj(raw);if(str(i.get("templateName")).equals("generic_https"))return obj(i.get("properties"));}return null;}
 static String base(Map<String,Object>props){var addresses=strings(props.get("accessAddresses"));if(addresses.isEmpty())throw new ApiError(502,"Service has no access address");return "https://"+addresses.getFirst()+":"+props.get("accessPort");}
 static Endpoint endpoint(Map<String,Object>props,String operation,long expires){var op=obj(obj(props.get("operations")).get(operation));if(op.isEmpty())throw new ApiError(502,"Service does not offer "+operation);return new Endpoint(base(props)+str(props.get("basePath"))+str(op.get("path")),str(op.get("method")).isEmpty()?"POST":str(op.get("method")),expires);}
 // a registered path is a template ("/verify/{token}"); an older provider registers it without the placeholder
 static String withPathParam(String url,String value){String v=java.net.URLEncoder.encode(value,StandardCharsets.UTF_8);return url.matches(".*\\{[^}/]+\\}.*")?url.replaceFirst("\\{[^}/]+\\}",java.util.regex.Matcher.quoteReplacement(v)):url+"/"+v;}
 static String path(HttpExchange x){String p=x.getRequestURI().getPath();return p.length()>1?p.replaceAll("/+$",""):p;}
 static Map<String,List<String>> query(HttpExchange x){var out=new HashMap<String,List<String>>();String q=x.getRequestURI().getRawQuery();if(q!=null)for(String part:q.split("&")){String[]kv=part.split("=",2);out.computeIfAbsent(decode(kv[0]),k->new ArrayList<>()).add(kv.length==1?"":decode(kv[1]));}return out;}
 static String decode(String s){return URLDecoder.decode(s,StandardCharsets.UTF_8);}
 static Object body(HttpExchange x)throws Exception{byte[]b=x.getRequestBody().readNBytes(1024*1024+1);if(b.length>1024*1024)throw new ApiError(413,"Request exceeds 1 MiB");return b.length==0?m():parse(b);}
 static void send(HttpExchange x,int status,Object value)throws Exception{sendBytes(x,status,bytes(value),"application/json; charset=utf-8");}
 static void sendBytes(HttpExchange x,int status,byte[]value,String type)throws Exception{x.getResponseHeaders().set("Content-Type",type);x.getResponseHeaders().set("Cache-Control","no-store");x.getResponseHeaders().set("X-Content-Type-Options","nosniff");x.sendResponseHeaders(status,status==204?-1:value.length);if(status!=204)x.getResponseBody().write(value);}
 @Override public final void handle(HttpExchange x){boolean acquired=requests.tryAcquire();try{if(!acquired){send(x,503,m("error","Too many requests"));return;}if(path(x).equals("/health")||path(x).equals("/certificate-authority/health")){send(x,healthCode(),liveness());return;}route(x);}catch(ApiError e){try{send(x,e.status,m("error",e.getMessage()));}catch(Exception ignored){}}catch(IllegalArgumentException e){try{send(x,400,m("error",e.getMessage()));}catch(Exception ignored){}}catch(Exception e){lastError=e.getClass().getSimpleName();System.err.println(name+" request failed: "+lastError);try{send(x,500,m("error","Internal service failure"));}catch(Exception ignored){}}finally{if(acquired)requests.release();x.close();}}
 void start()throws Exception{
  server=HttpsServer.create(new InetSocketAddress("0.0.0.0",port),64);server.setHttpsConfigurator(new HttpsConfigurator(tls){@Override public void configure(HttpsParameters p){var parameters=tls.getDefaultSSLParameters();parameters.setProtocols(new String[]{"TLSv1.3","TLSv1.2"});parameters.setNeedClientAuth(true);p.setSSLParameters(parameters);}});server.createContext("/",this);server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());server.start();
  if(healthPort>0){// plain-HTTP health only for the container's own healthcheck (J11); the same document is /<system>/mgmt/status over mTLS
   healthServer=HttpServer.create(new InetSocketAddress(env("HEALTH_BIND","127.0.0.1"),healthPort),16);healthServer.createContext("/",x->{try{if(!path(x).equals("/health")||!x.getRequestMethod().equals("GET")){send(x,426,m("error","Mutual TLS required"));return;}send(x,healthCode(),health());}catch(Exception ignored){}finally{x.close();}});healthServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());healthServer.start();}
  periodic(()->{if(!Boolean.TRUE.equals(registration.get("registered")))register();},5);
  startWorkers();System.out.println(name+" Java 5.2.2 mTLS listening on "+port);
 }
 interface Checked{void run()throws Exception;}
 void periodic(Checked job,int seconds){scheduler.scheduleWithFixedDelay(()->{try{job.run();}catch(Exception e){lastError=e.getClass().getSimpleName();System.err.println(name+" background task failed: "+lastError);}},0,Math.max(seconds,1),TimeUnit.SECONDS);}
 // Token-free liveness on the mTLS port (J18): status only. The detail of health() is for the container-local plain
 // health listener (J11) and for callers authorized for /<system>/mgmt/status.
 Map<String,Object> liveness(){return m("status",healthCode()==200?"ok":"degraded","system",name);}
 Map<String,Object> health(){return m("status","ok","system",name,"version","5.2.2","implementation","java","x509",true,"registration",registration);}
 int healthCode(){return 200;}
 Object registerPost(String path,Object payload)throws Exception{return call(registry+path,"POST",payload);}
 void registerSystem(Map<String,Object> metadata)throws Exception{registerPost("/serviceregistry/system-discovery/register",m("version","5.2.2","addresses",List.of(host),"metadata",metadata,"deviceName",null));}
 String registerService(String def,String version,String base,Map<String,Object>ops,Object metadata)throws Exception{return str(obj(registerPost("/serviceregistry/service-discovery/register",m("serviceDefinitionName",def,"version",version,"expiresAt",null,"metadata",metadata,"interfaces",List.of(m("templateName","generic_https","protocol","https","policy","CERT_AUTH","properties",m("accessAddresses",List.of(host),"accessPort",port,"basePath",base,"operations",ops)))))).get("instanceId"));}
 static Map<String,Object> op(String method,String path){return m("method",method,"path",path);}
 void registerMonitor(String instance,List<String>ids)throws Exception{ids.add(str(obj(registerPost("/serviceregistry/service-discovery/register",m("serviceDefinitionName","sysMonitor","version","1.0.0","expiresAt",null,"metadata",m("contract","SysMonitor-AMQP-IDD-5.2.2","schemaVersion","1.0","publisherMode","native","observationTypes",List.of("echo","power","status"),"delivery","persistent, publisher-confirmed"),"interfaces",List.of(m("templateName","generic_amqps","protocol","amqps","policy","CERT_AUTH","properties",m("accessAddresses",List.of(env("AMQP_HOST","amqp-broker")),"accessPort",envInt("AMQP_PORT",5671),"exchange","arrowhead","baseRoutingKey",cloud+".sysmonitor."+name+"."+instance,"operations",List.of("echo","power","status"))))))).get("instanceId")));}
 void registered(List<String>ids){registration=m("registered",true,"instances",ids,"error",null);}
 abstract void route(HttpExchange x)throws Exception;abstract void register()throws Exception;void startWorkers()throws Exception{}
 @Override public void close(){scheduler.shutdownNow();if(server!=null)server.stop(1);if(healthServer!=null)healthServer.stop(1);client.close();authClient.close();}
 static final class ApiError extends RuntimeException{final int status;ApiError(int status,String message){super(message);this.status=status;}}
}
