package eu.arrowhead.localcloudconfigurator;
import com.sun.net.httpserver.*;
import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.JsonNode;
public final class ApiServer{
  public interface Backend{
    List<String>status()throws Exception;
    String submit(Planner.Plan p)throws Exception;
    Object deployment(String id)throws Exception;
  }
  public record Point(double x,double y){
  }
  public record Draft(String name,List<String>systems,List<String>remove,boolean reapply,boolean smoke,Map<String,Point>layout,Long expectedRevision){
  }
  public record Configuration(String id,String name,List<String>systems,List<String>remove,boolean reapply,boolean smoke,Map<String,Point>layout,long revision,String updatedAt){
  }
  public record Apply(List<String>systems,List<String>remove,boolean reapply,boolean smoke,String expectedPlanHash){
  }
  public static final class Refused extends RuntimeException{
    final int status;
    Refused(int status,String msg){
      super(msg);
      this.status=status;
    }
  }
  private final Catalog catalog;
  private final Backend backend;
  private final byte[]token;
  private final Path state;
  private final Object lock=new Object();
  private volatile List<String>cached;
  private volatile long cachedAt;
  private String activeDeployment;
  public ApiServer(Catalog c,Backend b,String token,Path state)throws IOException{
    if(token==null||token.length()<24)throw new IllegalArgumentException("Operator token required");
    catalog=c;
    backend=b;
    this.token=token.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    this.state=state;
    Files.createDirectories(state.resolve("configurations"));
    Files.createDirectories(state.resolve("deployments"));
    Path active=state.resolve("active-deployment.json");
    if(Files.exists(active))activeDeployment=Json.LENIENT.readTree(Files.readString(active)).path("id").asText(null);
  }
  public HttpServer start(InetSocketAddress addr)throws IOException{
    HttpServer s=HttpServer.create(addr,50);
    s.createContext("/",this::handle);
    s.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    s.start();
    return s;
  }
  private void handle(HttpExchange x)throws IOException{
    x.getResponseHeaders().set("X-Content-Type-Options","nosniff");
    x.getResponseHeaders().set("Cache-Control","no-store");
    x.getResponseHeaders().set("Content-Security-Policy","default-src 'none'; frame-ancestors 'none'");
    try{
      if(x.getRequestURI().getPath().equals("/healthz")){
        send(x,200,Map.of("status","ok"));
        return;
      }
      String auth=x.getRequestHeaders().getFirst("Authorization");
      if(auth==null||!auth.startsWith("Bearer ")||!MessageDigest.isEqual(auth.substring(7).getBytes(java.nio.charset.StandardCharsets.UTF_8),token))throw new Refused(401,"Operator token required");
      dispatch(x);
    }
    catch(Refused e){
      send(x,e.status,Map.of("error",e.getMessage()));
    }
    catch(IllegalArgumentException|com.fasterxml.jackson.core.JsonProcessingException e){
      send(x,400,Map.of("error",String.valueOf(e.getMessage())));
    }
    catch(Exception e){
      send(x,502,Map.of("error",Runner.redact(String.valueOf(e.getMessage()))));
    }
    finally{
      x.close();
    }
  }
  private static void send(HttpExchange x,int code,Object v)throws IOException{
    byte[]body=Json.write(v).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    x.getResponseHeaders().set("Content-Type","application/json");
    x.sendResponseHeaders(code,body.length);
    x.getResponseBody().write(body);
  }
  private <T>T read(HttpExchange x,Class<T>type)throws IOException{
    byte[]b=x.getRequestBody().readNBytes(65537);
    if(b.length>65536)throw new Refused(413,"Request exceeds 64 KiB");
    return Json.MAPPER.readValue(b,type);
  }
  private synchronized List<String>running(boolean fresh)throws Exception{
    long now=System.nanoTime();
    if(fresh||cached==null||now-cachedAt>2_000_000_000L){
      cached=List.copyOf(backend.status());
      cachedAt=System.nanoTime();
    }
    return cached;
  }
  private Planner.Request request(Draft d){
    return new Planner.Request(d.systems(),d.remove(),d.reapply(),d.smoke());
  }
  private Planner.Plan plan(Planner.Request r,boolean fresh)throws Exception{
    return Planner.build(catalog,r,running(fresh));
  }
  private void dispatch(HttpExchange x)throws Exception{
    String path=x.getRequestURI().getPath(),method=x.getRequestMethod();
    if(method.equals("GET")&&path.equals("/api/catalog")){
      send(x,200,catalog);
      return;
    }
    if(method.equals("GET")&&path.equals("/api/status")){
      List<String>run=running(false);
      List<Map<String,Object>>systems=new ArrayList<>();
      for(var c:catalog.systems()){
        long n=c.services().stream().filter(run::contains).count();
        systems.add(Map.of("id",c.id(),"state",n==0?"stopped":n==c.services().size()?"running":"partial"));
      }
      send(x,200,Map.of("cloud",catalog.cloud(),"running",run,"systems",systems));
      return;
    }
    if(method.equals("POST")&&path.equals("/api/plan")){
      var r=read(x,Planner.Request.class);
      var p=plan(r,true);
      send(x,200,Map.of("plan",p,"hash",p.hash()));
      return;
    }
    if(method.equals("POST")&&path.equals("/api/deployments")){
      Apply r=read(x,Apply.class);
      synchronized(lock){
        if(activeDeployment!=null){
          String phase=Json.LENIENT.valueToTree(backend.deployment(activeDeployment)).path("state").asText();
          if(!Set.of("success","failed").contains(phase))throw new Refused(409,"A deployment is still running. Wait for it before applying another plan.");
          activeDeployment=null;
          Files.deleteIfExists(state.resolve("active-deployment.json"));
        }
        var p=plan(new Planner.Request(r.systems(),r.remove(),r.reapply(),r.smoke()),true);
        if(r.expectedPlanHash()==null||!p.hash().equals(r.expectedPlanHash()))throw new Refused(409,"Plan or cloud state changed. Review a fresh plan before applying.");
        if(p.steps().isEmpty()){
          send(x,200,Map.of("id","","plan",p,"state","success"));
          return;
        }
        String id=backend.submit(p);
        if(!id.matches("[A-Za-z0-9-]{1,128}"))throw new IllegalStateException("Unexpected workflow ID");
        activeDeployment=id;
        write(state.resolve("active-deployment.json"),Map.of("id",id));
        write(state.resolve("deployments").resolve(id+".json"),Map.of("id",id,"plan",p,"submittedAt",Instant.now().toString()));
        send(x,202,Map.of("id",id,"plan",p,"state","submitted"));
      }
      return;
    }
    if(method.equals("GET")&&path.matches("/api/deployments/[A-Za-z0-9-]{1,128}")){
      String id=path.substring(path.lastIndexOf('/')+1);
      if(!Files.exists(state.resolve("deployments").resolve(id+".json")))throw new Refused(404,"Deployment not found");
      send(x,200,backend.deployment(id));
      return;
    }
    if(path.equals("/api/configurations")&&method.equals("GET")){
      List<Configuration>rows=new ArrayList<>();
      try(var files=Files.list(state.resolve("configurations"))){
        for(Path p:files.filter(p->p.toString().endsWith(".json")).limit(1000).toList())rows.add(Json.MAPPER.readValue(Files.readString(p),Configuration.class));
      }
      rows.sort(Comparator.comparing(Configuration::name));
      send(x,200,Map.of("entries",rows));
      return;
    }
    if(path.equals("/api/configurations")&&method.equals("POST")){
      Draft draft=read(x,Draft.class);
      synchronized(lock){
        var c=configuration(UUID.randomUUID().toString(),draft,1);
        write(configPath(c.id()),c);
        send(x,201,c);
      }
      return;
    }
    if(path.matches("/api/configurations/[a-f0-9-]{36}")){
      String id=path.substring(path.lastIndexOf('/')+1);
      synchronized(lock){
        Path p=configPath(id);
        if(!Files.exists(p))throw new Refused(404,"Configuration not found");
        var old=Json.MAPPER.readValue(Files.readString(p),Configuration.class);
        if(method.equals("GET")){
          send(x,200,old);
          return;
        }
        if(method.equals("PUT")){
          Draft d=read(x,Draft.class);
          if(d.expectedRevision()==null||d.expectedRevision()!=old.revision())throw new Refused(409,"Configuration changed; reload before saving");
          var next=configuration(id,d,old.revision()+1);
          write(p,next);
          send(x,200,next);
          return;
        }
        if(method.equals("DELETE")){
          JsonNode d=read(x,JsonNode.class);
          if(d.path("expectedRevision").asLong(-1)!=old.revision())throw new Refused(409,"Configuration changed; reload before deleting");
          Files.delete(p);
          send(x,200,Map.of("removed",id));
          return;
        }
      }
    }
    throw new Refused(404,"Unknown configurator operation");
  }
  private Configuration configuration(String id,Draft d,long revision){
    if(d.name()==null||d.name().isBlank()||d.name().length()>200)throw new IllegalArgumentException("Configuration name required (1–200 characters)");
    Planner.Request req=request(d);
    Planner.build(catalog,req,List.of());
    List<String>selected=new ArrayList<>(new LinkedHashSet<>(catalog.defaults()));
    for(String raw:req.systems()){String s=catalog.hostId(raw);if(!selected.contains(s))selected.add(s);}
    Map<String,Point>layout=new LinkedHashMap<>();
    if(d.layout()!=null){
      if(d.layout().size()>200)throw new IllegalArgumentException("Too many layout nodes");
      for(var e:d.layout().entrySet()){
        if(!catalog.hostId(e.getKey()).equals(e.getKey()))continue;
        catalog.entry(e.getKey());
        var p=e.getValue();
        if(p==null||!Double.isFinite(p.x())||!Double.isFinite(p.y())||p.x()<0||p.y()<0||p.x()>10000||p.y()>10000)throw new IllegalArgumentException("Invalid graphical position");
        layout.put(e.getKey(),p);
      }
    }
    return new Configuration(id,d.name().trim(),selected,req.remove(),req.reapply(),req.smoke(),layout,revision,Instant.now().toString());
  }
  private Path configPath(String id){
    return state.resolve("configurations").resolve(id+".json");
  }
  private void write(Path p,Object v)throws IOException{
    Path tmp=Files.createTempFile(p.getParent(),"lcc-",".tmp");
    try{
      Files.writeString(tmp,Json.write(v));
      try{
        Files.move(tmp,p,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
      }
      catch(AtomicMoveNotSupportedException e){
        Files.move(tmp,p,StandardCopyOption.REPLACE_EXISTING);
      }
    }
    finally{
      Files.deleteIfExists(tmp);
    }
  }
}
