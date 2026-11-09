package eu.arrowhead.localcloudconfigurator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.JsonNode;
class ConfiguratorTest{
  @TempDir Path temp;
  Catalog catalog()throws Exception{
    return Catalog.load(Path.of("catalogs/javacloud.json"));
  }
  List<String>all(Catalog c){
    return c.systems().stream().flatMap(s->s.services().stream()).toList();
  }
  @Test void defaultCoreExcludesSimpleAndIncludesNecessaryInfrastructure()throws Exception{
    var c=catalog();
    var p=Planner.build(c,new Planner.Request(List.of(),List.of(),false,false),List.of());
    assertEquals(Set.of("serviceregistry","authentication","consumerauthorization","serviceorchestration-dynamic"),Set.copyOf(c.defaults()));
    assertFalse(p.systems().contains("serviceorchestration-simple"));
    assertEquals(5,p.systems().size());
    assertTrue(p.systems().contains("amqp-broker"));
    assertFalse(p.systems().contains("serviceregistry-db"));
    assertTrue(p.steps().indexOf(new Planner.Step("up","serviceregistry-db","serviceregistry"))<p.steps().indexOf(new Planner.Step("up","serviceregistry","serviceregistry")));
  }
  @Test void runningComponentsAreNotRecreatedAndVerificationIsNonMutating()throws Exception{
    var c=catalog();
    assertTrue(Planner.build(c,new Planner.Request(List.of("timemanager"),List.of(),false,false),all(c)).steps().isEmpty());
    var p=Planner.build(c,new Planner.Request(List.of(),List.of(),false,true),all(c));
    assertEquals(List.of(new Planner.Step("smoke","verify-localcloud",null)),p.steps());
  }
  @Test void refusesCoreInfraRemovalConflictingSelectionAndRunningDependents()throws Exception{
    var c=catalog();
    for(String id:List.of("serviceregistry","amqp-broker","authentication-db","localcloudmanager"))assertThrows(IllegalArgumentException.class,()->Planner.build(c,new Planner.Request(List.of(),List.of(id),false,false),all(c)));
    assertThrows(IllegalArgumentException.class,()->Planner.build(c,new Planner.Request(List.of(),List.of("datamanager"),false,false),all(c)));
    assertThrows(IllegalArgumentException.class,()->Planner.build(c,new Planner.Request(List.of("timemanager"),List.of("timemanager"),false,false),all(c)));
    assertThrows(IllegalArgumentException.class,()->Planner.build(c,new Planner.Request(List.of("not-catalogued"),List.of(),false,false),all(c)));
  }
  @Test void runnerRefusesForgedOwnersKindsScriptsCatalogVersionsAndCoreStops()throws Exception{
    var c=catalog();
    Files.writeString(temp.resolve("compose.yaml"),"services: {}\n");
    List<List<String>>commands=new ArrayList<>();
    var runner=new Runner(c,temp,(d,args)->{
      commands.add(args);return "[]";
    }
    ,"docker");
    for(var step:List.of(new Planner.Step("up","serviceregistry","timemanager"),new Planner.Step("stop","serviceregistry","serviceregistry"),new Planner.Step("policy","../../secret","timemanager"),new Planner.Step("shell","sh","timemanager")))assertThrows(Exception.class,()->runner.step(step,c.hash()));
    assertThrows(Exception.class,()->runner.step(new Planner.Step("up","timemanager","timemanager"),"wrong"));
    assertTrue(commands.isEmpty());
  }
  @Test void runnerUsesArgumentListAndNeverBuildsLegacySources()throws Exception{
    var c=catalog();
    Files.writeString(temp.resolve("compose.yaml"),"services: {}\n");
    List<List<String>>commands=new ArrayList<>();
    var runner=new Runner(c,temp,(d,args)->{
      commands.add(args);return args.contains("ps")?"\"timemanager-db\"":"ok";
    }
    ,"docker");
    runner.step(new Planner.Step("up","timemanager","timemanager"),c.hash());
    assertTrue(commands.getLast().containsAll(List.of("--no-deps","--no-build","--pull","never")));
    assertEquals("timemanager",commands.getLast().getLast());
  }
  @Test void statusParsingAndVerification()throws Exception{
    var c=catalog();
    Files.writeString(temp.resolve("compose.yaml"),"services: {}\n");
    String rows=all(c).stream().map(s->"{\"Service\":\""+s+"\",\"State\":\"running\"}").collect(java.util.stream.Collectors.joining("\n"));
    var runner=new Runner(c,temp,(d,args)->rows,"docker");
    assertEquals(all(c).size(),runner.running().size());
    assertTrue(runner.step(new Planner.Step("smoke","verify-localcloud",null),c.hash()).contains("no services changed"));
  }
  @Test void compactStatusNamesAvoidVerboseDockerOutput()throws Exception{
    var c=catalog();
    Files.writeString(temp.resolve("compose.yaml"),"services: {}\n");
    var runner=new Runner(c,temp,(dir,args)->{
      assertEquals("{{json .Service}}",args.getLast());return "\"serviceregistry\"\n\"authentication\"\n";
    }
    ,"docker");
    assertEquals(List.of("serviceregistry","authentication"),runner.running());
  }
  @Test void redactionMasksCredentialsAndPrivateKeys(){
    String out=Runner.redact("password=abc Bearer abc123 token=secret\n-----BEGIN PRIVATE KEY-----\nabc\n-----END PRIVATE KEY-----");
    assertFalse(out.contains("abc"));
    assertFalse(out.contains("token=secret"));
  }
  @Test void apiAuthPersistentConfigurationsStaleRevisionsAndReviewedWorkflow()throws Exception{
    var c=catalog();
    List<Planner.Plan>submitted=new ArrayList<>();
    ApiServer.Backend b=new ApiServer.Backend(){
      public List<String>status(){
        return all(c);
      }
      public String submit(Planner.Plan p){
        submitted.add(p);
        return "workflow-test";
      }
      public Object deployment(String id){
        return Map.of("id",id,"state","success");
      }
    }
    ;
    String token="test-token-abcdefghijklmnopqrstuvwxyz";
    HttpServer s=new ApiServer(c,b,token,temp).start(new InetSocketAddress("127.0.0.1",0));
    try{
      String base="http://127.0.0.1:"+s.getAddress().getPort();
      assertEquals(401,request(base,"GET","/api/catalog",null,null).statusCode());
      String req="{\"systems\":[],\"remove\":[],\"smoke\":true}";
      var plan=request(base,"POST","/api/plan",req,token);
      assertEquals(200,plan.statusCode());
      String hash=Json.LENIENT.readTree(plan.body()).path("hash").asText();
      assertEquals(409,request(base,"POST","/api/deployments","{\"smoke\":true,\"expectedPlanHash\":\"stale\"}",token).statusCode());
      var deploy=request(base,"POST","/api/deployments","{\"smoke\":true,\"expectedPlanHash\":\""+hash+"\"}",token);
      assertEquals(202,deploy.statusCode());
      assertEquals(1,submitted.size());
      assertEquals(1,submitted.get(0).steps().size());
      assertEquals(200,request(base,"GET","/api/deployments/workflow-test",null,token).statusCode());
      assertEquals(404,request(base,"GET","/api/deployments/foreign",null,token).statusCode());
      var create=request(base,"POST","/api/configurations","{\"name\":\"Test design\",\"systems\":[],\"remove\":[],\"layout\":{\"serviceregistry\":{\"x\":1,\"y\":2}}}",token);
      assertEquals(201,create.statusCode(),create.body());
      var row=Json.LENIENT.readTree(create.body());
      String id=row.path("id").asText();
      assertEquals(4,row.path("systems").size());
      assertEquals(409,request(base,"PUT","/api/configurations/"+id,"{\"name\":\"stale\",\"expectedRevision\":0}",token).statusCode());
      assertEquals(200,request(base,"PUT","/api/configurations/"+id,"{\"name\":\"edited\",\"expectedRevision\":1}",token).statusCode());
      assertEquals(409,request(base,"DELETE","/api/configurations/"+id,"{\"expectedRevision\":1}",token).statusCode());
      assertEquals(200,request(base,"DELETE","/api/configurations/"+id,"{\"expectedRevision\":2}",token).statusCode());
      assertEquals(404,request(base,"GET","/api/configurations/"+id,null,token).statusCode());
    }
    finally{
      s.stop(0);
    }
  }
  @Test void alreadyRunningServiceIsNeverRecreatedByAQueuedStep()throws Exception{
    var c=catalog();Files.writeString(temp.resolve("compose.yaml"),"services: {}\n");List<List<String>> commands=new ArrayList<>();
    var runner=new Runner(c,temp,(dir,args)->{commands.add(args);if(!args.contains("ps"))throw new AssertionError("Must not issue up for a running service");return "\"timemanager\"\n\"timemanager-db\"\n";},"docker");
    assertTrue(runner.step(new Planner.Step("up","timemanager","timemanager"),c.hash()).contains("Already running"));assertEquals(2,commands.size());
  }
  @Test void inFlightDeploymentGuardSurvivesApiRestart()throws Exception{
    var c=catalog();var phase=new java.util.concurrent.atomic.AtomicReference<>("running");var submissions=new java.util.concurrent.atomic.AtomicInteger();
    ApiServer.Backend backend=new ApiServer.Backend(){public List<String>status(){return all(c);}public String submit(Planner.Plan p){return "job-"+submissions.incrementAndGet();}public Object deployment(String id){return Map.of("id",id,"state",phase.get());}};
    String token="test-token-abcdefghijklmnopqrstuvwxyz";HttpServer server=new ApiServer(c,backend,token,temp).start(new InetSocketAddress("127.0.0.1",0));String base="http://127.0.0.1:"+server.getAddress().getPort();
    String hash=Json.LENIENT.readTree(request(base,"POST","/api/plan","{\"smoke\":true}",token).body()).path("hash").asText();String body="{\"smoke\":true,\"expectedPlanHash\":\""+hash+"\"}";
    assertEquals(202,request(base,"POST","/api/deployments",body,token).statusCode());assertEquals(409,request(base,"POST","/api/deployments",body,token).statusCode());server.stop(0);
    server=new ApiServer(c,backend,token,temp).start(new InetSocketAddress("127.0.0.1",0));base="http://127.0.0.1:"+server.getAddress().getPort();try{assertEquals(409,request(base,"POST","/api/deployments",body,token).statusCode());phase.set("success");assertEquals(202,request(base,"POST","/api/deployments",body,token).statusCode());assertEquals(2,submissions.get());}finally{server.stop(0);}
  }
  @Test void databasesAreOwnedAssetsWithOrderedLifecycle()throws Exception{
    var c=catalog();int count=0;
    for(var host:c.systems())for(String db:host.internalAssets()){
      count++;assertEquals(host.id(),c.hostId(db));assertThrows(IllegalArgumentException.class,()->c.entry(db));assertTrue(host.services().indexOf(db)<host.services().indexOf(host.id()));
      assertThrows(IllegalArgumentException.class,()->Planner.build(c,new Planner.Request(List.of(),List.of(db),false,false),all(c)));
      assertFalse(c.closure(List.of(host.id())).contains(db));
    }
    assertEquals(16,count);
    var plan=Planner.build(c,new Planner.Request(List.of(),List.of("timemanager"),false,false),List.of("timemanager","timemanager-db"));
    assertEquals(List.of(new Planner.Step("stop","timemanager","timemanager"),new Planner.Step("stop","timemanager-db","timemanager")),plan.steps().stream().filter(s->s.kind().equals("stop")).toList());
    assertTrue(Planner.build(c,new Planner.Request(List.of("timemanager-db"),List.of(),false,false),List.of()).systems().contains("timemanager"));
  }
  static HttpResponse<String>request(String base,String method,String path,String body,String token)throws Exception{
    var b=HttpRequest.newBuilder(URI.create(base+path)).timeout(Duration.ofSeconds(10)).header("Content-Type","application/json");
    if(token!=null)b.header("Authorization","Bearer "+token);
    return HttpClient.newHttpClient().send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
  }
}
