package eu.arrowhead.port;

import com.rabbitmq.client.*;
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
}
