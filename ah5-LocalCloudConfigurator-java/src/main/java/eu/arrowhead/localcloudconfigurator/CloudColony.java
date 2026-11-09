package eu.arrowhead.localcloudconfigurator;
import eu.arrowhead.localcloudconfigurator.colony.ColonyClient;
import java.util.*;
import java.time.*;
import java.io.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
public final class CloudColony{
  private final ColonyClient client;
  private final Catalog catalog;
  public CloudColony(ColonyClient client,Catalog catalog){
    this.client=client;
    this.catalog=catalog;
  }
  public void keepApiAlive(){
    Thread.startVirtualThread(()->{
      while(!Thread.currentThread().isInterrupted()){
        try{
          var process=client.assign(1);if(process!=null)client.fail(process.id(),List.of("API identity does not execute deployment functions"));
        }
        catch(Exception e){
          System.err.println("API heartbeat: "+Runner.redact(String.valueOf(e.getMessage())));
        }
        try{
          Thread.sleep(60000);
        }
        catch(InterruptedException e){
          Thread.currentThread().interrupt();
        }
      }
    }
    );
  }
  public List<String>status()throws Exception{
    var p=client.submit(client.functionSpec("lcc.status",catalog.executorType(),Map.of(),20,60,"localcloudconfigurator"));
    Instant end=Instant.now().plusSeconds(25);
    while(Instant.now().isBefore(end)){
      Thread.sleep(200);
      p=client.getProcess(p.id());
      if(p.state()==ColonyClient.FAILED)throw new IOException("Executor status failed: "+Runner.redact(String.join("; ",p.errors())));
      if(p.state()==ColonyClient.SUCCESS){
        if(p.output().isEmpty())throw new IOException("Empty executor status");
        return Json.MAPPER.readValue(String.valueOf(p.output().get(0)),Json.MAPPER.getTypeFactory().constructCollectionType(List.class,String.class));
      }
    }
    throw new IOException("Java cloud executor did not answer");
  }
  public String submit(Planner.Plan plan)throws Exception{
    List<ObjectNode>specs=new ArrayList<>();
    List<String>nodes=new ArrayList<>();
    int i=0;
    for(var s:plan.steps()){
      Map<String,Object>kw=new LinkedHashMap<>();
      kw.put("kind",s.kind());
      kw.put("target",s.target());
      kw.put("step",Json.write(s));
      kw.put("catalogHash",plan.catalogHash());
      specs.add(client.functionSpec("lcc.step",catalog.executorType(),kw,-1,600,"localcloudconfigurator"));
      nodes.add(String.format("s%03d-%s-%s",++i,s.kind(),s.target()));
    }
    return client.submitChain(specs,nodes);
  }
  public ColonyClient.Deployment deployment(String id)throws IOException{
    return client.deployment(id);
  }
  public void serve(Runner runner){
    while(!Thread.currentThread().isInterrupted()){
      try{
        var p=client.assign(10);
        if(p==null){
          Thread.sleep(200);
          continue;
        }
        try{
          String out=switch(p.funcName()){
            case "lcc.status"->Json.write(runner.running());
            case "lcc.step"->runner.step(Json.MAPPER.readValue(String.valueOf(p.kwargs().get("step")),Planner.Step.class),String.valueOf(p.kwargs().get("catalogHash")));
            default->throw new IllegalArgumentException("Unknown executor function");
          }
          ;
          if(!p.funcName().equals("lcc.status"))client.addLog(p.id(),Runner.redact(out));
          client.close(p.id(),List.of(out));
        }
        catch(Exception e){
          client.fail(p.id(),List.of(Runner.redact(String.valueOf(e.getMessage()))));
        }
      }
      catch(Exception e){
        System.err.println(Runner.redact(e.getMessage()));
        try{
          Thread.sleep(1000);
        }
        catch(InterruptedException x){
          Thread.currentThread().interrupt();
        }
      }
    }
  }
}
