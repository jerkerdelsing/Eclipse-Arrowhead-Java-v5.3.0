package eu.arrowhead.port;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static eu.arrowhead.port.Json.*;

class ContractTest {
 @TempDir Path dir;
 @Test void legacyControlDatabasePersistsAndReopens()throws Exception{
  var db=new Database(dir.resolve("gatetunnel.db").toString());var store=new GateTunnel.ControlStore(db);
  var request=m("consumer",m("name","Consumer"),"provider",m("name","Provider"),"consumerCloud",m("name","CloudA","operator","Example"),"providerCloud",m("name","CloudB","operator","Example"),"relay",m("name","relay","address","relay","port",61617),"serviceDefinition","temperature");
  var created=store.create(request);assertEquals("PROVIDER_READY",created.get("state"));var active=store.connect(str(created.get("queueId")));assertEquals("ACTIVE",active.get("state"));assertEquals(created.get("tunnelPort"),active.get("tunnelPort"));assertTrue(store.close(str(created.get("sessionId")),0));assertEquals("CLOSED",new GateTunnel.ControlStore(new Database(db.path.toString())).all(200).getFirst().get("state"));assertThrows(Service.ApiError.class,()->store.connect(str(created.get("queueId"))));
 }
 @Test void controlRejectsMissingFields()throws Exception{var store=new GateTunnel.ControlStore(new Database(dir.resolve("sessions.db").toString()));assertThrows(IllegalArgumentException.class,()->store.create(m("serviceDefinition","temperature")));}
 @Test void parameterizedOperationsCannotEscapeScope(){
  var operations=m("put-data",m("method","PUT","path","/systems/{systemName}/services/{serviceName}/data"));String base="/datamanager/data-historian",valid=base+"/systems/Producer/services/reading/data";
  assertTrue(GateTunnel.operationAllowed("PUT",valid,base,operations));assertTrue(GateTunnel.operationAllowed("PUT",valid+"?count=1",base,operations));
  for(String wrong:List.of(valid+"/../mgmt",base+"/systems/%2e%2e/services/reading/data",base+"/systems/Producer%2fother/services/reading/data",base+"/systems/%252e/services/reading/data","https://attacker.invalid"+valid,"//attacker.invalid"+valid,base+"/systems/Producer/services/reading/mgmt",base+"/systems/Producer/services//data"))assertFalse(GateTunnel.operationAllowed("PUT",wrong,base,operations),wrong);
  assertFalse(GateTunnel.operationAllowed("DELETE",valid,base,operations));
 }
}
