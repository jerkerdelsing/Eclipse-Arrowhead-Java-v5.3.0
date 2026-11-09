package eu.arrowhead.localcloudconfigurator;
import java.nio.file.*;
import java.util.*;
import java.security.*;
import com.fasterxml.jackson.annotation.JsonProperty;
public record Catalog(String cloud,String composeProject,String colony,String executorType,List<String> defaults,List<String> smoke,List<Component> systems){
  public record Component(String id,String name,String kind,List<String> services,List<String> internalAssets,List<String> requires,List<String> provision,List<String> policies,@JsonProperty("protected") boolean protectedComponent){
    public Component{
      services=safe(services);
      internalAssets=safe(internalAssets);
      requires=safe(requires);
      provision=safe(provision);
      policies=safe(policies);
    }
  }
  static <T>List<T>safe(List<T>x){
    return x==null?List.of():List.copyOf(x);
  }
  public Catalog{
    defaults=safe(defaults);
    smoke=safe(smoke);
    systems=safe(systems);
  }
  public static Catalog load(Path p)throws Exception{
    Catalog c=Json.MAPPER.readValue(Files.readString(p),Catalog.class);
    c.validate();
    return c;
  }
  public Component entry(String id){
    return systems.stream().filter(s->s.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown component: "+id));
  }
  public String hostId(String id){
    return systems.stream().filter(s->s.internalAssets().contains(id)).map(Component::id).findFirst().orElse(id);
  }
  public List<String> closure(Collection<String> ids){
    LinkedHashSet<String>out=new LinkedHashSet<>();
    Set<String>path=new HashSet<>();
    for(String id:ids)visit(id,out,path);
    return List.copyOf(out);
  }
  private void visit(String id,Set<String>out,Set<String>path){
    if(out.contains(id))return;
    if(!path.add(id))throw new IllegalArgumentException("Dependency cycle: "+id);
    for(String d:entry(id).requires())visit(d,out,path);
    path.remove(id);
    out.add(id);
  }
  public Set<String> protectedIds(){
    Set<String>out=new HashSet<>(closure(defaults));
    for(Component c:systems)if(c.protectedComponent())out.add(c.id());
    return out;
  }
  public String hash(){
    return digest(Json.write(this));
  }
  public static String digest(String s){
    try{
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
    catch(Exception e){
      throw new IllegalStateException(e);
    }
  }
  public void validate(){
    if(cloud==null||cloud.isBlank()||composeProject==null||!composeProject.matches("[a-z0-9][a-z0-9_-]{0,62}")||colony==null||executorType==null)throw new IllegalArgumentException("Invalid catalog identity");
    Set<String>ids=new HashSet<>(),owners=new HashSet<>();
    for(Component s:systems){
      if(!s.id().matches("[a-z0-9][a-z0-9-]{0,62}")||!ids.add(s.id())||!Set.of("core","support","application","infrastructure").contains(s.kind())||s.services().isEmpty())throw new IllegalArgumentException("Invalid catalog component");
      if(!s.services().containsAll(s.internalAssets())||s.kind().equals("infrastructure")&&!s.internalAssets().isEmpty())throw new IllegalArgumentException("Invalid internal database ownership");
      for(String service:s.services())if(!service.matches("[a-z0-9][a-z0-9-]{0,62}")||!owners.add(service))throw new IllegalArgumentException("Invalid or shared Compose service");
      for(String script:java.util.stream.Stream.concat(s.provision().stream(),s.policies().stream()).toList())if(!script.matches("[a-z0-9][a-z0-9-]{0,80}\\.sh"))throw new IllegalArgumentException("Invalid catalog script");
    }
    closure(ids);
    for(String id:defaults)if(!entry(id).kind().equals("core"))throw new IllegalArgumentException("Default must be a core system");
    if(!smoke.equals(List.of("verify-localcloud")))throw new IllegalArgumentException("Unknown verification operation");
  }
}
