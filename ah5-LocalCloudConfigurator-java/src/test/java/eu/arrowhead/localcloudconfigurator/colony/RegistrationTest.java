package eu.arrowhead.localcloudconfigurator.colony;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import eu.arrowhead.localcloudconfigurator.Json;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.io.*;
import java.time.*;
class RegistrationTest{
  static final String MEMBER="1".repeat(64),API="2".repeat(64),OWNER="3".repeat(64),SERVER="4".repeat(64);
  static class Fake extends ColonyClient{
    int added,approved;
    boolean wrongOwner,wrongApi;
    Fake(){
      super(new Config("localhost",1,false,"example",MEMBER));
    }
    @Override JsonNode send(String type,ObjectNode msg,String key,Duration timeout)throws IOException{
      switch(type){
        case "getcoloniesmsg":assertEquals(SERVER,key);
        return Json.MAPPER.createArrayNode().add(Json.MAPPER.createObjectNode().put("name","example").put("colonyid",ColonyCrypto.id(wrongOwner?API:OWNER)));
        case "getexecutormsg":assertEquals(MEMBER,key);
        return Json.MAPPER.createObjectNode().put("executorid",ColonyCrypto.id(wrongApi?OWNER:API)).put("executortype","api").put("state",3);
        case "approveexecutormsg":assertEquals(OWNER,key);
        approved++;
        return Json.MAPPER.nullNode();
        case "addcolonymsg":case "addexecutormsg":added++;
        return Json.MAPPER.nullNode();
        default:throw new IOException("Unexpected registration action: "+type);
      }
    }
  }
  @Test void existingColonyUsesServerOwnerListingAndIsNotRecreated()throws Exception{
    var f=new Fake();
    f.ensureColony(SERVER,OWNER);
    assertEquals(0,f.added);
  }
  @Test void mismatchedColonyOwnerFailsClosed(){
    var f=new Fake();
    f.wrongOwner=true;
    assertThrows(IOException.class,()->f.ensureColony(SERVER,OWNER));
    assertEquals(0,f.added);
  }
  @Test void retiredMatchingExecutorIsApprovedByItsOwnerWithoutReregistering()throws Exception{
    var f=new Fake();
    f.ensureExecutor(OWNER,API,"api","api");
    assertEquals(1,f.approved);
    assertEquals(0,f.added);
  }
  @Test void unexpectedExistingExecutorIdentityIsNeverApprovedOrRemoved(){
    var f=new Fake();
    f.wrongApi=true;
    assertThrows(IOException.class,()->f.ensureExecutor(OWNER,API,"api","api"));
    assertEquals(0,f.approved);
    assertEquals(0,f.added);
  }
}
