package eu.arrowhead.localcloudconfigurator;
import java.util.*;
public final class Planner{
  public record Request(List<String>systems,List<String>remove,boolean reapply,boolean smoke){
    public Request{
      systems=systems==null?List.of():List.copyOf(systems);
      remove=remove==null?List.of():List.copyOf(remove);
    }
  }
  public record Step(String kind,String target,String system){
  }
  public record Plan(String cloud,String catalogHash,List<String>systems,List<Step>steps,List<String>notes){
    public String hash(){
      return Catalog.digest(Json.write(this));
    }
  }
  public static Plan build(Catalog c,Request req,Collection<String>running){
    if(req.systems().size()>200||req.remove().size()>200)throw new IllegalArgumentException("Too many components");
    Set<String>on=new HashSet<>(running),roots=new LinkedHashSet<>(c.defaults());
    for(String id:req.systems())roots.add(c.hostId(id));
    List<String>ordered=c.closure(roots);
    Set<String>keep=new HashSet<>(ordered),remove=new HashSet<>(req.remove());
    for(String id:remove){
      c.entry(id);
      if(keep.contains(id)||c.protectedIds().contains(id))throw new IllegalArgumentException("Cannot remove a selected, required or protected component: "+id);
      for(var other:c.systems())if(!remove.contains(other.id())&&other.services().stream().anyMatch(on::contains)&&c.closure(List.of(other.id())).contains(id))throw new IllegalArgumentException(id+" is still required by running "+other.id());
    }
    List<Step>steps=new ArrayList<>();
    List<String>notes=new ArrayList<>();
    for(String id:ordered){
      var s=c.entry(id);
      var missing=s.services().stream().filter(x->!on.contains(x)).toList();
      if(missing.isEmpty()&&!req.reapply()){
        notes.add(s.name()+": already running");
        continue;
      }
      for(String script:s.provision())steps.add(new Step("provision",script,id));
      for(String service:missing)steps.add(new Step("up",service,id));
      for(String script:s.policies())steps.add(new Step("policy",script,id));
    }
    List<String>removeOrder=new ArrayList<>(c.closure(remove));
    Collections.reverse(removeOrder);
    for(String id:removeOrder)if(remove.contains(id)){
      List<String>services=new ArrayList<>(c.entry(id).services());
      Collections.reverse(services);
      for(String service:services)if(on.contains(service))steps.add(new Step("stop",service,id));
    }
    if(req.smoke())steps.add(new Step("smoke","verify-localcloud",null));
    return new Plan(c.cloud(),c.hash(),ordered,List.copyOf(steps),List.copyOf(notes));
  }
}
