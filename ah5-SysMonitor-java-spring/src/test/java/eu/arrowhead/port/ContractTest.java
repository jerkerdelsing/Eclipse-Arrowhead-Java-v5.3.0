package eu.arrowhead.port;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static eu.arrowhead.port.Json.*;

class ContractTest {
 @TempDir Path dir;
 @Test void observationIdentityBoundToBrokerRoutingKey()throws Exception{
  var original=m("schemaVersion","1.0","providerSystem","Provider","serviceInstance","instance","observationType","echo","sourceTime","2026-10-15T12:00:00Z","value",m("status","alive"));var normalized=SysMonitor.normalize(original,"TestCloud.sysmonitor.Provider.instance.echo","TestCloud");assertEquals("Provider",normalized.get("providerSystem"));assertNotNull(normalized.get("observationId"));assertEquals(normalized.get("observationId"),SysMonitor.normalize(original,"TestCloud.sysmonitor.Provider.instance.echo","TestCloud").get("observationId"));
  assertThrows(IllegalArgumentException.class,()->SysMonitor.normalize(original,"TestCloud.sysmonitor.Other.instance.echo","TestCloud"));assertThrows(IllegalArgumentException.class,()->SysMonitor.normalize(original,"GoCloud.sysmonitor.Provider.instance.echo","TestCloud"));assertThrows(IllegalArgumentException.class,()->SysMonitor.normalize(m("value",m("status","fiction"),"sourceTime",now()),"TestCloud.sysmonitor.Provider.instance.echo","TestCloud"));
 }
 @Test void historyOutboxDeduplicatesAndSurvivesRestart()throws Exception{
  var db=new Database(dir.resolve("sysmonitor.db").toString());var store=new MonitorStore(db);var observation=SysMonitor.normalize(m("observationId","fixture-1","sourceTime","2026-10-15T12:00:00Z","observationType","status","value",m("state","running")),"TestCloud.sysmonitor.Provider.instance.status","TestCloud");
  assertTrue(store.enqueue(observation));assertFalse(store.enqueue(observation));assertEquals(1,store.pending().size());assertEquals(1,store.systems().size());assertEquals(1,store.history("Provider",List.of("status"),null,null,100).size());assertTrue(store.history("Other",List.of(),null,null,100).isEmpty());assertTrue(store.latest("Provider").containsKey("status"));store.published("fixture-1");var reopened=new MonitorStore(new Database(db.path.toString()));assertTrue(reopened.pending().isEmpty());assertEquals(1,reopened.history("Provider",List.of(),null,null,100).size());
 }
 @Test void subscriptionRevisionConflictsAndWildcardValidation()throws Exception{
  var store=new MonitorStore(new Database(dir.resolve("subscriptions.db").toString()));var first=store.put("Provider",List.of("echo","status"),true,"LocalCloudManager",null);assertEquals(1,integer(first.get("revision"),0));var second=store.put("Provider",List.of("*"),false,"LocalCloudManager",1);assertEquals(2,integer(second.get("revision"),0));assertFalse((Boolean)second.get("enabled"));assertEquals(409,assertThrows(Service.ApiError.class,()->store.put("Provider",List.of("echo"),true,"LocalCloudManager",1)).status);
  assertThrows(IllegalArgumentException.class,()->store.put("Bad.Provider",List.of("echo"),true,"LocalCloudManager",null));assertThrows(IllegalArgumentException.class,()->store.put("Provider",List.of("*","echo"),true,"LocalCloudManager",null));store.bindings(Set.of("TestCloud.sysmonitor.Provider.instance.echo"));assertEquals(Set.of("TestCloud.sysmonitor.Provider.instance.echo"),store.bindings());
 }
 @Test void sqlFilterCannotSelectAnotherSystem()throws Exception{
  var store=new MonitorStore(new Database(dir.resolve("history.db").toString()));store.enqueue(SysMonitor.normalize(m("sourceTime",now(),"value",m("status","alive")),"TestCloud.sysmonitor.Provider.instance.echo","TestCloud"));assertTrue(store.history("' OR 1=1 --",List.of(),null,null,10).isEmpty());
 }
}
