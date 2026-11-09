package eu.arrowhead.localcloudconfigurator;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import com.fasterxml.jackson.databind.JsonNode;
public final class Runner{
  public interface Commander{
    String run(Path directory,List<String>args)throws Exception;
  }
  public static final class Exec implements Commander{
    public String run(Path dir,List<String>args)throws Exception{
      Process p=new ProcessBuilder(args).directory(dir.toFile()).redirectErrorStream(true).start();
      ByteArrayOutputStream out=new ByteArrayOutputStream();
      Thread reader=Thread.startVirtualThread(()->{
        try(var in=p.getInputStream()){
          byte[]b=new byte[4096];int n;while((n=in.read(b))>=0){
            int take=Math.min(n,Math.max(0,65536-out.size()));if(take>0)out.write(b,0,take);
          }
        }
        catch(IOException ignored){
        }
      }
      );
      boolean done=p.waitFor(args.contains("ps")?30:540,TimeUnit.SECONDS);
      if(!done){
        p.descendants().forEach(ProcessHandle::destroyForcibly);
        p.destroyForcibly();
      }
      reader.join(2000);
      String text=redact(out.toString(java.nio.charset.StandardCharsets.UTF_8));
      if(!done||p.exitValue()!=0)throw new IOException("Command failed: "+text);
      return text;
    }
  }
  private final Catalog catalog;
  private final Path cloud;
  private final Commander commands;
  private final String docker;
  public Runner(Catalog c,Path cloud,Commander cmd,String docker)throws IOException{
    this.catalog=c;
    this.cloud=cloud.toRealPath();
    this.commands=cmd;
    this.docker=docker;
    if(!Files.isRegularFile(this.cloud.resolve("compose.yaml")))throw new IOException("Cloud compose.yaml missing");
  }
  private List<String>compose(String...args){
    List<String>cmd=new ArrayList<>(List.of(docker,"compose","-p",catalog.composeProject(),"-f","compose.yaml"));
    cmd.addAll(List.of(args));
    return cmd;
  }
  public List<String> running()throws Exception{
    String raw=commands.run(cloud,compose("ps","--status","running","--format","{{json .Service}}")).trim();
    List<String>out=new ArrayList<>();
    if(raw.isEmpty())return out;
    if(raw.startsWith("[")){
      for(JsonNode n:Json.LENIENT.readTree(raw))if(n.path("State").asText("running").equals("running"))out.add(n.path("Service").asText());
    }
    else for(String line:raw.split("\\R")){
      JsonNode n=Json.LENIENT.readTree(line);
      if(n.isTextual())out.add(n.asText());
      else if(n.path("State").asText("running").equals("running"))out.add(n.path("Service").asText());
    }
    return out;
  }
  public String step(Planner.Step s,String hash)throws Exception{
    if(!catalog.hash().equals(hash))throw new IllegalArgumentException("Executor catalog changed; review a new plan");
    if(s.kind().equals("smoke")){
      if(!s.target().equals("verify-localcloud")||s.system()!=null)throw new IllegalArgumentException("Unknown verification");
      var on=running();
      for(String id:catalog.closure(catalog.defaults()))if(!on.containsAll(catalog.entry(id).services()))throw new IllegalStateException("Baseline component not running: "+id);
      return "Verified default core minus SimpleOrchestration and necessary infrastructure; no services changed.";
    }
    var owner=catalog.entry(s.system());
    switch(s.kind()){
      case "up":case "stop":if(!owner.services().contains(s.target()))throw new IllegalArgumentException("Service is not owned by the claimed component");
      if(s.kind().equals("stop")){
        if(catalog.protectedIds().contains(owner.id()))throw new IllegalArgumentException("Protected component");
        Planner.build(catalog,new Planner.Request(List.of(),List.of(owner.id()),false,false),running());
        if(owner.internalAssets().contains(s.target())&&running().stream().anyMatch(x->owner.services().contains(x)&&!owner.internalAssets().contains(x)))throw new IllegalArgumentException("Stop the host before its internal database");
        return redact(commands.run(cloud,compose("stop",s.target())));
      }
      if(!owner.internalAssets().contains(s.target())&&!running().containsAll(owner.internalAssets()))throw new IllegalArgumentException("Start internal databases before their host");
      if(running().contains(s.target()))return "Already running; unchanged: "+s.target();
      return redact(commands.run(cloud,compose("up","-d","--wait","--no-deps","--no-build","--pull","never",s.target())));
      case "provision":case "policy":List<String>allow=s.kind().equals("provision")?owner.provision():owner.policies();
      if(!allow.contains(s.target()))throw new IllegalArgumentException("Script is not permitted for this component and operation");
      Path scripts=cloud.resolve("scripts").toRealPath(),file=scripts.resolve(s.target()).toRealPath();
      if(!file.getParent().equals(scripts)||!Files.isRegularFile(file))throw new IllegalArgumentException("Script escapes catalog directory");
      return redact(commands.run(cloud,List.of(file.toString())));
      default:throw new IllegalArgumentException("Unknown deployment step");
    }
  }
  public static String redact(String text){
    return text.replaceAll("(?i)(\\\"?(?:password|passwd|token|secret|prvkey|private[_-]?key)\\\"?\\s*[:=]\\s*)(\\\"[^\\\"]*\\\"|[^\\s,}]+)","$1***").replaceAll("(?i)(Bearer\\s+|IDENTITY-TOKEN//)[^\\s\\\"]+","$1***").replaceAll("-----BEGIN [^-]*PRIVATE KEY-----[\\s\\S]*?-----END [^-]*PRIVATE KEY-----","[PRIVATE KEY REDACTED]");
  }
}
