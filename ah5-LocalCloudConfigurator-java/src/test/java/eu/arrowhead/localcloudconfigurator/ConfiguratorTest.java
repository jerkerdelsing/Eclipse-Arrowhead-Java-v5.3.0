package eu.arrowhead.localcloudconfigurator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import eu.arrowhead.localcloudconfigurator.service.ConfiguratorBackend;
import eu.arrowhead.localcloudconfigurator.service.DeploymentCoordinator;
import eu.arrowhead.localcloudconfigurator.service.dto.ApplyRequest;
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
  @Test void alreadyRunningServiceIsNeverRecreatedByAQueuedStep()throws Exception{
    var c=catalog();Files.writeString(temp.resolve("compose.yaml"),"services: {}\n");List<List<String>> commands=new ArrayList<>();
    var runner=new Runner(c,temp,(dir,args)->{commands.add(args);if(!args.contains("ps"))throw new AssertionError("Must not issue up for a running service");return "\"timemanager\"\n\"timemanager-db\"\n";},"docker");
    assertTrue(runner.step(new Planner.Step("up","timemanager","timemanager"),c.hash()).contains("Already running"));assertEquals(2,commands.size());
  }
  @Test void inFlightDeploymentGuardSurvivesApiRestart()throws Exception{
    var c=catalog();var phase=new java.util.concurrent.atomic.AtomicReference<>("running");var submissions=new java.util.concurrent.atomic.AtomicInteger();
    ConfiguratorBackend backend=new ConfiguratorBackend(){public List<String>status(){return all(c);}public String submit(Planner.Plan p){return "job-"+submissions.incrementAndGet();}public Object deployment(String id){return Map.of("id",id,"state",phase.get());}public String kind(){return "colony";}};
    var store=new MemoryDeploymentStore();var coordinator=new DeploymentCoordinator(c,backend,store);
    String hash=String.valueOf(coordinator.plan(new Planner.Request(List.of(),List.of(),false,true)).get("hash"));var apply=new ApplyRequest(List.of(),List.of(),false,true,hash);
    assertEquals(202,coordinator.apply(apply).status());assertThrows(Refused.class,()->coordinator.apply(apply));
    var restarted=new DeploymentCoordinator(c,backend,store);
    assertEquals(409,assertThrows(Refused.class,()->restarted.apply(apply)).status());phase.set("success");assertEquals(202,restarted.apply(apply).status());assertEquals(2,submissions.get());
    assertEquals(409,assertThrows(Refused.class,()->restarted.apply(new ApplyRequest(List.of(),List.of(),false,true,"stale"))).status());
    assertEquals(404,assertThrows(Refused.class,()->restarted.deployment("foreign")).status());
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
}
