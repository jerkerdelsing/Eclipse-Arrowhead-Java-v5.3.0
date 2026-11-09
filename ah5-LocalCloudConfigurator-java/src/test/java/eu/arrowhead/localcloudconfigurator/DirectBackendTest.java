package eu.arrowhead.localcloudconfigurator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
class DirectBackendTest {
 @TempDir Path temp;
 Catalog catalog()throws Exception{return Catalog.load(Path.of("catalogs/javacloud.json"));}
 Runner runner(Catalog c,AtomicInteger mutations)throws Exception{
  Files.writeString(temp.resolve("compose.yaml"),"services: {}\n");
  return new Runner(c,temp,(dir,args)->{
   if(!args.contains("ps")){mutations.incrementAndGet();throw new AssertionError("Verification must not mutate the cloud");}
   return String.join("\n",c.systems().stream().flatMap(s->s.services().stream()).map(s->"\""+s+"\"").toList());
  },"docker");
 }
 DirectBackend.Deployment done(DirectBackend b,String id)throws Exception{
  for(int i=0;i<100;i++){var d=b.deployment(id);if(Set.of("success","failed").contains(d.state()))return d;Thread.sleep(10);}
  throw new AssertionError("Deployment did not finish");
 }
 @Test void directVerificationPersistsAndNeverCallsColonyOrMutates()throws Exception{
  var c=catalog();var mutations=new AtomicInteger();var runner=runner(c,mutations);String id;
  try(var b=new DirectBackend(c,runner,temp.resolve("state"))){
   var plan=Planner.build(c,new Planner.Request(c.defaults(),List.of(),false,true),b.status());
   id=b.submit(plan);assertEquals("success",done(b,id).state());assertEquals(0,mutations.get());
  }
  try(var b=new DirectBackend(c,runner,temp.resolve("state"))){assertEquals("success",b.deployment(id).state());}
 }
 @Test void interruptedWorkIsFailedAndNeverReplayed()throws Exception{
  var c=catalog();var calls=new AtomicInteger();var runner=runner(c,calls);Path state=temp.resolve("state");Files.createDirectories(state);
  String id="local-"+UUID.randomUUID();Files.writeString(state.resolve(id+".json"),Json.write(new DirectBackend.Deployment(id,"running",List.of(new DirectBackend.StepState("s1","","stop","timemanager","running",List.of(),"")))));
  try(var b=new DirectBackend(c,runner,state)){assertEquals("failed",b.deployment(id).state());assertEquals(0,calls.get());assertTrue(b.deployment(id).steps().getFirst().errors().getFirst().contains("not replayed"));}
 }
 @Test void duplicateWorkerAndChangedCatalogAreRejected()throws Exception{
  var c=catalog();var r=runner(c,new AtomicInteger());
  try(var b=new DirectBackend(c,r,temp.resolve("state"))){
   assertThrows(java.io.IOException.class,()->new DirectBackend(c,r,temp.resolve("state")));
   assertThrows(IllegalArgumentException.class,()->b.submit(new Planner.Plan(c.cloud(),"changed",List.of(),List.of(),List.of())));
   assertThrows(Refused.class,()->b.deployment("../../secret"));
  }
 }
}
