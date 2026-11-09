package eu.arrowhead.localcloudconfigurator;
import eu.arrowhead.localcloudconfigurator.colony.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.net.*;
import java.util.*;
import java.security.*;
public final class Main{
  public static void main(String[]args)throws Exception{
    if(args.length<1)throw new IllegalArgumentException("Commands: keygen, register, serve, executor, plan");
    Map<String,String>o=new HashMap<>();
    for(int i=1;i<args.length;i++){
      if(!args[i].startsWith("-"))throw new IllegalArgumentException("Expected option");
      String k=args[i].replaceFirst("^-+","");
      if(i+1==args.length||args[i+1].startsWith("-"))o.put(k,"true");
      else o.put(k,args[++i]);
    }
    switch(args[0]){
      case "keygen"->{
        Path dir=Path.of(need(o,"dir"));
        Files.createDirectories(dir);
        Files.setPosixFilePermissions(dir,PosixFilePermissions.fromString("rwx------"));
        for(String name:List.of("colony-key","api-key","executor-key","api-token")){
          Path p=dir.resolve(name);
          if(!Files.exists(p)){
            String value=name.equals("api-token")?token():ColonyCrypto.generatePrivateKey();
            Files.writeString(p,value+"\n",StandardOpenOption.CREATE_NEW);
            Files.setPosixFilePermissions(p,PosixFilePermissions.fromString("rw-------"));
          }
        }
        System.out.println("LocalCloudConfigurator private credentials ready");
      }
      case "register"->{
        var c=Catalog.load(Path.of(need(o,"catalog")));
        Path dir=Path.of(need(o,"dir"));
        String colonyKey=Files.readString(dir.resolve("colony-key")).trim();
        var client=new ColonyClient(new ColonyClient.Config(o.getOrDefault("host","127.0.0.1"),Integer.parseInt(o.getOrDefault("port","50081")),false,c.colony(),Files.readString(dir.resolve("executor-key")).trim()));
        client.ensureColony(Files.readString(Path.of(need(o,"server-key-file"))).trim(),colonyKey);
        client.ensureExecutor(colonyKey,Files.readString(dir.resolve("api-key")).trim(),"java-lcc-api","java-lcc-api");
        var apiClient=new ColonyClient(new ColonyClient.Config(o.getOrDefault("host","127.0.0.1"),Integer.parseInt(o.getOrDefault("port","50081")),false,c.colony(),Files.readString(dir.resolve("api-key")).trim()));
        var probe=apiClient.assign(1);
        if(probe!=null)apiClient.fail(probe.id(),List.of("API identity does not execute deployment functions"));
        client.ensureExecutor(colonyKey,Files.readString(dir.resolve("executor-key")).trim(),c.executorType(),"java-lcc-executor");
        System.out.println("Java local-cloud colony and executor identities registered");
      }
      case "serve"->{
        var c=Catalog.load(Path.of(need(o,"catalog")));
        var colony=new CloudColony(new ColonyClient(ColonyClient.Config.fromEnv()),c);
        colony.keepApiAlive();
        var backend=new ApiServer.Backend(){
          public List<String>status()throws Exception{
            return colony.status();
          }
          public String submit(Planner.Plan p)throws Exception{
            return colony.submit(p);
          }
          public Object deployment(String id)throws Exception{
            return colony.deployment(id);
          }
        }
        ;
        String token=Files.readString(Path.of(System.getenv("LCC_API_TOKEN_FILE"))).trim();
        String listen=o.getOrDefault("listen","0.0.0.0:8097");
        int idx=listen.lastIndexOf(':');
        new ApiServer(c,backend,token,Path.of(o.getOrDefault("state","/state"))).start(new InetSocketAddress(listen.substring(0,idx),Integer.parseInt(listen.substring(idx+1))));
        System.out.println("Java LocalCloudConfigurator serving on "+listen);
        Thread.currentThread().join();
      }
      case "executor"->{
        var c=Catalog.load(Path.of(need(o,"catalog")));
        var r=new Runner(c,Path.of(need(o,"cloud-dir")),new Runner.Exec(),o.getOrDefault("docker","docker"));
        System.out.println("Java LocalCloudConfigurator executor ready for "+c.cloud());
        new CloudColony(new ColonyClient(ColonyClient.Config.fromEnv()),c).serve(r);
      }
      case "plan"->{
        var c=Catalog.load(Path.of(need(o,"catalog")));
        var req=Json.MAPPER.readValue(Files.readString(Path.of(need(o,"request"))),Planner.Request.class);
        System.out.println(Json.write(Planner.build(c,req,List.of())));
      }
      default->throw new IllegalArgumentException("Unknown command");
    }
  }
  static String need(Map<String,String>o,String k){
    if(!o.containsKey(k)||o.get(k).equals("true"))throw new IllegalArgumentException("Missing -"+k);
    return o.get(k);
  }
  static String token(){
    byte[]b=new byte[32];
    new SecureRandom().nextBytes(b);
    return HexFormat.of().formatHex(b);
  }
}
