package eu.arrowhead.localcloudconfigurator;

import java.io.IOException;
import eu.arrowhead.localcloudconfigurator.service.ConfiguratorBackend;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Durable local execution. No ColonyOS connection, identity or registration is used. */
public final class DirectBackend implements ConfiguratorBackend, AutoCloseable {
 public record StepState(String node,String processId,String kind,String target,String state,List<String> errors,String output) {}
 public record Deployment(String id,String state,List<StepState> steps) {}
 private final Runner runner;
 private final Catalog catalog;
 private final Path directory;
 private final ExecutorService executor=Executors.newSingleThreadExecutor();
 private final java.nio.channels.FileChannel lockChannel;
 private final java.nio.channels.FileLock lock;
 private String active;
 public DirectBackend(Catalog catalog,Runner runner,Path directory)throws IOException {
  this.catalog=catalog;this.runner=runner;this.directory=directory;Files.createDirectories(directory);
  lockChannel=java.nio.channels.FileChannel.open(directory.resolve("worker.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
  java.nio.channels.FileLock acquired;
  try { acquired=lockChannel.tryLock(); } catch(java.nio.channels.OverlappingFileLockException e){lockChannel.close();throw new IOException("Another direct worker owns this state",e);}
  if(acquired==null){lockChannel.close();throw new IOException("Another direct worker owns this state");}lock=acquired;
  try(var files=Files.list(directory)){
   for(Path f:files.filter(p->p.getFileName().toString().endsWith(".json")).toList()){
    Deployment d=Json.MAPPER.readValue(Files.readString(f),Deployment.class);
    if(!Set.of("success","failed").contains(d.state())){
     List<StepState> steps=new ArrayList<>();
     for(StepState s:d.steps())steps.add(Set.of("success","failed").contains(s.state())?s:new StepState(s.node(),s.processId(),s.kind(),s.target(),"failed",List.of("Worker restarted; inspect runtime state and review a fresh plan. Actions were not replayed."),s.output()));
     save(new Deployment(d.id(),"failed",List.copyOf(steps)));
    }
   }
  }catch(IOException|RuntimeException e){lock.release();lockChannel.close();executor.shutdownNow();throw e;}
 }
 public List<String> status()throws Exception{return runner.running();}
 public synchronized String submit(Planner.Plan plan)throws Exception{
  if(!catalog.hash().equals(plan.catalogHash()))throw new IllegalArgumentException("Catalog changed; review a fresh plan");
  if(active!=null)throw new Refused(409,"A deployment is already running");
  String id="local-"+UUID.randomUUID();List<StepState> steps=new ArrayList<>();int i=0;
  for(var s:plan.steps())steps.add(new StepState("s"+(++i),"",s.kind(),s.target(),"waiting",List.of(),""));
  save(new Deployment(id,"running",List.copyOf(steps)));active=id;
  executor.submit(()->execute(id,plan,steps));return id;
 }
 private void execute(String id,Planner.Plan plan,List<StepState> steps){
  try{
   for(int i=0;i<steps.size();i++){
    var s=steps.get(i);steps.set(i,new StepState(s.node(),"",s.kind(),s.target(),"running",List.of(),""));save(new Deployment(id,"running",List.copyOf(steps)));
    try{
     String output=Runner.redact(runner.step(plan.steps().get(i),plan.catalogHash()));
     steps.set(i,new StepState(s.node(),"",s.kind(),s.target(),"success",List.of(),output));
    }catch(Exception e){
     steps.set(i,new StepState(s.node(),"",s.kind(),s.target(),"failed",List.of(Runner.redact(String.valueOf(e.getMessage()))),""));
     for(int j=i+1;j<steps.size();j++){var pending=steps.get(j);steps.set(j,new StepState(pending.node(),"",pending.kind(),pending.target(),"failed",List.of("Not executed because an earlier step failed"),""));}
     save(new Deployment(id,"failed",List.copyOf(steps)));return;
    }
   }
   save(new Deployment(id,"success",List.copyOf(steps)));
  }catch(Exception e){System.err.println("Direct deployment persistence failure: "+Runner.redact(String.valueOf(e.getMessage())));}
  finally{synchronized(this){active=null;}}
 }
 private synchronized void save(Deployment d)throws IOException{
  Path tmp=Files.createTempFile(directory,".deployment-",".tmp");
  try{Files.writeString(tmp,Json.write(d));Files.move(tmp,directory.resolve(d.id()+".json"),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(tmp);}
 }
 public synchronized Deployment deployment(String id)throws IOException{
  if(!id.matches("local-[a-f0-9-]{36}"))throw new Refused(404,"Workflow belongs to the previous backend; its historical records were retained");
  Path p=directory.resolve(id+".json");if(!Files.isRegularFile(p))throw new Refused(404,"Deployment not found");
  return Json.MAPPER.readValue(Files.readString(p),Deployment.class);
 }
 public void close()throws IOException{
  executor.shutdown();try{if(!executor.awaitTermination(550,TimeUnit.SECONDS))throw new IOException("Deployment still running; state lock retained");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}
  lock.release();lockChannel.close();
 }
}
