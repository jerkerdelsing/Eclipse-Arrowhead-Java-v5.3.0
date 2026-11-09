package eu.arrowhead.port;
import static eu.arrowhead.port.Json.*;
public final class Main {
 public static void main(String[]args)throws Exception{
  if(args.length>0&&args[0].equals("storage-selftest")){var dir=java.nio.file.Files.createTempDirectory("arrowhead-java-storage-");var db=new Database(dir.resolve("probe.db").toString());try{db.schema("CREATE TABLE probe(value INTEGER)");db.update("INSERT INTO probe VALUES(1)");if(integer(db.query("SELECT value FROM probe").getFirst().get("value"),0)!=1)throw new IllegalStateException("Storage self-test failed");System.out.println("Java packaged SQLite self-test passed");}finally{java.nio.file.Files.deleteIfExists(db.path);java.nio.file.Files.deleteIfExists(dir);}return;}
  if(args.length>0&&args[0].equals("healthcheck")){var c=java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(3)).build();var r=c.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+env("HEALTH_PORT","8480")+"/health")).timeout(java.time.Duration.ofSeconds(4)).build(),java.net.http.HttpResponse.BodyHandlers.discarding());if(r.statusCode()!=200)System.exit(1);return;}
  System.setProperty("jdk.httpserver.maxConnections","128");System.setProperty("sun.net.httpserver.maxReqTime","30");System.setProperty("sun.net.httpserver.maxRspTime","30");
  Service s=new Gatekeeper();
  Runtime.getRuntime().addShutdownHook(new Thread(s::close));s.start();new java.util.concurrent.CountDownLatch(1).await();
 }
}
