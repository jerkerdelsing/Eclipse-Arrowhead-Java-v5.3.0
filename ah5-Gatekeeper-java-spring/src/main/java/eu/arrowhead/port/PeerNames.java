package eu.arrowhead.port;

import java.util.*;
import static eu.arrowhead.port.Json.*;

/** Explicit cross-organisation bindings; an ordinary neighbour row is not enough. */
final class PeerNames {
 static Map<String,String>parse(String raw){
  var result=new LinkedHashMap<String,String>();
  if(raw.isBlank())return result;
  for(String item:raw.split(",",-1)){
   String[]pair=item.trim().split("=",-1);
   if(pair.length!=2||!pair[0].matches("[A-Za-z][A-Za-z0-9_]{0,62}")||!pair[1].matches("[A-Za-z][A-Za-z0-9_]{0,62}")||result.putIfAbsent(pair[0],pair[1])!=null||result.size()>32)throw new IllegalArgumentException("Invalid explicit federation peer organisation mapping");
  }
  return Collections.unmodifiableMap(result);
 }
 static String organization(Service service,String cloud){
  var peers=parse(env("FEDERATION_PEER_ORGANIZATIONS",""));
  if(peers.containsKey(service.cloud)&&!peers.get(service.cloud).equals(service.organization))throw new IllegalArgumentException("Own organisation cannot be remapped");
  return peers.getOrDefault(cloud,service.organization);
 }
 static String cloudIdentity(String peer){String[]p=peer.split("\\.");if(p.length!=5)throw new IllegalArgumentException("Invalid peer name");return p[1]+"|"+p[2];}
}
