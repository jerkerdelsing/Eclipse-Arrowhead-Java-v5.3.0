package eu.arrowhead.port;

import com.rabbitmq.client.*;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import javax.net.ssl.SSLContext;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import static eu.arrowhead.port.Json.*;

final class Messaging {
 static ConnectionFactory amqpFactory(Service service){var f=new ConnectionFactory();f.setHost(env("AMQP_HOST","amqp-broker"));f.setPort(envInt("AMQP_PORT",5671));f.useSslProtocol(service.tls);f.enableHostnameVerification();f.setSaslConfig(DefaultSaslConfig.EXTERNAL);f.setRequestedHeartbeat(30);f.setConnectionTimeout(10000);f.setAutomaticRecoveryEnabled(true);f.setTopologyRecoveryEnabled(false);return f;}
 static final class Publisher implements AutoCloseable{
  final Service service;Connection connection;Channel channel;Publisher(Service s){service=s;}
  synchronized void publish(String routing,Object payload)throws Exception{try{if(connection==null||!connection.isOpen()||channel==null||!channel.isOpen()){close();connection=amqpFactory(service).newConnection(service.name+"-java-native");channel=connection.createChannel();channel.confirmSelect();}channel.basicPublish("arrowhead",routing,new AMQP.BasicProperties.Builder().contentType("application/json").deliveryMode(2).messageId(str(obj(payload).get("observationId"))).build(),bytes(payload));channel.waitForConfirmsOrDie(10000);}catch(Exception e){close();throw e;}}
  public synchronized void close(){try{if(connection!=null)connection.close();}catch(Exception ignored){}connection=null;channel=null;}
 }
 static void nativeMonitor(Service service,String instance,Publisher publisher,Map<String,Object>status)throws Exception{
  for(String type:List.of("echo","power","status")){
   Object value=switch(type){case "echo"->m("status","alive");case "power"->m("supplyType","line","state","available");default->status;};
   var payload=m("schemaVersion","1.0","observationId",id(),"providerSystem",service.name,"serviceInstance",instance,"observationType",type,"sourceTime",now(),"value",value,"unit",type.equals("echo")?null:"1","quality","good","metadata",m("implementation","java","publisherMode","native","arrowheadVersion","5.2.2"));
   publisher.publish(service.cloud+".sysmonitor."+service.name+"."+instance+"."+type,payload);
  }
 }
 interface Incoming{Map<String,Object>handle(String peer,Map<String,Object>message)throws Exception;}
 static final class Relay implements AutoCloseable{
  final Service service;final String role,ownCN;final Predicate<String>trusted;final Incoming handler;final MqttClient client;
  final Map<String,Waiter>pending=new ConcurrentHashMap<>();final Map<String,Reply>seen=new ConcurrentHashMap<>();
  final ExecutorService dispatch=new ThreadPoolExecutor(4,16,60,TimeUnit.SECONDS,new ArrayBlockingQueue<>(128));
  record Waiter(String peer,CompletableFuture<Map<String,Object>>future){}record Reply(long expires,CompletableFuture<Map<String,Object>>future){}
  Relay(Service s,String role,Predicate<String>trusted,Incoming handler)throws Exception{
   service=s;this.role=role;ownCN=s.commonName(s.name);this.trusted=trusted;this.handler=handler;
   client=new MqttClient("ssl://"+env("RELAY_HOST","arrowhead-relay")+":"+envInt("RELAY_PORT",8883),ownCN+"-java-"+id(),new MemoryPersistence());client.setTimeToWait(10000);
   client.setCallback(new MqttCallbackExtended(){public void connectComplete(boolean reconnect,String uri){try{client.subscribe(inbox(),1);}catch(Exception e){service.lastError="Relay subscribe failed";}}public void connectionLost(Throwable cause){}public void deliveryComplete(IMqttDeliveryToken token){}public void messageArrived(String topic,MqttMessage message){receive(topic,message.getPayload(),message.isRetained());}});
  }
  String inbox(){return "arrowhead/relay/"+service.cloud+"/"+role+"/in/#";}
  void connect()throws Exception{if(client.isConnected())return;SSLContext tls=Tls.context(env("RELAY_CHAIN_CERT","/certs/"+role+"-relay-chain.crt"),env("RELAY_KEY","/certs/"+role+"-relay.key"),env("RELAY_CA_CERT","/certs/org-ca.crt"),null);var options=new MqttConnectOptions();options.setSocketFactory(tls.getSocketFactory());options.setHttpsHostnameVerificationEnabled(true);options.setAutomaticReconnect(true);options.setCleanSession(true);options.setConnectionTimeout(10);options.setKeepAliveInterval(30);options.setMaxInflight(64);client.connect(options);}
  Map<String,Object>status(){return m("connected",client.isConnected(),"broker",env("RELAY_HOST","arrowhead-relay")+":"+envInt("RELAY_PORT",8883),"inbox",inbox(),"profile",env("FEDERATION_PROFILE","legacy-mtls"),"endToEndEncrypted",env("FEDERATION_PROFILE","legacy-mtls").equals("ah-gt-1"));}
  void send(String cloud,Map<String,Object>message)throws Exception{if(!cloud.matches("[A-Za-z0-9_-]+"))throw new IllegalArgumentException("Invalid relay target cloud");if(!client.isConnected())throw new Service.ApiError(502,"Relay not connected");message.putIfAbsent("id",id());byte[]b=bytes(message);if(b.length>2*1024*1024)throw new IllegalArgumentException("Relay payload exceeds 2 MiB");client.publish("arrowhead/relay/"+cloud+"/"+role+"/in/"+ownCN+"/"+message.get("id"),b,1,false);}
  Map<String,Object>request(String cloud,Map<String,Object>message)throws Exception{
   String peer=(role.equals("gatekeeper")?"Gatekeeper":"GateTunnel")+"."+cloud+"."+PeerNames.organization(service,cloud)+".arrowhead.eu";if(!trusted.test(peer))throw new Service.ApiError(403,"Unconfigured relay neighbour");if(pending.size()>=128)throw new Service.ApiError(503,"Too many relay requests");String correlation=id();var future=new CompletableFuture<Map<String,Object>>();pending.put(correlation,new Waiter(peer,future));var copy=new LinkedHashMap<>(message);copy.put("correlationId",correlation);
   try{send(cloud,copy);return future.get(15,TimeUnit.SECONDS);}finally{pending.remove(correlation);}
  }
  void receive(String topic,byte[]data,boolean retained){try{
   if(retained||data.length>2*1024*1024)return;String[]parts=topic.split("/",-1);if(parts.length!=7||!parts[0].equals("arrowhead")||!parts[1].equals("relay")||!parts[2].equals(service.cloud)||!parts[3].equals(role)||!parts[4].equals("in")||!trusted.test(parts[5]))return;
   Map<String,Object>message=obj(parse(data));if(!parts[6].equals(str(message.get("id"))))return;String peer=parts[5];
   if(message.get("type").equals("reply")){Waiter waiter=pending.get(str(message.get("correlationId")));if(waiter!=null&&waiter.peer.equals(peer)){message.put("senderCN",peer);waiter.future.complete(message);}return;}
   seen.entrySet().removeIf(e->e.getValue().expires<System.currentTimeMillis());String key=peer+"/"+parts[6];if(seen.size()>=4096)return;var candidate=new Reply(System.currentTimeMillis()+120000,new CompletableFuture<>());Reply old=seen.putIfAbsent(key,candidate);Reply reply=old==null?candidate:old;
   dispatch.submit(()->{try{if(old==null){Map<String,Object>answer;try{answer=handler.handle(peer,message);}catch(Exception e){answer=m("accepted",false,"reason","Relay operation refused","error","Relay operation refused");service.lastError=e.getClass().getSimpleName();}answer=new LinkedHashMap<>(answer);answer.put("id",id());answer.put("type","reply");answer.put("correlationId",message.get("correlationId"));reply.future.complete(answer);}send(peer.split("\\.")[1],reply.future.get(15,TimeUnit.SECONDS));}catch(Exception e){service.lastError="Relay response failed";}});
  }catch(Exception ignored){}}
  public void close(){dispatch.shutdownNow();try{if(client.isConnected())client.disconnect(1000);client.close();}catch(Exception ignored){}pending.values().forEach(p->p.future.completeExceptionally(new IllegalStateException("Relay stopped")));}
 }
}
